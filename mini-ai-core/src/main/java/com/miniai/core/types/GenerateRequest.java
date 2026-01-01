package com.miniai.core.types;

import java.util.List;
import java.util.Optional;

/**
 * 텍스트 생성 요청
 *
 * 학습 포인트:
 * - prompt: 생성의 시작점
 * - maxTokens: 생성 길이 제한
 * - temperature: 창의성 조절 (높을수록 다양)
 * - topK: 후보 토큰 수 제한
 * - seed: 재현 가능성 보장
 */
public class GenerateRequest {
    private final String prompt;
    private final int maxTokens;
    private final double temperature;
    private final int topK;
    private final Long seed;
    private final List<String> stopSequences;

    private GenerateRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.topK = builder.topK;
        this.seed = builder.seed;
        this.stopSequences = builder.stopSequences;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getTopK() {
        return topK;
    }

    public Optional<Long> getSeed() {
        return Optional.ofNullable(seed);
    }

    public List<String> getStopSequences() {
        return stopSequences != null ? stopSequences : List.of();
    }

    public static Builder builder(String prompt) {
        return new Builder(prompt);
    }

    public static class Builder {
        private final String prompt;
        private int maxTokens = 50;
        private double temperature = 1.0;
        private int topK = 50;
        private Long seed = null;
        private List<String> stopSequences = List.of();

        public Builder(String prompt) {
            this.prompt = prompt;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public GenerateRequest build() {
            return new GenerateRequest(this);
        }
    }
}
