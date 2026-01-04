package com.aiprocess.step10;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.expr.*;

import java.util.*;

/**
 * STEP 10: 패턴 매칭
 *
 * 핵심 질문: 나쁜 코드를 어떻게 찾는가?
 *
 * 코드 스멜(Code Smell)은 문제가 될 수 있는 코드 패턴입니다.
 * AST를 분석하여 이런 패턴을 자동으로 탐지합니다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ 탐지 가능한 코드 스멜:                                   │
 * │                                                          │
 * │ 1. System.out.println 사용                               │
 * │    → 프로덕션에서는 Logger 사용 권장                     │
 * │                                                          │
 * │ 2. 빈 catch 블록                                         │
 * │    → 예외를 무시하면 디버깅 어려움                       │
 * │                                                          │
 * │ 3. 매직 넘버                                             │
 * │    → 의미 없는 숫자는 상수로 정의                        │
 * │                                                          │
 * │ 4. 너무 긴 메서드                                        │
 * │    → 20줄 이상은 분리 고려                               │
 * │                                                          │
 * │ 5. 너무 많은 파라미터                                    │
 * │    → 3개 이상은 객체로 묶기 고려                         │
 * └─────────────────────────────────────────────────────────┘
 */
public class PatternMatcher {

    /**
     * 모든 코드 스멜 탐지
     */
    public List<CodeSmell> findSmells(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        smells.addAll(findSystemOut(cu));
        smells.addAll(findEmptyCatch(cu));
        smells.addAll(findMagicNumbers(cu));
        smells.addAll(findLongMethods(cu));
        smells.addAll(findTooManyParameters(cu));
        smells.addAll(findMissingBraces(cu));

        return smells;
    }

    /**
     * System.out.println 사용 탐지
     */
    public List<CodeSmell> findSystemOut(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.toString().startsWith("System.out.") ||
                call.toString().startsWith("System.err.")) {
                int line = call.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.SYSTEM_OUT,
                    line,
                    "System.out 대신 Logger 사용 권장",
                    Severity.WARNING
                ));
            }
        });

        return smells;
    }

    /**
     * 빈 catch 블록 탐지
     */
    public List<CodeSmell> findEmptyCatch(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(CatchClause.class).forEach(catchClause -> {
            BlockStmt body = catchClause.getBody();
            if (body.getStatements().isEmpty()) {
                int line = catchClause.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.EMPTY_CATCH,
                    line,
                    "빈 catch 블록 - 예외를 무시하면 안 됩니다",
                    Severity.CRITICAL
                ));
            }
        });

        return smells;
    }

    /**
     * 매직 넘버 탐지
     */
    public List<CodeSmell> findMagicNumbers(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();
        Set<Integer> allowedNumbers = Set.of(-1, 0, 1, 2);

        cu.findAll(IntegerLiteralExpr.class).forEach(literal -> {
            int value = literal.asNumber().intValue();
            if (!allowedNumbers.contains(value)) {
                int line = literal.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.MAGIC_NUMBER,
                    line,
                    "매직 넘버 " + value + " - 상수로 정의하세요",
                    Severity.WARNING
                ));
            }
        });

        return smells;
    }

    /**
     * 너무 긴 메서드 탐지
     */
    public List<CodeSmell> findLongMethods(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();
        int threshold = 20;

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int lines = method.toString().split("\n").length;
            if (lines > threshold) {
                int line = method.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.LONG_METHOD,
                    line,
                    method.getNameAsString() + "(): " + lines + "줄 (권장: " + threshold + "줄 이하)",
                    Severity.WARNING
                ));
            }
        });

        return smells;
    }

    /**
     * 너무 많은 파라미터 탐지
     */
    public List<CodeSmell> findTooManyParameters(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();
        int threshold = 3;

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int paramCount = method.getParameters().size();
            if (paramCount > threshold) {
                int line = method.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.TOO_MANY_PARAMS,
                    line,
                    method.getNameAsString() + "(): 파라미터 " + paramCount + "개 (권장: " + threshold + "개 이하)",
                    Severity.INFO
                ));
            }
        });

        return smells;
    }

    /**
     * if문 중괄호 누락 탐지
     */
    public List<CodeSmell> findMissingBraces(CompilationUnit cu) {
        List<CodeSmell> smells = new ArrayList<>();

        cu.findAll(IfStmt.class).forEach(ifStmt -> {
            if (!(ifStmt.getThenStmt() instanceof BlockStmt)) {
                int line = ifStmt.getBegin().map(p -> p.line).orElse(0);
                smells.add(new CodeSmell(
                    SmellType.MISSING_BRACES,
                    line,
                    "if문에 중괄호 없음",
                    Severity.INFO
                ));
            }
        });

        return smells;
    }

    /**
     * 코드 스멜 유형
     */
    public enum SmellType {
        SYSTEM_OUT("System.out 사용"),
        EMPTY_CATCH("빈 catch 블록"),
        MAGIC_NUMBER("매직 넘버"),
        LONG_METHOD("긴 메서드"),
        TOO_MANY_PARAMS("파라미터 과다"),
        MISSING_BRACES("중괄호 누락"),
        DEEP_NESTING("깊은 중첩");

        private final String description;

        SmellType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 심각도
     */
    public enum Severity {
        CRITICAL("🚨"),
        WARNING("⚠️"),
        INFO("💡");

        private final String emoji;

        Severity(String emoji) {
            this.emoji = emoji;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    /**
     * 코드 스멜 정보
     */
    public record CodeSmell(
        SmellType type,
        int line,
        String message,
        Severity severity
    ) {}
}
