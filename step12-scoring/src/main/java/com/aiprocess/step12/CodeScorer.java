package com.aiprocess.step12;

import java.util.*;

/**
 * STEP 12: 점수화
 *
 * 핵심 질문: 코드 품질을 어떻게 측정하는가?
 *
 * 탐지된 이슈들을 종합하여 코드 품질 점수를 산정합니다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ 코드 품질 점수 체계 (100점 만점)                         │
 * │                                                          │
 * │ 카테고리별 배점:                                         │
 * │   - 구조 (Structure)      : 20점                         │
 * │   - 가독성 (Readability)  : 20점                         │
 * │   - 유지보수성 (Maintainability): 20점                   │
 * │   - 신뢰성 (Reliability)  : 15점                         │
 * │   - 보안 (Security)       : 15점                         │
 * │   - 성능 (Performance)    : 10점                         │
 * │                                                          │
 * │ 등급:                                                    │
 * │   A: 90-100점 | B: 80-89점 | C: 70-79점                  │
 * │   D: 60-69점  | F: 0-59점                                │
 * └─────────────────────────────────────────────────────────┘
 */
public class CodeScorer {

    /**
     * 점수 계산
     */
    public ScoreResult calculateScore(ScoreInput input) {
        Map<Category, Integer> categoryScores = new HashMap<>();
        Map<Category, List<String>> categoryIssues = new HashMap<>();

        // 초기화
        for (Category cat : Category.values()) {
            categoryScores.put(cat, cat.getMaxScore());
            categoryIssues.put(cat, new ArrayList<>());
        }

        // 이슈별 감점
        for (ScoreInput.Issue issue : input.issues()) {
            Category category = mapToCategory(issue.type());
            int penalty = calculatePenalty(issue.severity());

            int current = categoryScores.get(category);
            categoryScores.put(category, Math.max(0, current - penalty));
            categoryIssues.get(category).add(issue.message());
        }

        // 메트릭 기반 감점
        applyMetricPenalties(input, categoryScores, categoryIssues);

        // 총점 계산
        int totalScore = categoryScores.values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        // 등급 결정
        Grade grade = determineGrade(totalScore);

        // 잘한 점 추출
        List<String> positives = findPositives(input, categoryScores);

        return new ScoreResult(
            totalScore,
            grade,
            categoryScores,
            categoryIssues,
            positives
        );
    }

    private Category mapToCategory(String issueType) {
        return switch (issueType.toUpperCase()) {
            case "SQL_INJECTION", "HARDCODED_SECRET", "WEAK_CRYPTO", "XSS" -> Category.SECURITY;
            case "NULL_RISK", "EMPTY_CATCH" -> Category.RELIABILITY;
            case "LONG_METHOD", "TOO_MANY_PARAMS", "DEEP_NESTING" -> Category.MAINTAINABILITY;
            case "SYSTEM_OUT", "MAGIC_NUMBER", "MISSING_BRACES" -> Category.READABILITY;
            default -> Category.STRUCTURE;
        };
    }

    private int calculatePenalty(String severity) {
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> 10;
            case "WARNING" -> 5;
            case "INFO" -> 2;
            default -> 1;
        };
    }

    private void applyMetricPenalties(ScoreInput input,
                                       Map<Category, Integer> scores,
                                       Map<Category, List<String>> issues) {
        // 중첩 깊이 감점
        if (input.maxNestingDepth() > 4) {
            int penalty = (input.maxNestingDepth() - 4) * 3;
            scores.merge(Category.MAINTAINABILITY, -penalty, Integer::sum);
            issues.get(Category.MAINTAINABILITY).add("중첩 깊이: " + input.maxNestingDepth());
        }

        // 순환 복잡도 감점
        if (input.cyclomaticComplexity() > 15) {
            int penalty = (input.cyclomaticComplexity() - 15) * 2;
            scores.merge(Category.MAINTAINABILITY, -penalty, Integer::sum);
            issues.get(Category.MAINTAINABILITY).add("순환 복잡도: " + input.cyclomaticComplexity());
        }

        // 긴 파일 감점
        if (input.lineCount() > 300) {
            int penalty = (input.lineCount() - 300) / 50;
            scores.merge(Category.STRUCTURE, -penalty, Integer::sum);
            issues.get(Category.STRUCTURE).add("파일이 너무 김: " + input.lineCount() + "줄");
        }
    }

    private Grade determineGrade(int score) {
        if (score >= 90) return Grade.A;
        if (score >= 80) return Grade.B;
        if (score >= 70) return Grade.C;
        if (score >= 60) return Grade.D;
        return Grade.F;
    }

    private List<String> findPositives(ScoreInput input, Map<Category, Integer> scores) {
        List<String> positives = new ArrayList<>();

        if (scores.get(Category.SECURITY) >= 14) {
            positives.add("보안 이슈 없음");
        }
        if (input.maxNestingDepth() <= 2) {
            positives.add("코드 중첩이 적절함");
        }
        if (input.cyclomaticComplexity() <= 10) {
            positives.add("복잡도가 낮음");
        }
        if (input.methodCount() > 0 && input.lineCount() / input.methodCount() < 15) {
            positives.add("메서드가 적절히 분리됨");
        }

        return positives;
    }

    public enum Category {
        STRUCTURE("구조", 20),
        READABILITY("가독성", 20),
        MAINTAINABILITY("유지보수성", 20),
        RELIABILITY("신뢰성", 15),
        SECURITY("보안", 15),
        PERFORMANCE("성능", 10);

        private final String korean;
        private final int maxScore;

        Category(String korean, int maxScore) {
            this.korean = korean;
            this.maxScore = maxScore;
        }

        public String getKorean() { return korean; }
        public int getMaxScore() { return maxScore; }
    }

    public enum Grade {
        A("우수", "🏆"),
        B("양호", "👍"),
        C("보통", "😐"),
        D("미흡", "⚠️"),
        F("불량", "🚨");

        private final String description;
        private final String emoji;

        Grade(String description, String emoji) {
            this.description = description;
            this.emoji = emoji;
        }

        public String getDescription() { return description; }
        public String getEmoji() { return emoji; }
    }

    public record ScoreInput(
        List<Issue> issues,
        int lineCount,
        int methodCount,
        int maxNestingDepth,
        int cyclomaticComplexity
    ) {
        public record Issue(String type, String severity, String message) {}
    }

    public record ScoreResult(
        int totalScore,
        Grade grade,
        Map<Category, Integer> categoryScores,
        Map<Category, List<String>> categoryIssues,
        List<String> positives
    ) {}
}
