package com.miniai.core.tokenizer;

import java.util.List;

/**
 * 텍스트를 토큰으로 변환하는 인터페이스
 *
 * 학습 포인트:
 * - 토큰화는 텍스트를 모델이 이해할 수 있는 "조각"으로 나누는 과정
 * - encode/decode는 양방향 변환을 지원해야 함
 */
public interface Tokenizer {

    /**
     * 텍스트를 토큰 ID 리스트로 변환
     *
     * @param text 입력 텍스트
     * @return 토큰 ID 리스트
     */
    List<Integer> encode(String text);

    /**
     * 토큰 ID 리스트를 텍스트로 복원
     *
     * @param tokens 토큰 ID 리스트
     * @return 복원된 텍스트
     */
    String decode(List<Integer> tokens);

    /**
     * 어휘 크기 반환
     *
     * @return 어휘 사전의 크기
     */
    int vocabSize();
}
