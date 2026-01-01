package com.miniai.model.smoothing;

import com.miniai.model.ngram.NgramArtifact;

import java.util.List;
import java.util.Map;

/**
 * Smoothing 전략 인터페이스
 *
 * 학습 포인트:
 * - N-gram 모델의 희소성(Sparsity) 문제 해결
 * - 본 적 없는 토큰 조합에도 확률 부여
 * - 다양한 Smoothing 기법 교체 가능
 *
 * 구현체:
 * - SimpleBackoff: 단순 backoff (Trigram → Bigram → Unigram)
 * - KneserNey: Kneser-Ney Smoothing (가장 효과적)
 * - AddOne: Add-One (Laplace) Smoothing
 */
public interface SmoothingStrategy {

    /**
     * 주어진 문맥에서 각 토큰의 확률 분포 계산
     *
     * @param artifact N-gram artifact
     * @param context 문맥 토큰들 (N-1개)
     * @return 각 토큰 ID → 확률 맵
     */
    Map<Integer, Double> getSmoothedProbabilities(NgramArtifact artifact, List<Integer> context);

    /**
     * Smoothing 전략 이름
     */
    String strategyName();

    /**
     * 설명
     */
    default String description() {
        return strategyName() + " smoothing strategy";
    }
}
