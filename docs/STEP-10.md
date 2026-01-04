# STEP 10: 코드에서 나는 냄새 - 패턴 매칭

> 코드가 문법적으로 맞아도, "뭔가 이상한" 코드가 있어요.
> 당장 에러는 아닌데 유지보수하기 힘든 코드. 이런 걸 **코드 스멜(Code Smell)**이라고 불러요.

---

## 코드 스멜이 뭔데?

코드 스멜 = 나쁜 코드의 "냄새"

냉장고 안에서 이상한 냄새가 나면 뭔가 상했다는 신호잖아요. 코드도 마찬가지예요.

```
코드 스멜 예시:
├─ 긴 메서드 (30줄 이상)
├─ 너무 많은 매개변수 (5개 이상)
├─ 깊은 중첩 (if 안에 if 안에 if...)
├─ 중복 코드
└─ 만능 클래스 (God Class)
```

이런 코드들은 당장 에러는 안 나지만, 나중에 문제가 생길 확률이 높아요.

---

## 패턴 매칭의 흐름

우리가 AST를 탐색하면서 "이 패턴이 있나?" 확인하는 거예요.

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   AST        │     │   패턴       │     │   결과       │
│   분석       │ --> │   매칭       │ --> │   리포트     │
│              │     │              │     │              │
│ 구조 파악    │     │ 스멜 감지    │     │ 이슈 목록    │
└──────────────┘     └──────────────┘     └──────────────┘
```

---

## 대표적인 코드 스멜들

### 1. 긴 메서드 (Long Method)

```java
public void processOrder(Order order) {
    // ... 50줄의 코드 ...
    // 이 메서드가 뭘 하는지 이해하려면 5분은 걸림
}
```

30줄이 넘어가면 **"쪼개야 할 때"**예요.

### 2. 매개변수가 너무 많음

```java
public void createUser(String name, String email, String phone,
                       String address, String city, String zipCode) {
    // 매개변수가 6개? 뭐가 뭔지 헷갈려...
}
```

4개가 넘으면 **Parameter Object** 패턴을 고려하세요.

### 3. 깊은 중첩

```java
if (user != null) {
    if (user.isActive()) {
        if (user.hasPermission()) {
            if (user.getRole().equals("ADMIN")) {
                // 4단계 중첩... 이해하기 힘들어요
            }
        }
    }
}
```

3단계가 넘으면 **Early Return**으로 바꾸세요:

```java
if (user == null) return;
if (!user.isActive()) return;
if (!user.hasPermission()) return;
if (!user.getRole().equals("ADMIN")) return;

// 핵심 로직
```

### 4. 빈 catch 블록

```java
try {
    riskyOperation();
} catch (Exception e) {
    // 아무것도 안 함... 에러 무시??
}
```

에러를 삼키면 나중에 디버깅이 지옥이에요.

### 5. 매직 넘버

```java
if (age > 18) {  // 18이 뭐야?
if (timeout > 86400) {  // 86400?? 뭔 숫자야?
```

상수로 정의하면 의미가 명확해져요:

```java
private static final int ADULT_AGE = 18;
private static final int SECONDS_PER_DAY = 86400;
```

---

## 코드로 구현하기

AST를 탐색하면서 패턴을 찾아볼게요:

```java
public class PatternMatcher {
    private final List<CodeSmell> detectedSmells = new ArrayList<>();

    private static final int MAX_METHOD_LENGTH = 30;
    private static final int MAX_PARAMETERS = 4;
    private static final int MAX_NESTING_DEPTH = 3;

    public List<CodeSmell> findSmells(CompilationUnit cu) {
        detectLongMethods(cu);
        detectTooManyParameters(cu);
        detectDeepNesting(cu);
        detectEmptyCatch(cu);
        detectMagicNumbers(cu);

        return detectedSmells;
    }

    /**
     * 긴 메서드 감지
     */
    private void detectLongMethods(CompilationUnit cu) {
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            int lines = method.getRange()
                .map(r -> r.end.line - r.begin.line + 1)
                .orElse(0);

            if (lines > MAX_METHOD_LENGTH) {
                detectedSmells.add(new CodeSmell(
                    "LONG_METHOD",
                    "메서드 '" + method.getNameAsString() + "'가 너무 깁니다 (" + lines + "줄)",
                    "20줄 이하로 분리하세요"
                ));
            }
        });
    }

    /**
     * 빈 catch 블록 감지
     */
    private void detectEmptyCatch(CompilationUnit cu) {
        cu.findAll(CatchClause.class).forEach(catchClause -> {
            if (catchClause.getBody().getStatements().isEmpty()) {
                detectedSmells.add(new CodeSmell(
                    "EMPTY_CATCH",
                    "빈 catch 블록이 있습니다",
                    "최소한 로깅을 추가하세요"
                ));
            }
        });
    }
}
```

`findAll()`로 특정 노드를 찾고, 조건을 체크하는 패턴이에요.

---

## 실제로 나쁜 코드 분석해보기

이 코드를 분석해볼게요:

```java
public class BadExample {
    private String a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p;

    public void longMethod(String p1, String p2, String p3,
                           String p4, String p5, String p6) {
        if (p1 != null) {
            if (p2 != null) {
                if (p3 != null) {
                    if (p4 != null) {
                        // 깊은 중첩
                        int timeout = 86400;  // 매직 넘버
                        try {
                            process();
                        } catch (Exception e) {
                            // 빈 catch
                        }
                    }
                }
            }
        }
    }
}
```

분석 결과:

```
=== 코드 스멜 감지 결과 ===

WARNING [GOD_CLASS] Line 1
  클래스 'BadExample'가 너무 큽니다 (필드: 16개)
  → 클래스를 분리하세요.

WARNING [TOO_MANY_PARAMS] Line 4
  메서드 'longMethod'의 매개변수가 너무 많습니다 (6개)
  → Parameter Object 패턴을 고려하세요.

WARNING [DEEP_NESTING] Line 4
  메서드 'longMethod'의 중첩이 너무 깊습니다 (4레벨)
  → Early return 또는 메서드 분리를 고려하세요.

INFO [MAGIC_NUMBER] Line 11
  매직 넘버 86400가 발견되었습니다
  → 의미 있는 상수로 정의하세요.

WARNING [EMPTY_CATCH] Line 14
  빈 catch 블록이 있습니다
  → 최소한 로깅을 추가하세요.

총 5개의 코드 스멜 발견
```

한 눈에 문제가 보이죠?

---

## 감지할 수 있는 패턴들

### 코드 스멜
| 코드 | 설명 | 기준 |
|------|------|------|
| `LONG_METHOD` | 긴 메서드 | >30줄 |
| `TOO_MANY_PARAMS` | 매개변수 과다 | >4개 |
| `DEEP_NESTING` | 깊은 중첩 | >3레벨 |
| `EMPTY_CATCH` | 빈 catch 블록 | 비어있음 |
| `MAGIC_NUMBER` | 매직 넘버 | 리터럴 숫자 |
| `GOD_CLASS` | 만능 클래스 | 메서드 >20 또는 필드 >15 |

### 안티패턴
| 코드 | 설명 |
|------|------|
| `STRING_CONCAT_LOOP` | 루프 안에서 String + 연결 |
| `INSTANCEOF_CHAIN` | 연속 instanceof 체크 |
| `RETURN_NULL` | null 반환 (Optional 권장) |

---

## 새 패턴 추가하기

패턴 추가는 간단해요. 찾고 싶은 노드를 `findAll()`로 찾고, 조건을 체크하면 끝!

```java
/**
 * System.out.println 사용 감지
 */
private void detectSystemOut(CompilationUnit cu) {
    cu.findAll(MethodCallExpr.class).forEach(call -> {
        if (call.toString().startsWith("System.out.print")) {
            detectedSmells.add(new CodeSmell(
                "SYSTEM_OUT",
                "System.out 사용이 발견되었습니다",
                "로깅 프레임워크(SLF4J 등)를 사용하세요"
            ));
        }
    });
}
```

---

## 왜 정규표현식 대신 AST를 쓸까?

정규표현식으로 `if`를 세면?

```java
/*
if (password != null) {  // 주석 안의 if
    return password;
}
*/
String message = "if you want";  // 문자열 안의 if
if (true) {  // 진짜 if
```

정규표현식은 주석이나 문자열 안의 `if`도 세버려요. AST는 **진짜 코드만** 봅니다.

---

## 핵심 정리

1. **코드 스멜** → 나쁜 코드의 징후 (에러는 아니지만 문제 암시)
2. **패턴 매칭** → AST에서 특정 패턴을 찾아내는 것
3. **findAll()** → 원하는 노드 타입을 전부 찾기
4. **임계값** → 메서드 30줄, 매개변수 4개 등 기준 설정

---

## 다음 시간 예고

코드 스멜은 "냄새"일 뿐이에요. 당장 문제는 아니죠.

근데 진짜 **위험한** 코드가 있어요:
- SQL Injection 취약점
- 하드코딩된 비밀번호
- Null Pointer 위험

다음 STEP에서는 **진짜 버그와 보안 취약점을 찾는 방법**을 알아볼게요!

---

## 실습

```bash
cd code-ai-part2-analyzer
../gradlew :step10-pattern:run
```

여러분의 코드에서 코드 스멜을 찾아보세요. 몇 개나 발견되나요?
