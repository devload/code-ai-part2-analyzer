package com.miniai.model.smoothing;

import com.miniai.model.ngram.NgramArtifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 단순 Backoff Smoothing
 *
 * 학습 포인트:
 * - 가장 기본적인 Smoothing 방법
 * - N-gram이 없으면 (N-1)-gram으로 폴백
 * - 각 단계에 가중치 적용 (interpolation)
 *
 * 예시 (5-gram):
 * 1. 5-gram "a:b:c:d" → "e" 카운트 확인
 * 2. 없으면 4-gram "b:c:d" → "e" 확인
 * 3. 없으면 3-gram "c:d" → "e" 확인
 * 4. 없으면 2-gram "d" → "e" 확인
 * 5. 없으면 1-gram "e" 빈도 사용
 */
public class SimpleBackoff implements SmoothingStrategy {

    /**
     * Backoff 가중치 (interpolation)
     * 높을수록 하위 N-gram 비중 증가
     */
    private final double backoffWeight;

    public SimpleBackoff() {
        this(0.4); // 기본값: 40% 하위 N-gram
    }

    public SimpleBackoff(double backoffWeight) {
        if (backoffWeight < 0 || backoffWeight > 1) {
            throw new IllegalArgumentException("Backoff weight must be between 0 and 1");
        }
        this.backoffWeight = backoffWeight;
    }

    @Override
    public Map<Integer, Double> getSmoothedProbabilities(NgramArtifact artifact, List<Integer> context) {
        Map<Integer, Double> result = new HashMap<>();
        int n = artifact.getN();

        // 1. 주 N-gram 확률
        Map<Integer, Integer> primaryCounts = artifact.getNextTokenCounts(context);
        double primaryTotal = primaryCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (primaryTotal > 0) {
            for (Map.Entry<Integer, Integer> entry : primaryCounts.entrySet()) {
                double prob = entry.getValue() / primaryTotal;
                result.put(entry.getKey(), prob * (1 - backoffWeight));
            }
        }

        // 2. Backoff: 하위 N-gram들 순차 적용
        double remainingWeight = backoffWeight;
        List<Integer> currentContext = new ArrayList<>(context);

        for (int order = n - 1; order >= 1 && remainingWeight > 0.01; order--) {
            // 문맥 줄이기 (앞에서부터 제거)
            if (!currentContext.isEmpty()) {
                currentContext = currentContext.subList(1, currentContext.size());
            }

            Map<Integer, Integer> lowerCounts = artifact.getLowerOrderCounts(order, currentContext);
            double lowerTotal = lowerCounts.values().stream().mapToInt(Integer::intValue).sum();

            if (lowerTotal > 0) {
                double levelWeight = remainingWeight * (1 - backoffWeight);

                for (Map.Entry<Integer, Integer> entry : lowerCounts.entrySet()) {
                    double prob = entry.getValue() / lowerTotal;
                    result.merge(entry.getKey(), prob * levelWeight, Double::sum);
                }

                remainingWeight *= backoffWeight;
            }
        }

        // 3. Unigram fallback
        if (remainingWeight > 0.01) {
            Map<Integer, Integer> unigramCounts = artifact.getLowerOrderCounts(1, new ArrayList<>());
            double unigramTotal = unigramCounts.values().stream().mapToInt(Integer::intValue).sum();

            if (unigramTotal > 0) {
                for (Map.Entry<Integer, Integer> entry : unigramCounts.entrySet()) {
                    double prob = entry.getValue() / unigramTotal;
                    result.merge(entry.getKey(), prob * remainingWeight, Double::sum);
                }
            }
        }

        return result;
    }

    @Override
    public String strategyName() {
        return "SimpleBackoff";
    }

    @Override
    public String description() {
        return String.format("Simple Backoff (weight=%.2f)", backoffWeight);
    }

    public double getBackoffWeight() {
        return backoffWeight;
    }
}
