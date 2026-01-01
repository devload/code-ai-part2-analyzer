package com.miniai.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 확률 기반 토큰 샘플러
 *
 * 학습 포인트:
 * - 생성 = "다음 토큰 예측"의 반복
 * - 샘플링 = 확률 분포에서 토큰 선택
 * - temperature = 창의성 조절
 * - topK = 후보 제한
 */
public class Sampler {

    private final Random random;
    private final double temperature;
    private final int topK;

    /**
     * @param temperature 창의성 (0.0 ~ 2.0)
     *                    - 낮음 (0.1): 확정적, 가장 높은 확률 선택
     *                    - 중간 (1.0): 균형
     *                    - 높음 (2.0): 창의적, 낮은 확률도 선택
     * @param topK       후보 제한 (상위 K개만 고려)
     * @param seed       랜덤 시드 (재현성)
     */
    public Sampler(double temperature, int topK, Long seed) {
        this.temperature = temperature;
        this.topK = topK;
        this.random = seed != null ? new Random(seed) : new Random();
    }

    /**
     * 카운트 맵에서 다음 토큰 샘플링
     *
     * @param counts prev 토큰 다음에 올 수 있는 토큰들의 카운트
     * @return 선택된 토큰 ID
     */
    public int sample(Map<Integer, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            throw new IllegalArgumentException("카운트가 비어있습니다");
        }

        // 1. 카운트 → 확률 변환
        List<TokenProb> probs = countsToProbs(counts);

        // 2. Temperature 적용
        if (temperature != 1.0) {
            probs = applyTemperature(probs, temperature);
        }

        // 3. TopK 필터링
        if (topK > 0 && topK < probs.size()) {
            probs = applyTopK(probs, topK);
        }

        // 4. 확률 기반 샘플링
        return sampleFromProbs(probs);
    }

    /**
     * 카운트를 확률로 변환
     */
    private List<TokenProb> countsToProbs(Map<Integer, Integer> counts) {
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        return counts.entrySet().stream()
            .map(e -> new TokenProb(
                e.getKey(),
                (double) e.getValue() / total
            ))
            .sorted((a, b) -> Double.compare(b.prob, a.prob)) // 확률 높은 순
            .collect(Collectors.toList());
    }

    /**
     * Temperature 적용
     *
     * temperature = 0.5:
     *   높은 확률 → 더 높게 (집중)
     *   낮은 확률 → 더 낮게
     *
     * temperature = 2.0:
     *   확률 차이 완화 (분산)
     */
    private List<TokenProb> applyTemperature(List<TokenProb> probs, double temp) {
        // logits = log(prob)
        // scaled_logits = logits / temperature
        // new_prob = exp(scaled_logits) / sum(exp(scaled_logits))

        double[] logits = probs.stream()
            .mapToDouble(p -> Math.log(p.prob + 1e-10)) // log(0) 방지
            .toArray();

        double[] scaledLogits = new double[logits.length];
        for (int i = 0; i < logits.length; i++) {
            scaledLogits[i] = logits[i] / temp;
        }

        // Softmax
        double maxLogit = Arrays.stream(scaledLogits).max().orElse(0.0);
        double[] expLogits = Arrays.stream(scaledLogits)
            .map(l -> Math.exp(l - maxLogit)) // 안정성을 위해 max 빼기
            .toArray();

        double sumExp = Arrays.stream(expLogits).sum();

        List<TokenProb> result = new ArrayList<>();
        for (int i = 0; i < probs.size(); i++) {
            result.add(new TokenProb(
                probs.get(i).tokenId,
                expLogits[i] / sumExp
            ));
        }

        return result.stream()
            .sorted((a, b) -> Double.compare(b.prob, a.prob))
            .collect(Collectors.toList());
    }

    /**
     * TopK 필터링 (상위 K개만 유지)
     */
    private List<TokenProb> applyTopK(List<TokenProb> probs, int k) {
        List<TokenProb> topK = probs.stream()
            .limit(k)
            .collect(Collectors.toList());

        // 확률 재정규화
        double totalProb = topK.stream().mapToDouble(p -> p.prob).sum();
        return topK.stream()
            .map(p -> new TokenProb(p.tokenId, p.prob / totalProb))
            .collect(Collectors.toList());
    }

    /**
     * 확률 분포에서 샘플링
     */
    private int sampleFromProbs(List<TokenProb> probs) {
        double r = random.nextDouble();
        double cumulative = 0.0;

        for (TokenProb p : probs) {
            cumulative += p.prob;
            if (r < cumulative) {
                return p.tokenId;
            }
        }

        // fallback (반올림 오차 대비)
        return probs.get(0).tokenId;
    }

    /**
     * 토큰-확률 쌍
     */
    private static class TokenProb {
        final int tokenId;
        final double prob;

        TokenProb(int tokenId, double prob) {
            this.tokenId = tokenId;
            this.prob = prob;
        }

        @Override
        public String toString() {
            return String.format("Token(%d, %.4f)", tokenId, prob);
        }
    }

    @Override
    public String toString() {
        return String.format("Sampler(temp=%.2f, topK=%d)", temperature, topK);
    }
}
