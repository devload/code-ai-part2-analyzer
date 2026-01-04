package com.aiprocess.step7;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ParseProblemException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * STEP 7: 코드 파싱
 *
 * 핵심 질문: 코드를 어떻게 읽는가?
 *
 * AI가 코드를 분석하려면 먼저 코드를 "이해"해야 합니다.
 * 단순 텍스트가 아닌 구조화된 형태로 파싱합니다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ 원본 코드 (텍스트)                                       │
 * │ ┌─────────────────────────────────────────┐             │
 * │ │ public class User {                     │             │
 * │ │     private String name;                │             │
 * │ │     public void setName(String n) {     │             │
 * │ │         this.name = n;                  │             │
 * │ │     }                                   │             │
 * │ │ }                                       │             │
 * │ └─────────────────────────────────────────┘             │
 * │                     │                                    │
 * │                     ▼ 파싱                               │
 * │ ┌─────────────────────────────────────────┐             │
 * │ │ CompilationUnit (AST)                   │             │
 * │ │  └── ClassDeclaration: User             │             │
 * │ │       ├── FieldDeclaration: name        │             │
 * │ │       └── MethodDeclaration: setName    │             │
 * │ │            └── Parameter: n             │             │
 * │ └─────────────────────────────────────────┘             │
 * └─────────────────────────────────────────────────────────┘
 */
public class CodeParser {

    /**
     * 코드 문자열 파싱
     *
     * @param code Java 코드 문자열
     * @return 파싱된 CompilationUnit
     */
    public ParseResult parse(String code) {
        long startTime = System.currentTimeMillis();

        try {
            CompilationUnit cu = StaticJavaParser.parse(code);
            long latency = System.currentTimeMillis() - startTime;

            return new ParseResult(
                true,
                Optional.of(cu),
                Optional.empty(),
                latency
            );
        } catch (ParseProblemException e) {
            long latency = System.currentTimeMillis() - startTime;

            return new ParseResult(
                false,
                Optional.empty(),
                Optional.of(e.getMessage()),
                latency
            );
        }
    }

    /**
     * 파일에서 파싱
     */
    public ParseResult parseFile(Path filePath) throws IOException {
        String code = Files.readString(filePath);
        return parse(code);
    }

    /**
     * 파싱 결과
     */
    public record ParseResult(
        boolean success,
        Optional<CompilationUnit> ast,
        Optional<String> error,
        long latencyMs
    ) {
        public CompilationUnit getAST() {
            return ast.orElseThrow(() -> new IllegalStateException("파싱 실패: " + error.orElse("unknown")));
        }
    }
}
