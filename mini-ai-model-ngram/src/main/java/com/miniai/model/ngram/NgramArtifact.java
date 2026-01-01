package com.miniai.model.ngram;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * N-gram 학습 결과 (Artifact) - 일반화된 버전
 *
 * 학습 포인트:
 * - N을 파라미터로 받아 Bigram, Trigram, 5-gram 등 모두 지원
 * - 키: "tok1:tok2:tok3:tok4" 형식 (N-1개 토큰)
 * - 하위 N-gram도 함께 저장 (backoff용)
 *
 * 예시:
 * - 5-gram: "public:static:void:main" → "("
 * - 4-gram backoff: "static:void:main" → "("
 * - 3-gram backoff: "void:main" → "("
 */
public class NgramArtifact {

    /**
     * N-gram order (예: 5 = 5-gram)
     */
    private int n;

    /**
     * N-gram 카운트: context_key -> next_token -> count
     * 키 형식: "tok1:tok2:...:tok(n-1)"
     */
    private Map<String, Map<Integer, Integer>> counts;

    /**
     * 하위 N-gram 카운트 (backoff용)
     * 인덱스 0 = unigram, 1 = bigram, ...
     */
    private Map<Integer, Map<String, Map<Integer, Integer>>> lowerOrderCounts;

    /**
     * Continuation counts (Kneser-Ney용)
     * 각 토큰이 몇 개의 다른 문맥에서 나타났는지
     */
    private Map<Integer, Integer> continuationCounts;

    /**
     * Vocabulary: word -> token_id
     */
    private Map<String, Integer> vocabulary;

    /**
     * Metadata: 학습 정보
     */
    private Metadata metadata;

    public NgramArtifact() {
        this.n = 3; // 기본값 trigram
        this.counts = new HashMap<>();
        this.lowerOrderCounts = new HashMap<>();
        this.continuationCounts = new HashMap<>();
        this.vocabulary = new HashMap<>();
        this.metadata = new Metadata();
    }

    public NgramArtifact(int n) {
        this();
        this.n = n;
        this.metadata.setModelType(n + "-gram");
    }

    /**
     * 문맥 키 생성 (여러 토큰을 ":"로 연결)
     */
    public static String makeKey(List<Integer> context) {
        return context.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(":"));
    }

    /**
     * 문맥 키 생성 (가변 인자)
     */
    public static String makeKey(int... tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    /**
     * N-gram 카운트 조회
     */
    public int getCount(List<Integer> context, int next) {
        String key = makeKey(context);
        return counts.getOrDefault(key, new HashMap<>())
                     .getOrDefault(next, 0);
    }

    /**
     * 특정 문맥 다음에 올 수 있는 모든 토큰과 카운트
     */
    public Map<Integer, Integer> getNextTokenCounts(List<Integer> context) {
        String key = makeKey(context);
        return counts.getOrDefault(key, new HashMap<>());
    }

    /**
     * 하위 N-gram 카운트 조회 (backoff용)
     * order: 1=unigram, 2=bigram, ...
     */
    public Map<Integer, Integer> getLowerOrderCounts(int order, List<Integer> context) {
        if (!lowerOrderCounts.containsKey(order)) {
            return new HashMap<>();
        }
        String key = makeKey(context);
        return lowerOrderCounts.get(order).getOrDefault(key, new HashMap<>());
    }

    /**
     * Continuation count 조회 (Kneser-Ney용)
     */
    public int getContinuationCount(int token) {
        return continuationCounts.getOrDefault(token, 0);
    }

    /**
     * 전체 고유 bigram 수 (Kneser-Ney 정규화용)
     */
    public int getTotalUniqueBigrams() {
        if (!lowerOrderCounts.containsKey(2)) {
            return 0;
        }
        return lowerOrderCounts.get(2).values().stream()
            .mapToInt(Map::size)
            .sum();
    }

    /**
     * 전체 N-gram 카운트 합계
     */
    public int getTotalNgramCount() {
        return counts.values().stream()
            .mapToInt(nextCounts -> nextCounts.values().stream().mapToInt(Integer::intValue).sum())
            .sum();
    }

    // Getters and Setters
    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public Map<String, Map<Integer, Integer>> getCounts() {
        return counts;
    }

    public void setCounts(Map<String, Map<Integer, Integer>> counts) {
        this.counts = counts;
    }

    public Map<Integer, Map<String, Map<Integer, Integer>>> getLowerOrderCounts() {
        return lowerOrderCounts;
    }

    public void setLowerOrderCounts(Map<Integer, Map<String, Map<Integer, Integer>>> lowerOrderCounts) {
        this.lowerOrderCounts = lowerOrderCounts;
    }

    public Map<Integer, Integer> getContinuationCounts() {
        return continuationCounts;
    }

    public void setContinuationCounts(Map<Integer, Integer> continuationCounts) {
        this.continuationCounts = continuationCounts;
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
     * 학습 메타데이터
     */
    public static class Metadata {
        private String modelType = "ngram";
        private int n;
        private String tokenizerType;
        private int vocabSize;
        private int totalTokens;
        private int totalNgrams;
        private String trainedAt;
        private String corpusInfo;
        private String smoothingType;

        public Metadata() {
            this.trainedAt = Instant.now().toString();
        }

        // Getters and Setters
        public String getModelType() { return modelType; }
        public void setModelType(String modelType) { this.modelType = modelType; }

        public int getN() { return n; }
        public void setN(int n) { this.n = n; }

        public String getTokenizerType() { return tokenizerType; }
        public void setTokenizerType(String tokenizerType) { this.tokenizerType = tokenizerType; }

        public int getVocabSize() { return vocabSize; }
        public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

        public int getTotalTokens() { return totalTokens; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

        public int getTotalNgrams() { return totalNgrams; }
        public void setTotalNgrams(int totalNgrams) { this.totalNgrams = totalNgrams; }

        public String getTrainedAt() { return trainedAt; }
        public void setTrainedAt(String trainedAt) { this.trainedAt = trainedAt; }

        public String getCorpusInfo() { return corpusInfo; }
        public void setCorpusInfo(String corpusInfo) { this.corpusInfo = corpusInfo; }

        public String getSmoothingType() { return smoothingType; }
        public void setSmoothingType(String smoothingType) { this.smoothingType = smoothingType; }

        @Override
        public String toString() {
            return String.format("Metadata(model=%s, n=%d, vocab=%d, tokens=%d, ngrams=%d, smoothing=%s)",
                modelType, n, vocabSize, totalTokens, totalNgrams, smoothingType);
        }
    }

    @Override
    public String toString() {
        return String.format("NgramArtifact(n=%d, vocab=%d, ngrams=%d, %s)",
            n, vocabulary.size(), getTotalNgramCount(), metadata);
    }
}
