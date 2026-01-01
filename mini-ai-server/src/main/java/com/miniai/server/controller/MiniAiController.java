package com.miniai.server.controller;

import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.model.BigramModel;
import com.miniai.model.BigramTrainer;
import com.miniai.server.dto.GenerateRequestDto;
import com.miniai.server.dto.GenerateResponseDto;
import com.miniai.server.dto.TrainRequest;
import com.miniai.tokenizer.WhitespaceTokenizer;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Mini AI REST API Controller
 *
 * Step 5: REST API 엔드포인트
 */
@RestController
@RequestMapping("/v1")
public class MiniAiController {

    private BigramModel model;
    private final String defaultArtifactPath = "data/sample-bigram.json";

    public MiniAiController() {
        // 기본 모델 로드 시도
        try {
            Path artifactPath = Paths.get(defaultArtifactPath);
            if (Files.exists(artifactPath)) {
                this.model = BigramModel.fromArtifact(artifactPath);
                System.out.println("✅ 기본 모델 로드: " + defaultArtifactPath);
            } else {
                System.out.println("⚠️  기본 모델 없음. /v1/train으로 학습 필요");
            }
        } catch (Exception e) {
            System.err.println("⚠️  모델 로드 실패: " + e.getMessage());
        }
    }

    /**
     * POST /v1/train
     * 모델 학습
     */
    @PostMapping("/train")
    public Map<String, Object> train(@RequestBody TrainRequest request) {
        try {
            Path corpusPath = Paths.get(request.getCorpusPath());
            Path outputPath = Paths.get(request.getOutputPath());

            // Corpus 읽기
            String corpus = Files.readString(corpusPath);
            WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

            // 학습
            long startTime = System.currentTimeMillis();
            BigramTrainer trainer = new BigramTrainer(tokenizer);
            trainer.train(corpusPath, outputPath);
            long latency = System.currentTimeMillis() - startTime;

            // 학습된 모델 로드
            this.model = BigramModel.fromArtifact(outputPath);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "학습 완료");
            response.put("artifactPath", outputPath.toString());
            response.put("vocabSize", tokenizer.vocabSize());
            response.put("latencyMs", latency);

            return response;

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    /**
     * POST /v1/generate
     * 텍스트 생성
     */
    @PostMapping("/generate")
    public GenerateResponseDto generate(@RequestBody GenerateRequestDto request) {
        if (model == null) {
            throw new IllegalStateException("모델이 로드되지 않았습니다. /v1/train을 먼저 호출하세요.");
        }

        // DTO → Core Request 변환
        GenerateRequest.Builder builder = GenerateRequest.builder(request.getPrompt())
            .maxTokens(request.getMaxTokens())
            .temperature(request.getTemperature())
            .topK(request.getTopK());

        if (request.getSeed() != null) {
            builder.seed(request.getSeed());
        }

        if (request.getStopSequences() != null) {
            builder.stopSequences(request.getStopSequences());
        }

        GenerateRequest coreRequest = builder.build();

        // 생성
        GenerateResponse coreResponse = model.generate(coreRequest);

        // Core Response → DTO 변환
        GenerateResponseDto.UsageDto usageDto = new GenerateResponseDto.UsageDto(
            coreResponse.getUsage().getInputTokens(),
            coreResponse.getUsage().getOutputTokens(),
            coreResponse.getUsage().getTotalTokens()
        );

        return new GenerateResponseDto(
            coreResponse.getGeneratedText(),
            usageDto,
            coreResponse.getLatencyMs(),
            coreResponse.getModel()
        );
    }

    /**
     * GET /v1/health
     * 헬스 체크
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("modelLoaded", model != null);
        if (model != null) {
            response.put("model", model.toString());
        }
        return response;
    }
}
