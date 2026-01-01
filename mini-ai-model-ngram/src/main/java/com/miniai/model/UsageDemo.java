package com.miniai.model;

import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.core.types.Usage;

import java.nio.file.Paths;

/**
 * Usage 측정 데모
 *
 * Step 4 데모용 프로그램
 */
public class UsageDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Usage 측정 데모 - Step 4");
        System.out.println("=".repeat(60));

        String projectRoot = TrainDemo.findProjectRoot();

        // 1. 기본 Usage 측정
        System.out.println("\n[1] 기본 Usage 측정");
        System.out.println("-".repeat(60));
        demonstrateBasicUsage(projectRoot + "/data/sample-bigram.json");

        // 2. MaxTokens 변화에 따른 Usage
        System.out.println("\n[2] MaxTokens 변화에 따른 Usage");
        System.out.println("-".repeat(60));
        demonstrateMaxTokensEffect(projectRoot + "/data/sample-bigram.json");

        // 3. Prompt 길이에 따른 Usage
        System.out.println("\n[3] Prompt 길이에 따른 Usage");
        System.out.println("-".repeat(60));
        demonstratePromptLengthEffect(projectRoot + "/data/sample-bigram.json");

        // 4. 비용 계산 시뮬레이션
        System.out.println("\n[4] 비용 계산 시뮬레이션 (GPT-4 가격 기준)");
        System.out.println("-".repeat(60));
        demonstrateCostCalculation(projectRoot + "/data/sample-bigram.json");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("데모 완료!");
        System.out.println("=".repeat(60));
    }

    private static void demonstrateBasicUsage(String artifactPath) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));

            String prompt = "the cat";
            GenerateRequest request = GenerateRequest.builder(prompt)
                .maxTokens(10)
                .seed(42L)
                .build();

            GenerateResponse response = model.generate(request);
            Usage usage = response.getUsage();

            System.out.println("Prompt: \"" + prompt + "\"");
            System.out.println("MaxTokens: 10");
            System.out.println("\nGenerated: " + response.getGeneratedText());
            System.out.println("\nUsage:");
            System.out.println("  Input tokens:  " + usage.getInputTokens());
            System.out.println("  Output tokens: " + usage.getOutputTokens());
            System.out.println("  Total tokens:  " + usage.getTotalTokens());
            System.out.println("\n검증: input + output = " +
                (usage.getInputTokens() + usage.getOutputTokens()) +
                " = total " + usage.getTotalTokens() + " ✓");

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }

    private static void demonstrateMaxTokensEffect(String artifactPath) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));
            String prompt = "I love";

            int[] maxTokensList = {5, 10, 20, 50};

            System.out.println("Prompt: \"" + prompt + "\" (고정)");
            System.out.println("\nMaxTokens 변화:");
            System.out.println(String.format("%-12s %-8s %-8s %-8s",
                "MaxTokens", "Input", "Output", "Total"));
            System.out.println("-".repeat(40));

            for (int maxTokens : maxTokensList) {
                GenerateRequest request = GenerateRequest.builder(prompt)
                    .maxTokens(maxTokens)
                    .seed(42L)
                    .build();

                GenerateResponse response = model.generate(request);
                Usage usage = response.getUsage();

                System.out.println(String.format("%-12d %-8d %-8d %-8d",
                    maxTokens,
                    usage.getInputTokens(),
                    usage.getOutputTokens(),
                    usage.getTotalTokens()));
            }

            System.out.println("\n관찰:");
            System.out.println("  - Input tokens는 항상 동일 (같은 prompt)");
            System.out.println("  - Output tokens는 maxTokens에 비례");
            System.out.println("  - Total = Input + Output");

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }

    private static void demonstratePromptLengthEffect(String artifactPath) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));

            String[] prompts = {
                "the",
                "the cat",
                "the cat sat on",
                "the cat sat on the mat"
            };

            System.out.println("MaxTokens: 5 (고정)");
            System.out.println("\nPrompt 길이 변화:");
            System.out.println(String.format("%-25s %-8s %-8s %-8s",
                "Prompt", "Input", "Output", "Total"));
            System.out.println("-".repeat(55));

            for (String prompt : prompts) {
                GenerateRequest request = GenerateRequest.builder(prompt)
                    .maxTokens(5)
                    .seed(42L)
                    .build();

                GenerateResponse response = model.generate(request);
                Usage usage = response.getUsage();

                String shortPrompt = prompt.length() > 20 ?
                    prompt.substring(0, 17) + "..." : prompt;

                System.out.println(String.format("%-25s %-8d %-8d %-8d",
                    "\"" + shortPrompt + "\"",
                    usage.getInputTokens(),
                    usage.getOutputTokens(),
                    usage.getTotalTokens()));
            }

            System.out.println("\n관찰:");
            System.out.println("  - Prompt가 길수록 Input tokens 증가");
            System.out.println("  - Output은 maxTokens에 의해 제한");
            System.out.println("  - Total tokens = Input + Output");

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }

    private static void demonstrateCostCalculation(String artifactPath) {
        try {
            BigramModel model = BigramModel.fromArtifact(Paths.get(artifactPath));

            // GPT-4 가격 (2024년 기준, 실제는 다를 수 있음)
            double inputPricePer1K = 0.03;   // $0.03 / 1K tokens
            double outputPricePer1K = 0.06;  // $0.06 / 1K tokens

            String prompt = "the quick brown fox";
            int maxTokens = 100;

            GenerateRequest request = GenerateRequest.builder(prompt)
                .maxTokens(maxTokens)
                .seed(42L)
                .build();

            GenerateResponse response = model.generate(request);
            Usage usage = response.getUsage();

            // 비용 계산
            double inputCost = (usage.getInputTokens() / 1000.0) * inputPricePer1K;
            double outputCost = (usage.getOutputTokens() / 1000.0) * outputPricePer1K;
            double totalCost = inputCost + outputCost;

            System.out.println("시뮬레이션 (GPT-4 가격):");
            System.out.println("  Input:  $0.03 / 1K tokens");
            System.out.println("  Output: $0.06 / 1K tokens");
            System.out.println("\nPrompt: \"" + prompt + "\"");
            System.out.println("MaxTokens: " + maxTokens);
            System.out.println("\nUsage:");
            System.out.println("  Input tokens:  " + usage.getInputTokens() + " tokens");
            System.out.println("  Output tokens: " + usage.getOutputTokens() + " tokens");
            System.out.println("  Total tokens:  " + usage.getTotalTokens() + " tokens");
            System.out.println("\n예상 비용:");
            System.out.println(String.format("  Input cost:  $%.6f", inputCost));
            System.out.println(String.format("  Output cost: $%.6f", outputCost));
            System.out.println(String.format("  Total cost:  $%.6f", totalCost));

            // 대량 사용 시 비용
            int requests = 1000;
            double costFor1000 = totalCost * requests;

            System.out.println(String.format("\n1,000번 호출 시: $%.2f", costFor1000));
            System.out.println(String.format("10,000번 호출 시: $%.2f", costFor1000 * 10));

            System.out.println("\n💡 왜 토큰이 비용 단위인가?");
            System.out.println("  - 토큰 수 = 모델 처리량");
            System.out.println("  - 처리량 = 계산 비용 (GPU 시간)");
            System.out.println("  - 따라서 토큰으로 과금");

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
        }
    }
}
