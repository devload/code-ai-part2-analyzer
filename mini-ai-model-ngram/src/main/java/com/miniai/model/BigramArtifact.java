package com.miniai.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Bigram 학습 결과 (Artifact)
 *
 * 학습 포인트:
 * - 학습 결과 = 데이터에서 추출한 패턴
 * - Bigram = "A 다음에 B가 올 확률"의 카운트 테이블
 * - Artifact = 재사용 가능한 형태로 저장
 */
public class BigramArtifact {

    /**
     * Bigram 카운트: prev_token -> next_token -> count
     * 예: counts.get(42).get(123) = 5
     *     → 토큰 42 다음에 토큰 123이 5번 나타남
     */
    private Map<Integer, Map<Integer, Integer>> counts;

    /**
     * Vocabulary: word -> token_id
     */
    private Map<String, Integer> vocabulary;

    /**
     * Metadata: 학습 정보
     */
    private Metadata metadata;

    public BigramArtifact() {
        this.counts = new HashMap<>();
        this.vocabulary = new HashMap<>();
        this.metadata = new Metadata();
    }

    public BigramArtifact(Map<Integer, Map<Integer, Integer>> counts,
                          Map<String, Integer> vocabulary,
                          Metadata metadata) {
        this.counts = counts;
        this.vocabulary = vocabulary;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Map<Integer, Map<Integer, Integer>> getCounts() {
        return counts;
    }

    public void setCounts(Map<Integer, Map<Integer, Integer>> counts) {
        this.counts = counts;
    }

    public Map<String, Integer> getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(Map<String, Integer> vocabulary) {
        this.vocabulary = vocabulary;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * 특정 토큰 쌍의 카운트 조회
     */
    public int getCount(int prevToken, int nextToken) {
        return counts.getOrDefault(prevToken, new HashMap<>())
                     .getOrDefault(nextToken, 0);
    }

    /**
     * 특정 토큰 다음에 올 수 있는 모든 토큰과 카운트
     */
    public Map<Integer, Integer> getNextTokenCounts(int prevToken) {
        return counts.getOrDefault(prevToken, new HashMap<>());
    }

    /**
     * 전체 bigram 쌍의 개수
     */
    public int getTotalBigramCount() {
        return counts.values().stream()
            .mapToInt(nextCounts -> nextCounts.values().stream().mapToInt(Integer::intValue).sum())
            .sum();
    }

    /**
     * 학습 메타데이터
     */
    public static class Metadata {
        private String modelType = "bigram";
        private String tokenizerType;
        private int vocabSize;
        private int totalTokens;
        private int totalBigrams;
        private String trainedAt;
        private String corpusInfo;

        public Metadata() {
            this.trainedAt = Instant.now().toString();
        }

        // Getters and Setters
        public String getModelType() {
            return modelType;
        }

        public void setModelType(String modelType) {
            this.modelType = modelType;
        }

        public String getTokenizerType() {
            return tokenizerType;
        }

        public void setTokenizerType(String tokenizerType) {
            this.tokenizerType = tokenizerType;
        }

        public int getVocabSize() {
            return vocabSize;
        }

        public void setVocabSize(int vocabSize) {
            this.vocabSize = vocabSize;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }

        public int getTotalBigrams() {
            return totalBigrams;
        }

        public void setTotalBigrams(int totalBigrams) {
            this.totalBigrams = totalBigrams;
        }

        public String getTrainedAt() {
            return trainedAt;
        }

        public void setTrainedAt(String trainedAt) {
            this.trainedAt = trainedAt;
        }

        public String getCorpusInfo() {
            return corpusInfo;
        }

        public void setCorpusInfo(String corpusInfo) {
            this.corpusInfo = corpusInfo;
        }

        @Override
        public String toString() {
            return String.format("Metadata(model=%s, vocab=%d, tokens=%d, bigrams=%d)",
                modelType, vocabSize, totalTokens, totalBigrams);
        }
    }

    @Override
    public String toString() {
        return String.format("BigramArtifact(vocab=%d, bigrams=%d, %s)",
            vocabulary.size(), getTotalBigramCount(), metadata);
    }
}
