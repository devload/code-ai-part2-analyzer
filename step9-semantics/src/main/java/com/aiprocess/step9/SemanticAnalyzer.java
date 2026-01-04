package com.aiprocess.step9;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;

import java.util.*;

/**
 * STEP 9: 의미 분석
 *
 * 핵심 질문: 변수/타입을 어떻게 추적하는가?
 *
 * 파싱만으로는 부족합니다. "이 변수가 어디서 정의되었는지",
 * "이 메서드가 무엇을 반환하는지" 등 의미를 파악해야 합니다.
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │ 의미 분석이 필요한 이유:                                 │
 * │                                                          │
 * │ 코드:  String name = user.getName();                     │
 * │                                                          │
 * │ 질문들:                                                  │
 * │   - user는 어디서 정의되었나?                            │
 * │   - user의 타입은 무엇인가?                              │
 * │   - getName()은 String을 반환하나?                       │
 * │   - name 변수는 나중에 어디서 사용되나?                  │
 * │                                                          │
 * │ → 이런 질문에 답하려면 "의미 분석"이 필요                │
 * └─────────────────────────────────────────────────────────┘
 */
public class SemanticAnalyzer {

    /**
     * 변수 사용 분석
     */
    public VariableUsage analyzeVariables(CompilationUnit cu) {
        Map<String, List<String>> fieldUsage = new HashMap<>();
        Map<String, Integer> variableUsageCount = new HashMap<>();
        List<String> unusedVariables = new ArrayList<>();

        // 필드 수집
        Set<String> fields = new HashSet<>();
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            field.getVariables().forEach(var -> {
                fields.add(var.getNameAsString());
                fieldUsage.put(var.getNameAsString(), new ArrayList<>());
            });
        });

        // 필드 사용 추적
        cu.findAll(NameExpr.class).forEach(nameExpr -> {
            String name = nameExpr.getNameAsString();
            if (fields.contains(name)) {
                // 사용된 메서드 찾기
                nameExpr.findAncestor(MethodDeclaration.class).ifPresent(method -> {
                    fieldUsage.get(name).add(method.getNameAsString());
                });
            }
            variableUsageCount.merge(name, 1, Integer::sum);
        });

        // 미사용 필드 찾기
        fieldUsage.forEach((field, usages) -> {
            if (usages.isEmpty()) {
                unusedVariables.add(field);
            }
        });

        return new VariableUsage(fieldUsage, variableUsageCount, unusedVariables);
    }

    /**
     * 메서드 호출 분석
     */
    public MethodCallAnalysis analyzeMethodCalls(CompilationUnit cu) {
        Map<String, List<String>> methodCalls = new HashMap<>();
        Map<String, Integer> callCount = new HashMap<>();

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            String methodName = method.getNameAsString();
            List<String> calls = new ArrayList<>();

            method.findAll(MethodCallExpr.class).forEach(call -> {
                String calledMethod = call.getNameAsString();
                calls.add(calledMethod);
                callCount.merge(calledMethod, 1, Integer::sum);
            });

            methodCalls.put(methodName, calls);
        });

        return new MethodCallAnalysis(methodCalls, callCount);
    }

    /**
     * 타입 사용 분석
     */
    public TypeUsage analyzeTypes(CompilationUnit cu) {
        Map<String, Integer> typeCount = new HashMap<>();
        Set<String> imports = new HashSet<>();

        // Import 수집
        cu.getImports().forEach(imp -> {
            imports.add(imp.getNameAsString());
        });

        // 타입 사용 카운트
        cu.findAll(VariableDeclarator.class).forEach(var -> {
            String type = var.getType().asString();
            typeCount.merge(type, 1, Integer::sum);
        });

        cu.findAll(MethodDeclaration.class).forEach(method -> {
            String returnType = method.getTypeAsString();
            typeCount.merge(returnType, 1, Integer::sum);

            method.getParameters().forEach(param -> {
                String paramType = param.getTypeAsString();
                typeCount.merge(paramType, 1, Integer::sum);
            });
        });

        return new TypeUsage(typeCount, imports);
    }

    public record VariableUsage(
        Map<String, List<String>> fieldUsage,
        Map<String, Integer> variableUsageCount,
        List<String> unusedVariables
    ) {}

    public record MethodCallAnalysis(
        Map<String, List<String>> methodCalls,
        Map<String, Integer> callCount
    ) {}

    public record TypeUsage(
        Map<String, Integer> typeCount,
        Set<String> imports
    ) {}
}
