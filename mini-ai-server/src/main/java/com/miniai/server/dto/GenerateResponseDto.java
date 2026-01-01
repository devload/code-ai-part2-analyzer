package com.miniai.server.dto;

/**
 * /v1/generate 응답 DTO
 */
public class GenerateResponseDto {
    private String generatedText;
    private UsageDto usage;
    private Long latencyMs;
    private String model;

    public GenerateResponseDto() {
    }

    public GenerateResponseDto(String generatedText, UsageDto usage, Long latencyMs, String model) {
        this.generatedText = generatedText;
        this.usage = usage;
        this.latencyMs = latencyMs;
        this.model = model;
    }

    public String getGeneratedText() {
        return generatedText;
    }

    public void setGeneratedText(String generatedText) {
        this.generatedText = generatedText;
    }

    public UsageDto getUsage() {
        return usage;
    }

    public void setUsage(UsageDto usage) {
        this.usage = usage;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static class UsageDto {
        private int inputTokens;
        private int outputTokens;
        private int totalTokens;

        public UsageDto() {
        }

        public UsageDto(int inputTokens, int outputTokens, int totalTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.totalTokens = totalTokens;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public void setInputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public void setOutputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(int totalTokens) {
            this.totalTokens = totalTokens;
        }
    }
}
