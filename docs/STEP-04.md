# Step 4: Usage 측정 (비용 감각 만들기)

## 학습 포인트

**"왜 토큰이 비용 단위인지"를 시스템이 직접 보여주게 만듭니다.**

핵심 질문:
- 왜 AI 서비스는 토큰으로 과금하는가?
- Input과 Output 토큰의 차이는?
- Usage 측정은 왜 중요한가?

답:
- 토큰 수 = **모델 처리량** = **계산 비용**
- Input = 읽기, Output = 생성 (출력이 더 비쌈)
- Usage 추적 = **비용 예측 및 제어**

---

## Usage란?

### 정의

**Usage** = AI 서비스 사용량 (토큰 단위)

```java
public class Usage {
    int inputTokens;   // 프롬프트 토큰 수
    int outputTokens;  // 생성된 토큰 수
    int totalTokens;   // 전체 토큰 수 (input + output)
}
```

### 계산 방식

```
Prompt: "the cat"
  → Tokenize → [the, cat]
  → inputTokens = 2

Generated: "the cat sat on the"
  → 추가된 토큰 → [sat, on, the]
  → outputTokens = 3

Total = 2 + 3 = 5
```

---

## 실행 예시 (docs/demo/STEP-04.log)

### 1. 기본 Usage 측정

```
Prompt: "the cat"
MaxTokens: 10

Generated: the cat I love love love natural language processing natural language is

Usage:
  Input tokens:  2
  Output tokens: 10
  Total tokens:  12

검증: input + output = 12 = total 12 ✓
```

### 2. MaxTokens 변화에 따른 Usage

```
Prompt: "I love" (고정)

MaxTokens    Input    Output   Total
----------------------------------------
5            2        5        7
10           2        10       12
20           2        20       22
50           2        50       52

관찰:
  - Input tokens는 항상 동일 (같은 prompt)
  - Output tokens는 maxTokens에 비례
  - Total = Input + Output
```

**학습**: maxTokens를 늘리면 비용이 증가!

### 3. Prompt 길이에 따른 Usage

```
MaxTokens: 5 (고정)

Prompt                    Input    Output   Total
-------------------------------------------------------
"the"                     1        5        6
"the cat"                 2        5        7
"the cat sat on"          4        5        9
"the cat sat on the mat"  6        5        11

관찰:
  - Prompt가 길수록 Input tokens 증가
  - Output은 maxTokens에 의해 제한
  - Total tokens = Input + Output
```

**학습**: Prompt가 길면 Input 비용 증가!

### 4. 비용 계산 시뮬레이션

```
GPT-4 가격 (2024):
  Input:  $0.03 / 1,000 tokens
  Output: $0.06 / 1,000 tokens

Prompt: "the quick brown fox"
MaxTokens: 100

Usage:
  Input tokens:  4 tokens
  Output tokens: 100 tokens
  Total tokens:  104 tokens

예상 비용:
  Input cost:  $0.000120  (4 / 1000 * $0.03)
  Output cost: $0.006000  (100 / 1000 * $0.06)
  Total cost:  $0.006120

1,000번 호출 시: $6.12
10,000번 호출 시: $61.20
```

---

## 왜 토큰이 비용 단위인가?

### 1. 토큰 = 처리량

```
토큰 수가 많을수록:
  - 모델이 더 많이 계산
  - GPU 시간 더 많이 소비
  - 전력 소비 증가
  - 메모리 사용 증가
```

### 2. Input vs Output 비용

**Output이 Input보다 2배 비싼 이유**:

```
Input (읽기):
  - 프롬프트 인코딩
  - 한 번의 forward pass

Output (생성):
  - 각 토큰마다 forward pass 반복
  - N 토큰 생성 = N번의 계산
  - 더 많은 GPU 시간 필요
```

### 3. 실제 LLM 가격 (2024)

| 모델 | Input | Output |
|------|-------|--------|
| GPT-4 | $0.03/1K | $0.06/1K |
| GPT-3.5 | $0.0005/1K | $0.0015/1K |
| Claude 3 Sonnet | $0.003/1K | $0.015/1K |
| Claude 3 Haiku | $0.00025/1K | $0.00125/1K |

**공통점**: Output이 Input보다 비쌈 (약 2~5배)

---

## Usage 측정의 중요성

### 1. 비용 예측

```
"이 API 호출은 얼마나 비용이 들까?"

Usage 없이: ❓ 모름
Usage 있으면: ✓ 정확히 계산 가능
```

### 2. 비용 제어

```
월 예산: $100

실시간 Usage 추적:
  - 현재까지 사용: $45.23
  - 남은 예산: $54.77
  - 제한 도달 시 알림
```

### 3. 최적화

```
Usage 분석:
  - 평균 input: 50 tokens
  - 평균 output: 200 tokens

최적화 방향:
  - Prompt 간결하게 (input 줄이기)
  - maxTokens 조절 (output 제한)
  - 캐싱 활용 (중복 방지)
```

### 4. 할당량 관리

```
API 제한:
  - Free tier: 1M tokens/month
  - 현재 사용: 850K tokens
  - 남은 토큰: 150K tokens
```

---

## 구현 상세

### Usage 클래스 (Step 0에서 정의)

```java
public class Usage {
    private final int inputTokens;
    private final int outputTokens;
    private final int totalTokens;

    public Usage(int inputTokens, int outputTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = inputTokens + outputTokens;
    }
}
```

### BigramModel에서 Usage 계산 (Step 3에서 구현)

```java
public GenerateResponse generate(GenerateRequest request) {
    // 1. Prompt 토큰화
    List<Integer> promptTokens = tokenizer.encode(request.getPrompt());

    // 2. 생성 루프
    List<Integer> generatedTokens = new ArrayList<>(promptTokens);
    for (int i = 0; i < maxTokens; i++) {
        // ... 생성 로직
    }

    // 3. Usage 계산
    Usage usage = new Usage(
        promptTokens.size(),                           // Input
        generatedTokens.size() - promptTokens.size()   // Output
    );

    return new GenerateResponse(text, usage, latency, model);
}
```

**간단!** 토큰 수만 세면 됨.

---

## 실전 활용

### OpenAI API Response

```json
{
  "id": "chatcmpl-123",
  "choices": [{"message": {"content": "Hello!"}}],
  "usage": {
    "prompt_tokens": 10,
    "completion_tokens": 5,
    "total_tokens": 15
  }
}
```

**우리 시스템과 동일한 구조!**

### Anthropic Claude API Response

```json
{
  "content": [{"text": "Hello!"}],
  "usage": {
    "input_tokens": 10,
    "output_tokens": 5
  }
}
```

**용어만 다를 뿐, 본질은 같음!**

---

## Usage 기반 최적화 전략

### 1. Prompt 최적화

```
Before:
  "Please provide a detailed explanation of..."
  → 8 tokens

After:
  "Explain..."
  → 2 tokens

Input 비용: 75% 감소
```

### 2. MaxTokens 조절

```
필요한 답변 길이: ~50 tokens

maxTokens = 1000 ✗ → 낭비
maxTokens = 100  ✓ → 적절
```

### 3. 스트리밍 + 조기 종료

```
stopSequences = ["END", "\n\n"]

불필요한 생성 중단 → Output 비용 절감
```

### 4. 캐싱

```
동일한 Prompt:
  1회: 실제 API 호출
  2회~: 캐시된 결과 반환

비용: 최소화
```

---

## 대화 시스템에서의 Usage

### 문맥 누적

```
Turn 1:
  User: "Hello"
  AI: "Hi!"
  Usage: input=1, output=1, total=2

Turn 2:
  Context: "Hello" + "Hi!" + "How are you?"
  AI: "I'm good!"
  Usage: input=5, output=2, total=7
```

**문제**: 대화가 길어질수록 Input 폭증!

### 문맥 관리 전략

```
1. Sliding Window:
   최근 N개 turn만 유지

2. Summarization:
   긴 대화 → 요약으로 압축

3. Selective Context:
   중요한 부분만 포함
```

---

## 코드 구조

```
mini-ai-core/src/main/java/com/miniai/core/types/
└── Usage.java  (Step 0에서 정의)

mini-ai-model-ngram/src/main/java/com/miniai/model/
├── BigramModel.java  (generate()에서 Usage 계산)
└── UsageDemo.java    (Usage 측정 데모) ⭐
```

---

## 왜 이렇게 했는가?

### 실전 API와 동일한 구조

```
우리 시스템: Usage(input, output, total)
OpenAI:      usage{prompt, completion, total}
Claude:      usage{input, output}

→ 개념 이해 후 실전 API 사용 시 즉시 적응 가능
```

### 비용 감각 훈련

```
"100 tokens는 얼마?"
"1,000번 호출하면?"

→ Usage 측정으로 직접 계산해보며 감각 습득
```

---

## 다음 단계: Step 5

**목표**: Server 만들기 (Spring Boot REST API)

구현할 것:
- POST /v1/train
- POST /v1/generate
- Request/Response JSON
- Latency 측정

학습할 것:
- 모델을 "서빙" 형태로 제공
- HTTP API 설계
- 실전 LLM API와 동일한 구조

**준비 완료!** Usage 측정도 완성되었습니다.

---

## DoD 체크리스트

- [x] Usage 클래스 구현 (Step 0)
- [x] BigramModel에서 Usage 계산 (Step 3)
- [x] GenerateResponse에 usage 포함 (Step 3)
- [x] **input + output = total 일관성 검증**
- [x] UsageDemo 프로그램
- [x] **MaxTokens 변화 → Usage 변화 확인**
- [x] **Prompt 길이 → Usage 변화 확인**
- [x] **비용 계산 시뮬레이션**
- [x] docs/STEP-04.md 작성
- [x] docs/demo/STEP-04.log 생성
- [ ] Git 커밋 및 step-04 태그 (다음 단계)

---

**토큰 = 비용이라는 감각을 직접 체험했습니다!** 💰
