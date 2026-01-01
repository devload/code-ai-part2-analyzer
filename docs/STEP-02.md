# Step 2: Bigram 학습 구현 (학습=세기)

## 학습 포인트

**학습은 "카운트 테이블 만들기"라는 감각을 잡습니다.**

핵심 질문:
- AI 모델의 "학습"이란 무엇인가?
- Bigram은 무엇이고, 왜 유용한가?
- 학습 결과는 어떻게 저장하고 재사용하는가?

답:
- 학습 = **데이터에서 패턴 추출**
- Bigram = "A 다음에 B가 올 확률"을 세는 가장 단순한 모델
- Artifact = 학습 결과를 **JSON으로 저장**하여 재사용

---

## Bigram이란?

### 정의

**Bigram** = 연속된 두 개의 토큰 쌍

```
텍스트: "the cat sat on the mat"
토큰:   [the, cat, sat, on, the, mat]

Bigrams:
  (the, cat)
  (cat, sat)
  (sat, on)
  (on, the)
  (the, mat)
```

### 왜 필요한가?

Bigram은 **"다음에 무엇이 올까?"** 를 예측하는 가장 단순한 방법입니다.

```
"I love ___"

Bigram 카운트:
  (I, love) → 10회
  (love, you) → 5회
  (love, coffee) → 3회
  (love, programming) → 2회

예측: "you"가 가장 높은 확률 (50%)
```

---

## Bigram 학습 과정

### 1단계: 토큰화

```
Corpus: "the cat sat on the mat"
     ↓ (Tokenizer)
Tokens: [1, 2, 3, 4, 1, 5]
```

### 2단계: Bigram 추출

```
Tokens: [1, 2, 3, 4, 1, 5]

Bigram 쌍:
  (1, 2)  ← "the" → "cat"
  (2, 3)  ← "cat" → "sat"
  (3, 4)  ← "sat" → "on"
  (4, 1)  ← "on" → "the"
  (1, 5)  ← "the" → "mat"
```

### 3단계: 카운트 생성

```java
Map<Integer, Map<Integer, Integer>> counts

counts[1][2] = 1  // "the" → "cat" 1회
counts[1][5] = 1  // "the" → "mat" 1회
counts[2][3] = 1  // "cat" → "sat" 1회
...
```

**핵심**: `counts[prev][next]++` (단순 카운팅!)

### 4단계: Artifact 저장

```json
{
  "counts": {
    "1": { "2": 1, "5": 1 },
    "2": { "3": 1 },
    "3": { "4": 1 },
    "4": { "1": 1 }
  },
  "vocabulary": {
    "the": 1,
    "cat": 2,
    "sat": 3,
    ...
  },
  "metadata": {
    "totalTokens": 6,
    "totalBigrams": 5
  }
}
```

---

## 학습 예시

### 예시 1: 영어 Corpus

**입력**:
```
the cat sat on the mat
the dog sat on the log
the bird sat on the wire
```

**Vocabulary**: `{the, cat, dog, bird, sat, on, mat, log, wire}`

**주요 Bigram**:
```
[sat] → [on]  : 3회  (가장 빈도 높음!)
[on]  → [the] : 3회
[the] → [cat] : 1회
[the] → [dog] : 1회
[the] → [bird]: 1회
```

**학습 결과 해석**:
- "sat" 다음에는 항상 "on"이 온다 (100%)
- "on" 다음에는 항상 "the"가 온다 (100%)
- "the" 다음에는 cat/dog/bird가 골고루 온다 (33% 씩)

### 예시 2: 한글 Corpus

**입력**:
```
오늘은 날씨가 좋다
내일도 날씨가 좋을까
날씨가 좋으면 산책을 간다
```

**주요 Bigram**:
```
[날씨가] → [좋다]  : 1회
[날씨가] → [좋을까]: 1회
[날씨가] → [좋으면]: 1회
```

**학습 결과 해석**:
- "날씨가" 다음에는 "좋다", "좋을까", "좋으면"이 각각 1/3 확률

---

## BigramArtifact 구조

### counts (카운트 테이블)

```java
Map<Integer, Map<Integer, Integer>> counts
```

- **구조**: `counts[prev_token][next_token] = count`
- **의미**: prev 다음에 next가 몇 번 나왔는지
- **예시**: `counts[42][123] = 5` → 토큰 42 다음에 토큰 123이 5번

### vocabulary (어휘 사전)

```java
Map<String, Integer> vocabulary
```

- Tokenizer의 vocabulary와 동일
- 학습 시 사용한 어휘를 artifact에 저장하여 재사용

### metadata (메타데이터)

```java
class Metadata {
    String modelType = "bigram";
    String tokenizerType;
    int vocabSize;
    int totalTokens;
    int totalBigrams;
    String trainedAt;
    String corpusInfo;
}
```

- 학습 시간, 데이터 크기 등 메타정보

---

## BigramTrainer 구현

### 핵심 메서드

```java
public BigramArtifact trainFromText(String corpus, Tokenizer tokenizer) {
    // 1. 토큰화
    List<Integer> tokens = tokenizer.encode(corpus);

    // 2. Bigram 카운트
    Map<Integer, Map<Integer, Integer>> counts = new HashMap<>();
    for (int i = 0; i < tokens.size() - 1; i++) {
        int prev = tokens.get(i);
        int next = tokens.get(i + 1);

        // counts[prev][next]++
        counts.putIfAbsent(prev, new HashMap<>());
        counts.get(prev).put(
            next,
            counts.get(prev).getOrDefault(next, 0) + 1
        );
    }

    // 3. Artifact 생성 및 반환
    return new BigramArtifact(counts, vocabulary, metadata);
}
```

### 파일 저장/로드

```java
// 저장
trainer.train(corpusPath, outputPath);
// → bigram.json 생성

// 로드
BigramArtifact artifact = BigramTrainer.loadArtifact(outputPath);
```

---

## 실행 예시 (docs/demo/STEP-02.log)

### 영어 Corpus 학습

```
[1] 영어 Corpus 학습
------------------------------------------------------------
Corpus: /Users/devload/aimaker/data/sample-corpus.txt
Tokenizer vocabulary: 56
✅ 학습 완료: /Users/devload/aimaker/data/sample-bigram.json
   Vocabulary: 56
   Total tokens: 103
   Total bigrams: 102

=== Bigram 학습 결과 요약 ===
Metadata(model=bigram, vocab=56, tokens=103, bigrams=102)

가장 빈도 높은 Bigram (상위 5개):
  [on] → [the] : 3회
  [sat] → [on] : 3회
  [the] → [world] : 3회
  [the] → [cat] : 3회
  [the] → [dog] : 3회
```

**해석**:
- 103개 토큰에서 102개 bigram 추출
- "on → the", "sat → on"이 가장 빈번 (3회)

### 한글 Corpus 학습

```
[2] 한글 Corpus 학습
------------------------------------------------------------
Corpus: /Users/devload/aimaker/data/korean-corpus.txt
Tokenizer vocabulary: 41
✅ 학습 완료: /Users/devload/aimaker/data/korean-bigram.json
   Vocabulary: 41
   Total tokens: 47
   Total bigrams: 46

가장 빈도 높은 Bigram (상위 5개):
  [미래를] → [위해] : 1회
  [기분이] → [좋다] : 1회
  [기분이] → [좋으면] : 1회
  ...
```

---

## Artifact JSON 형식

### 실제 JSON 예시 (일부)

```json
{
  "counts": {
    "1": {  // 토큰 1 ("the") 다음에
      "2": 3,   // 토큰 2가 3번
      "5": 1    // 토큰 5가 1번
    },
    "3": {  // 토큰 3 ("sat") 다음에
      "4": 3    // 토큰 4가 3번
    }
  },
  "vocabulary": {
    "[UNK]": 0,
    "the": 1,
    "cat": 2,
    "sat": 3,
    "on": 4,
    "mat": 5
  },
  "metadata": {
    "modelType": "bigram",
    "tokenizerType": "WhitespaceTokenizer",
    "vocabSize": 56,
    "totalTokens": 103,
    "totalBigrams": 102,
    "trainedAt": "2026-01-01T13:10:41.730688Z",
    "corpusInfo": "103 characters, 103 tokens"
  }
}
```

---

## Bigram의 한계

### 1. 짧은 문맥 (Context Window = 1)

```
텍스트: "I love you because you are kind"

Bigram은 1개 토큰만 봄:
  "you" → "because" or "you" → "are" ?

Trigram이라면:
  ("I", "love", "you") → 문맥이 더 명확
```

**해결 방법**: Trigram, 4-gram, ... N-gram

### 2. 희소성 (Sparsity)

```
학습: "I love cats"
추론: "I love dogs"

Bigram ("love", "dogs")는 학습 데이터에 없음!
→ 확률 0 (생성 불가)
```

**해결 방법**:
- Smoothing (확률 보정)
- Backoff (낮은 차수로 후퇴)
- Interpolation (여러 차수 혼합)

### 3. 장거리 의존성 무시

```
텍스트: "The cat that I saw yesterday sat on the mat"

"cat"과 "sat"의 관계를 Bigram은 포착 못 함 (거리가 멀어서)
```

**해결 방법**: Transformer, Attention 메커니즘

### 4. 의미 이해 없음

```
"I love coffee" (5회)
"I hate coffee" (1회)

Bigram은 "love coffee"가 더 흔하다는 것만 알 뿐,
"love"와 "hate"가 반대 의미라는 것은 모름
```

**해결 방법**: Word embeddings, BERT, GPT

---

## 실전 적용

### GPT 이전의 N-gram 모델

- **Google Search**: N-gram 기반 자동완성
- **스팸 필터**: 이메일 패턴 학습
- **음성 인식**: 단어 시퀀스 확률

### 현대 LLM과의 비교

| 특성 | Bigram | GPT-3 |
|------|--------|-------|
| 문맥 | 1 토큰 | 2048 토큰 |
| 파라미터 | 카운트 테이블 | 175B |
| 학습 | 카운팅 | 그래디언트 하강 |
| 의미 이해 | 없음 | 있음 |
| 속도 | 매우 빠름 | 느림 |
| 메모리 | 작음 | 매우 큼 |

**Bigram의 가치**:
- 개념 이해용
- 빠른 프로토타이핑
- 베이스라인 모델

---

## 코드 구조

```
mini-ai-model-ngram/
├── src/main/java/com/miniai/model/
│   ├── BigramArtifact.java     # 학습 결과 저장 형식
│   ├── BigramTrainer.java      # 학습 로직
│   └── TrainDemo.java          # 데모 프로그램
└── src/test/java/com/miniai/model/
    └── BigramTrainerTest.java  # 7개 테스트
```

---

## 왜 이렇게 했는가?

### 학습 = 카운팅

가장 단순한 형태의 학습:
```
for each (prev, next) in corpus:
    counts[prev][next]++
```

- **딥러닝 아님** (그래디언트 없음)
- **통계적 방법** (빈도 기반)
- **해석 가능** (카운트 테이블 직접 확인)

### JSON으로 저장

```java
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String json = gson.toJson(artifact);
Files.writeString(outputPath, json);
```

- **사람이 읽을 수 있음**
- **디버깅 쉬움**
- **재사용 가능**

### 교육 목적

1. **학습의 본질 이해**
   - 복잡한 수식 없이 "세기"로 학습 경험

2. **artifact 개념**
   - 학습 결과 = 재사용 가능한 파일
   - GPT 모델도 동일 (checkpoint 파일)

3. **다음 단계 준비**
   - Step 3에서 이 artifact로 텍스트 생성
   - "학습"과 "추론" 분리

---

## 다음 단계: Step 3

**목표**: Bigram 생성 구현 (서빙=다음 토큰 선택)

구현할 것:
- BigramModel (artifact 로드)
- Sampler (topK, temperature)
- generate(request) 구현

학습할 것:
- **생성 루프**: "다음 토큰 예측"을 반복
- **샘플링**: 확률 기반 토큰 선택
- **재현성**: seed 고정

**준비 완료!** Step 2에서 만든 artifact를 사용합니다.

---

## DoD (Definition of Done) 체크리스트

- [x] BigramArtifact 클래스 구현
- [x] BigramTrainer 구현
- [x] trainFromText() 메서드
- [x] JSON 저장/로드
- [x] JUnit 테스트 7개 작성 (모두 통과)
- [x] Corpus 샘플 파일 생성
- [x] 실제 학습 실행
- [x] docs/STEP-02.md 작성
- [x] docs/demo/STEP-02.log 생성
- [ ] Git 커밋 및 step-02 태그 (다음 단계)

---

## 참고: N-gram 확장

- **Unigram**: 단일 토큰 빈도 (P(token))
- **Bigram**: 2개 토큰 쌍 (P(next | prev))
- **Trigram**: 3개 토큰 (P(next | prev1, prev2))
- **4-gram, 5-gram, ...**

**Trade-off**:
- N이 클수록 문맥 많지만, 희소성 증가
- N이 작을수록 희소성 낮지만, 문맥 부족

**Step 7에서 Trigram 확장 포인트를 확보합니다!**
