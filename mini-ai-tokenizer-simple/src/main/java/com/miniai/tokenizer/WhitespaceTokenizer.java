package com.miniai.tokenizer;

import com.miniai.core.tokenizer.Tokenizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 공백 기준 토크나이저
 *
 * 학습 포인트:
 * - 가장 단순한 토크나이저: 공백으로 단어 분리
 * - vocabulary = {단어: ID} 매핑
 * - unknown 토큰 처리
 *
 * 한계:
 * - 구두점 처리 안 됨 ("Hello," != "Hello")
 * - 대소문자 구분 ("hello" != "Hello")
 * - 어휘에 없는 단어는 모두 [UNK]로 처리
 */
public class WhitespaceTokenizer implements Tokenizer {

    // 특수 토큰
    public static final String UNK_TOKEN = "[UNK]";
    public static final int UNK_ID = 0;

    private final Map<String, Integer> wordToId;
    private final Map<Integer, String> idToWord;
    private final int vocabSize;

    /**
     * 기존 vocabulary로 초기화
     */
    public WhitespaceTokenizer(Map<String, Integer> wordToId) {
        this.wordToId = new HashMap<>(wordToId);
        this.idToWord = new HashMap<>();

        // 역방향 매핑 생성
        for (Map.Entry<String, Integer> entry : wordToId.entrySet()) {
            idToWord.put(entry.getValue(), entry.getKey());
        }

        this.vocabSize = wordToId.size();
    }

    /**
     * 텍스트로부터 vocabulary 생성
     *
     * @param corpus 학습 텍스트
     * @return WhitespaceTokenizer 인스턴스
     */
    public static WhitespaceTokenizer fromText(String corpus) {
        Map<String, Integer> wordToId = new HashMap<>();

        // [UNK] 토큰 추가
        wordToId.put(UNK_TOKEN, UNK_ID);

        // 공백으로 분리된 모든 단어 수집
        Set<String> uniqueWords = Arrays.stream(corpus.split("\\s+"))
            .filter(word -> !word.isEmpty())
            .collect(Collectors.toSet());

        // ID 할당 (1부터 시작, 0은 UNK)
        int nextId = 1;
        for (String word : uniqueWords) {
            if (!wordToId.containsKey(word)) {
                wordToId.put(word, nextId++);
            }
        }

        return new WhitespaceTokenizer(wordToId);
    }

    /**
     * 빈 vocabulary로 초기화 (UNK만 포함)
     */
    public static WhitespaceTokenizer empty() {
        Map<String, Integer> wordToId = new HashMap<>();
        wordToId.put(UNK_TOKEN, UNK_ID);
        return new WhitespaceTokenizer(wordToId);
    }

    @Override
    public List<Integer> encode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split("\\s+"))
            .filter(word -> !word.isEmpty())
            .map(word -> wordToId.getOrDefault(word, UNK_ID))
            .collect(Collectors.toList());
    }

    @Override
    public String decode(List<Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }

        return tokens.stream()
            .map(id -> idToWord.getOrDefault(id, UNK_TOKEN))
            .collect(Collectors.joining(" "));
    }

    @Override
    public int vocabSize() {
        return vocabSize;
    }

    /**
     * Vocabulary 조회
     */
    public Map<String, Integer> getVocabulary() {
        return new HashMap<>(wordToId);
    }

    /**
     * 특정 단어의 ID 조회
     */
    public int getTokenId(String word) {
        return wordToId.getOrDefault(word, UNK_ID);
    }

    /**
     * 특정 ID의 단어 조회
     */
    public String getToken(int id) {
        return idToWord.getOrDefault(id, UNK_TOKEN);
    }

    @Override
    public String toString() {
        return String.format("WhitespaceTokenizer(vocab_size=%d)", vocabSize);
    }
}
