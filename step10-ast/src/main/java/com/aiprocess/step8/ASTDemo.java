package com.aiprocess.step8;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

/**
 * STEP 8: AST 분석 데모
 */
public class ASTDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 8: AST 분석 (Abstract Syntax Tree)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 코드의 구조를 어떻게 파악하는가?");
        System.out.println();

        // 1. 코드 메트릭 분석
        demoMetrics();

        System.out.println();

        // 2. 복잡도 분석
        demoComplexity();
    }

    private static void demoMetrics() {
        System.out.println("─".repeat(60));
        System.out.println("1. 코드 메트릭 분석");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class UserService {
                private UserRepository repository;
                private EmailService emailService;

                public User findById(Long id) {
                    return repository.findById(id);
                }

                public User save(User user) {
                    User saved = repository.save(user);
                    emailService.sendWelcomeEmail(user.getEmail());
                    return saved;
                }

                public void delete(Long id) {
                    repository.deleteById(id);
                }

                public List<User> findAll() {
                    return repository.findAll();
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        ASTAnalyzer analyzer = new ASTAnalyzer();
        ASTAnalyzer.ASTMetrics metrics = analyzer.analyze(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 코드 메트릭                              │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();
        System.out.printf("  📦 클래스 수:      %d%n", metrics.classCount());
        System.out.printf("  📌 메서드 수:      %d%n", metrics.methodCount());
        System.out.printf("  🔧 필드 수:        %d%n", metrics.fieldCount());
        System.out.printf("  📄 라인 수:        %d%n", metrics.lineCount());
        System.out.printf("  📊 중첩 깊이:      %d%n", metrics.maxNestingDepth());
        System.out.printf("  🔀 순환 복잡도:    %d%n", metrics.cyclomaticComplexity());
        System.out.println();

        System.out.println("  📌 메서드별 정보:");
        for (ASTAnalyzer.MethodInfo method : metrics.methods()) {
            System.out.printf("     - %s(): %d줄, 파라미터 %d개, 복잡도 %d%n",
                method.name(), method.lineCount(), method.paramCount(), method.complexity());
        }
    }

    private static void demoComplexity() {
        System.out.println("─".repeat(60));
        System.out.println("2. 복잡도 분석");
        System.out.println("─".repeat(60));
        System.out.println();

        // 복잡한 코드
        String complexCode = """
            public class ComplexProcessor {
                public void process(Data data) {
                    if (data != null) {
                        for (Item item : data.getItems()) {
                            if (item.isActive()) {
                                try {
                                    if (item.getType() == Type.A) {
                                        while (item.hasNext()) {
                                            // 깊은 중첩
                                        }
                                    } else if (item.getType() == Type.B) {
                                        // 또 다른 분기
                                    }
                                } catch (Exception e) {
                                    // 예외 처리
                                }
                            }
                        }
                    }
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(complexCode);
        ASTAnalyzer analyzer = new ASTAnalyzer();
        ASTAnalyzer.ASTMetrics metrics = analyzer.analyze(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 복잡한 코드 분석                         │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();
        System.out.printf("  📊 중첩 깊이:      %d", metrics.maxNestingDepth());
        if (metrics.maxNestingDepth() > 3) {
            System.out.println(" ⚠️ 너무 깊음!");
        } else {
            System.out.println(" ✅ 적정");
        }

        System.out.printf("  🔀 순환 복잡도:    %d", metrics.cyclomaticComplexity());
        if (metrics.cyclomaticComplexity() > 10) {
            System.out.println(" ⚠️ 너무 복잡함!");
        } else {
            System.out.println(" ✅ 적정");
        }

        System.out.println();
        System.out.println("  💡 복잡도 기준:");
        System.out.println("     - 중첩 깊이: 3 이하 권장");
        System.out.println("     - 순환 복잡도: 10 이하 권장");
        System.out.println();
        System.out.println("  💡 AST 분석이 중요한 이유:");
        System.out.println("     - 코드 품질을 수치로 측정 가능");
        System.out.println("     - 리팩토링이 필요한 부분 식별");
        System.out.println("     - 자동화된 코드 리뷰 가능");
    }
}
