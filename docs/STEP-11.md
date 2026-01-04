# STEP 11: 진짜 위험한 코드 찾기 - 이슈 탐지

> 코드 스멜은 "냄새"일 뿐이에요. 리팩토링하면 좋지만 당장 문제는 아니죠.
> 이번에는 **진짜 위험한 것들**: 보안 취약점, 버그, 치명적 실수를 찾아볼 거예요.

---

## 코드 스멜 vs 진짜 이슈

| 구분 | 코드 스멜 (STEP-10) | 이슈 (STEP-11) |
|------|-------------------|----------------|
| 심각도 | 낮음~중간 | 중간~치명적 |
| 영향 | 유지보수 어려움 | **해킹당함, 서버 터짐** |
| 예시 | 긴 메서드 | SQL Injection |
| 조치 | 권장 | **필수** |

코드 스멜은 "나중에 고치면 좋겠다"이지만, 이슈는 **"지금 당장 고쳐야 한다"**예요.

---

## 이슈 분류

이슈는 심각도에 따라 나눠요:

```
🚨 CRITICAL (보안)
├─ SQL Injection
├─ 하드코딩된 비밀정보
└─ Command Injection

❌ ERROR (버그)
├─ Null Pointer 가능성
├─ 리소스 미해제
└─ 무한 루프 가능성

⚠️ WARNING (품질)
├─ 비효율적 코드
└─ 베스트 프랙티스 위반
```

---

## CRITICAL: 해킹당할 수 있는 코드

### 1. 하드코딩된 비밀정보

```java
private String password = "admin123";  // 🚨 소스 코드에 비번이?!
private String apiKey = "sk-12345";    // 🚨 API 키가 그대로?!
```

이런 코드가 GitHub에 올라가면? 해커들이 자동으로 스캔해서 찾아내요.

**해결책**: 환경변수나 Vault 사용
```java
private String password = System.getenv("DB_PASSWORD");
```

### 2. SQL Injection

```java
String sql = "SELECT * FROM users WHERE id = '" + userId + "'";
```

만약 `userId`에 `'; DROP TABLE users; --`를 넣으면?

```sql
SELECT * FROM users WHERE id = ''; DROP TABLE users; --'
```

테이블이 날아가요! 🔥

**해결책**: PreparedStatement 사용
```java
String sql = "SELECT * FROM users WHERE id = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, userId);
```

### 3. Command Injection

```java
Runtime.getRuntime().exec("ping " + userInput);
```

`userInput`에 `; rm -rf /`를 넣으면? 서버 파일 전체 삭제!

---

## ERROR: 버그가 될 코드

### 1. Null Pointer 위험

```java
return getUser(userId).getName().toUpperCase();
```

`getUser()`가 null을 반환하면? **NullPointerException!**

**해결책**:
```java
User user = getUser(userId);
if (user == null) return null;
return user.getName().toUpperCase();

// 또는 Optional 사용
return getUser(userId)
    .map(User::getName)
    .map(String::toUpperCase)
    .orElse(null);
```

### 2. 리소스 누수

```java
Connection conn = DriverManager.getConnection(url);
// conn.close()를 안 하면?
// 커넥션이 계속 쌓여서 결국 서버 다운!
```

**해결책**: try-with-resources
```java
try (Connection conn = DriverManager.getConnection(url)) {
    // 자동으로 close됨
}
```

---

## WARNING: 고치면 좋은 코드

### 문자열 == 비교

```java
if (status == "active") {  // ⚠️ 안 될 수도 있어!
```

Java에서 문자열은 `==`가 아니라 `equals()`로 비교해야 해요.

```java
if ("active".equals(status)) {  // ✅ 올바른 방법
```

---

## 코드로 구현하기

```java
public class IssueDetector {
    private final List<Issue> detectedIssues = new ArrayList<>();

    public List<Issue> detectIssues(CompilationUnit cu) {
        detectHardcodedSecrets(cu);
        detectSqlInjection(cu);
        detectNullPointerRisk(cu);
        detectResourceLeak(cu);
        detectStringEquality(cu);

        return detectedIssues;
    }

    /**
     * 하드코딩된 비밀정보 감지
     */
    private void detectHardcodedSecrets(CompilationUnit cu) {
        List<String> sensitiveNames = Arrays.asList(
            "password", "passwd", "secret", "apikey", "api_key", "token"
        );

        cu.findAll(FieldDeclaration.class).forEach(field -> {
            field.getVariables().forEach(var -> {
                String name = var.getNameAsString().toLowerCase();

                boolean isSensitive = sensitiveNames.stream()
                    .anyMatch(name::contains);

                if (isSensitive && var.getInitializer().isPresent()) {
                    if (var.getInitializer().get().isStringLiteralExpr()) {
                        detectedIssues.add(new Issue(
                            "HARDCODED_SECRET",
                            Severity.CRITICAL,
                            "하드코딩된 비밀정보: " + var.getNameAsString(),
                            "환경변수나 Vault를 사용하세요"
                        ));
                    }
                }
            });
        });
    }

    /**
     * SQL Injection 감지
     */
    private void detectSqlInjection(CompilationUnit cu) {
        cu.findAll(BinaryExpr.class).forEach(expr -> {
            if (expr.getOperator() == BinaryExpr.Operator.PLUS) {
                String exprStr = expr.toString().toLowerCase();

                if ((exprStr.contains("select") || exprStr.contains("insert") ||
                     exprStr.contains("update") || exprStr.contains("delete")) &&
                    exprStr.contains("+")) {

                    detectedIssues.add(new Issue(
                        "SQL_INJECTION",
                        Severity.CRITICAL,
                        "SQL Injection 위험: 문자열 연결로 쿼리 생성",
                        "PreparedStatement를 사용하세요"
                    ));
                }
            }
        });
    }
}
```

---

## 실제로 취약한 코드 분석해보기

이 코드를 분석해볼게요:

```java
public class VulnerableService {
    private String password = "admin123";  // 하드코딩
    private String apiKey = "sk-12345";    // 하드코딩

    public User findUser(String userId) {
        // SQL Injection
        String sql = "SELECT * FROM users WHERE id = '" + userId + "'";
        Connection conn = DriverManager.getConnection(url);  // 리소스 누수

        // Null Pointer 위험
        return getUser(userId).getName().toUpperCase();
    }

    public void compare(String a) {
        if (a == "test") {  // 문자열 == 비교
            System.exit(0);
        }
    }
}
```

분석 결과:

```
=== 이슈 탐지 결과 ===

CRITICAL (3개):
  [HARDCODED_SECRET] Line 2
    하드코딩된 비밀정보: password
    → 환경변수나 Vault를 사용하세요.
  [HARDCODED_SECRET] Line 3
    하드코딩된 비밀정보: apiKey
    → 환경변수나 Vault를 사용하세요.
  [SQL_INJECTION] Line 7
    SQL Injection 위험: 문자열 연결로 쿼리 생성
    → PreparedStatement를 사용하세요.

ERROR (1개):
  [RESOURCE_LEAK] Line 8
    리소스 누수 위험: Connection이 try-with-resources 없이 생성됨
    → try-with-resources 문을 사용하세요.

WARNING (2개):
  [NULL_POINTER_RISK] Line 11
    Null Pointer 위험: getUser() 결과를 바로 사용
    → null 체크 또는 Optional을 사용하세요.
  [STRING_EQUALITY] Line 15
    문자열 비교에 == 사용
    → equals() 메서드를 사용하세요.

=== 요약 ===
CRITICAL: 3, ERROR: 1, WARNING: 2
```

CRITICAL이 3개나 있어요. **이 코드는 프로덕션에 배포하면 안 돼요!**

---

## 이슈 카탈로그

### 보안 이슈 (CRITICAL)
| 코드 | 설명 | OWASP |
|------|------|-------|
| `SQL_INJECTION` | SQL 쿼리에 문자열 연결 | A03:2021 |
| `HARDCODED_SECRET` | 소스에 비밀정보 | A02:2021 |
| `COMMAND_INJECTION` | Runtime.exec()에 변수 | A03:2021 |
| `XSS` | HTML에 이스케이프 없이 출력 | A03:2021 |

### 버그 위험 (ERROR)
| 코드 | 설명 |
|------|------|
| `RESOURCE_LEAK` | Closeable 리소스 미해제 |
| `NULL_POINTER_RISK` | null 체크 없이 사용 |
| `INFINITE_LOOP` | 종료 조건 없는 루프 |

### 베스트 프랙티스 (WARNING)
| 코드 | 설명 |
|------|------|
| `STRING_EQUALITY` | 문자열 == 비교 |
| `SYSTEM_EXIT` | System.exit() 호출 |

---

## 핵심 정리

1. **이슈 vs 스멜** → 이슈는 당장 고쳐야 하는 위험한 코드
2. **심각도 분류** → CRITICAL > ERROR > WARNING > INFO
3. **보안 이슈** → SQL Injection, 하드코딩된 비밀번호 등
4. **OWASP** → 보안 취약점의 국제 표준 분류

---

## 다음 시간 예고

이제 우리는 이런 걸 알아냈어요:
- 메트릭: "복잡도 15, 메서드 10개"
- 스멜: "긴 메서드 2개, 깊은 중첩 1개"
- 이슈: "SQL Injection 1개, 하드코딩 2개"

근데 이게 좋은 건가요? 나쁜 건가요? 숫자만 봐서는 모르겠어요.

다음 STEP에서는 이 모든 걸 종합해서 **"85점, B등급"**처럼 점수로 바꾸는 방법을 알아볼게요!

---

## 실습

```bash
cd code-ai-part2-analyzer
../gradlew :step11-detection:run
```

여러분의 코드에서 보안 취약점을 찾아보세요. CRITICAL이 있다면 지금 바로 고쳐야 해요!
