package com.miniai.model;

import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.tokenizer.WhitespaceTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BigramModelTest {

    private BigramModel model;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        // 간단한 corpus로 학습
        String corpus = "the cat sat on the mat " +
                        "the dog sat on the log " +
                        "the cat loves the dog";

        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);
        BigramArtifact artifact = trainer.trainFromText(corpus, tokenizer);

        model = new BigramModel(artifact, tokenizer);
    }

    @Test
    @DisplayName("기본 텍스트 생성")
    void testBasicGenerate() {
        GenerateRequest request = GenerateRequest.builder("the")
            .maxTokens(5)
            .temperature(1.0)
            .topK(50)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        assertNotNull(response);
        assertNotNull(response.getGeneratedText());
        assertTrue(response.getGeneratedText().startsWith("the"));
        assertTrue(response.getUsage().getTotalTokens() > 1);

        System.out.println("\n=== 기본 텍스트 생성 ===");
        System.out.println("Prompt: the");
        System.out.println("Generated: " + response.getGeneratedText());
        System.out.println("Usage: " + response.getUsage());
        System.out.println("Latency: " + response.getLatencyMs() + "ms");
    }

    @Test
    @DisplayName("Seed 고정 시 재현성")
    void testReproducibility() {
        Long seed = 42L;

        GenerateRequest request1 = GenerateRequest.builder("the cat")
            .maxTokens(10)
            .seed(seed)
            .build();

        GenerateRequest request2 = GenerateRequest.builder("the cat")
            .maxTokens(10)
            .seed(seed)
            .build();

        String result1 = model.generate(request1).getGeneratedText();
        String result2 = model.generate(request2).getGeneratedText();

        assertEquals(result1, result2, "동일한 seed는 동일한 결과 생성");

        System.out.println("\n=== Seed 고정 재현성 ===");
        System.out.println("Seed: " + seed);
        System.out.println("Result 1: " + result1);
        System.out.println("Result 2: " + result2);
        System.out.println("일치: " + result1.equals(result2));
    }

    @Test
    @DisplayName("다른 Seed는 다른 결과")
    void testDifferentSeeds() {
        GenerateRequest request1 = GenerateRequest.builder("the")
            .maxTokens(10)
            .seed(42L)
            .build();

        GenerateRequest request2 = GenerateRequest.builder("the")
            .maxTokens(10)
            .seed(123L)
            .build();

        String result1 = model.generate(request1).getGeneratedText();
        String result2 = model.generate(request2).getGeneratedText();

        // 다를 가능성이 높음 (항상 다르지는 않을 수 있음)
        System.out.println("\n=== 다른 Seed ===");
        System.out.println("Seed 42:  " + result1);
        System.out.println("Seed 123: " + result2);
    }

    @Test
    @DisplayName("Temperature 변화 테스트")
    void testTemperatureEffect() {
        String prompt = "the cat";

        // Low temperature (확정적)
        GenerateRequest lowTemp = GenerateRequest.builder(prompt)
            .maxTokens(5)
            .temperature(0.1)
            .seed(42L)
            .build();

        // High temperature (창의적)
        GenerateRequest highTemp = GenerateRequest.builder(prompt)
            .maxTokens(5)
            .temperature(2.0)
            .seed(42L)
            .build();

        String resultLow = model.generate(lowTemp).getGeneratedText();
        String resultHigh = model.generate(highTemp).getGeneratedText();

        System.out.println("\n=== Temperature 효과 ===");
        System.out.println("Prompt: " + prompt);
        System.out.println("Low temp (0.1):  " + resultLow);
        System.out.println("High temp (2.0): " + resultHigh);
    }

    @Test
    @DisplayName("TopK 필터링 테스트")
    void testTopKFiltering() {
        String prompt = "the";

        // TopK = 1 (가장 높은 확률만)
        GenerateRequest topK1 = GenerateRequest.builder(prompt)
            .maxTokens(5)
            .topK(1)
            .temperature(1.0)
            .seed(42L)
            .build();

        // TopK = 50 (모든 후보)
        GenerateRequest topK50 = GenerateRequest.builder(prompt)
            .maxTokens(5)
            .topK(50)
            .temperature(1.0)
            .seed(42L)
            .build();

        String result1 = model.generate(topK1).getGeneratedText();
        String result50 = model.generate(topK50).getGeneratedText();

        System.out.println("\n=== TopK 효과 ===");
        System.out.println("Prompt: " + prompt);
        System.out.println("TopK=1:  " + result1);
        System.out.println("TopK=50: " + result50);
    }

    @Test
    @DisplayName("MaxTokens 제한")
    void testMaxTokensLimit() {
        GenerateRequest request = GenerateRequest.builder("the")
            .maxTokens(3)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        // Output tokens <= maxTokens (prompt 제외)
        assertTrue(response.getUsage().getOutputTokens() <= 3);

        System.out.println("\n=== MaxTokens 제한 ===");
        System.out.println("MaxTokens: 3");
        System.out.println("Output tokens: " + response.getUsage().getOutputTokens());
        System.out.println("Generated: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("Stop sequence 테스트")
    void testStopSequences() {
        GenerateRequest request = GenerateRequest.builder("the cat")
            .maxTokens(20)
            .stopSequences(List.of("dog"))
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        System.out.println("\n=== Stop Sequence ===");
        System.out.println("Stop at: 'dog'");
        System.out.println("Generated: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("Usage 측정")
    void testUsageMeasurement() {
        GenerateRequest request = GenerateRequest.builder("the cat sat")
            .maxTokens(5)
            .seed(42L)
            .build();

        GenerateResponse response = model.generate(request);

        // Input = 3 (the, cat, sat)
        assertEquals(3, response.getUsage().getInputTokens());

        // Total = Input + Output
        assertEquals(
            response.getUsage().getInputTokens() + response.getUsage().getOutputTokens(),
            response.getUsage().getTotalTokens()
        );

        System.out.println("\n=== Usage 측정 ===");
        System.out.println("Prompt: the cat sat");
        System.out.println("Input tokens: " + response.getUsage().getInputTokens());
        System.out.println("Output tokens: " + response.getUsage().getOutputTokens());
        System.out.println("Total tokens: " + response.getUsage().getTotalTokens());
    }

    @Test
    @DisplayName("파일로부터 모델 로드")
    void testLoadFromFile(@TempDir Path tempDir) throws Exception {
        // Artifact 저장
        Path artifactPath = tempDir.resolve("test-bigram.json");
        String corpus = "hello world hello java";

        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);
        trainer.train(
            Files.writeString(tempDir.resolve("corpus.txt"), corpus),
            artifactPath
        );

        // 모델 로드
        BigramModel loadedModel = BigramModel.fromArtifact(artifactPath);

        // 생성 테스트
        GenerateRequest request = GenerateRequest.builder("hello")
            .maxTokens(3)
            .seed(42L)
            .build();

        GenerateResponse response = loadedModel.generate(request);

        assertNotNull(response);
        assertTrue(response.getGeneratedText().startsWith("hello"));

        System.out.println("\n=== 파일 로드 테스트 ===");
        System.out.println("Loaded from: " + artifactPath);
        System.out.println("Generated: " + response.getGeneratedText());
    }

    @Test
    @DisplayName("다음 토큰 확률 조회")
    void testGetNextTokenProbs() {
        // "the" 토큰 ID
        WhitespaceTokenizer tokenizer = (WhitespaceTokenizer) model.getTokenizer();
        int theId = tokenizer.getTokenId("the");

        // "the" 다음에 올 수 있는 토큰들의 확률
        var probs = model.getNextTokenProbs(theId);

        assertFalse(probs.isEmpty());

        // 확률 합 = 1.0
        double totalProb = probs.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.0, totalProb, 0.001);

        System.out.println("\n=== 다음 토큰 확률 ===");
        System.out.println("'the' 다음에 올 수 있는 토큰들:");
        probs.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                String word = tokenizer.getToken(e.getKey());
                System.out.println(String.format("  %s: %.2f%%", word, e.getValue() * 100));
            });
    }
}
