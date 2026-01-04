package com.aiprocess.step10;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * STEP 10: 패턴 매칭 데모
 */
public class PatternDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 10: 패턴 매칭 (Pattern Matching)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 나쁜 코드를 어떻게 찾는가?");
        System.out.println();

        // 1. 코드 스멜 탐지
        demoCodeSmellDetection();
    }

    private static void demoCodeSmellDetection() {
        System.out.println("─".repeat(60));
        System.out.println("코드 스멜 자동 탐지");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class BadExample {
                public void processData(String a, String b, String c, String d) {
                    // 매직 넘버
                    int timeout = 3600;
                    int maxRetry = 5;

                    // System.out 사용
                    System.out.println("Processing...");

                    // 빈 catch 블록
                    try {
                        doSomething();
                    } catch (Exception e) {
                    }

                    // 중괄호 없는 if
                    if (a != null)
                        process(a);

                    // 또 다른 매직 넘버
                    for (int i = 0; i < 100; i++) {
                        System.out.println(i);
                    }
                }

                private void doSomething() {}
                private void process(String s) {}
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
        PatternMatcher matcher = new PatternMatcher();
        List<PatternMatcher.CodeSmell> smells = matcher.findSmells(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 탐지된 코드 스멜: " + smells.size() + "개" + " ".repeat(21) + "│");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        // 심각도별 그룹화
        Map<PatternMatcher.Severity, List<PatternMatcher.CodeSmell>> bySeverity =
            smells.stream().collect(Collectors.groupingBy(PatternMatcher.CodeSmell::severity));

        // 심각도 순서대로 출력
        for (PatternMatcher.Severity severity : PatternMatcher.Severity.values()) {
            List<PatternMatcher.CodeSmell> group = bySeverity.get(severity);
            if (group != null && !group.isEmpty()) {
                System.out.println("  " + severity.getEmoji() + " " + severity.name() + ":");
                for (PatternMatcher.CodeSmell smell : group) {
                    System.out.printf("     Line %d: [%s] %s%n",
                        smell.line(), smell.type().name(), smell.message());
                }
                System.out.println();
            }
        }

        // 통계
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 코드 스멜 유형별 통계                    │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        smells.stream()
            .collect(Collectors.groupingBy(PatternMatcher.CodeSmell::type, Collectors.counting()))
            .forEach((type, count) -> {
                int barLen = (int) (count * 5);
                System.out.printf("  %-15s %s %d개%n",
                    type.getDescription(), "█".repeat(barLen), count);
            });

        System.out.println();
        System.out.println("  💡 코드 스멜 해결 방법:");
        System.out.println("     - System.out → Logger 사용");
        System.out.println("     - 빈 catch → 로깅 또는 예외 전파");
        System.out.println("     - 매직 넘버 → 상수 정의");
        System.out.println("     - 긴 메서드 → 함수 분리");
        System.out.println("     - 파라미터 과다 → 객체로 묶기");
    }
}
