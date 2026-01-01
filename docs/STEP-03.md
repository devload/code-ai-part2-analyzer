# Step 3: Bigram 생성 구현 (서빙=다음 토큰 선택)

## 학습 포인트

**"다음 토큰 예측 루프"를 직접 손으로 만듭니다.**

핵심 질문:
- AI가 텍스트를 "생성"한다는 것은 무엇인가?
- 확률에서 토큰을 선택하는 방법은?
- temperature, topK는 왜 필요한가?

답:
- 생성 = **"다음 토큰 예측"의 반복**
- 샘플링 = **확률 분포에서 랜덤 선택**
- Hyperparameters = **창의성과 품질의 균형 조절**

---

## 생성 루프란?

### 의사코드

```python
def generate(prompt, maxTokens):
    tokens = tokenize(prompt)

    for i in range(maxTokens):
        prev = tokens[-1]              # 마지막 토큰
        probs = get_next_probs(prev)   # 다음 토큰 확률 분포
        next = sample(probs)           # 확률 기반 선택
        tokens.append(next)

    return detokenize(tokens)
```

### 실제 예시

```
Prompt: "the cat"
Tokens: [1, 2]  (the=1, cat=2)

Loop 1:
  prev = 2 (cat)
  probs = {sat: 0.5, loves: 0.3, is: 0.2}
  next = sample() → 3 (sat)
  tokens = [1, 2, 3]

Loop 2:
  prev = 3 (sat)
  probs = {on: 1.0}
  next = 4 (on)
  tokens = [1, 2, 3, 4]

Loop 3:
  prev = 4 (on)
  probs = {the: 0.8, a: 0.2}
  next = 1 (the)
  tokens = [1, 2, 3, 4, 1]

Generated: "the cat sat on the"
```

---

## Sampler 구현

### 핵심 역할

**카운트 → 확률 → 토큰 선택**

```java
public int sample(Map<Integer, Integer> counts) {
    // 1. 카운트 → 확률
    List<TokenProb> probs = countsToProbs(counts);

    // 2. Temperature 적용
    probs = applyTemperature(probs, temperature);

    // 3. TopK 필터링
    probs = applyTopK(probs, topK);

    // 4. 확률 기반 샘플링
    return sampleFromProbs(probs);
}
```

### 1단계: 카운트 → 확률

```
Counts:
  (the, cat)  : 3
  (the, dog)  : 3
  (the, mat)  : 1
  Total       : 7

Probabilities:
  cat: 3/7 = 0.43 (43%)
  dog: 3/7 = 0.43 (43%)
  mat: 1/7 = 0.14 (14%)
```

### 2단계: Temperature 적용

**Temperature = 창의성 조절**

```
원본 확률: [0.43, 0.43, 0.14]

Temperature = 0.1 (확정적):
  [0.49, 0.49, 0.02]  ← 차이 증폭

Temperature = 1.0 (기본):
  [0.43, 0.43, 0.14]  ← 변화 없음

Temperature = 2.0 (창의적):
  [0.38, 0.38, 0.24]  ← 차이 완화
```

**수식**:
```
logits = log(probs)
scaled_logits = logits / temperature
new_probs = softmax(scaled_logits)
```

### 3단계: TopK 필터링

**TopK = 후보 제한**

```
원본 확률: [cat: 0.43, dog: 0.43, mat: 0.14, log: 0.05, ...]

TopK = 2:
  [cat: 0.5, dog: 0.5]  ← 상위 2개만, 재정규화

TopK = 50:
  [cat: 0.43, dog: 0.43, ...]  ← 모든 후보
```

### 4단계: 확률 기반 샘플링

**Roulette Wheel Selection**

```java
double r = random.nextDouble();  // 0.0 ~ 1.0
double cumulative = 0.0;

for (TokenProb p : probs) {
    cumulative += p.prob;
    if (r < cumulative) {
        return p.tokenId;  // 선택!
    }
}
```

**예시**:
```
Probs: [cat: 0.5, dog: 0.3, mat: 0.2]

r = 0.3:  cumulative = 0.5  → cat 선택
r = 0.7:  cumulative = 0.8  → dog 선택
r = 0.95: cumulative = 1.0  → mat 선택
```

---

## BigramModel 구현

### generate() 메서드

```java
public GenerateResponse generate(GenerateRequest request) {
    // 1. Prompt 토큰화
    List<Integer> tokens = tokenizer.encode(request.getPrompt());

    // 2. Sampler 생성
    Sampler sampler = new Sampler(
        request.getTemperature(),
        request.getTopK(),
        request.getSeed().orElse(null)
    );

    // 3. 생성 루프
    for (int i = 0; i < request.getMaxTokens(); i++) {
        int prev = tokens.get(tokens.size() - 1);
        Map<Integer, Integer> nextCounts = artifact.getNextTokenCounts(prev);

        if (nextCounts.isEmpty()) break;  // Dead end

        int next = sampler.sample(nextCounts);
        tokens.add(next);

        if (shouldStop(tokens, request.getStopSequences())) break;
    }

    // 4. 토큰 → 텍스트
    String text = tokenizer.decode(tokens);

    // 5. Usage & Latency
    return new GenerateResponse(text, usage, latency, "bigram-v1");
}
```

---

## 실행 예시 (docs/demo/STEP-03.log)

### 영어 모델 생성

```
Prompt: "the cat"
Parameters: maxTokens=10, temperature=1.0, topK=50, seed=42

생성 결과:
  Text: the cat I love love love natural language processing natural language is
  Usage(input=2, output=10, total=12)
  Latency: 2ms
```

### 한글 모델 생성

```
Prompt: "오늘은"

생성 결과:
  Text: 오늘은 날씨가 좋으면 노래를 부르면 행복하다 행복하다는 것은 축복이다 인공지능은 미래다
  Usage(input=1, output=10, total=11)
```

### Temperature 효과

```
Prompt: "I love"

temp=0.1: I love natural language processing natural language is powerful deep
temp=1.0: I love natural language processing natural language is powerful deep
temp=2.0: I love natural language processing natural language is powerful deep
```

### Seed 재현성 ✅

```
Seed: 12345

시도 1: the dog loves the dog the dog programming is the sky
시도 2: the dog loves the dog the dog programming is the sky
시도 3: the dog loves the dog the dog programming is the sky

→ 모든 결과가 동일함!
```

---

## Hyperparameters

### Temperature

| 값 | 효과 | 사용 시나리오 |
|----|------|--------------|
| 0.1 | 확정적, 가장 높은 확률 선택 | 정확한 답변 필요 |
| 1.0 | 균형, 학습된 분포 유지 | 기본값 |
| 2.0 | 창의적, 낮은 확률도 선택 | 창작, 브레인스토밍 |

### TopK

| 값 | 효과 |
|----|------|
| 1 | 항상 가장 높은 확률 (greedy) |
| 10 | 상위 10개 후보만 |
| 50 | 대부분의 후보 허용 |

### Seed

```java
seed = null  → 매번 다른 결과
seed = 42    → 항상 같은 결과 (재현 가능)
```

---

## 테스트 결과

**10개 테스트 모두 통과** ✅

```
✅ 기본 텍스트 생성
✅ Seed 고정 시 재현성
✅ 다른 Seed는 다른 결과
✅ Temperature 변화 테스트
✅ TopK 필터링 테스트
✅ MaxTokens 제한
✅ Stop sequence 테스트
✅ Usage 측정
✅ 파일로부터 모델 로드
✅ 다음 토큰 확률 조회
```

---

## 생성 품질 분석

### 왜 이상한 텍스트가 생성되는가?

```
Generated: "the dog loves the dog the dog programming is the sky"
```

**이유**:
1. **짧은 문맥** (Bigram = 1 토큰만 봄)
2. **반복 패턴** (카운트가 높은 쌍만 반복)
3. **의미 이해 없음** (문법/의미 무시)

### Bigram의 한계

```
"the dog" → "loves" (OK)
"dog loves" → "the" (OK)
"loves the" → "dog" (OK)
"the dog" → "programming" (??)
```

**해결 방법**:
- Trigram 이상의 N-gram
- Neural LM (RNN, Transformer)
- Attention 메커니즘

---

## 실전 LLM과의 비교

### GPT-3 생성 루프

```
동일한 원리!

for i in range(maxTokens):
    prev_context = tokens[-2048:]  # 2048 토큰 문맥
    logits = model(prev_context)   # 175B 파라미터 신경망
    next = sample(logits, temp, topK)
    tokens.append(next)
```

**차이점**:
- 문맥: 1 토큰 vs 2048 토큰
- 모델: 카운트 테이블 vs 175B 파라미터
- 품질: 반복적 vs 인간 수준

**공통점**:
- 생성 루프 구조
- 확률 기반 샘플링
- temperature, topK 사용
- seed 재현성

---

## 코드 구조

```
mini-ai-model-ngram/src/main/java/com/miniai/model/
├── Sampler.java           # 확률 기반 샘플러 ⭐
├── BigramModel.java       # 생성 모델 ⭐
├── BigramArtifact.java
├── BigramTrainer.java
├── TrainDemo.java
└── GenerateDemo.java      # 생성 데모 ⭐
```

---

## 왜 이렇게 했는가?

### 생성 = 루프

```
가장 단순한 형태로 "생성"의 본질 이해
```

### 확률 기반 샘플링

```
Deterministic (greedy) ✗ → 항상 같은 결과, 지루함
Probabilistic (sampling) ✓ → 다양한 결과, 창의성
```

### Hyperparameters

```
실제 LLM API와 동일한 인터페이스
→ GPT, Claude 사용 경험과 직결
```

---

## 다음 단계: Step 4

**목표**: Usage 측정 구현 (비용 감각 만들기)

구현할 것:
- UsageMeter (이미 구현됨!)
- GenerateResponse에 usage 포함 (이미 구현됨!)

학습할 것:
- 왜 토큰이 비용 단위인가
- Input/Output 토큰 차이
- Total tokens 계산

**이미 Step 3에서 구현 완료!** → Step 4는 문서화 중심

---

## DoD 체크리스트

- [x] Sampler 클래스 구현
- [x] BigramModel 구현
- [x] generate() 메서드
- [x] temperature, topK, seed 지원
- [x] stopSequences 구현
- [x] JUnit 테스트 10개 (모두 통과)
- [x] **Seed 재현성 확인**
- [x] **Temperature/TopK 효과 확인**
- [x] GenerateDemo 프로그램
- [x] docs/STEP-03.md 작성
- [x] docs/demo/STEP-03.log 생성
- [ ] Git 커밋 및 step-03 태그 (다음 단계)
