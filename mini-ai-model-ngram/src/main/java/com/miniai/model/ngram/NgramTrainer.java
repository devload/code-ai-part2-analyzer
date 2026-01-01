package com.miniai.model.ngram;

import com.codeai.tokenizer.CodeTokenizer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.miniai.core.model.Trainer;
import com.miniai.core.tokenizer.Tokenizer;
import com.miniai.tokenizer.WhitespaceTokenizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * N-gram 모델 학습기 (일반화된 버전)
 *
 * 학습 포인트:
 * - N을 파라미터로 받아 어떤 N-gram이든 학습 가능
 * - 하위 N-gram도 자동으로 학습 (backoff용)
 * - Continuation count 계산 (Kneser-Ney용)
 *
 * 예시:
 * - n=5: 5-gram, 4-gram, 3-gram, 2-gram, 1-gram 모두 학습
 */
public class NgramTrainer implements Trainer {

    private final int n;
    private final Tokenizer tokenizer;
    private final Gson gson;

    /**
     * N-gram 학습기 생성
     * @param n N-gram order (예: 5 = 5-gram)
     * @param tokenizer 토크나이저
     */
    public NgramTrainer(int n, Tokenizer tokenizer) {
        if (n < 2) {
            throw new IllegalArgumentException("N must be at least 2 (bigram)");
        }
        this.n = n;
        this.tokenizer = tokenizer;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    @Override
    public void train(Path corpusPath, Path outputPath) {
        try {
            // 1. Corpus 읽기
            String corpus = Files.readString(corpusPath);

            // 2. N-gram 학습
            NgramArtifact artifact = trainFromText(corpus, tokenizer);

            // 3. JSON으로 저장
            String json = gson.toJson(artifact);
            Files.writeString(outputPath, json);

            System.out.println("✅ " + n + "-gram 학습 완료: " + outputPath);
            System.out.println("   Vocabulary: " + artifact.getVocabulary().size());
            System.out.println("   Total tokens: " + artifact.getMetadata().getTotalTokens());
            System.out.println("   Total " + n + "-grams: " + artifact.getMetadata().getTotalNgrams());

        } catch (IOException e) {
            throw new RuntimeException("학습 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 텍스트로부터 N-gram 학습
     */
    public NgramArtifact trainFromText(String corpus, Tokenizer tokenizer) {
        // 1. 토큰화
        List<Integer> tokens = tokenizer.encode(corpus);

        NgramArtifact artifact = new NgramArtifact(n);

        // 2. N-gram 카운트 (주 N-gram)
        Map<String, Map<Integer, Integer>> ngramCounts = new HashMap<>();
        for (int i = 0; i <= tokens.size() - n; i++) {
            List<Integer> context = tokens.subList(i, i + n - 1);
            int next = tokens.get(i + n - 1);

            String key = NgramArtifact.makeKey(context);
            ngramCounts.putIfAbsent(key, new HashMap<>());
            Map<Integer, Integer> nextCounts = ngramCounts.get(key);
            nextCounts.put(next, nextCounts.getOrDefault(next, 0) + 1);
        }
        artifact.setCounts(ngramCounts);

        // 3. 하위 N-gram 카운트 (backoff용)
        Map<Integer, Map<String, Map<Integer, Integer>>> lowerOrderCounts = new HashMap<>();

        for (int order = n - 1; order >= 1; order--) {
            Map<String, Map<Integer, Integer>> orderCounts = new HashMap<>();

            for (int i = 0; i <= tokens.size() - order; i++) {
                List<Integer> context;
                int next;

                if (order == 1) {
                    // Unigram: 빈 문맥, 다음 토큰만
                    context = new ArrayList<>();
                    next = tokens.get(i);
                } else {
                    context = tokens.subList(i, i + order - 1);
                    next = tokens.get(i + order - 1);
                }

                String key = order == 1 ? "" : NgramArtifact.makeKey(context);
                orderCounts.putIfAbsent(key, new HashMap<>());
                Map<Integer, Integer> nextCounts = orderCounts.get(key);
                nextCounts.put(next, nextCounts.getOrDefault(next, 0) + 1);
            }

            lowerOrderCounts.put(order, orderCounts);
        }
        artifact.setLowerOrderCounts(lowerOrderCounts);

        // 4. Continuation counts 계산 (Kneser-Ney용)
        // 각 토큰이 몇 개의 다른 문맥 뒤에서 나타났는지
        Map<Integer, Set<String>> tokenContexts = new HashMap<>();

        // Bigram 기준으로 continuation count 계산
        Map<String, Map<Integer, Integer>> bigramCounts = lowerOrderCounts.get(2);
        if (bigramCounts != null) {
            for (Map.Entry<String, Map<Integer, Integer>> entry : bigramCounts.entrySet()) {
                String context = entry.getKey();
                for (Integer next : entry.getValue().keySet()) {
                    tokenContexts.putIfAbsent(next, new HashSet<>());
                    tokenContexts.get(next).add(context);
                }
            }
        }

        Map<Integer, Integer> continuationCounts = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : tokenContexts.entrySet()) {
            continuationCounts.put(entry.getKey(), entry.getValue().size());
        }
        artifact.setContinuationCounts(continuationCounts);

        // 5. Vocabulary 추출 및 토크나이저 타입 결정
        Map<String, Integer> vocabulary = new HashMap<>();
        String tokenizerType;

        if (tokenizer instanceof CodeTokenizer) {
            vocabulary = ((CodeTokenizer) tokenizer).getVocabulary();
            tokenizerType = "CodeTokenizer";
        } else if (tokenizer instanceof WhitespaceTokenizer) {
            vocabulary = ((WhitespaceTokenizer) tokenizer).getVocabulary();
            tokenizerType = "WhitespaceTokenizer";
        } else {
            tokenizerType = tokenizer.getClass().getSimpleName();
        }
        artifact.setVocabulary(vocabulary);

        // 6. Metadata 생성
        NgramArtifact.Metadata metadata = artifact.getMetadata();
        metadata.setN(n);
        metadata.setModelType(n + "-gram");
        metadata.setTokenizerType(tokenizerType);
        metadata.setVocabSize(tokenizer.vocabSize());
        metadata.setTotalTokens(tokens.size());
        metadata.setTotalNgrams(tokens.size() - n + 1);
        metadata.setCorpusInfo(String.format("%d characters, %d tokens", corpus.length(), tokens.size()));

        return artifact;
    }

    /**
     * JSON 파일로부터 Artifact 로드
     */
    public static NgramArtifact loadArtifact(Path artifactPath) {
        try {
            String json = Files.readString(artifactPath);
            Gson gson = new Gson();
            return gson.fromJson(json, NgramArtifact.class);
        } catch (IOException e) {
            throw new RuntimeException("Artifact 로드 실패: " + e.getMessage(), e);
        }
    }

    @Override
    public String trainerName() {
        return n + "-gramTrainer";
    }
}
