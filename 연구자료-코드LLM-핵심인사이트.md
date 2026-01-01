# 코드 특화 LLM 연구 핵심 인사이트
## GPT 조사 자료 분석 및 Fork 계획 연계

> **출처**: "코드 특화 LLM vs 일반 LLM: 토크나이저와 문맥 모델링의 차이.pdf"
> **분석 목적**: Code-AI Fork 개발에 적용할 핵심 기술 파악

---

## 🎯 가장 중요한 발견

### 1. 토크나이저가 모든 것을 결정한다

**문제 상황**:
```python
# 동일한 코드, 공백 하나 차이
".factorial"  → [".factor", "ial"]  # 틀린 분절!
". factorial" → [".", "␣factorial"]  # 다른 분절!

→ 모델 출력이 완전히 달라짐!
```

**핵심**:
- BPE는 "통계적으로 자주 나온 문자열"을 합치는데, 이게 코드 문법과 안 맞음
- **공백 하나로 토큰 시퀀스가 완전히 바뀜 = 예측 불안정**

**우리 프로젝트에 적용**:
```java
// ❌ 기존 WhitespaceTokenizer
"public void getName() {"
→ ["public", "void", "getName()", "{"]  // 괄호가 붙어버림!

// ✅ 개선된 CodeTokenizer 필요
"public void getName() {"
→ ["public", "void", "getName", "(", ")", "{"]  // 문법 단위로 분리!
```

---

### 2. 들여쓰기 토큰 낭비의 심각성

**GPT-2 vs GPT-4 비교** (문서에서 가장 충격적인 부분!):

```python
def categorize_number(number):
    if number > 0:
        print("positive")
    elif number % 2 == 0:
        print("even")
    else:
        print("negative")
```

| 모델 | 토큰 수 | 들여쓰기 처리 |
|------|---------|--------------|
| GPT-2 | 147개 | 공백 1개 = 토큰 1개 (4칸 들여쓰기 = 4토큰!) |
| GPT-4 | 70개 | 공백 묶음 = 토큰 1개 (4칸 들여쓰기 = 1토큰) |

**비용 차이**: 같은 코드를 처리하는데 **2배 이상** 토큰 차이!

**우리 프로젝트에 적용**:
```java
public class CodeTokenizer {

    private List<String> tokenize(String code) {
        // ❌ 나쁜 방법: 공백마다 토큰
        // "    if" → ["␣", "␣", "␣", "␣", "if"]  // 5토큰

        // ✅ 좋은 방법: 들여쓰기 묶기
        // "    if" → ["INDENT_4", "if"]  // 2토큰

        // ✅ 더 좋은 방법: 들여쓰기 레벨
        // "    if" → ["INDENT_1", "if"]  // 레벨로 표현
    }
}
```

---

### 3. 코드 전용 특수 토큰

**Code Llama의 혁신**:
```
<PRE>  : 앞부분 (prefix)
<MID>  : 채울 부분 (middle)
<SUF>  : 뒷부분 (suffix)
```

**사용 예시** (Fill-in-the-Middle):
```java
<PRE>
public class User {
    private String name;
<MID>
<SUF>
    public String getName() {
        return name;
    }
}

→ 모델이 <MID> 부분에 생성자를 채워넣음:
    public User(String name) {
        this.name = name;
    }
```

**우리 프로젝트에 적용**:
```java
// 우리도 특수 토큰 정의
<BLOCK_START>
<BLOCK_END>
<INDENT_N>
<METHOD_START>
<CLASS_START>
```

---

### 4. Context Window의 중요성

**모델별 비교**:

| 모델 | Context Window | 코드 활용 |
|------|---------------|----------|
| GPT-4 | 32K 토큰 | 중형 파일 |
| Claude 2 | 100K 토큰 | **전체 프로젝트!** |
| Code Llama | 16K → 100K | 여러 파일 |
| Gemini 3 | **1M 토큰** | 거의 무제한 |

**실용적 의미**:
```
32K 토큰 = 약 24,000 단어 = 코드 약 800줄
100K 토큰 = 약 75,000 단어 = 코드 약 2,500줄
1M 토큰 = 약 750,000 단어 = 전체 코드베이스!
```

**우리 Bigram의 한계**:
- Bigram: **직전 1개만** 기억
- Trigram: **직전 2개만** 기억
- 실전 코드: 수백 줄 위의 변수/함수 참조 필요

**해결 방향**:
1. N-gram N 늘리기 (5-gram, 10-gram)
2. 중요한 토큰만 기억 (키워드, 식별자)
3. 구조 정보 활용 (들여쓰기 레벨)

---

### 5. 코드 구조 이해의 중요성

**문서의 핵심 주장**:
> "LLM은 AST를 직접 사용하지 않지만,
> 대량의 코드 학습으로 **암묵적으로** 구조를 학습한다"

**예시**:
```python
if condition:
    do_something()
else:
    do_other()
```

**Transformer가 학습하는 패턴**:
- `if` 다음에 `:`가 온다
- `:` 다음에 들여쓰기가 증가한다
- `else` 전에 들여쓰기가 감소한다
- `{`와 `}`는 짝을 이룬다

**우리 Bigram으로 할 수 있는 것**:
```java
// 학습 데이터:
"if ( condition ) {"
"for ( int i = 0 ;"
"} else {"

// Bigram 학습:
"if" → "(" (99%)
"(" → "condition" (변수명 다양)
")" → "{" (80%)
"{" → "\n" (95%)

// 한계:
- 여는 {와 닫는 }의 매칭은 못 함 (거리가 멀어서)
- 하지만 바로 다음 패턴은 학습 가능!
```

---

### 6. 식별자(변수명) 처리의 어려움

**문서의 발견**:
> "코드 LLM은 **구문 토큰** 간 관계는 잘 학습하지만,
> **식별자와 구문**의 관계는 덜 명시적"

**무슨 뜻?**
```java
// 잘 학습하는 것:
if → ( → ) → {
for → ( → ; → ) → {

// 못 학습하는 것:
if (user == null) → user 스코프 인식
for (int i = ...) → i의 유효 범위
```

**실전 문제**:
```java
public void method1() {
    String name = "foo";
}

public void method2() {
    // ❌ 모델이 여기서 name을 제안할 수 있음
    // (스코프 밖인데!)
}
```

**우리 프로젝트 대응**:
- 완벽한 스코프 인식은 어려움 (n-gram 한계)
- 하지만 "자주 쓰는 변수명" 제안은 가능
- 예: `User` 클래스 → `user`, `userId`, `username` 제안

---

## 🔧 우리 CodeTokenizer 설계 지침

### 참고한 모델들의 접근:

1. **GPT-4 방식**:
   - 반복 공백을 하나로 묶음
   - 100K 어휘로 코드 패턴 포함

2. **Code Llama 방식**:
   - SentencePiece 기반 (32K 어휘)
   - 특수 토큰 추가 (`<PRE>`, `<MID>`, `<SUF>`)

3. **우리 방식 (실용적 절충)**:
   ```java
   public class CodeTokenizer implements Tokenizer {

       @Override
       public List<String> tokenize(String code) {
           List<String> tokens = new ArrayList<>();

           // 1. 들여쓰기 감지 및 묶기
           tokens.addAll(handleIndentation(code));

           // 2. 키워드 인식
           tokens.addAll(tokenizeKeywords(code));

           // 3. 괄호/세미콜론 분리
           tokens.addAll(splitSymbols(code));

           // 4. 식별자 유지 (camelCase 보존)
           tokens.addAll(preserveIdentifiers(code));

           return tokens;
       }

       private List<String> handleIndentation(String code) {
           // GPT-4처럼: 공백 묶음을 하나의 토큰으로
           // "    " → "INDENT_4"
       }

       private List<String> tokenizeKeywords(String code) {
           // Java 키워드: public, private, class, if, for, ...
           // 절대 쪼개지 않음!
       }

       private List<String> splitSymbols(String code) {
           // (, ), {, }, ;, . 등은 독립 토큰
           // "getName()" → ["getName", "(", ")"]
       }
   }
   ```

---

## 📊 Benchmark 참고

**HumanEval** (코드 생성 벤치마크):

| 모델 | Pass@1 | 특징 |
|------|--------|------|
| GPT-4 | 80%+ | 최고 성능 |
| Claude 2 | ~80% | GPT-4 수준 |
| Code Llama 70B | 53% | 오픈소스 최고 |

**우리 Bigram 기대치**:
- HumanEval: ~5-10% (매우 낮을 것)
- 하지만 **교육 목적**과 **단순 자동완성**에는 충분!

**현실적 목표**:
```
❌ "복잡한 알고리즘 문제 풀기"
✅ "자주 쓰는 패턴 자동완성"
✅ "boilerplate 코드 생성"
✅ "코드 패턴 학습 체험"
```

---

## 💡 즉시 적용 가능한 개선

### 개선 1: 들여쓰기 토큰 압축

**Before (현재)**:
```
"    if (true) {"
→ ["    ", "if", "(true)", "{"]  // 공백이 통째로
```

**After (개선)**:
```
"    if (true) {"
→ ["INDENT_1", "if", "(", "true", ")", "{"]  // 레벨 표현
```

### 개선 2: 심볼 분리

**Before**:
```
"getName()"
→ ["getName()"]  // 괄호가 붙어있음
```

**After**:
```
"getName()"
→ ["getName", "(", ")"]  // 문법 단위
```

### 개선 3: 키워드 보호

**Before**:
```
"publicvoid"  // 띄어쓰기 없으면
→ ["publicvoid"]  // 하나로 인식
```

**After**:
```
"publicvoid"
→ ["public", "void"]  // 키워드는 강제 분리
```

---

## 🎯 Fork 프로젝트 적용 계획

### Phase 1: 기본 CodeTokenizer (1-2주)

**구현 우선순위**:
1. ✅ **들여쓰기 압축** (GPT-4 방식)
   - 가장 큰 효과 (토큰 수 50% 절감)

2. ✅ **괄호/세미콜론 분리**
   - 문법 정확도 향상

3. ✅ **키워드 인식**
   - Java 키워드 리스트 작성

4. ⏸️ **특수 토큰** (나중에)
   - Fill-in-the-Middle은 고급 기능

### Phase 2: 코드 코퍼스 (1주)

**수집 전략** (문서 기반):
- GitHub 공개 코드 (Code Llama: 500B 토큰!)
- 우리는 작게: 100MB ~ 1GB 정도
- 품질 > 양

**초점**:
- Spring Boot 패턴
- 자주 쓰는 boilerplate
- 표준 디자인 패턴

### Phase 3: Context 확장 (2주)

**Bigram → Trigram**:
- 직전 1개 → 직전 2개
- 토큰 조합 폭발 문제 해결:
  - Smoothing
  - Backoff
  - 희소성 처리

**목표**:
- "public static void" 같은 3-word 패턴 학습
- 더 자연스러운 코드 생성

---

## 📚 참고 논문 & 자료

1. **TokDrift 논문**: 토큰화가 코드 예측에 미치는 영향
2. **Code Llama 문서**: Fill-in-the-Middle, Long Context
3. **GPT-4 토크나이저**: 들여쓰기 압축 기법
4. **Claude 100K**: 긴 컨텍스트 활용 사례

---

## ✅ 핵심 Takeaways

### 1. **토크나이저가 80%**
   - 아무리 좋은 모델도 토크나이저가 나쁘면 망함
   - 우리 프로젝트: CodeTokenizer에 집중!

### 2. **들여쓰기 압축 필수**
   - GPT-2 → GPT-4: 147토큰 → 70토큰
   - 구현 쉬움 + 효과 큼

### 3. **Context는 길수록 좋지만...**
   - Bigram/Trigram: 짧은 컨텍스트
   - 하지만 "바로 다음" 예측에는 충분

### 4. **완벽함 < 실용성**
   - GPT-4처럼 80% 정확도는 무리
   - 하지만 "자주 쓰는 패턴"만 잘해도 유용

### 5. **교육 + 실용 겸용**
   - 토큰 개념 학습 (교육)
   - 단순 자동완성 (실용)

---

**이 문서를 기반으로 CodeTokenizer 설계를 구체화하겠습니다!** 🚀

GPT가 찾아준 자료 덕분에 방향이 명확해졌습니다!
