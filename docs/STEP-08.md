# STEP 8: 코드의 지도를 그려보자 - AST 탐색하기

> 지난 시간에 코드를 트리 구조로 바꾸는 법을 배웠어요.
> 이제 이 트리를 탐험하면서 "이 코드가 얼마나 복잡한지" 측정해볼 거예요.

---

## 트리를 얻었는데, 그 다음은?

파싱을 통해 코드가 이런 트리가 됐어요:

```
CompilationUnit
└── Class: Calculator
    ├── Method: add
    └── Method: subtract
```

이 트리를 탐색하면 재미있는 질문들에 답할 수 있어요:

- "메서드가 몇 개야?" → 2개
- "가장 복잡한 메서드는?" → findByStatus
- "if문이 너무 많이 중첩됐나?" → 확인 가능

이런 숫자들을 **코드 메트릭**이라고 불러요. 코드의 "건강 검진 결과"라고 생각하면 됩니다.

---

## 가장 중요한 메트릭: 순환 복잡도

**순환 복잡도(Cyclomatic Complexity)**는 "코드가 얼마나 복잡한가"를 숫자로 표현한 거예요.

계산법은 의외로 간단해요:

```
복잡도 = 1 + (분기점 개수)
```

분기점이 뭐냐고요? 코드의 흐름이 나뉘는 지점이에요:

- `if` → +1
- `for`, `while` → +1
- `case` → +1
- `&&`, `||` → +1
- `? :` (삼항 연산자) → +1

---

## 예시로 이해하기

이 코드의 복잡도는 몇일까요?

```java
public void process(int x) {
    if (x > 0) {                    // +1
        for (int i = 0; i < x; i++) {  // +1
            if (i % 2 == 0) {       // +1
                doSomething();
            }
        }
    } else if (x < 0) {             // +1
        doOther();
    }
}
```

계산해볼게요:
- 기본값: 1
- if문 2개: +2
- for문 1개: +1
- else if 1개: +1
- **총 복잡도: 5**

복잡도 5는 괜찮은 편이에요. 10이 넘어가면 "이 메서드 좀 쪼개야겠다"라는 신호예요.

| 복잡도 | 의미 |
|--------|------|
| 1-5 | 심플하고 좋아요 |
| 6-10 | 조금 복잡하지만 괜찮아요 |
| 11-20 | 리팩토링을 고려해보세요 |
| 21+ | 반드시 쪼개세요! |

---

## 코드로 복잡도 계산하기

JavaParser로 트리를 탐색하면서 분기점을 세면 돼요:

```java
public int calculateComplexity(MethodDeclaration method) {
    int complexity = 1;  // 시작값

    // if문 세기
    complexity += method.findAll(IfStmt.class).size();

    // for문 세기
    complexity += method.findAll(ForStmt.class).size();

    // while문 세기
    complexity += method.findAll(WhileStmt.class).size();

    // && 또는 || 세기
    complexity += method.findAll(BinaryExpr.class).stream()
        .filter(expr -> expr.getOperator() == AND || expr.getOperator() == OR)
        .count();

    return complexity;
}
```

`findAll()`이 핵심이에요. 트리에서 특정 타입의 노드를 전부 찾아줍니다.

---

## 실제로 분석해보기

이 코드를 분석해볼게요:

```java
public class OrderService {
    private OrderRepository repository;

    public Order findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        return repository.findById(id);
    }

    public List<Order> findByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return Collections.emptyList();
        }
        for (Order order : repository.findAll()) {
            if (order.getStatus().equals(status)) {
                // 처리
            }
        }
        return filtered;
    }
}
```

분석 결과:
```
=== AST 분석 결과 ===
클래스 수: 1
메서드 수: 2
필드 수: 1

=== 메서드별 복잡도 ===
findById: 2       ← if 1개 + 기본값 1
findByStatus: 4   ← if 1개 + || 1개 + for 1개 + if 1개... 어? 5 아닌가?
```

복잡도 계산이 조금 다를 수 있어요. 도구마다 세는 방식이 조금씩 달라요. 중요한 건 **상대적인 비교**예요.

---

## 왜 정규표현식 대신 AST를 쓸까?

정규표현식으로 `if`를 세면 안 될까요?

```java
// 이 코드를 정규표현식으로 분석하면...
/*
if (password != null) {  // 이것도 if로 세버림!
    return password;
}
*/
String password = "secret";
if (password.length() > 0) {  // 진짜 if
    // ...
}
```

정규표현식은 주석 안의 `if`도 세버려요. AST는 주석을 무시하고 **진짜 코드만** 분석해요.

| 방식 | 주석 처리 | 정확도 |
|------|----------|--------|
| 정규표현식 | 주석도 분석 (오탐) | 낮음 |
| AST 분석 | 주석 무시 | 높음 |

---

## 트리 탐색의 비밀: findAll()

JavaParser에서 가장 많이 쓰는 메서드예요:

```java
// 모든 메서드 찾기
List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

// 모든 if문 찾기
List<IfStmt> ifs = cu.findAll(IfStmt.class);

// 모든 메서드 호출 찾기
List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
```

어떤 노드 타입이든 찾을 수 있어요. 트리 전체를 자동으로 탐색해줍니다.

---

## 자주 쓰는 노드 타입들

| 노드 타입 | 의미 | 예시 |
|-----------|------|------|
| `ClassOrInterfaceDeclaration` | 클래스 | `class User { }` |
| `MethodDeclaration` | 메서드 | `void save() { }` |
| `FieldDeclaration` | 필드 | `private int count;` |
| `IfStmt` | if문 | `if (x > 0)` |
| `ForStmt` | for문 | `for (int i...)` |
| `MethodCallExpr` | 메서드 호출 | `list.add(item)` |

---

## 핵심 정리

1. **AST를 탐색해서 메트릭 추출** → 코드의 "건강 상태" 파악
2. **순환 복잡도** → 분기점 개수로 복잡도 측정
3. **findAll()** → 원하는 노드 타입을 쉽게 검색
4. **AST > 정규표현식** → 주석 무시, 정확한 분석

---

## 다음 시간 예고

메서드 개수, 복잡도는 알았어요. 그런데...

- "이 변수는 어디서 선언됐지?"
- "이 변수가 어디서 사용되지?"
- "사용 안 하는 변수는 없나?"

다음 STEP에서는 **변수와 타입을 추적하는 의미 분석**을 배워볼게요!

---

## 실습

```bash
cd code-ai-part2-analyzer
../gradlew :step8-ast:run
```

복잡한 코드를 넣어보고 복잡도가 어떻게 나오는지 확인해보세요. 복잡도 10 이상인 메서드를 만들 수 있을까요?
