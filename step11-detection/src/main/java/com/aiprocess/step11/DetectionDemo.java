package com.aiprocess.step11;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.util.List;

/**
 * STEP 11: 이슈 탐지 데모
 */
public class DetectionDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 11: 이슈 탐지 (Issue Detection)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 버그/보안 문제를 어떻게 발견하는가?");
        System.out.println();

        // 보안 이슈 탐지
        demoSecurityIssues();
    }

    private static void demoSecurityIssues() {
        System.out.println("─".repeat(60));
        System.out.println("보안 이슈 자동 탐지");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class VulnerableCode {
                private String password = "admin123";
                private String apiKey = "sk-1234567890";

                public User findUser(String userId) {
                    // SQL Injection 취약점!
                    String query = "SELECT * FROM users WHERE id=" + userId;
                    return db.execute(query);
                }

                public String hashPassword(String pass) {
                    // 취약한 해시 알고리즘
                    return MessageDigest.getInstance("MD5")
                        .digest(pass.getBytes());
                }

                public void processUser(String id) {
                    // Null 체크 없는 체이닝
                    String name = findUser(id).getName().toUpperCase();
                }
            }
            """;

        System.out.println("  분석할 코드:");
        System.out.println("  " + "─".repeat(50));
        int lineNum = 1;
        for (String line : code.split("\n")) {
            System.out.printf("  %3d │ %s%n", lineNum++, line);
        }
        System.out.println("  " + "─".repeat(50));
        System.out.println();

        CompilationUnit cu = StaticJavaParser.parse(code);
        IssueDetector detector = new IssueDetector();
        List<IssueDetector.Issue> issues = detector.detectAll(cu);

        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │ 탐지된 보안 이슈: " + issues.size() + "개" + " ".repeat(29) + "│");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.println();

        // 심각도별 출력
        int criticalCount = 0, warningCount = 0, infoCount = 0;

        for (IssueDetector.Issue issue : issues) {
            System.out.printf("  %s [%s] Line %d%n",
                issue.severity().getEmoji(),
                issue.type().name(),
                issue.line());
            System.out.println("     → " + issue.message());
            System.out.println();

            switch (issue.severity()) {
                case CRITICAL -> criticalCount++;
                case WARNING -> warningCount++;
                case INFO -> infoCount++;
            }
        }

        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │ 요약                                            │");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  🚨 CRITICAL: " + criticalCount + "개");
        System.out.println("  ⚠️  WARNING:  " + warningCount + "개");
        System.out.println("  💡 INFO:     " + infoCount + "개");
        System.out.println();

        // 해결 방법 안내
        System.out.println("  ┌─────────────────────────────────────────────────┐");
        System.out.println("  │ 해결 방법                                       │");
        System.out.println("  └─────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  SQL Injection:");
        System.out.println("    ❌ \"SELECT * FROM users WHERE id=\" + userId");
        System.out.println("    ✅ PreparedStatement: \"SELECT * FROM users WHERE id=?\"");
        System.out.println();
        System.out.println("  하드코딩된 비밀:");
        System.out.println("    ❌ String password = \"admin123\";");
        System.out.println("    ✅ String password = System.getenv(\"DB_PASSWORD\");");
        System.out.println();
        System.out.println("  취약한 해시:");
        System.out.println("    ❌ MessageDigest.getInstance(\"MD5\")");
        System.out.println("    ✅ MessageDigest.getInstance(\"SHA-256\")");
    }
}
