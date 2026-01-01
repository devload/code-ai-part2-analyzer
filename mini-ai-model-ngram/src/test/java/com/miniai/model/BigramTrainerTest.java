package com.miniai.model;

import com.miniai.tokenizer.WhitespaceTokenizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BigramTrainerTest {

    @Test
    @DisplayName("기본 Bigram 학습 테스트")
    void testBasicTraining() {
        // Given: 간단한 텍스트
        String corpus = "the cat sat on the mat";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);

        // When: 학습
        BigramArtifact artifact = trainer.trainFromText(corpus, tokenizer);

        // Then: 카운트 확인
        assertNotNull(artifact);
        assertEquals(6, artifact.getMetadata().getTotalTokens()); // 6 words
        assertEquals(5, artifact.getMetadata().getTotalBigrams()); // 5 pairs

        // Bigram 확인: "the" -> "cat" 1회
        int theId = tokenizer.getTokenId("the");
        int catId = tokenizer.getTokenId("cat");
        assertEquals(1, artifact.getCount(theId, catId));

        System.out.println("\n=== 기본 Bigram 학습 ===");
        System.out.println("Corpus: " + corpus);
        System.out.println(artifact.getMetadata());
    }

    @Test
    @DisplayName("반복되는 Bigram 카운트")
    void testRepeatedBigrams() {
        // Given: "the"가 반복되는 텍스트
        String corpus = "the cat the dog the bird";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);

        // When: 학습
        BigramArtifact artifact = trainer.trainFromText(corpus, tokenizer);

        // Then: "the" 다음에 나오는 토큰들의 카운트
        int theId = tokenizer.getTokenId("the");
        Map<Integer, Integer> afterThe = artifact.getNextTokenCounts(theId);

        assertEquals(3, afterThe.size()); // cat, dog, bird
        assertEquals(1, afterThe.get(tokenizer.getTokenId("cat")));
        assertEquals(1, afterThe.get(tokenizer.getTokenId("dog")));
        assertEquals(1, afterThe.get(tokenizer.getTokenId("bird")));

        System.out.println("\n=== 반복 Bigram 카운트 ===");
        System.out.println("'the' 다음에 나오는 단어들:");
        afterThe.forEach((tokenId, count) -> {
            String word = tokenizer.getToken(tokenId);
            System.out.println("  " + word + " : " + count + "회");
        });
    }

    @Test
    @DisplayName("한글 텍스트 Bigram 학습")
    void testKoreanBigram() {
        // Given: 한글 텍스트
        String corpus = "오늘은 날씨가 좋다 내일도 날씨가 좋을까";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);

        // When: 학습
        BigramArtifact artifact = trainer.trainFromText(corpus, tokenizer);

        // Then
        assertEquals(6, artifact.getMetadata().getTotalTokens());
        assertEquals(5, artifact.getMetadata().getTotalBigrams());

        // "날씨가"가 2번 나타남 확인
        int 날씨가Id = tokenizer.getTokenId("날씨가");
        Map<Integer, Integer> after날씨가 = artifact.getNextTokenCounts(날씨가Id);
        assertEquals(2, after날씨가.size()); // 좋다, 좋을까

        System.out.println("\n=== 한글 Bigram 학습 ===");
        System.out.println("Corpus: " + corpus);
        trainer.printSummary(artifact, 5);
    }

    @Test
    @DisplayName("파일로부터 학습 및 저장")
    void testTrainFromFile(@TempDir Path tempDir) throws Exception {
        // Given: 임시 corpus 파일 생성
        Path corpusPath = tempDir.resolve("corpus.txt");
        String corpus = "the quick brown fox jumps over the lazy dog";
        Files.writeString(corpusPath, corpus);

        // And: 출력 경로
        Path outputPath = tempDir.resolve("bigram.json");

        // When: 학습 및 저장
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);
        trainer.train(corpusPath, outputPath);

        // Then: 파일 생성 확인
        assertTrue(Files.exists(outputPath));

        // And: JSON 로드 가능
        BigramArtifact loaded = BigramTrainer.loadArtifact(outputPath);
        assertNotNull(loaded);
        assertEquals(9, loaded.getMetadata().getTotalTokens());
        assertEquals(8, loaded.getMetadata().getTotalBigrams());

        System.out.println("\n=== 파일 저장/로드 ===");
        System.out.println("Artifact 경로: " + outputPath);
        System.out.println("파일 크기: " + Files.size(outputPath) + " bytes");
        System.out.println(loaded.getMetadata());
    }

    @Test
    @DisplayName("JSON 형식 확인")
    void testJsonFormat(@TempDir Path tempDir) throws Exception {
        // Given: 학습 및 저장
        String corpus = "hello world hello java";
        Path corpusPath = tempDir.resolve("corpus.txt");
        Path outputPath = tempDir.resolve("bigram.json");
        Files.writeString(corpusPath, corpus);

        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);
        trainer.train(corpusPath, outputPath);

        // When: JSON 읽기
        String json = Files.readString(outputPath);

        // Then: JSON 구조 확인
        assertTrue(json.contains("\"counts\""));
        assertTrue(json.contains("\"vocabulary\""));
        assertTrue(json.contains("\"metadata\""));
        assertTrue(json.contains("\"modelType\": \"bigram\""));

        System.out.println("\n=== JSON 형식 ===");
        System.out.println(json);
    }

    @Test
    @DisplayName("다양한 Corpus로 재학습 가능")
    void testRetraining(@TempDir Path tempDir) throws Exception {
        Path corpusPath1 = tempDir.resolve("corpus1.txt");
        Path corpusPath2 = tempDir.resolve("corpus2.txt");
        Path outputPath1 = tempDir.resolve("bigram1.json");
        Path outputPath2 = tempDir.resolve("bigram2.json");

        // Corpus 1
        String corpus1 = "hello world";
        Files.writeString(corpusPath1, corpus1);
        WhitespaceTokenizer tokenizer1 = WhitespaceTokenizer.fromText(corpus1);
        BigramTrainer trainer1 = new BigramTrainer(tokenizer1);
        trainer1.train(corpusPath1, outputPath1);

        // Corpus 2
        String corpus2 = "goodbye world goodbye moon";
        Files.writeString(corpusPath2, corpus2);
        WhitespaceTokenizer tokenizer2 = WhitespaceTokenizer.fromText(corpus2);
        BigramTrainer trainer2 = new BigramTrainer(tokenizer2);
        trainer2.train(corpusPath2, outputPath2);

        // 결과 비교
        BigramArtifact artifact1 = BigramTrainer.loadArtifact(outputPath1);
        BigramArtifact artifact2 = BigramTrainer.loadArtifact(outputPath2);

        assertNotEquals(artifact1.getVocabulary().size(),
                        artifact2.getVocabulary().size());

        System.out.println("\n=== 재학습 테스트 ===");
        System.out.println("Artifact 1: " + artifact1.getMetadata());
        System.out.println("Artifact 2: " + artifact2.getMetadata());
    }

    @Test
    @DisplayName("실제 문장으로 Bigram 학습")
    void testRealSentence() {
        // Given: 실제 문장
        String corpus = "I love natural language processing " +
                        "natural language is amazing " +
                        "I love programming";

        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);
        BigramTrainer trainer = new BigramTrainer(tokenizer);

        // When: 학습
        BigramArtifact artifact = trainer.trainFromText(corpus, tokenizer);

        // Then: "I" 다음에 "love"가 2번
        int iId = tokenizer.getTokenId("I");
        int loveId = tokenizer.getTokenId("love");
        assertEquals(2, artifact.getCount(iId, loveId));

        // "natural" 다음에 "language"가 2번
        int naturalId = tokenizer.getTokenId("natural");
        int languageId = tokenizer.getTokenId("language");
        assertEquals(2, artifact.getCount(naturalId, languageId));

        System.out.println("\n=== 실제 문장 Bigram ===");
        trainer.printSummary(artifact, 10);
    }
}
