# Step 8: N-gram 한계와 개선

## 학습 포인트

**"왜 Trigram만으로는 부족한가?"와 "어떻게 개선하는가?"를 배웁니다.**

---

## 현재 상황 (Trigram 모델)

### 구현 완료된 기능

```
Phase 0: Fork 설정         ✅
Phase 1: CodeTokenizer    ✅ (10개 테스트)
Phase 2: 코드 코퍼스       ✅ (1,323줄 Java)
Phase 3: Trigram 모델      ✅ (7개 테스트)
Phase 4: CLI 개선         ✅ (complete 명령어)
```

### 현재 모델 통계

```
Vocabulary: 960 토큰
Total Trigrams: 7,806개
Total Bigrams: 7,807개
Backoff Weight: 0.4 (40% Bigram, 60% Trigram)
```

### 코드 자동완성 예시

```bash
$ code-ai complete "public class User {" -n 3

[1] public class User { private User > findAll () ; List <
[2] public class User { return new Cat () ; / UUID .
[3] public class User { this : items ) ; } @ GetMapping (
```

---

## 한계점 분석

### 1. 짧은 문맥 (Context Window)

```
Bigram:  P(next | prev)           → 1 토큰
Trigram: P(next | prev1, prev2)   → 2 토큰
5-gram:  P(next | prev1..prev4)   → 4 토큰  ← 개선 목표
```

**문제 예시:**
```java
// Trigram이 보는 것: "= 0 ;"
// 실제 필요한 문맥: "for (int i = 0 ; i < length"
for (int i = 0; |  // 여기서 다음 토큰 예측
```

### 2. 희소성 (Sparsity)

```
Trigram 조합 수 = V × V × V = 960³ = 884,736,000
실제 학습된 Trigram = 7,806
커버리지 = 0.0009%
```

**해결책:**
- Backoff: Trigram → Bigram → Unigram 폴백
- Smoothing: 본 적 없는 조합에도 확률 부여
- **Kneser-Ney**: 가장 효과적인 Smoothing 기법

### 3. 의미 무시 (No Semantics)

```java
// N-gram은 단순 통계
String name;     // "name" 다음에 ";" 자주 옴
String email;    // "email" 다음에도 ";" 자주 옴

// 변수명의 의미를 이해하지 못함
String user_name;   // camelCase vs snake_case 무관
String userName;    // 둘 다 같은 패턴으로 인식
```

---

## 개선 방안 개요

### 1단계: 현재 아키텍처 개선 (이번 Step)

| 개선 항목 | 효과 | 구현 난이도 |
|-----------|------|-------------|
| 5-gram 확장 | 문맥 2배 증가 | 낮음 |
| Kneser-Ney Smoothing | 희소성 해결 | 중간 |
| AST 토큰화 | 문법 구조 인식 | 중간 |

### 2단계: 신경망 도입 (미래)

```
LSTM/GRU → 긴 의존성
Pointer Network → 변수명 복사
Transformer → 병렬 처리 + Attention
```

### 3단계: 사전학습 LLM 활용 (미래)

```
Qwen2.5-Coder, CodeLlama, DeepSeek-Coder
→ 즉시 높은 품질, 로컬 실행 가능
```

---

## 이론: Kneser-Ney Smoothing

### 왜 단순 Backoff는 부족한가?

```
문제: "San Francisco" vs "I saw a"

"Francisco"는 거의 "San" 뒤에만 나옴
"saw"는 다양한 문맥에서 나옴

단순 Backoff:
  P(Francisco) = count(Francisco) / total
  → "Francisco"가 자주 등장하면 확률 높음 (잘못됨!)

Kneser-Ney:
  P_KN(Francisco) = 다양한 문맥에서 등장한 횟수 기반
  → "Francisco"는 특정 문맥에만 등장하므로 낮은 확률
```

### Kneser-Ney 수식

```
P_KN(w_i | w_{i-1}) = max(count(w_{i-1}, w_i) - d, 0) / count(w_{i-1})
                     + λ(w_{i-1}) × P_continuation(w_i)

여기서:
  d = 할인 상수 (보통 0.75)
  λ(w_{i-1}) = 정규화 가중치
  P_continuation(w_i) = |{w : count(w, w_i) > 0}| / |all bigrams|
```

### 핵심 아이디어: Continuation Count

```
일반 Count:
  count("Francisco") = 100 (문서에서 100번 등장)

Continuation Count:
  continuation("Francisco") = 1 (오직 "San" 뒤에서만)
  continuation("the") = 500 (500개 다른 단어 뒤에서)

→ "the"가 더 일반적인 단어이므로 backoff 시 더 높은 확률
```

---

## 구현 계획

### 파일 구조

```
mini-ai-model-ngram/
├── src/main/java/com/miniai/model/
│   ├── ngram/
│   │   ├── NgramArtifact.java       ← 신규 (N-gram 일반화)
│   │   ├── NgramTrainer.java        ← 신규 (N-gram 일반화)
│   │   ├── NgramModel.java          ← 신규 (N-gram 일반화)
│   │   ├── BigramArtifact.java      (유지)
│   │   ├── TrigramArtifact.java     (유지)
│   │   └── ...
│   └── smoothing/
│       ├── SmoothingStrategy.java   ← 신규 (인터페이스)
│       ├── SimpleBackoff.java       ← 신규 (현재 방식)
│       └── KneserNey.java           ← 신규 (개선된 방식)
└── src/test/java/com/miniai/model/
    ├── NgramModelTest.java          ← 신규
    └── KneserNeyTest.java           ← 신규
```

### 구현 순서

```
1. NgramArtifact: N을 파라미터로 받는 일반화된 Artifact
2. NgramTrainer: N-gram 학습기 일반화
3. NgramModel: N-gram 모델 일반화
4. SmoothingStrategy: Smoothing 인터페이스
5. KneserNey: Kneser-Ney Smoothing 구현
6. 테스트 작성
```

---

## 기대 효과

### Before (Trigram + Simple Backoff)

```
입력: "for (int i = 0;"
출력: [랜덤한 토큰들]
품질: 낮음 (문맥 부족)
```

### After (5-gram + Kneser-Ney)

```
입력: "for (int i = 0;"
출력: "i < length ; i ++"
품질: 중간 (더 긴 문맥 + 더 나은 smoothing)
```

### 측정 지표

```
1. Perplexity: 낮을수록 좋음
   - Trigram: ~500
   - 5-gram + KN: ~200 (목표)

2. 정확도: 상위 5개 후보에 정답 포함률
   - Trigram: 30%
   - 5-gram + KN: 50% (목표)
```

---

## DoD (Definition of Done)

- [ ] NgramArtifact 구현 (N 파라미터화)
- [ ] NgramTrainer 구현 (N-gram 학습)
- [ ] NgramModel 구현 (N-gram 생성)
- [ ] KneserNey Smoothing 구현
- [ ] 테스트 작성 (최소 10개)
- [ ] 5-gram으로 학습 및 비교 실험
- [ ] 문서 업데이트

---

## 다음 Step 미리보기

### Step 9: AST 기반 토큰화

```java
// 현재: 단순 토큰 시퀀스
["public", "class", "User", "{", "}"]

// 개선: AST 구조 인식
{
  type: "ClassDeclaration",
  name: "User",
  modifiers: ["public"],
  body: { ... }
}
```

### Step 10: 신경망 언어 모델

```
LSTM을 이용한 언어 모델
→ 100+ 토큰 문맥 처리 가능
→ 희소성 문제 근본 해결
```

---

## 참고 자료

- [Kneser-Ney Smoothing 논문](https://www.cs.cmu.edu/~roni/papers/95kneser.pdf)
- [Speech and Language Processing (Jurafsky)](https://web.stanford.edu/~jurafsky/slp3/)
- [N-gram Language Models Tutorial](https://nlp.stanford.edu/IR-book/html/htmledition/language-models-1.html)
