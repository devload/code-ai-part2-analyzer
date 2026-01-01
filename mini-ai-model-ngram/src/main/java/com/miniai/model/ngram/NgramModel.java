package com.miniai.model.ngram;

import com.codeai.tokenizer.CodeTokenizer;
import com.google.gson.Gson;
import com.miniai.core.model.LanguageModel;
import com.miniai.core.tokenizer.Tokenizer;
import com.miniai.core.types.GenerateRequest;
import com.miniai.core.types.GenerateResponse;
import com.miniai.core.types.Usage;
import com.miniai.model.Sampler;
import com.miniai.model.smoothing.KneserNey;
import com.miniai.model.smoothing.SimpleBackoff;
import com.miniai.model.smoothing.SmoothingStrategy;
import com.miniai.tokenizer.WhitespaceTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * N-gram 언어 모델 (일반화된 버전)
 *
 * 학습 포인트:
 * - N을 파라미터로 받아 어떤 N-gram이든 지원
 * - Smoothing 전략 교체 가능 (SimpleBackoff, KneserNey)
 * - 문맥 윈도우: N-1 토큰
 *
 * 예시 (5-gram):
 * - 입력: "for (int i = 0;"
 * - 문맥: 마지막 4토큰 ["i", "=", "0", ";"]
 * - 예측: 다음 토큰 확률 분포
 */
public class NgramModel implements LanguageModel {

    private final NgramArtifact artifact;
    private final Tokenizer tokenizer;
    private final SmoothingStrategy smoothing;
    private final Map<Integer, String> reverseVocab;

    /**
     * 기본 생성자 (SimpleBackoff 사용)
     */
    public NgramModel(NgramArtifact artifact, Tokenizer tokenizer) {
        this(artifact, tokenizer, new SimpleBackoff());
    }

    /**
     * Smoothing 전략 지정 생성자
     */
    public NgramModel(NgramArtifact artifact, Tokenizer tokenizer, SmoothingStrategy smoothing) {
        this.artifact = artifact;
        this.tokenizer = tokenizer;
        this.smoothing = smoothing;

        // Reverse vocabulary 생성 (id → word)
        this.reverseVocab = new HashMap<>();
        for (Map.Entry<String, Integer> entry : artifact.getVocabulary().entrySet()) {
            reverseVocab.put(entry.getValue(), entry.getKey());
        }
    }

    @Override
    public GenerateResponse generate(GenerateRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. 프롬프트 토큰화
        List<Integer> tokens = new ArrayList<>(tokenizer.encode(request.getPrompt()));
        int inputTokenCount = tokens.size();

        // 2. Sampler 생성
        Long seed = request.getSeed().orElse(System.currentTimeMillis());
        Sampler sampler = new Sampler(request.getTemperature(), request.getTopK(), seed);
        int n = artifact.getN();

        for (int i = 0; i < request.getMaxTokens(); i++) {
            // 문맥 추출 (마지막 N-1개 토큰)
            int contextStart = Math.max(0, tokens.size() - (n - 1));
            List<Integer> context = tokens.subList(contextStart, tokens.size());

            // 다음 토큰 확률 분포 (smoothing 적용)
            Map<Integer, Double> probs = smoothing.getSmoothedProbabilities(artifact, context);

            if (probs.isEmpty()) {
                break; // 더 이상 생성 불가
            }

            // 확률을 카운트로 변환 (Sampler가 Integer 카운트를 받음)
            Map<Integer, Integer> counts = probsToCounts(probs);

            // 샘플링
            int nextToken = sampler.sample(counts);

            // Stop sequence 체크
            String nextWord = reverseVocab.getOrDefault(nextToken, "[UNK]");
            if (request.getStopSequences() != null && request.getStopSequences().contains(nextWord)) {
                break;
            }

            tokens.add(nextToken);
        }

        // 3. 결과 디코딩
        String generatedText = tokenizer.decode(tokens);

        // 4. Usage 계산
        int outputTokenCount = tokens.size() - inputTokenCount;
        Usage usage = new Usage(inputTokenCount, outputTokenCount);

        long latency = System.currentTimeMillis() - startTime;

        return new GenerateResponse(generatedText, usage, latency, modelName());
    }

    /**
     * 확률 분포를 카운트로 변환 (Sampler 호환용)
     * 확률을 1000배로 스케일업하여 정수로 변환
     */
    private Map<Integer, Integer> probsToCounts(Map<Integer, Double> probs) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : probs.entrySet()) {
            // 확률을 1000배하여 정수 카운트로 변환
            int count = (int) (entry.getValue() * 1000) + 1; // 최소 1
            counts.put(entry.getKey(), count);
        }
        return counts;
    }

    @Override
    public String modelName() {
        return artifact.getN() + "-gram-v1";
    }

    public NgramArtifact getArtifact() {
        return artifact;
    }

    public SmoothingStrategy getSmoothing() {
        return smoothing;
    }

    /**
     * Artifact 파일로부터 모델 로드
     */
    public static NgramModel fromArtifact(Path artifactPath) throws IOException {
        return fromArtifact(artifactPath, new SimpleBackoff());
    }

    /**
     * Artifact 파일로부터 모델 로드 (Smoothing 전략 지정)
     */
    public static NgramModel fromArtifact(Path artifactPath, SmoothingStrategy smoothing) throws IOException {
        String json = Files.readString(artifactPath);
        Gson gson = new Gson();
        NgramArtifact artifact = gson.fromJson(json, NgramArtifact.class);

        // 토크나이저 타입에 따라 생성
        String tokenizerType = artifact.getMetadata().getTokenizerType();
        Tokenizer tokenizer;

        if ("CodeTokenizer".equals(tokenizerType)) {
            tokenizer = new CodeTokenizer(artifact.getVocabulary());
            System.out.println("🔧 " + artifact.getN() + "-gram 모델 로드: CodeTokenizer 사용");
        } else {
            tokenizer = new WhitespaceTokenizer(artifact.getVocabulary());
            System.out.println("📝 " + artifact.getN() + "-gram 모델 로드: WhitespaceTokenizer 사용");
        }

        System.out.println("📊 Smoothing: " + smoothing.description());

        return new NgramModel(artifact, tokenizer, smoothing);
    }

    /**
     * Kneser-Ney Smoothing으로 모델 로드
     */
    public static NgramModel fromArtifactWithKneserNey(Path artifactPath) throws IOException {
        return fromArtifact(artifactPath, new KneserNey());
    }

    @Override
    public String toString() {
        return String.format("NgramModel(n=%d, vocab=%d, smoothing=%s)",
            artifact.getN(),
            artifact.getVocabulary().size(),
            smoothing.strategyName());
    }
}
