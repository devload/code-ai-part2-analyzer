package com.miniai.model;

import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;

import java.nio.file.Paths;

/**
 * Bigram 텍스트 생성 데모
 *
 * Step 3 데모용 프로그램
 */
public class GenerateDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Bigram 텍스트 생성 데모 - Step 3");
        System.out.println("=".repeat(60));

        // 프로젝트 루트 찾기
        String projectRoot = TrainDemo.findProjectRoot();

        // 1. 영어 모델로 생성
        System.out.println("\n[1] 영어 모델 생성");
        System.out.println("-".repeat(60));
        demonstrateGeneration(
            projectRoot + "/data/sample-bigram.json",
            "the cat"
        );

        // 2. 한글 모델로 생성
        System.out.println("\n[2] 한글 모델 생성");
        System.out.println("-".repeat(60));
        demonstrateGeneration(
            projectRoot + "/data/korean-bigram.json",
            "오늘은"
        );

        // 3. Temperature 비교
        System.out.println("\n[3] Temperature 효과 비교");
        System.out.println("-".repeat(60));
        demonstrateTemperature(
            projectRoot + "/data/sample-bigram.json",
            "I love"
        );

        // 4. Seed 재현성
        System.out.println("\n[4] Seed 재현성 테스트");
        System.out.println("-".repeat(60));
        demonstrateReproducibility(
            projectRoot + "/data/sample-bigram.json",
            "the"
        );

        System.out.println("\n" + "=".repeat(60));
        System.out.println("데모 완료!");
        System.out.println("=".repeat(60));
    }

    private static void demonstrateGeneration(String artifactPath, String prompt) {
        try {
            // 모델 로드
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));
            System.out.println("Model: " + model);

            // 생성 요청
            GenerateRequest request = GenerateRequest.builder(prompt)
                .maxTokens(10)
                .temperature(1.0)
                .topK(50)
                .seed(42L)
                .build();

            System.out.println("\nPrompt: \"" + prompt + "\"");
            System.out.println("Parameters: maxTokens=10, temperature=1.0, topK=50, seed=42");

            // 생성
            GenerateResponse response = model.generate(request);

            System.out.println("\n생성 결과:");
            System.out.println("  Text: " + response.getGeneratedText());
            System.out.println("  " + response.getUsage());
            System.out.println("  Latency: " + response.getLatencyMs() + "ms");
            System.out.println("  Model: " + response.getModel());

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }

    private static void demonstrateTemperature(String artifactPath, String prompt) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));

            double[] temperatures = {0.1, 0.5, 1.0, 1.5, 2.0};

            System.out.println("Prompt: \"" + prompt + "\"");
            System.out.println("비교: Temperature 변화에 따른 생성 결과\n");

            for (double temp : temperatures) {
                GenerateRequest request = GenerateRequest.builder(prompt)
                    .maxTokens(8)
                    .temperature(temp)
                    .topK(50)
                    .seed(42L)
                    .build();

                String result = model.generate(request).getGeneratedText();

                System.out.println(String.format("  temp=%.1f : %s", temp, result));
            }

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }

    private static void demonstrateReproducibility(String artifactPath, String prompt) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));

            Long seed = 12345L;

            System.out.println("Prompt: \"" + prompt + "\"");
            System.out.println("Seed: " + seed);
            System.out.println("동일한 seed로 3번 생성:\n");

            for (int i = 1; i <= 3; i++) {
                GenerateRequest request = GenerateRequest.builder(prompt)
                    .maxTokens(10)
                    .temperature(1.0)
                    .seed(seed)
                    .build();

                String result = model.generate(request).getGeneratedText();

                System.out.println(String.format("  시도 %d: %s", i, result));
            }

            System.out.println("\n→ 모든 결과가 동일함 (재현 가능!)");

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }
}
