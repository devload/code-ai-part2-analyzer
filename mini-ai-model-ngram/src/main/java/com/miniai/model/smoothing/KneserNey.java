package com.miniai.model.smoothing;

import com.miniai.model.ngram.NgramArtifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kneser-Ney Smoothing
 *
 * 학습 포인트:
 * - 가장 효과적인 N-gram Smoothing 기법
 * - Absolute Discounting + Continuation Probability
 * - "San Francisco" vs "I saw" 문제 해결
 *
 * 핵심 아이디어:
 * 1. Absolute Discounting: 모든 카운트에서 고정값(d) 빼기
 * 2. Continuation Probability: 토큰이 다양한 문맥에서 나타난 횟수 사용
 *
 * 수식:
 * P_KN(w_i | w_{i-1}) = max(count(w_{i-1}, w_i) - d, 0) / count(w_{i-1})
 *                      + λ(w_{i-1}) × P_continuation(w_i)
 *
 * λ(w_{i-1}) = d × |{w : count(w_{i-1}, w) > 0}| / count(w_{i-1})
 *
 * P_continuation(w_i) = |{w : count(w, w_i) > 0}| / |all bigrams|
 */
public class KneserNey implements SmoothingStrategy {

    /**
     * Discount 값 (보통 0.75)
     * 모든 카운트에서 빼는 고정값
     */
    private final double discount;

    public KneserNey() {
        this(0.75); // 일반적으로 0.75가 좋은 성능
    }

    public KneserNey(double discount) {
        if (discount < 0 || discount > 1) {
            throw new IllegalArgumentException("Discount must be between 0 and 1");
        }
        this.discount = discount;
    }

    @Override
    public Map<Integer, Double> getSmoothedProbabilities(NgramArtifact artifact, List<Integer> context) {
        Map<Integer, Double> result = new HashMap<>();

        // 1. 주 N-gram에서 discounted 확률 계산
        Map<Integer, Integer> primaryCounts = artifact.getNextTokenCounts(context);
        double contextTotal = primaryCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (contextTotal == 0) {
            // 문맥이 없으면 continuation probability만 사용
            return getContinuationProbabilities(artifact);
        }

        // 2. Lambda (backoff weight) 계산
        // λ = d × (해당 문맥 뒤에 나온 고유 토큰 수) / (해당 문맥 총 카운트)
        int uniqueFollowingTokens = primaryCounts.size();
        double lambda = (discount * uniqueFollowingTokens) / contextTotal;

        // 3. 각 토큰에 대해 확률 계산
        // P_KN(w) = max(count - d, 0) / contextTotal + λ × P_continuation(w)
        Map<Integer, Double> continuationProbs = getContinuationProbabilities(artifact);

        // 관찰된 토큰들의 discounted 확률
        for (Map.Entry<Integer, Integer> entry : primaryCounts.entrySet()) {
            int token = entry.getKey();
            int count = entry.getValue();

            double discountedCount = Math.max(count - discount, 0);
            double discountedProb = discountedCount / contextTotal;

            double contProb = continuationProbs.getOrDefault(token, 1.0 / artifact.getVocabulary().size());
            double finalProb = discountedProb + lambda * contProb;

            result.put(token, finalProb);
        }

        // 관찰되지 않은 토큰들은 continuation probability만 사용
        for (Map.Entry<Integer, Double> entry : continuationProbs.entrySet()) {
            if (!result.containsKey(entry.getKey())) {
                result.put(entry.getKey(), lambda * entry.getValue());
            }
        }

        return result;
    }

    /**
     * Continuation Probability 계산
     *
     * P_continuation(w) = |{v : count(v, w) > 0}| / |all unique bigrams|
     *
     * 즉, 토큰 w가 몇 개의 다른 토큰 뒤에서 나타났는지
     * "Francisco"는 거의 "San" 뒤에만 → 낮은 continuation probability
     * "the"는 수많은 토큰 뒤에서 → 높은 continuation probability
     */
    private Map<Integer, Double> getContinuationProbabilities(NgramArtifact artifact) {
        Map<Integer, Double> result = new HashMap<>();

        Map<Integer, Integer> continuationCounts = artifact.getContinuationCounts();
        int totalUniqueBigrams = artifact.getTotalUniqueBigrams();

        if (totalUniqueBigrams == 0) {
            // fallback to uniform
            int vocabSize = artifact.getVocabulary().size();
            double uniform = 1.0 / Math.max(vocabSize, 1);
            for (String word : artifact.getVocabulary().keySet()) {
                result.put(artifact.getVocabulary().get(word), uniform);
            }
            return result;
        }

        for (Map.Entry<Integer, Integer> entry : continuationCounts.entrySet()) {
            double prob = (double) entry.getValue() / totalUniqueBigrams;
            result.put(entry.getKey(), prob);
        }

        return result;
    }

    @Override
    public String strategyName() {
        return "KneserNey";
    }

    @Override
    public String description() {
        return String.format("Kneser-Ney Smoothing (discount=%.2f)", discount);
    }

    public double getDiscount() {
        return discount;
    }
}
