# STEP 7: AI는 코드를 어떻게 읽을까?

> 우리가 코드를 눈으로 읽듯이, AI도 코드를 "읽어야" 분석할 수 있습니다.
> 그런데 AI에게 코드는 그냥 긴 문자열일 뿐이에요. 어떻게 의미를 파악할 수 있을까요?

---

## 문제 상황: AI에게 코드는 그냥 글자 덩어리

여러분이 이런 코드를 본다고 해볼게요:

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
}
```

사람인 우리는 한눈에 알 수 있죠:
- "아, Calculator라는 클래스구나"
- "add라는 메서드가 있네"
- "두 숫자를 더해서 반환하는군"

그런데 AI 입장에서 이건 그냥 이런 문자열이에요:

```
"public class Calculator {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}"
```

이 상태로는 "메서드가 몇 개야?"라는 질문에도 답할 수 없어요. 그냥 글자 나열일 뿐이니까요.

---

## 해결책: 파싱 (Parsing)

**파싱**이란 문자열을 의미 있는 구조로 바꾸는 과정이에요.

마치 문장을 분석할 때 "주어", "동사", "목적어"로 나누는 것처럼, 코드도 "클래스", "메서드", "변수" 같은 구조로 나눌 수 있어요.

```
문자열 코드                        구조화된 트리
      ↓                                ↓
"public class Calculator {      CompilationUnit
    public int add(...) {        └─ Class: Calculator
        return a + b;                 └─ Method: add
    }                                      └─ Return: a + b
}"
```

이렇게 트리 구조로 바꾸면, AI가 "클래스가 몇 개야?", "메서드 이름이 뭐야?" 같은 질문에 쉽게 답할 수 있게 돼요.

---

## 파싱은 두 단계로 이루어져요

### 1단계: 어휘 분석 (Lexing)

먼저 코드를 의미 있는 조각(토큰)으로 쪼개요.

```
"int x = 5;"

     ↓ 어휘 분석

[KEYWORD: int] [IDENTIFIER: x] [OPERATOR: =] [NUMBER: 5] [SEMICOLON]
```

마치 문장을 단어로 나누는 것과 같아요.

### 2단계: 구문 분석 (Parsing)

토큰들을 문법에 맞게 트리로 조립해요.

```
[KEYWORD: int] [IDENTIFIER: x] [OPERATOR: =] [NUMBER: 5]

     ↓ 구문 분석

VariableDeclaration
├─ type: int
├─ name: x
└─ value: 5
```

이제 "이건 변수 선언이고, 타입은 int고, 이름은 x야"라고 AI가 이해할 수 있게 됐어요!

---

## 직접 해보기: JavaParser 사용하기

Java 코드를 파싱하는 건 JavaParser 라이브러리가 대신 해줘요. 우리는 그냥 사용하면 됩니다.

```java
public class CodeParser {
    private final JavaParser parser = new JavaParser();

    public CompilationUnit parse(String code) {
        // 코드를 넣으면 트리 구조로 변환!
        ParseResult<CompilationUnit> result = parser.parse(code);
        return result.getResult().get();
    }
}
```

정말 간단하죠? `parse()` 메서드 하나로 문자열이 트리로 변신해요.

---

## 실제로 돌려보면?

```java
String code = """
    public class Calculator {
        public int add(int a, int b) {
            return a + b;
        }
        public int subtract(int a, int b) {
            return a - b;
        }
    }
    """;

CompilationUnit cu = parser.parse(code);

// 이제 구조적인 질문에 답할 수 있어요!
System.out.println("클래스 수: " + cu.findAll(ClassOrInterfaceDeclaration.class).size());
System.out.println("메서드 수: " + cu.findAll(MethodDeclaration.class).size());
```

출력:
```
클래스 수: 1
메서드 수: 2
```

문자열일 때는 불가능했던 질문에 이제 답할 수 있어요!

---

## AST가 뭔데?

파싱 결과물을 **AST (Abstract Syntax Tree)**라고 불러요. 번역하면 "추상 구문 트리"인데, 그냥 "코드의 구조를 트리로 표현한 것"이라고 생각하면 돼요.

```
CompilationUnit (파일 전체)
└── ClassDeclaration (클래스)
    ├── name: "Calculator"
    ├── MethodDeclaration (메서드)
    │   ├── name: "add"
    │   ├── parameters: [a, b]
    │   └── body: return a + b
    └── MethodDeclaration (메서드)
        ├── name: "subtract"
        └── ...
```

이 트리를 탐색하면 코드에 대한 모든 정보를 알 수 있어요.

---

## 잘못된 코드는 어떻게 될까?

문법에 맞지 않는 코드는 파싱이 실패해요:

```java
String badCode = "public class { }";  // 클래스 이름이 없어!

ParseResult result = parser.parse(badCode);
if (!result.isSuccessful()) {
    System.out.println("파싱 실패!");
    result.getProblems().forEach(p ->
        System.out.println("  - " + p.getMessage())
    );
}
```

출력:
```
파싱 실패!
  - identifier expected
```

파서가 "클래스 이름이 있어야 하는데 없어!"라고 알려주네요.

---

## 핵심 정리

1. **AI에게 코드는 그냥 문자열** → 구조를 모름
2. **파싱으로 트리(AST)로 변환** → 구조 파악 가능
3. **JavaParser가 다 해줌** → 우리는 결과만 사용

```
"public class Foo { }"  →  파싱  →  트리 구조  →  분석 가능!
```

---

## 다음 시간 예고

파싱해서 트리를 얻었어요. 그런데 이 트리를 어떻게 탐색해서 유용한 정보를 뽑아낼까요?

다음 STEP에서는 **AST를 탐색해서 코드 메트릭(클래스 수, 복잡도 등)을 추출하는 방법**을 알아볼게요!

---

## 실습

```bash
cd code-ai-part2-analyzer
../gradlew :step7-parsing:run
```

직접 코드를 바꿔가면서 파싱 결과가 어떻게 달라지는지 확인해보세요!
