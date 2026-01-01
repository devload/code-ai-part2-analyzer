package com.miniai.model;

import com.miniai.core.model.LanguageModel;
import com.miniai.core.tokenizer.Tokenizer;
import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.core.types.Usage;
import com.miniai.tokenizer.WhitespaceTokenizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bigram 언어 모델
 *
 * 학습 포인트:
 * - 생성 = "다음 토큰 예측" 루프
 * - Bigram: prev 토큰 기반으로 next 예측
 * - 샘플링: 확률 분포에서 토큰 선택
 */
public class BigramModel implements LanguageModel {

    private final BigramArtifact artifact;
    private final Tokenizer tokenizer;
    private final String modelName;

    public BigramModel(BigramArtifact artifact, Tokenizer tokenizer) {
        this.artifact = artifact;
        this.tokenizer = tokenizer;
        this.modelName = "bigram-v1";
    }

    /**
     * Artifact 파일로부터 모델 로드
     */
    public static BigramModel fromArtifact(Path artifactPath) {
        BigramArtifact artifact = BigramTrainer.loadArtifact(artifactPath);

        // Vocabulary로부터 Tokenizer 생성
        Tokenizer tokenizer = new WhitespaceTokenizer(artifact.getVocabulary());

        return new BigramModel(artifact, tokenizer);
    }

    @Override
    public GenerateResponse generate(GenerateRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Prompt 토큰화
        List<Integer> promptTokens = tokenizer.encode(request.getPrompt());
        if (promptTokens.isEmpty()) {
            throw new IllegalArgumentException("Prompt가 비어있습니다");
        }

        // 2. Sampler 생성
        Sampler sampler = new Sampler(
            request.getTemperature(),
            request.getTopK(),
            request.getSeed().orElse(null)
        );

        // 3. 생성 루프
        List<Integer> generatedTokens = new ArrayList<>(promptTokens);
        int maxTokens = request.getMaxTokens();
        List<String> stopSequences = request.getStopSequences();

        for (int i = 0; i < maxTokens; i++) {
            // 마지막 토큰 기반으로 다음 토큰 예측
            int prevToken = generatedTokens.get(generatedTokens.size() - 1);

            // 다음 토큰 후보들
            Map<Integer, Integer> nextCounts = artifact.getNextTokenCounts(prevToken);

            if (nextCounts.isEmpty()) {
                // 더 이상 생성 불가 (dead end)
                break;
            }

            // 샘플링
            int nextToken = sampler.sample(nextCounts);
            generatedTokens.add(nextToken);

            // Stop sequence 확인
            if (shouldStop(generatedTokens, stopSequences)) {
                break;
            }
        }

        // 4. 토큰 → 텍스트
        String generatedText = tokenizer.decode(generatedTokens);

        // 5. Usage 계산
        Usage usage = new Usage(
            promptTokens.size(),
            generatedTokens.size() - promptTokens.size()
        );

        // 6. Latency 계산
        long latency = System.currentTimeMillis() - startTime;

        return new GenerateResponse(generatedText, usage, latency, modelName);
    }

    /**
     * Stop sequence 확인
     */
    private boolean shouldStop(List<Integer> tokens, List<String> stopSequences) {
        if (stopSequences.isEmpty()) {
            return false;
        }

        String currentText = tokenizer.decode(tokens);

        for (String stopSeq : stopSequences) {
            if (currentText.endsWith(stopSeq)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 단일 다음 토큰 예측
     */
    public int predictNext(int prevToken, double temperature, int topK, Long seed) {
        Map<Integer, Integer> nextCounts = artifact.getNextTokenCounts(prevToken);

        if (nextCounts.isEmpty()) {
            throw new IllegalStateException("토큰 " + prevToken + " 다음에 올 수 있는 토큰이 없습니다");
        }

        Sampler sampler = new Sampler(temperature, topK, seed);
        return sampler.sample(nextCounts);
    }

    /**
     * 특정 토큰 다음에 올 수 있는 토큰들과 확률
     */
    public Map<Integer, Double> getNextTokenProbs(int prevToken) {
        Map<Integer, Integer> counts = artifact.getNextTokenCounts(prevToken);
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        Map<Integer, Double> probs = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            probs.put(entry.getKey(), (double) entry.getValue() / total);
        }

        return probs;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    public BigramArtifact getArtifact() {
        return artifact;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    @Override
    public String toString() {
        return String.format("BigramModel(vocab=%d, bigrams=%d)",
            artifact.getVocabulary().size(),
            artifact.getTotalBigramCount());
    }
}
