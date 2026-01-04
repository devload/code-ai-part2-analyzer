# STEP 9: 변수야, 넌 어디서 왔니? - 의미 분석

> 지난 시간에 AST를 탐색해서 메서드 개수, 복잡도를 알아냈어요.
> 그런데 "이 변수가 어디서 선언됐지?", "이 변수 안 쓰이는 것 같은데?"라는 질문에는 아직 답할 수 없어요.

---

## 구문 분석 vs 의미 분석

지금까지 배운 건 **구문 분석**이에요. 코드가 문법적으로 맞는지, 어떤 구조인지 파악하는 거죠.

```
구문 분석: "if (x > 0)"이라는 구조가 있구나
의미 분석: 근데 x가 뭐지? 어디서 왔어? 타입이 뭐야?
```

예를 들어 이 코드를 볼게요:

```java
public void process() {
    String name = "hello";    // 선언
    System.out.println(name); // 사용
    name = "world";           // 재할당
}
```

`name`이라는 이름이 세 번 나오지만 역할이 달라요:
- 첫 번째: **선언** (처음 등장)
- 두 번째: **사용** (값을 읽음)
- 세 번째: **재할당** (값을 바꿈)

의미 분석은 이런 차이를 구분해요.

---

## 의미 분석으로 알 수 있는 것들

의미 분석을 하면 이런 질문에 답할 수 있어요:

```
┌────────────────────────────────────┐
│ Variable: name                     │
│ ├─ Type: String                    │
│ ├─ Declared at: line 2             │
│ ├─ Used at: line 3, line 4         │
│ └─ Scope: method 'process'         │
└────────────────────────────────────┘
```

- **타입**: 이 변수가 String인지 int인지
- **선언 위치**: 어느 라인에서 처음 나왔는지
- **사용 위치**: 어디서 쓰이는지
- **스코프**: 어디까지 살아있는지

---

## 심볼 테이블이 뭔데?

의미 분석의 핵심은 **심볼 테이블**이에요. 모든 변수, 메서드, 클래스 정보를 저장하는 표라고 생각하면 돼요.

```
┌──────────┬──────────┬──────────┬─────────┐
│ Name     │ Type     │ Scope    │ Line    │
├──────────┼──────────┼──────────┼─────────┤
│ service  │ String   │ field    │ 2       │
│ repo     │ UserRepo │ field    │ 3       │
│ id       │ Long     │ param    │ 5       │
│ msg      │ String   │ local    │ 6       │
└──────────┴──────────┴──────────┴─────────┘
```

컴파일러가 "이 변수 뭐야?"라고 물으면, 심볼 테이블에서 찾아보는 거예요.

---

## 코드로 구현하기

JavaParser로 변수 정보를 수집해볼게요:

```java
public class SemanticAnalyzer {
    private final Map<String, VariableInfo> variables = new HashMap<>();

    public void analyze(CompilationUnit cu) {
        // 1. 변수 선언 수집
        collectVariableDeclarations(cu);

        // 2. 변수 사용 추적
        trackVariableUsages(cu);
    }

    private void collectVariableDeclarations(CompilationUnit cu) {
        // 필드 선언
        cu.findAll(FieldDeclaration.class).forEach(field -> {
            field.getVariables().forEach(var -> {
                String name = var.getNameAsString();
                String type = var.getType().asString();
                int line = var.getBegin().map(p -> p.line).orElse(0);

                variables.put(name, new VariableInfo(name, type, line, "field"));
            });
        });

        // 지역 변수 선언
        cu.findAll(VariableDeclarator.class).forEach(var -> {
            String name = var.getNameAsString();
            String type = var.getType().asString();
            int line = var.getBegin().map(p -> p.line).orElse(0);

            variables.put(name, new VariableInfo(name, type, line, "local"));
        });
    }

    private void trackVariableUsages(CompilationUnit cu) {
        cu.findAll(NameExpr.class).forEach(expr -> {
            String name = expr.getNameAsString();
            int line = expr.getBegin().map(p -> p.line).orElse(0);

            if (variables.containsKey(name)) {
                variables.get(name).addUsage(line);
            }
        });
    }
}
```

`NameExpr`가 핵심이에요. 코드에서 변수 이름이 사용될 때마다 `NameExpr` 노드가 생겨요.

---

## 실제로 분석해보기

이 코드를 분석해볼게요:

```java
public class UserService {
    private String serviceName;      // 필드
    private UserRepository repository;

    public User findUser(Long id) {
        String logMessage = "Finding user: " + id;
        System.out.println(logMessage);
        return repository.findById(id);
    }
}
```

분석 결과:

```
=== 변수 분석 ===
serviceName:
  타입: String
  선언: line 2
  사용 횟수: 0    ← 어? 안 쓰이네?

repository:
  타입: UserRepository
  선언: line 3
  사용 횟수: 1

logMessage:
  타입: String
  선언: line 6
  사용 횟수: 1

=== 사용되지 않는 변수 ===
serviceName    ← 이거 지워도 되겠다!
```

`serviceName`이 선언만 되고 한 번도 안 쓰였어요. 이런 걸 찾아주는 거예요!

---

## 스코프 체인 이해하기

변수가 어디까지 "보이는지"를 스코프라고 해요.

```java
class Example {
    int x = 1;                    // 클래스 스코프

    void method() {
        int x = 2;                // 메서드 스코프 (섀도잉!)

        if (true) {
            int y = 3;            // 블록 스코프
            System.out.println(x); // 어떤 x? → 메서드의 x (2)
        }
        // y는 여기서 접근 불가!
    }
}
```

같은 이름이 여러 개면? 가장 가까운 스코프의 변수가 이겨요. 이걸 **섀도잉**이라고 해요.

---

## 의미 분석으로 찾을 수 있는 문제들

### 1. 사용되지 않는 변수
```java
String unused = "never used";  // ⚠️ 지워도 됨
```

### 2. 초기화되지 않은 변수 사용
```java
String name;
System.out.println(name);  // ⚠️ 값이 없는데?
```

### 3. 변수 섀도잉 (실수할 수 있음)
```java
class Example {
    int value = 10;

    void method(int value) {  // ⚠️ 필드를 가리네!
        System.out.println(value);  // 어떤 value?
    }
}
```

---

## 핵심 정리

1. **의미 분석** → 변수의 "의미"를 파악 (타입, 선언 위치, 사용 위치)
2. **심볼 테이블** → 모든 식별자 정보를 저장하는 표
3. **스코프** → 변수가 살아있는 범위
4. **활용** → 안 쓰는 변수 찾기, 타입 체크, 리팩토링 지원

```
변수 등장 → 심볼 테이블에서 찾기 → 타입/스코프 확인 → 문제 발견!
```

---

## 다음 시간 예고

변수가 어디서 왔는지는 알았어요. 근데...

- "이 코드 왜 이렇게 복잡해?"
- "이런 패턴은 별로 안 좋은데..."
- "이거 전형적인 안티패턴이잖아!"

다음 STEP에서는 **나쁜 코드 패턴(코드 스멜)을 찾는 방법**을 알아볼게요!

---

## 실습

```bash
cd code-ai-part2-analyzer
../gradlew :step9-semantics:run
```

여러분의 코드에서 사용되지 않는 변수를 찾아보세요!
