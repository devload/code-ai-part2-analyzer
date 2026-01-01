package com.miniai.server.dto;

/**
 * /v1/train 요청 DTO
 */
public class TrainRequest {
    private String corpusPath;
    private String outputPath;

    public TrainRequest() {
    }

    public TrainRequest(String corpusPath, String outputPath) {
        this.corpusPath = corpusPath;
        this.outputPath = outputPath;
    }

    public String getCorpusPath() {
        return corpusPath;
    }

    public void setCorpusPath(String corpusPath) {
        this.corpusPath = corpusPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
}
