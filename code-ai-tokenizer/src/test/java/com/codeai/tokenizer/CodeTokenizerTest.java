package com.codeai.tokenizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CodeTokenizer 테스트
 *
 * 핵심 테스트:
 * 1. 심볼 분리가 되는가?
 * 2. 들여쓰기가 압축되는가?
 * 3. 키워드가 보호되는가?
 */
public class CodeTokenizerTest {

    @Test
    @DisplayName("심볼이 독립 토큰으로 분리된다")
    void testSymbolSeparation() {
        String code = "public void getName()";
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Input: " + code);
        System.out.println("Tokens: " + tokens);

        // getName과 ()가 분리되어야 함
        assertTrue(tokens.contains("getName"), "getName should be separate");
        assertTrue(tokens.contains("("), "( should be separate");
        assertTrue(tokens.contains(")"), ") should be separate");

        // getName()가 하나로 붙어있으면 안 됨
        assertFalse(tokens.contains("getName()"), "getName() should NOT be together");
    }

    @Test
    @DisplayName("들여쓰기가 INDENT_N 토큰으로 압축된다")
    void testIndentationCompression() {
        String code = "    if (true) {";  // 4칸 들여쓰기
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Input: " + code);
        System.out.println("Tokens: " + tokens);

        // 4칸 = 1레벨
        assertTrue(tokens.contains("INDENT_1"), "Should have INDENT_1 token");

        // 4개의 개별 공백 토큰이 있으면 안 됨
        long spaceCount = tokens.stream().filter(t -> t.equals(" ")).count();
        assertEquals(0, spaceCount, "Should not have individual space tokens");
    }

    @Test
    @DisplayName("8칸 들여쓰기는 INDENT_2가 된다")
    void testDoubleIndentation() {
        String code = "        return true;";  // 8칸 들여쓰기
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Input: " + code);
        System.out.println("Tokens: " + tokens);

        assertTrue(tokens.contains("INDENT_2"), "Should have INDENT_2 token");
    }

    @Test
    @DisplayName("중괄호가 분리된다")
    void testBraceSeparation() {
        String code = "public class User {";
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Input: " + code);
        System.out.println("Tokens: " + tokens);

        assertTrue(tokens.contains("{"), "{ should be separate");
        assertTrue(tokens.contains("User"), "User should be separate");
    }

    @Test
    @DisplayName("문자열 리터럴이 보존된다")
    void testStringLiteralPreservation() {
        String code = "String name = \"Hello World\";";
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Input: " + code);
        System.out.println("Tokens: " + tokens);

        // 문자열이 하나의 토큰으로 유지되어야 함
        assertTrue(tokens.contains("\"Hello World\""), "String literal should be preserved");
    }

    @Test
    @DisplayName("Java 키워드가 올바르게 인식된다")
    void testJavaKeywords() {
        assertTrue(CodeTokenizer.isJavaKeyword("public"));
        assertTrue(CodeTokenizer.isJavaKeyword("class"));
        assertTrue(CodeTokenizer.isJavaKeyword("if"));
        assertTrue(CodeTokenizer.isJavaKeyword("return"));

        assertFalse(CodeTokenizer.isJavaKeyword("User"));
        assertFalse(CodeTokenizer.isJavaKeyword("getName"));
    }

    @Test
    @DisplayName("encode/decode 왕복 테스트")
    void testEncodeDecode() {
        String code = "public class User {\n    private String name;\n}";
        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);

        // 줄바꿈 토큰 추가
        tokenizer.addToken(CodeTokenizer.NEWLINE_TOKEN);

        List<Integer> encoded = tokenizer.encode(code);
        String decoded = tokenizer.decode(encoded);

        System.out.println("Original: " + code);
        System.out.println("Encoded: " + encoded);
        System.out.println("Decoded: " + decoded);

        // 기본 구조가 유지되어야 함
        assertTrue(decoded.contains("public"));
        assertTrue(decoded.contains("class"));
        assertTrue(decoded.contains("User"));
        assertTrue(decoded.contains("private"));
        assertTrue(decoded.contains("String"));
        assertTrue(decoded.contains("name"));
    }

    @Test
    @DisplayName("토큰 수 비교: WhitespaceTokenizer vs CodeTokenizer")
    void testTokenCountComparison() {
        String code = """
            public class User {
                private String name;

                public String getName() {
                    return name;
                }
            }
            """;

        CodeTokenizer tokenizer = CodeTokenizer.fromCode(code);
        List<String> tokens = tokenizer.tokenize(code);

        System.out.println("Code lines: " + code.split("\n").length);
        System.out.println("Total tokens: " + tokens.size());
        System.out.println("Tokens: " + tokens);

        // 들여쓰기 압축 확인
        long indentTokens = tokens.stream()
            .filter(t -> t.startsWith("INDENT_"))
            .count();

        System.out.println("Indent tokens: " + indentTokens);

        // 들여쓰기가 압축되었다면 INDENT_N 토큰이 존재해야 함
        assertTrue(indentTokens > 0, "Should have compressed indent tokens");
    }

    @Test
    @DisplayName("빈 vocabulary 생성")
    void testEmptyTokenizer() {
        CodeTokenizer tokenizer = CodeTokenizer.empty();

        assertEquals(1, tokenizer.vocabSize()); // UNK만 포함
        assertEquals("[UNK]", tokenizer.getVocabulary().keySet().iterator().next());
    }

    @Test
    @DisplayName("동적 토큰 추가")
    void testAddToken() {
        CodeTokenizer tokenizer = CodeTokenizer.empty();
        int initialSize = tokenizer.vocabSize();

        tokenizer.addToken("public");
        tokenizer.addToken("class");

        assertEquals(initialSize + 2, tokenizer.vocabSize());
        assertTrue(tokenizer.getVocabulary().containsKey("public"));
        assertTrue(tokenizer.getVocabulary().containsKey("class"));
    }
}
