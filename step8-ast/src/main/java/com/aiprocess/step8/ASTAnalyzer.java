package com.aiprocess.step8;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.expr.*;

import java.util.*;

/**
 * STEP 8: AST 분석
 *
 * 핵심 질문: 코드의 구조를 어떻게 파악하는가?
 *
 * AST(Abstract Syntax Tree)는 코드의 "뼈대"입니다.
 * AST를 분석하면 코드의 복잡도, 구조, 패턴을 파악할 수 있습니다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ AST 분석으로 알 수 있는 것:                              │
 * │                                                          │
 * │ 1. 코드 메트릭                                           │
 * │    - 메서드 수, 필드 수, 라인 수                         │
 * │    - 중첩 깊이 (Nesting Depth)                           │
 * │    - 순환 복잡도 (Cyclomatic Complexity)                 │
 * │                                                          │
 * │ 2. 코드 구조                                             │
 * │    - 상속 관계                                           │
 * │    - 메서드 호출 관계                                    │
 * │    - 필드 사용 패턴                                      │
 * │                                                          │
 * │ 3. 코드 스멜 탐지                                        │
 * │    - 너무 긴 메서드                                      │
 * │    - 너무 깊은 중첩                                      │
 * │    - 너무 많은 파라미터                                  │
 * └─────────────────────────────────────────────────────────┘
 */
public class ASTAnalyzer {

    /**
     * AST 분석 수행
     */
    public ASTMetrics analyze(CompilationUnit cu) {
        int classCount = cu.findAll(ClassOrInterfaceDeclaration.class).size();
        int methodCount = cu.findAll(MethodDeclaration.class).size();
        int fieldCount = cu.findAll(FieldDeclaration.class).size();
        int lineCount = cu.toString().split("\n").length;

        // 최대 중첩 깊이 계산
        int maxNestingDepth = calculateMaxNestingDepth(cu);

        // 순환 복잡도 계산
        int cyclomaticComplexity = calculateCyclomaticComplexity(cu);

        // 메서드별 정보 수집
        List<MethodInfo> methods = new ArrayList<>();
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            methods.add(analyzeMethod(method));
        });

        return new ASTMetrics(
            classCount,
            methodCount,
            fieldCount,
            lineCount,
            maxNestingDepth,
            cyclomaticComplexity,
            methods
        );
    }

    /**
     * 메서드 분석
     */
    private MethodInfo analyzeMethod(MethodDeclaration method) {
        String name = method.getNameAsString();
        int paramCount = method.getParameters().size();
        int lineCount = method.toString().split("\n").length;
        int complexity = calculateMethodComplexity(method);

        return new MethodInfo(name, paramCount, lineCount, complexity);
    }

    /**
     * 최대 중첩 깊이 계산
     */
    private int calculateMaxNestingDepth(CompilationUnit cu) {
        int[] maxDepth = {0};

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int depth = calculateNestingDepth(method, 0);
            maxDepth[0] = Math.max(maxDepth[0], depth);
        });

        return maxDepth[0];
    }

    private int calculateNestingDepth(com.github.javaparser.ast.Node node, int currentDepth) {
        int maxDepth = currentDepth;

        for (com.github.javaparser.ast.Node child : node.getChildNodes()) {
            int childDepth = currentDepth;

            if (child instanceof IfStmt ||
                child instanceof ForStmt ||
                child instanceof WhileStmt ||
                child instanceof DoStmt ||
                child instanceof SwitchStmt ||
                child instanceof TryStmt) {
                childDepth = currentDepth + 1;
            }

            maxDepth = Math.max(maxDepth, calculateNestingDepth(child, childDepth));
        }

        return maxDepth;
    }

    /**
     * 순환 복잡도 계산 (McCabe)
     * 복잡도 = 분기점 수 + 1
     */
    private int calculateCyclomaticComplexity(CompilationUnit cu) {
        int complexity = 1;  // 기본값

        // if, for, while, case, catch, && , || 카운트
        complexity += cu.findAll(IfStmt.class).size();
        complexity += cu.findAll(ForStmt.class).size();
        complexity += cu.findAll(ForEachStmt.class).size();
        complexity += cu.findAll(WhileStmt.class).size();
        complexity += cu.findAll(DoStmt.class).size();
        complexity += cu.findAll(CatchClause.class).size();
        complexity += cu.findAll(ConditionalExpr.class).size();  // 삼항 연산자

        // && 와 || 연산자
        complexity += cu.findAll(BinaryExpr.class).stream()
            .filter(expr -> expr.getOperator() == BinaryExpr.Operator.AND ||
                           expr.getOperator() == BinaryExpr.Operator.OR)
            .count();

        return complexity;
    }

    private int calculateMethodComplexity(MethodDeclaration method) {
        int complexity = 1;

        complexity += method.findAll(IfStmt.class).size();
        complexity += method.findAll(ForStmt.class).size();
        complexity += method.findAll(ForEachStmt.class).size();
        complexity += method.findAll(WhileStmt.class).size();
        complexity += method.findAll(DoStmt.class).size();
        complexity += method.findAll(CatchClause.class).size();

        return complexity;
    }

    /**
     * AST 메트릭 결과
     */
    public record ASTMetrics(
        int classCount,
        int methodCount,
        int fieldCount,
        int lineCount,
        int maxNestingDepth,
        int cyclomaticComplexity,
        List<MethodInfo> methods
    ) {}

    /**
     * 메서드 정보
     */
    public record MethodInfo(
        String name,
        int paramCount,
        int lineCount,
        int complexity
    ) {}
}
