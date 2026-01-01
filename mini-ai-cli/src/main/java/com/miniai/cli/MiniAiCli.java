package com.miniai.cli;

import com.google.gson.Gson;
import okhttp3.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Mini AI CLI
 * Step 6: 명령줄 인터페이스
 */
@Command(name = "mini-ai", version = "1.0",
         description = "Mini AI CLI - Bigram 언어 모델 CLI",
         subcommands = {
             MiniAiCli.Train.class,
             MiniAiCli.Run.class,
             MiniAiCli.Tokenize.class
         })
public class MiniAiCli implements Callable<Integer> {

    private static final String API_BASE = "http://localhost:8080/v1";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public Integer call() {
        System.out.println("Mini AI CLI");
        System.out.println("사용법: mini-ai [command]");
        System.out.println("\n명령어:");
        System.out.println("  train      - 모델 학습");
        System.out.println("  run        - 텍스트 생성");
        System.out.println("  tokenize   - 텍스트 토큰화");
        return 0;
    }

    /**
     * train 명령어
     */
    @Command(name = "train", description = "모델 학습")
    static class Train implements Callable<Integer> {
        @Option(names = {"--corpus"}, required = true, description = "Corpus 파일 경로")
        String corpusPath;

        @Option(names = {"--output"}, description = "Artifact 출력 경로",
                defaultValue = "data/cli-bigram.json")
        String outputPath;

        @Override
        public Integer call() {
            try {
                System.out.println("🚀 모델 학습 시작...");
                System.out.println("  Corpus: " + corpusPath);
                System.out.println("  Output: " + outputPath);

                String json = gson.toJson(Map.of(
                    "corpusPath", corpusPath,
                    "outputPath", outputPath
                ));

                Request request = new Request.Builder()
                    .url(API_BASE + "/train")
                    .post(RequestBody.create(json, JSON))
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body().string();
                    Map<String, Object> result = gson.fromJson(body, Map.class);

                    if ("success".equals(result.get("status"))) {
                        System.out.println("\n✅ 학습 완료!");
                        System.out.println("  Vocabulary: " + result.get("vocabSize"));
                        System.out.println("  Latency: " + result.get("latencyMs") + "ms");
                    } else {
                        System.err.println("❌ 학습 실패: " + result.get("message"));
                        return 1;
                    }
                }

                return 0;
            } catch (Exception e) {
                System.err.println("❌ 오류: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * run 명령어
     */
    @Command(name = "run", description = "텍스트 생성")
    static class Run implements Callable<Integer> {
        @Option(names = {"-p", "--prompt"}, required = true, description = "프롬프트")
        String prompt;

        @Option(names = {"--max-tokens"}, description = "최대 토큰 수", defaultValue = "20")
        int maxTokens;

        @Option(names = {"--temperature"}, description = "Temperature", defaultValue = "1.0")
        double temperature;

        @Option(names = {"--seed"}, description = "Random seed")
        Long seed;

        @Override
        public Integer call() {
            try {
                System.out.println("💬 텍스트 생성...");
                System.out.println("  Prompt: \"" + prompt + "\"");

                Map<String, Object> requestMap = Map.of(
                    "prompt", prompt,
                    "maxTokens", maxTokens,
                    "temperature", temperature,
                    "seed", seed != null ? seed : System.currentTimeMillis()
                );

                String json = gson.toJson(requestMap);

                Request request = new Request.Builder()
                    .url(API_BASE + "/generate")
                    .post(RequestBody.create(json, JSON))
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String body = response.body().string();
                    Map<String, Object> result = gson.fromJson(body, Map.class);

                    System.out.println("\n📝 생성 결과:");
                    System.out.println("  " + result.get("generatedText"));

                    Map<String, Object> usage = (Map<String, Object>) result.get("usage");
                    System.out.println("\n📊 Usage:");
                    System.out.println("  Input:  " + usage.get("inputTokens") + " tokens");
                    System.out.println("  Output: " + usage.get("outputTokens") + " tokens");
                    System.out.println("  Total:  " + usage.get("totalTokens") + " tokens");
                }

                return 0;
            } catch (Exception e) {
                System.err.println("❌ 오류: " + e.getMessage());
                e.printStackTrace();
                return 1;
            }
        }
    }

    /**
     * tokenize 명령어
     */
    @Command(name = "tokenize", description = "텍스트 토큰화")
    static class Tokenize implements Callable<Integer> {
        @Parameters(index = "0", description = "토큰화할 텍스트")
        String text;

        @Override
        public Integer call() {
            // 로컬에서 직접 토큰화
            String[] tokens = text.split("\\s+");

            System.out.println("📌 토큰화 결과:");
            System.out.println("  원본: \"" + text + "\"");
            System.out.println("  토큰 수: " + tokens.length);
            System.out.println("  토큰: [" + String.join(", ", tokens) + "]");

            return 0;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MiniAiCli()).execute(args);
        System.exit(exitCode);
    }
}
