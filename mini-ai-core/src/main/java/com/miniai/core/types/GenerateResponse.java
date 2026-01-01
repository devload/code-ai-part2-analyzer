package com.miniai.core.types;

/**
 * 텍스트 생성 응답
 *
 * 학습 포인트:
 * - generatedText: 실제 생성 결과
 * - usage: 토큰 사용량 (비용 계산 기준)
 * - latencyMs: 성능 측정
 * - model: 어떤 모델이 생성했는지 추적
 */
public class GenerateResponse {
    private final String generatedText;
    private final Usage usage;
    private final long latencyMs;
    private final String model;

    public GenerateResponse(String generatedText, Usage usage, long latencyMs, String model) {
        this.generatedText = generatedText;
        this.usage = usage;
        this.latencyMs = latencyMs;
        this.model = model;
    }

    public String getGeneratedText() {
        return generatedText;
    }

    public Usage getUsage() {
        return usage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public String getModel() {
        return model;
    }

    @Override
    public String toString() {
        return String.format("GenerateResponse(model=%s, latency=%dms, %s, text='%s')",
            model, latencyMs, usage, generatedText);
    }
}
