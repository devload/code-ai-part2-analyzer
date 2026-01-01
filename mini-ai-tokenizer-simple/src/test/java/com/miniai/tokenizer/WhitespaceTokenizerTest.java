package com.miniai.tokenizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhitespaceTokenizerTest {

    @Test
    @DisplayName("기본 encode/decode round-trip 테스트")
    void testEncodeDecodeRoundTrip() {
        // Given: 학습 텍스트
        String corpus = "hello world hello java";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // When: encode 후 decode
        String text = "hello world";
        List<Integer> tokens = tokenizer.encode(text);
        String decoded = tokenizer.decode(tokens);

        // Then: 원본 복원
        assertEquals(text, decoded);
    }

    @Test
    @DisplayName("한글 텍스트 토큰화: 오늘은 날씨가 좋다")
    void testKoreanText() {
        // Given: 한글 학습 텍스트
        String corpus = "오늘은 날씨가 좋다 내일도 날씨가 좋을까";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // When: "오늘은 날씨가 좋다" 토큰화
        String text = "오늘은 날씨가 좋다";
        List<Integer> tokens = tokenizer.encode(text);

        // Then: 3개 단어 = 3개 토큰
        assertEquals(3, tokens.size());

        // And: decode 시 원본 복원
        String decoded = tokenizer.decode(tokens);
        assertEquals(text, decoded);

        // And: vocabulary 확인
        System.out.println("\n=== 한글 토큰화 예시 ===");
        System.out.println("원본 텍스트: " + text);
        System.out.println("토큰 ID: " + tokens);
        System.out.println("복원 텍스트: " + decoded);
        System.out.println("Vocabulary 크기: " + tokenizer.vocabSize());

        // 각 단어별 ID 출력
        for (String word : text.split("\\s+")) {
            System.out.println("  '" + word + "' -> ID " + tokenizer.getTokenId(word));
        }
    }

    @Test
    @DisplayName("Unknown 토큰 처리")
    void testUnknownToken() {
        // Given: 제한된 vocabulary
        String corpus = "hello world";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // When: 어휘에 없는 단어 encode
        String text = "hello unknown";
        List<Integer> tokens = tokenizer.encode(text);

        // Then: "unknown"은 UNK_ID(0)로 변환
        assertEquals(Arrays.asList(
            tokenizer.getTokenId("hello"),
            WhitespaceTokenizer.UNK_ID
        ), tokens);

        // And: decode 시 [UNK]로 표시
        String decoded = tokenizer.decode(tokens);
        assertEquals("hello [UNK]", decoded);

        System.out.println("\n=== Unknown 토큰 처리 ===");
        System.out.println("원본: " + text);
        System.out.println("복원: " + decoded);
    }

    @Test
    @DisplayName("빈 텍스트 처리")
    void testEmptyText() {
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.empty();

        // Empty string
        assertEquals(List.of(), tokenizer.encode(""));
        assertEquals("", tokenizer.decode(List.of()));

        // Only whitespace
        assertEquals(List.of(), tokenizer.encode("   "));
    }

    @Test
    @DisplayName("다중 공백 처리")
    void testMultipleSpaces() {
        String corpus = "hello world";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // Multiple spaces are treated as single separator
        String text = "hello    world";
        List<Integer> tokens = tokenizer.encode(text);

        assertEquals(2, tokens.size());
        assertEquals("hello world", tokenizer.decode(tokens));
    }

    @Test
    @DisplayName("Vocabulary 크기 확인")
    void testVocabSize() {
        String corpus = "apple banana cherry apple banana";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // 3 unique words + 1 UNK = 4
        assertEquals(4, tokenizer.vocabSize());

        // Vocabulary 내용 확인
        Map<String, Integer> vocab = tokenizer.getVocabulary();
        assertTrue(vocab.containsKey("[UNK]"));
        assertTrue(vocab.containsKey("apple"));
        assertTrue(vocab.containsKey("banana"));
        assertTrue(vocab.containsKey("cherry"));
    }

    @Test
    @DisplayName("대소문자 구분 확인 (한계)")
    void testCaseSensitivity() {
        String corpus = "Hello world";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // "hello"와 "Hello"는 다른 토큰
        List<Integer> tokens1 = tokenizer.encode("Hello");
        List<Integer> tokens2 = tokenizer.encode("hello");

        assertNotEquals(tokens1, tokens2);
        assertEquals(WhitespaceTokenizer.UNK_ID, tokens2.get(0));

        System.out.println("\n=== 대소문자 구분 (한계) ===");
        System.out.println("'Hello' -> " + tokens1);
        System.out.println("'hello' -> " + tokens2 + " (UNK)");
    }

    @Test
    @DisplayName("구두점 처리 안 됨 (한계)")
    void testPunctuationNotHandled() {
        String corpus = "Hello world";
        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // "Hello"와 "Hello,"는 다른 토큰
        List<Integer> tokens1 = tokenizer.encode("Hello");
        List<Integer> tokens2 = tokenizer.encode("Hello,");

        assertNotEquals(tokens1, tokens2);
        assertEquals(WhitespaceTokenizer.UNK_ID, tokens2.get(0));

        System.out.println("\n=== 구두점 처리 안 됨 (한계) ===");
        System.out.println("'Hello' -> " + tokens1);
        System.out.println("'Hello,' -> " + tokens2 + " (UNK)");
    }

    @Test
    @DisplayName("실제 문장 토큰화 예시")
    void testRealSentence() {
        // Given: 긴 학습 텍스트
        String corpus = "the quick brown fox jumps over the lazy dog " +
                        "the dog was very lazy today";

        WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

        // When: 문장 토큰화
        String sentence = "the fox jumps over the dog";
        List<Integer> tokens = tokenizer.encode(sentence);
        String decoded = tokenizer.decode(tokens);

        // Then
        assertEquals(sentence, decoded);

        System.out.println("\n=== 실제 문장 토큰화 ===");
        System.out.println("Vocabulary 크기: " + tokenizer.vocabSize());
        System.out.println("문장: " + sentence);
        System.out.println("토큰 수: " + tokens.size());
        System.out.println("토큰 ID: " + tokens);

        // 각 단어의 ID 표시
        String[] words = sentence.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            System.out.println(String.format("  [%d] '%s' -> ID %d",
                i, words[i], tokens.get(i)));
        }
    }
}
