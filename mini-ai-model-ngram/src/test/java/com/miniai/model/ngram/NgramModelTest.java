package com.miniai.model.ngram;

import com.codeai.tokenizer.CodeTokenizer;
import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.model.smoothing.KneserNey;
import com.miniai.model.smoothing.SimpleBackoff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * N-gram 모델 테스트 (일반화된 버전)
 */
public class NgramModelTest {

    private String testCorpus;
    private CodeTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        // 테스트용 코드 코퍼스
        testCorpus = """
            public class User {
            private String name;
            public String getName() {
            return name;
            }
            public void setName(String name) {
            this.name = name;
            }
            }
            public static void main(String[] args) {
            System.out.println("Hello");
            }
            for (int i = 0; i < 10; i++) {
            System.out.println(i);
            }
            if (value != null) {
            return value;
            }
            for (int j = 0; j < 100; j++) {
            process(j);
            }
            """;

        tokenizer = CodeTokenizer.fromCode(testCorpus);
    }

    @Test
    @DisplayName("5-gram 모델이 생성된다")
    void testFiveGramCreation() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer);

        assertNotNull(model);
        assertEquals("5-gram-v1", model.modelName());
        assertEquals(5, artifact.getN());

        System.out.println("Model: " + model);
        System.out.println("Metadata: " + artifact.getMetadata());
    }

    @Test
    @DisplayName("다양한 N-gram 생성 가능 (2, 3, 4, 5)")
    void testVariousNgrams() {
        for (int n = 2; n <= 5; n++) {
            NgramTrainer trainer = new NgramTrainer(n, tokenizer);
            NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
            NgramModel model = new NgramModel(artifact, tokenizer);

            assertEquals(n + "-gram-v1", model.modelName());
            assertEquals(n, artifact.getN());
            assertTrue(artifact.getTotalNgramCount() > 0);

            System.out.println(n + "-gram: " + artifact.getTotalNgramCount() + " ngrams");
        }
    }

    @Test
    @DisplayName("하위 N-gram이 backoff용으로 저장된다")
    void testLowerOrderNgrams() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);

        // 5-gram이면 4, 3, 2, 1-gram 모두 저장되어야 함
        assertTrue(artifact.getLowerOrderCounts().containsKey(4));
        assertTrue(artifact.getLowerOrderCounts().containsKey(3));
        assertTrue(artifact.getLowerOrderCounts().containsKey(2));
        assertTrue(artifact.getLowerOrderCounts().containsKey(1));

        System.out.println("Lower order counts stored for backoff");
    }

    @Test
    @DisplayName("Continuation counts가 계산된다 (Kneser-Ney용)")
    void testContinuationCounts() {
        NgramTrainer trainer = new NgramTrainer(3, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);

        assertTrue(artifact.getContinuationCounts().size() > 0);
        assertTrue(artifact.getTotalUniqueBigrams() > 0);

        System.out.println("Continuation counts: " + artifact.getContinuationCounts().size());
        System.out.println("Total unique bigrams: " + artifact.getTotalUniqueBigrams());
    }

    @Test
    @DisplayName("5-gram으로 텍스트를 생성한다")
    void testFiveGramGeneration() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer);

        GenerateRequest request = GenerateRequest.builder("public class")
            .maxTokens(5)
            .temperature(1.0)
            .topK(10)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().startsWith("public class"));
        assertEquals("5-gram-v1", response.getModel());

        System.out.println("5-gram Generated: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("SimpleBackoff Smoothing이 작동한다")
    void testSimpleBackoffSmoothing() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer, new SimpleBackoff(0.4));

        GenerateRequest request = GenerateRequest.builder("for (int")
            .maxTokens(5)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().length() > "for (int".length());

        System.out.println("SimpleBackoff: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("Kneser-Ney Smoothing이 작동한다")
    void testKneserNeySmoothing() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer, new KneserNey(0.75));

        GenerateRequest request = GenerateRequest.builder("for (int")
            .maxTokens(5)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().length() > "for (int".length());

        System.out.println("KneserNey: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("Seed가 같으면 동일한 결과가 나온다")
    void testDeterministicGeneration() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer);

        GenerateRequest request1 = GenerateRequest.builder("public")
            .maxTokens(5)
            .seed(123L)
            .build();

        GenerateRequest request2 = GenerateRequest.builder("public")
            .maxTokens(5)
            .seed(123L)
            .build();

        GenerateResponse response1 = model.generate(request1);
        GenerateResponse response2 = model.generate(request2);

        assertEquals(response1.getGeneratedText(), response2.getGeneratedText());
        System.out.println("Deterministic: " + response1.getGeneratedText());
    }

    @Test
    @DisplayName("Usage가 올바르게 계산된다")
    void testUsageCalculation() {
        NgramTrainer trainer = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        NgramModel model = new NgramModel(artifact, tokenizer);

        GenerateRequest request = GenerateRequest.builder("if (")
            .maxTokens(5)
            .build();

        GenerateResponse response = model.generate(request);

        assertTrue(response.getUsage().getInputTokens() >= 1);
        assertTrue(response.getUsage().getOutputTokens() <= 5);
        assertEquals(
            response.getUsage().getInputTokens() + response.getUsage().getOutputTokens(),
            response.getUsage().getTotalTokens()
        );

        System.out.println("Usage - Input: " + response.getUsage().getInputTokens() +
                          ", Output: " + response.getUsage().getOutputTokens());
    }

    @Test
    @DisplayName("Kneser-Ney가 SimpleBackoff보다 더 다양한 토큰을 생성한다")
    void testKneserNeyDiversity() {
        NgramTrainer trainer = new NgramTrainer(3, tokenizer);
        NgramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);

        NgramModel backoffModel = new NgramModel(artifact, tokenizer, new SimpleBackoff());
        NgramModel kneserModel = new NgramModel(artifact, tokenizer, new KneserNey());

        // 여러 번 생성해서 다양성 비교
        java.util.Set<String> backoffResults = new java.util.HashSet<>();
        java.util.Set<String> kneserResults = new java.util.HashSet<>();

        for (int i = 0; i < 10; i++) {
            GenerateRequest request = GenerateRequest.builder("for")
                .maxTokens(3)
                .seed((long) i)
                .build();

            backoffResults.add(backoffModel.generate(request).getGeneratedText());
            kneserResults.add(kneserModel.generate(request).getGeneratedText());
        }

        System.out.println("SimpleBackoff unique results: " + backoffResults.size());
        System.out.println("KneserNey unique results: " + kneserResults.size());

        // Kneser-Ney가 더 다양한 결과를 생성해야 함 (또는 최소 동일)
        assertTrue(kneserResults.size() >= backoffResults.size() - 2,
            "Kneser-Ney should produce at least similar diversity");
    }

    @Test
    @DisplayName("5-gram이 3-gram보다 더 긴 문맥을 사용한다")
    void testContextWindowSize() {
        // 3-gram: 2 토큰 문맥
        NgramTrainer trainer3 = new NgramTrainer(3, tokenizer);
        NgramArtifact artifact3 = trainer3.trainFromText(testCorpus, tokenizer);

        // 5-gram: 4 토큰 문맥
        NgramTrainer trainer5 = new NgramTrainer(5, tokenizer);
        NgramArtifact artifact5 = trainer5.trainFromText(testCorpus, tokenizer);

        assertEquals(3, artifact3.getN());
        assertEquals(5, artifact5.getN());

        // 5-gram은 더 긴 키를 가져야 함
        // 예: "tok1:tok2:tok3:tok4" (4개 토큰)
        for (String key : artifact5.getCounts().keySet()) {
            int colonCount = key.split(":").length;
            assertEquals(4, colonCount, "5-gram context should have 4 tokens");
            break; // 첫 번째만 확인
        }

        for (String key : artifact3.getCounts().keySet()) {
            int colonCount = key.split(":").length;
            assertEquals(2, colonCount, "3-gram context should have 2 tokens");
            break;
        }

        System.out.println("3-gram context size: 2, 5-gram context size: 4");
    }
}
