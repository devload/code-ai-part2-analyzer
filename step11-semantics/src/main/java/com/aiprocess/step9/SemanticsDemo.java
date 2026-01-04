package com.aiprocess.step9;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.util.Map;

/**
 * STEP 9: 의미 분석 데모
 */
public class SemanticsDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 9: 의미 분석 (Semantic Analysis)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 변수/타입을 어떻게 추적하는가?");
        System.out.println();

        // 1. 변수 사용 분석
        demoVariableUsage();

        System.out.println();

        // 2. 메서드 호출 분석
        demoMethodCalls();

        System.out.println();

        // 3. 타입 사용 분석
        demoTypeUsage();
    }

    private static void demoVariableUsage() {
        System.out.println("─".repeat(60));
        System.out.println("1. 변수 사용 분석");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class UserService {
                private UserRepository repository;
                private EmailService emailService;
                private Logger logger;  // 사용되지 않음

                public User findById(Long id) {
                    return repository.findById(id);
                }

                public void sendEmail(User user) {
                    emailService.send(user.getEmail());
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticAnalyzer.VariableUsage usage = analyzer.analyzeVariables(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 필드 사용 분석                          │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        System.out.println("  📌 필드별 사용 위치:");
        usage.fieldUsage().forEach((field, methods) -> {
            if (methods.isEmpty()) {
                System.out.println("     - " + field + ": ⚠️ 미사용");
            } else {
                System.out.println("     - " + field + ": " + String.join(", ", methods) + "()");
            }
        });
        System.out.println();

        if (!usage.unusedVariables().isEmpty()) {
            System.out.println("  ⚠️ 미사용 필드 발견:");
            usage.unusedVariables().forEach(var -> {
                System.out.println("     - " + var);
            });
        }
    }

    private static void demoMethodCalls() {
        System.out.println("─".repeat(60));
        System.out.println("2. 메서드 호출 분석");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class OrderService {
                public Order createOrder(Cart cart) {
                    validateCart(cart);
                    Order order = buildOrder(cart);
                    saveOrder(order);
                    sendConfirmation(order);
                    return order;
                }

                private void validateCart(Cart cart) {
                    checkItems(cart);
                    checkStock(cart);
                }

                private Order buildOrder(Cart cart) {
                    return new Order(cart);
                }

                private void saveOrder(Order order) {
                    repository.save(order);
                }

                private void sendConfirmation(Order order) {
                    emailService.send(order);
                }

                private void checkItems(Cart cart) {}
                private void checkStock(Cart cart) {}
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticAnalyzer.MethodCallAnalysis analysis = analyzer.analyzeMethodCalls(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 메서드 호출 관계                        │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        System.out.println("  📌 메서드별 호출:");
        analysis.methodCalls().forEach((method, calls) -> {
            if (!calls.isEmpty()) {
                System.out.println("     " + method + "() → " + String.join(", ", calls));
            }
        });
        System.out.println();

        System.out.println("  📊 호출 빈도:");
        analysis.callCount().entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> {
                int barLen = Math.min(entry.getValue() * 3, 20);
                System.out.printf("     %-15s %s (%d회)%n",
                    entry.getKey(), "█".repeat(barLen), entry.getValue());
            });
    }

    private static void demoTypeUsage() {
        System.out.println("─".repeat(60));
        System.out.println("3. 타입 사용 분석");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            import java.util.List;
            import java.util.Map;

            public class DataProcessor {
                private String name;
                private int count;
                private List<String> items;
                private Map<String, Integer> cache;

                public String process(String input) {
                    return input.toUpperCase();
                }

                public int calculate(int a, int b) {
                    return a + b;
                }

                public List<String> getItems() {
                    return items;
                }
            }
            """;

        CompilationUnit cu = StaticJavaParser.parse(code);
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        SemanticAnalyzer.TypeUsage usage = analyzer.analyzeTypes(cu);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 타입 사용 분석                          │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println();

        System.out.println("  📌 사용된 타입:");
        usage.typeCount().entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> {
                System.out.printf("     %-20s: %d회%n", entry.getKey(), entry.getValue());
            });
        System.out.println();

        System.out.println("  📦 Import:");
        usage.imports().forEach(imp -> {
            System.out.println("     - " + imp);
        });
        System.out.println();

        System.out.println("  💡 의미 분석이 중요한 이유:");
        System.out.println("     - 미사용 변수/코드 탐지");
        System.out.println("     - 타입 불일치 오류 발견");
        System.out.println("     - 의존성 분석 및 리팩토링");
    }
}
