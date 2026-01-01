package com.miniai.core.model;

import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;

/**
 * 언어 모델 인터페이스
 *
 * 학습 포인트:
 * - 언어 모델은 "다음 토큰을 예측"하는 시스템
 * - 생성(generate)은 이 예측을 반복하는 루프
 */
public interface LanguageModel {

    /**
     * 주어진 요청에 따라 텍스트 생성
     *
     * @param request 생성 요청 (prompt, maxTokens, 샘플링 옵션 등)
     * @return 생성 응답 (생성된 텍스트, usage, latency 등)
     */
    GenerateResponse generate(GenerateRequest request);

    /**
     * 모델 이름 반환
     *
     * @return 모델 식별자 (예: "bigram-v1")
     */
    String modelName();
}
