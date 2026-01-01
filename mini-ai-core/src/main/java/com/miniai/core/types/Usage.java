package com.miniai.core.types;

/**
 * 토큰 사용량 정보
 *
 * 학습 포인트:
 * - 토큰은 AI 서비스의 "비용 단위"
 * - input(프롬프트) + output(생성) = total
 */
public class Usage {
    private final int inputTokens;
    private final int outputTokens;
    private final int totalTokens;

    public Usage(int inputTokens, int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = inputTokens + outputTokens;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    @Override
    public String toString() {
        return String.format("Usage(input=%d, output=%d, total=%d)",
            inputTokens, outputTokens, totalTokens);
    }
}
