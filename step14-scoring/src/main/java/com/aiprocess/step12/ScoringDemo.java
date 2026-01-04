package com.aiprocess.step12;

import java.util.*;

/**
 * STEP 12: 점수화 데모
 */
public class ScoringDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 12: 점수화 (Scoring)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 코드 품질을 어떻게 측정하는가?");
        System.out.println();

        // 점수 계산 데모
        demoScoring();
    }

    private static void demoScoring() {
        System.out.println("─".repeat(60));
        System.out.println("코드 품질 점수 산정");
        System.out.println("─".repeat(60));
        System.out.println();

        // 시뮬레이션: 탐지된 이슈들
        List<CodeScorer.ScoreInput.Issue> issues = List.of(
            new CodeScorer.ScoreInput.Issue("SYSTEM_OUT", "WARNING", "System.out.println 사용"),
            new CodeScorer.ScoreInput.Issue("MAGIC_NUMBER", "WARNING", "매직 넘버 3600 사용"),
            new CodeScorer.ScoreInput.Issue("EMPTY_CATCH", "CRITICAL", "빈 catch 블록"),
            new CodeScorer.ScoreInput.Issue("SQL_INJECTION", "CRITICAL", "SQL Injection 취약점")
        );

        CodeScorer.ScoreInput input = new CodeScorer.ScoreInput(
            issues,
            150,  // lineCount
            8,    // methodCount
            3,    // maxNestingDepth
            12    // cyclomaticComplexity
        );

        CodeScorer scorer = new CodeScorer();
        CodeScorer.ScoreResult result = scorer.calculateScore(input);

        // 결과 출력
        System.out.println("  ╔════════════════════════════════════════════════════════╗");
        System.out.println("  ║              🤖 AI 코드 분석 결과                       ║");
        System.out.println("  ╠════════════════════════════════════════════════════════╣");
        System.out.printf("  ║  📊 총점: %d/100점  등급: %s %s (%s)%s║%n",
            result.totalScore(),
            result.grade().getEmoji(),
            result.grade().name(),
            result.grade().getDescription(),
            " ".repeat(22 - result.grade().getDescription().length()));
        System.out.println("  ╚════════════════════════════════════════════════════════╝");
        System.out.println();

        // 카테고리별 점수
        System.out.println("  📈 카테고리별 점수:");
        System.out.println();

        for (CodeScorer.Category cat : CodeScorer.Category.values()) {
            int score = result.categoryScores().get(cat);
            int max = cat.getMaxScore();
            int barLen = (int) ((double) score / max * 20);
            int emptyLen = 20 - barLen;

            String bar = "█".repeat(barLen) + "░".repeat(emptyLen);
            System.out.printf("     %-10s %s %2d/%2d%n",
                cat.getKorean(), bar, score, max);
        }
        System.out.println();

        // 발견된 이슈
        System.out.println("  🔍 발견된 이슈 (" + issues.size() + "개):");
        System.out.println();

        for (CodeScorer.ScoreInput.Issue issue : issues) {
            String emoji = issue.severity().equals("CRITICAL") ? "🚨" :
                          issue.severity().equals("WARNING") ? "⚠️" : "💡";
            System.out.printf("     %s [%s] %s%n",
                emoji, issue.severity(), issue.message());
        }
        System.out.println();

        // 잘한 점
        if (!result.positives().isEmpty()) {
            System.out.println("  ✨ 잘한 점:");
            for (String positive : result.positives()) {
                System.out.println("     • " + positive);
            }
            System.out.println();
        }

        // 개선 권고
        System.out.println("  💡 개선 권고:");
        result.categoryIssues().forEach((cat, catIssues) -> {
            if (!catIssues.isEmpty()) {
                System.out.println("     " + cat.getKorean() + ":");
                for (String issue : catIssues) {
                    System.out.println("       - " + issue);
                }
            }
        });
        System.out.println();

        // 등급 기준 설명
        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 등급 기준                                │");
        System.out.println("  ├─────────────────────────────────────────┤");
        System.out.println("  │ 🏆 A: 90-100점 (우수)                    │");
        System.out.println("  │ 👍 B: 80-89점  (양호)                    │");
        System.out.println("  │ 😐 C: 70-79점  (보통)                    │");
        System.out.println("  │ ⚠️  D: 60-69점  (미흡)                    │");
        System.out.println("  │ 🚨 F: 0-59점   (불량)                    │");
        System.out.println("  └─────────────────────────────────────────┘");
    }
}
