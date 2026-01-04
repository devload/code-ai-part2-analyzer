package com.aiprocess.step11;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * STEP 11: 이슈 탐지
 *
 * 핵심 질문: 버그/보안 문제를 어떻게 발견하는가?
 *
 * 코드 스멜보다 심각한 문제들을 탐지합니다:
 * - 보안 취약점
 * - 잠재적 버그
 * - 성능 문제
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ 탐지 가능한 보안 이슈:                                   │
 * │                                                          │
 * │ 1. SQL Injection                                         │
 * │    String query = "SELECT * FROM users WHERE id=" + id;  │
 * │    → PreparedStatement 사용 필요                         │
 * │                                                          │
 * │ 2. 하드코딩된 비밀번호                                    │
 * │    String password = "admin123";                         │
 * │    → 환경변수 또는 Vault 사용                            │
 * │                                                          │
 * │ 3. 취약한 암호화                                          │
 * │    MD5, SHA1 사용                                        │
 * │    → SHA-256 이상 사용                                   │
 * └─────────────────────────────────────────────────────────┘
 */
public class IssueDetector {

    private static final Pattern SQL_PATTERN = Pattern.compile(
        "(?i)(SELECT|INSERT|UPDATE|DELETE|DROP).*\\+.*"
    );

    private static final Set<String> SECRET_KEYWORDS = Set.of(
        "password", "secret", "apikey", "api_key", "token", "credential"
    );

    private static final Set<String> WEAK_CRYPTO = Set.of(
        "MD5", "SHA1", "SHA-1", "DES"
    );

    /**
     * 모든 이슈 탐지
     */
    public List<Issue> detectAll(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();

        issues.addAll(detectSqlInjection(cu));
        issues.addAll(detectHardcodedSecrets(cu));
        issues.addAll(detectWeakCrypto(cu));
        issues.addAll(detectNullPointerRisks(cu));

        return issues;
    }

    /**
     * SQL Injection 탐지
     */
    public List<Issue> detectSqlInjection(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();

        cu.findAll(BinaryExpr.class).forEach(expr -> {
            String exprStr = expr.toString();
            if (SQL_PATTERN.matcher(exprStr).find()) {
                int line = expr.getBegin().map(p -> p.line).orElse(0);
                issues.add(new Issue(
                    IssueType.SQL_INJECTION,
                    line,
                    "SQL Injection 취약점 - PreparedStatement 사용 권장",
                    Severity.CRITICAL
                ));
            }
        });

        return issues;
    }

    /**
     * 하드코딩된 비밀 탐지
     */
    public List<Issue> detectHardcodedSecrets(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();

        cu.findAll(VariableDeclarator.class).forEach(var -> {
            String name = var.getNameAsString().toLowerCase();
            boolean isSecretVar = SECRET_KEYWORDS.stream()
                .anyMatch(name::contains);

            if (isSecretVar && var.getInitializer().isPresent()) {
                var init = var.getInitializer().get();
                if (init instanceof StringLiteralExpr) {
                    int line = var.getBegin().map(p -> p.line).orElse(0);
                    issues.add(new Issue(
                        IssueType.HARDCODED_SECRET,
                        line,
                        "하드코딩된 비밀값 - 환경변수 사용 권장",
                        Severity.CRITICAL
                    ));
                }
            }
        });

        return issues;
    }

    /**
     * 취약한 암호화 탐지
     */
    public List<Issue> detectWeakCrypto(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();

        cu.findAll(MethodCallExpr.class).forEach(call -> {
            String methodStr = call.toString();
            for (String weak : WEAK_CRYPTO) {
                if (methodStr.contains(weak)) {
                    int line = call.getBegin().map(p -> p.line).orElse(0);
                    issues.add(new Issue(
                        IssueType.WEAK_CRYPTO,
                        line,
                        weak + " 사용 - SHA-256 이상 권장",
                        Severity.WARNING
                    ));
                }
            }
        });

        return issues;
    }

    /**
     * NullPointer 위험 탐지
     */
    public List<Issue> detectNullPointerRisks(CompilationUnit cu) {
        List<Issue> issues = new ArrayList<>();

        // 메서드 호출 후 바로 .메서드() 체이닝 (null 체크 없이)
        cu.findAll(MethodCallExpr.class).forEach(call -> {
            if (call.getScope().isPresent()) {
                var scope = call.getScope().get();
                if (scope instanceof MethodCallExpr) {
                    // method().anotherMethod() 패턴
                    MethodCallExpr innerCall = (MethodCallExpr) scope;
                    String methodName = innerCall.getNameAsString();

                    // get으로 시작하거나 find로 시작하는 메서드 후 체이닝
                    if (methodName.startsWith("get") || methodName.startsWith("find")) {
                        int line = call.getBegin().map(p -> p.line).orElse(0);
                        issues.add(new Issue(
                            IssueType.NULL_RISK,
                            line,
                            methodName + "() 결과에 대한 null 체크 필요",
                            Severity.WARNING
                        ));
                    }
                }
            }
        });

        return issues;
    }

    public enum IssueType {
        SQL_INJECTION("SQL Injection"),
        HARDCODED_SECRET("하드코딩된 비밀"),
        WEAK_CRYPTO("취약한 암호화"),
        NULL_RISK("Null 위험"),
        XSS("XSS 취약점"),
        PATH_TRAVERSAL("경로 탐색");

        private final String description;

        IssueType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Severity {
        CRITICAL("🚨", 3),
        WARNING("⚠️", 2),
        INFO("💡", 1);

        private final String emoji;
        private final int weight;

        Severity(String emoji, int weight) {
            this.emoji = emoji;
            this.weight = weight;
        }

        public String getEmoji() { return emoji; }
        public int getWeight() { return weight; }
    }

    public record Issue(
        IssueType type,
        int line,
        String message,
        Severity severity
    ) {}
}
