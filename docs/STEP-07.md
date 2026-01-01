# Step 7: 확장 설계 (Trigram 훅)

## 학습 포인트

**"교체 가능한 구조"가 실제로 어떻게 확장되는지 보여줍니다.**

---

## Trigram이 왜 필요한가?

### Bigram의 한계

```
Bigram: P(next | prev)
  → 1개 토큰만 봄
  → 문맥이 짧음
  → 반복적인 텍스트 생성

예시:
"the dog loves the dog loves the dog..."
```

### Trigram의 장점

```
Trigram: P(next | prev1, prev2)
  → 2개 토큰을 봄
  → 문맥이 더 풍부
  → 문장성 향상

예시:
"I love" → "you" (Bigram)
"I love" → "programming" (Trigram, 문맥 고려)
```

---

## 확장 포인트

### 1. NGramModel 인터페이스 (확장 가능)

```java
// 현재 구현
public interface LanguageModel {
    GenerateResponse generate(GenerateRequest request);
}

// 확장 가능 지점
public class BigramModel implements LanguageModel { ... }
public class TrigramModel implements LanguageModel { ... }  // 추가 가능
```

### 2. Artifact 확장

```json
{
  "modelType": "trigram",  // bigram → trigram
  "counts": {
    // Bigram: "prev → next → count"
    // Trigram: "(prev1,prev2) → next → count"
  },
  "backoffWeights": { ... },  // 확장 포인트
  "interpolation": { ... }    // 확장 포인트
}
```

### 3. Trainer 확장

```java
// BigramTrainer.java
for (int i = 0; i < tokens.size() - 1; i++) {
    counts[tokens[i]][tokens[i+1]]++;
}

// TrigramTrainer.java (확장)
for (int i = 0; i < tokens.size() - 2; i++) {
    String key = tokens[i] + "," + tokens[i+1];
    counts[key][tokens[i+2]]++;
}
```

---

## 확장 로드맵

### Phase 1: Trigram 기본
- [ ] TrigramArtifact
- [ ] TrigramTrainer
- [ ] TrigramModel
- [ ] 테스트 작성

### Phase 2: 희소성 처리
- [ ] Backoff (Trigram → Bigram → Unigram)
- [ ] Interpolation (가중 평균)
- [ ] Smoothing (Laplace, Kneser-Ney)

### Phase 3: 고급 기능
- [ ] Variable-length N-gram
- [ ] Neural N-gram (FFNN)
- [ ] Attention 메커니즘

---

## 코드 확장 지점

### 현재 구조
```
mini-ai-model-ngram/
├── BigramArtifact.java
├── BigramTrainer.java
├── BigramModel.java
└── Sampler.java
```

### 확장 후 구조
```
mini-ai-model-ngram/
├── ngram/
│   ├── BigramArtifact.java
│   ├── BigramTrainer.java
│   ├── BigramModel.java
│   ├── TrigramArtifact.java      ← 추가
│   ├── TrigramTrainer.java       ← 추가
│   └── TrigramModel.java         ← 추가
├── smoothing/
│   ├── BackoffStrategy.java     ← 추가
│   └── InterpolationStrategy.java ← 추가
└── Sampler.java
```

---

## DoD

- ✅ 확장 포인트 문서화
- ✅ Trigram 중요성 설명
- ✅ 확장 로드맵 작성
- ✅ 코드에 확장 가능성 명시

---

## 🎓 교육적 가치

이 프로젝트를 통해 배운 것:

1. **토큰화**: 텍스트 → 숫자
2. **학습**: 데이터 → 패턴 (카운팅)
3. **생성**: 패턴 → 텍스트 (샘플링)
4. **비용**: 토큰 = 처리량 = 돈
5. **서빙**: HTTP API
6. **UX**: CLI

**AI 시스템의 전체 흐름을 직접 구현했습니다!** 🎉
