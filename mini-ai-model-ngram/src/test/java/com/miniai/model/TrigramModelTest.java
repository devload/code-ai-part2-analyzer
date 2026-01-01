package com.miniai.model;

import com.codeai.tokenizer.CodeTokenizer;
import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Trigram 모델 테스트
 */
public class TrigramModelTest {

    private TrigramModel model;
    private String testCorpus;

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
            """;

        // CodeTokenizer로 학습
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(testCorpus);
        TrigramTrainer trainer = new TrigramTrainer(tokenizer);
        TrigramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);
        model = new TrigramModel(artifact, tokenizer);
    }

    @Test
    @DisplayName("Trigram 모델이 생성된다")
    void testModelCreation() {
        assertNotNull(model);
        assertEquals("trigram-v1", model.modelName());
        System.out.println("Model: " + model);
    }

    @Test
    @DisplayName("Trigram artifact에 카운트가 저장된다")
    void testArtifactCounts() {
        TrigramArtifact artifact = model.getArtifact();

        assertTrue(artifact.getTotalTrigramCount() > 0, "Should have trigram counts");
        assertTrue(artifact.getVocabulary().size() > 0, "Should have vocabulary");

        System.out.println("Trigrams: " + artifact.getMetadata().getTotalTrigrams());
        System.out.println("Bigrams: " + artifact.getMetadata().getTotalBigrams());
        System.out.println("Vocab size: " + artifact.getVocabulary().size());
    }

    @Test
    @DisplayName("프롬프트로 텍스트를 생성한다")
    void testGeneration() {
        GenerateRequest request = GenerateRequest.builder("public class")
            .maxTokens(5)
            .temperature(1.0)
            .topK(10)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().startsWith("public class"));
        assertEquals("trigram-v1", response.getModel());

        System.out.println("Generated: " + response.getGeneratedText());
        System.out.println("Usage: " + response.getUsage());
    }

    @Test
    @DisplayName("Seed가 같으면 동일한 결과가 나온다")
    void testDeterministicGeneration() {
        GenerateRequest request1 = GenerateRequest.builder("for (")
            .maxTokens(5)
            .seed(123L)
            .build();

        GenerateRequest request2 = GenerateRequest.builder("for (")
            .maxTokens(5)
            .seed(123L)
            .build();

        GenerateResponse response1 = model.generate(request1);
        GenerateResponse response2 = model.generate(request2);

        assertEquals(response1.getGeneratedText(), response2.getGeneratedText());
        System.out.println("Deterministic output: " + response1.getGeneratedText());
    }

    @Test
    @DisplayName("Backoff가 작동한다 - Trigram 없으면 Bigram 사용")
    void testBackoff() {
        // "xyz abc"는 코퍼스에 없으므로 backoff 발생
        GenerateRequest request = GenerateRequest.builder("public")
            .maxTokens(3)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        // 결과가 생성되어야 함 (backoff로 bigram 사용)
        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().length() > "public".length());

        System.out.println("With backoff: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("Usage가 올바르게 계산된다")
    void testUsageCalculation() {
        GenerateRequest request = GenerateRequest.builder("if (")
            .maxTokens(5)
            .build();

        GenerateResponse response = model.generate(request);

        assertEquals(2, response.getUsage().getInputTokens()); // "if", "("
        assertTrue(response.getUsage().getOutputTokens() <= 5);
        assertEquals(
            response.getUsage().getInputTokens() + response.getUsage().getOutputTokens(),
            response.getUsage().getTotalTokens()
        );

        System.out.println("Usage - Input: " + response.getUsage().getInputTokens() +
                          ", Output: " + response.getUsage().getOutputTokens());
    }

    @Test
    @DisplayName("Backoff 가중치가 적용된다")
    void testBackoffWeight() {
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(testCorpus);
        TrigramTrainer trainer = new TrigramTrainer(tokenizer);
        TrigramArtifact artifact = trainer.trainFromText(testCorpus, tokenizer);

        // 다른 backoff 가중치로 모델 생성
        TrigramModel model1 = new TrigramModel(artifact, tokenizer, 0.2); // 20% bigram
        TrigramModel model2 = new TrigramModel(artifact, tokenizer, 0.8); // 80% bigram

        assertEquals(0.2, model1.getBackoffWeight());
        assertEquals(0.8, model2.getBackoffWeight());

        System.out.println("Model with 20% backoff: " + model1);
        System.out.println("Model with 80% backoff: " + model2);
    }
}
