package com.aiprocess.step7;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

/**
 * STEP 7: 파싱 데모
 *
 * 코드가 어떻게 구조화된 형태로 변환되는지 시각화합니다.
 */
public class ParsingDemo {

    public static void main(String[] args) {
        System.out.println("═".repeat(60));
        System.out.println("STEP 7: 파싱 (Parsing)");
        System.out.println("═".repeat(60));
        System.out.println();
        System.out.println("핵심 질문: 코드를 어떻게 읽는가?");
        System.out.println();

        // 1. 기본 파싱 데모
        demoBasicParsing();

        System.out.println();

        // 2. 파싱 오류 처리
        demoParsingError();

        System.out.println();

        // 3. 코드 요소 추출
        demoElementExtraction();
    }

    private static void demoBasicParsing() {
        System.out.println("─".repeat(60));
        System.out.println("1. 기본 파싱");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class User {
                private String name;
                private int age;

                public void setName(String name) {
                    this.name = name;
                }

                public String getName() {
                    return name;
                }
            }
            """;

        System.out.println("  원본 코드:");
        for (String line : code.split("\n")) {
            System.out.println("  │ " + line);
        }
        System.out.println();

        CodeParser parser = new CodeParser();
        CodeParser.ParseResult result = parser.parse(code);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 파싱 결과                                │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println("  성공: " + result.success());
        System.out.println("  소요시간: " + result.latencyMs() + "ms");
        System.out.println();

        if (result.success()) {
            CompilationUnit cu = result.getAST();
            System.out.println("  AST 구조:");
            System.out.println("  └── CompilationUnit");

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                System.out.println("      └── Class: " + cls.getNameAsString());

                cls.getFields().forEach(field -> {
                    String type = field.getElementType().asString();
                    String name = field.getVariable(0).getNameAsString();
                    System.out.println("          ├── Field: " + type + " " + name);
                });

                cls.getMethods().forEach(method -> {
                    System.out.println("          └── Method: " + method.getNameAsString() + "()");
                });
            });
        }
    }

    private static void demoParsingError() {
        System.out.println("─".repeat(60));
        System.out.println("2. 파싱 오류 처리");
        System.out.println("─".repeat(60));
        System.out.println();

        String invalidCode = """
            public class Broken {
                public void method( {  // 문법 오류: 괄호 불일치
                    System.out.println("hello");
                }
            }
            """;

        System.out.println("  잘못된 코드:");
        for (String line : invalidCode.split("\n")) {
            System.out.println("  │ " + line);
        }
        System.out.println();

        CodeParser parser = new CodeParser();
        CodeParser.ParseResult result = parser.parse(invalidCode);

        System.out.println("  ┌─────────────────────────────────────────┐");
        System.out.println("  │ 파싱 결과                                │");
        System.out.println("  └─────────────────────────────────────────┘");
        System.out.println("  성공: " + result.success());
        if (!result.success()) {
            System.out.println("  ❌ 오류: 문법 오류 감지됨");
        }
    }

    private static void demoElementExtraction() {
        System.out.println("─".repeat(60));
        System.out.println("3. 코드 요소 추출");
        System.out.println("─".repeat(60));
        System.out.println();

        String code = """
            public class Calculator {
                private int result;

                public int add(int a, int b) {
                    return a + b;
                }

                public int subtract(int a, int b) {
                    return a - b;
                }

                public int multiply(int a, int b) {
                    return a * b;
                }
            }
            """;

        CodeParser parser = new CodeParser();
        CodeParser.ParseResult result = parser.parse(code);

        if (result.success()) {
            CompilationUnit cu = result.getAST();

            System.out.println("  ┌─────────────────────────────────────────┐");
            System.out.println("  │ 추출된 정보                              │");
            System.out.println("  └─────────────────────────────────────────┘");
            System.out.println();

            // 클래스 정보
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                System.out.println("  📦 클래스: " + cls.getNameAsString());
                System.out.println();

                // 필드 정보
                System.out.println("  📌 필드:");
                cls.getFields().forEach(field -> {
                    String type = field.getElementType().asString();
                    String name = field.getVariable(0).getNameAsString();
                    String modifier = field.isPrivate() ? "private" : "public";
                    System.out.println("     - " + modifier + " " + type + " " + name);
                });
                System.out.println();

                // 메서드 정보
                System.out.println("  📌 메서드:");
                cls.getMethods().forEach(method -> {
                    String returnType = method.getTypeAsString();
                    String name = method.getNameAsString();
                    int paramCount = method.getParameters().size();
                    System.out.println("     - " + returnType + " " + name + "() [파라미터: " + paramCount + "개]");
                });
            });
        }

        System.out.println();
        System.out.println("  💡 파싱이 중요한 이유:");
        System.out.println("     - 코드의 구조를 이해할 수 있음");
        System.out.println("     - 특정 요소만 선택적으로 분석 가능");
        System.out.println("     - 코드 변환/리팩토링의 기반");
    }
}
