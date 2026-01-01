package com.codeai.tokenizer;

import com.miniai.core.tokenizer.Tokenizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 코드 특화 토크나이저
 *
 * 핵심 개선:
 * 1. 들여쓰기 압축: 공백 묶음을 하나의 토큰으로 (GPT-4 방식)
 * 2. 심볼 분리: 괄호, 세미콜론 등을 독립 토큰으로
 * 3. 키워드 보호: Java 키워드는 절대 쪼개지 않음
 *
 * 참고:
 * - GPT-2: 147 토큰 vs GPT-4: 70 토큰 (같은 Python 코드)
 * - 핵심은 들여쓰기 압축!
 */
public class CodeTokenizer implements Tokenizer {

    // 특수 토큰
    public static final String UNK_TOKEN = "[UNK]";
    public static final String INDENT_PREFIX = "INDENT_";
    public static final String NEWLINE_TOKEN = "[NL]";
    public static final int UNK_ID = 0;

    // 코드 심볼 패턴
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
        "([{}()\\[\\];,.<>:?!@#$%^&*+=|~`/\\\\-])"
    );

    // 문자열/문자 리터럴 패턴
    private static final Pattern STRING_PATTERN = Pattern.compile(
        "\"[^\"]*\"|'[^']*'"
    );

    // 들여쓰기 패턴 (라인 시작의 공백)
    private static final Pattern INDENT_PATTERN = Pattern.compile(
        "^([ \\t]+)"
    );

    // Java 키워드 (보호 대상)
    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch",
        "char", "class", "const", "continue", "default", "do", "double",
        "else", "enum", "extends", "final", "finally", "float", "for",
        "goto", "if", "implements", "import", "instanceof", "int", "interface",
        "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized",
        "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
        "true", "false", "null", "var", "record", "sealed", "permits", "yield"
    ));

    private final Map<String, Integer> wordToId;
    private final Map<Integer, String> idToWord;
    private int vocabSize;
    private int nextId;

    /**
     * 기존 vocabulary로 초기화
     */
    public CodeTokenizer(Map<String, Integer> wordToId) {
        this.wordToId = new HashMap<>(wordToId);
        this.idToWord = new HashMap<>();

        for (Map.Entry<String, Integer> entry : wordToId.entrySet()) {
            idToWord.put(entry.getValue(), entry.getKey());
        }

        this.vocabSize = wordToId.size();
        this.nextId = vocabSize;
    }

    /**
     * 코드 텍스트로부터 vocabulary 생성
     */
    public static CodeTokenizer fromCode(String code) {
        Map<String, Integer> wordToId = new HashMap<>();

        // 특수 토큰 추가
        wordToId.put(UNK_TOKEN, UNK_ID);
        int nextId = 1;

        // 줄 단위로 처리
        String[] lines = code.split("\n");
        Set<String> uniqueTokens = new LinkedHashSet<>();

        for (String line : lines) {
            List<String> tokens = tokenizeLine(line);
            uniqueTokens.addAll(tokens);
        }

        // ID 할당
        for (String token : uniqueTokens) {
            if (!wordToId.containsKey(token)) {
                wordToId.put(token, nextId++);
            }
        }

        return new CodeTokenizer(wordToId);
    }

    /**
     * 빈 vocabulary로 초기화
     */
    public static CodeTokenizer empty() {
        Map<String, Integer> wordToId = new HashMap<>();
        wordToId.put(UNK_TOKEN, UNK_ID);
        return new CodeTokenizer(wordToId);
    }

    /**
     * 한 줄을 토큰화
     */
    private static List<String> tokenizeLine(String line) {
        List<String> tokens = new ArrayList<>();

        if (line.isEmpty()) {
            return tokens;
        }

        // 1. 들여쓰기 처리
        Matcher indentMatcher = INDENT_PATTERN.matcher(line);
        String remaining = line;

        if (indentMatcher.find()) {
            String indent = indentMatcher.group(1);
            int level = calculateIndentLevel(indent);
            if (level > 0) {
                tokens.add(INDENT_PREFIX + level);
            }
            remaining = line.substring(indent.length());
        }

        // 2. 문자열 리터럴 보호 (임시 치환)
        Map<String, String> stringMap = new HashMap<>();
        Matcher stringMatcher = STRING_PATTERN.matcher(remaining);
        StringBuffer sb = new StringBuffer();
        int stringIndex = 0;

        while (stringMatcher.find()) {
            String placeholder = "___STRING_" + stringIndex + "___";
            stringMap.put(placeholder, stringMatcher.group());
            stringMatcher.appendReplacement(sb, placeholder);
            stringIndex++;
        }
        stringMatcher.appendTail(sb);
        remaining = sb.toString();

        // 3. 심볼 분리
        remaining = SYMBOL_PATTERN.matcher(remaining).replaceAll(" $1 ");

        // 4. 공백으로 분리
        String[] parts = remaining.trim().split("\\s+");

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // 문자열 리터럴 복원
            if (stringMap.containsKey(part)) {
                tokens.add(stringMap.get(part));
            } else {
                tokens.add(part);
            }
        }

        return tokens;
    }

    /**
     * 들여쓰기 레벨 계산 (4칸 = 1레벨)
     */
    private static int calculateIndentLevel(String indent) {
        int spaces = 0;
        for (char c : indent.toCharArray()) {
            if (c == '\t') {
                spaces += 4;
            } else {
                spaces++;
            }
        }
        return spaces / 4;
    }

    @Override
    public List<Integer> encode(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> result = new ArrayList<>();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            List<String> tokens = tokenizeLine(lines[i]);

            for (String token : tokens) {
                result.add(wordToId.getOrDefault(token, UNK_ID));
            }

            // 줄바꿈 토큰 추가 (마지막 줄 제외)
            if (i < lines.length - 1) {
                int nlId = wordToId.getOrDefault(NEWLINE_TOKEN, UNK_ID);
                if (nlId != UNK_ID) {
                    result.add(nlId);
                }
            }
        }

        return result;
    }

    @Override
    public String decode(List<Integer> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < tokens.size(); i++) {
            String token = idToWord.getOrDefault(tokens.get(i), UNK_TOKEN);

            // 줄바꿈 처리
            if (token.equals(NEWLINE_TOKEN)) {
                sb.append("\n");
                continue;
            }

            // 들여쓰기 처리
            if (token.startsWith(INDENT_PREFIX)) {
                int level = Integer.parseInt(token.substring(INDENT_PREFIX.length()));
                sb.append("    ".repeat(level));
                continue;
            }

            // 심볼 앞에 공백 제거
            if (i > 0 && !needsSpaceBefore(token)) {
                // 이전 토큰 확인
                String prevToken = idToWord.getOrDefault(tokens.get(i - 1), "");
                if (!prevToken.equals(NEWLINE_TOKEN) &&
                    !prevToken.startsWith(INDENT_PREFIX) &&
                    !needsSpaceAfter(prevToken)) {
                    // 공백 없이 붙임
                } else {
                    sb.append(" ");
                }
            } else if (i > 0) {
                String prevToken = idToWord.getOrDefault(tokens.get(i - 1), "");
                if (!prevToken.equals(NEWLINE_TOKEN) && !prevToken.startsWith(INDENT_PREFIX)) {
                    sb.append(" ");
                }
            }

            sb.append(token);
        }

        return sb.toString().trim();
    }

    private boolean needsSpaceBefore(String token) {
        return !token.matches("[)\\]};,.]");
    }

    private boolean needsSpaceAfter(String token) {
        return !token.matches("[({\\[]");
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
     * 토큰 추가 (동적 어휘 확장)
     */
    public void addToken(String token) {
        if (!wordToId.containsKey(token)) {
            wordToId.put(token, nextId);
            idToWord.put(nextId, token);
            nextId++;
            vocabSize++;
        }
    }

    /**
     * 텍스트를 토큰 문자열 리스트로 분리 (디버깅용)
     */
    public List<String> tokenize(String text) {
        List<String> result = new ArrayList<>();
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            result.addAll(tokenizeLine(lines[i]));
            if (i < lines.length - 1) {
                result.add(NEWLINE_TOKEN);
            }
        }

        return result;
    }

    /**
     * Java 키워드 여부 확인
     */
    public static boolean isJavaKeyword(String token) {
        return JAVA_KEYWORDS.contains(token);
    }

    @Override
    public String toString() {
        return String.format("CodeTokenizer(vocab_size=%d)", vocabSize);
    }
}
