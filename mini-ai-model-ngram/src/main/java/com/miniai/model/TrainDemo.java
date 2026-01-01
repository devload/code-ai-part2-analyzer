package com.miniai.model;

import com.miniai.tokenizer.WhitespaceTokenizer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Bigram 학습 데모
 *
 * Step 2 데모용 프로그램
 */
public class TrainDemo {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Bigram 학습 데모 - Step 2");
        System.out.println("=".repeat(60));
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        // 프로젝트 루트 찾기
        String projectRoot = findProjectRoot();
        System.out.println("Project root: " + projectRoot);

        // 영어 corpus 학습
        System.out.println("\n[1] 영어 Corpus 학습");
        System.out.println("-".repeat(60));
        trainCorpus(
            projectRoot + "/data/sample-corpus.txt",
            projectRoot + "/data/sample-bigram.json"
        );

        // 한글 corpus 학습
        System.out.println("\n[2] 한글 Corpus 학습");
        System.out.println("-".repeat(60));
        trainCorpus(
            projectRoot + "/data/korean-corpus.txt",
            projectRoot + "/data/korean-bigram.json"
        );

        System.out.println("\n" + "=".repeat(60));
        System.out.println("학습 완료!");
        System.out.println("=".repeat(60));
    }

    public static String findProjectRoot() {
        Path current = Paths.get(System.getProperty("user.dir"));
        // settings.gradle이 있는 디렉토리가 프로젝트 루트
        while (current != null) {
            if (java.nio.file.Files.exists(current.resolve("settings.gradle"))) {
                return current.toString();
            }
            current = current.getParent();
        }
        return System.getProperty("user.dir");
    }

    private static void trainCorpus(String corpusPath, String outputPath) {
        try {
            // Corpus 읽기
            Path corpus = Paths.get(corpusPath);
            Path output = Paths.get(outputPath);

            // Corpus로부터 Tokenizer 생성
            String text = java.nio.file.Files.readString(corpus);
            WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(text);

            System.out.println("Corpus: " + corpusPath);
            System.out.println("Tokenizer vocabulary: " + tokenizer.vocabSize());

            // Trainer 생성 및 학습
            BigramTrainer trainer = new BigramTrainer(tokenizer);
            trainer.train(corpus, output);

            // 결과 로드 및 요약
            BigramArtifact artifact = BigramTrainer.loadArtifact(output);
            trainer.printSummary(artifact, 5);

        } catch (Exception e) {
            System.err.println("❌ 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
