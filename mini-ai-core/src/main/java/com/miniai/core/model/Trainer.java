package com.miniai.core.model;

import java.nio.file.Path;

/**
 * 모델 학습 인터페이스
 *
 * 학습 포인트:
 * - 학습은 "데이터에서 패턴 추출"하는 과정
 * - N-gram의 경우, 학습 = 카운트 테이블 생성
 */
public interface Trainer {

    /**
     * 코퍼스 파일로부터 모델 학습
     *
     * @param corpusPath 학습 데이터 파일 경로
     * @param outputPath 학습 결과(artifact) 저장 경로
     */
    void train(Path corpusPath, Path outputPath);

    /**
     * 학습기 이름 반환
     *
     * @return 학습기 식별자 (예: "bigram-trainer")
     */
    String trainerName();
}
