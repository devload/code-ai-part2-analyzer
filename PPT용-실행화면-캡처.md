# 토큰으로 이해하는 AI - 실제 동작 화면 캡처
## PPT 슬라이드용 터미널 출력 스크린샷

> 이 문서는 발표 자료에 포함할 실제 실행 화면을 정리한 것입니다.
> 각 섹션은 하나의 슬라이드 또는 여러 슬라이드로 활용할 수 있습니다.

---

## 📌 Demo 1: 토큰화 - 텍스트를 조각으로 나누기

### 한글 문장 토큰화

```bash
$ ./gradlew :mini-ai-cli:run --args="tokenize '오늘은 날씨가 좋다'"

📌 토큰화 결과:
  원본: "오늘은 날씨가 좋다"
  토큰 수: 3
  토큰: [오늘은, 날씨가, 좋다]
```

**설명**:
- 띄어쓰기 기준으로 토큰 분리
- 3개의 단어 = 3개의 토큰
- 각 토큰이 AI가 "읽는 조각"

### 영문 문장 토큰화

```bash
$ ./gradlew :mini-ai-cli:run --args="tokenize 'hello world'"

📌 토큰화 결과:
  원본: "hello world"
  토큰 수: 2
  토큰: [hello, world]
```

**설명**:
- 영문도 동일한 방식
- 2개 단어 = 2개 토큰

---

## 💰 Demo 2: 토큰 수와 비용의 관계

### 케이스 1: 띄어쓰기 O

```bash
$ ./gradlew :mini-ai-cli:run --args="tokenize '할 수 있다'"

📌 토큰화 결과:
  원본: "할 수 있다"
  토큰 수: 3
  토큰: [할, 수, 있다]
```

### 케이스 2: 띄어쓰기 X

```bash
$ ./gradlew :mini-ai-cli:run --args="tokenize '할수있다'"

📌 토큰화 결과:
  원본: "할수있다"
  토큰 수: 1
  토큰: [할수있다]
```

### 비용 비교

| 입력 | 토큰 수 | 상대 비용 |
|------|---------|-----------|
| "할 수 있다" | 3개 | 100% (기준) |
| "할수있다" | 1개 | 33% (1/3) |

**💡 핵심 인사이트**:
- 띄어쓰기에 따라 토큰 수가 3배 차이!
- **토큰 수 = 비용의 기준**
- 불필요한 공백 제거로 비용 최적화 가능

---

## 🎓 Demo 3: Bigram 학습 - "자주 붙는 쌍을 센다"

### 학습 실행

```bash
$ ./gradlew :mini-ai-cli:run --args="train --corpus data/sample-corpus.txt --output data/cli-bigram.json"

🚀 모델 학습 시작...
  Corpus: data/sample-corpus.txt
  Output: data/cli-bigram.json

✅ 학습 완료!
  Vocabulary: 56
  Latency: 8ms
```

### 학습 결과 (Artifact)

**Metadata**:
```json
{
  "modelType": "bigram",
  "tokenizerType": "WhitespaceTokenizer",
  "vocabSize": 56,
  "totalTokens": 103,
  "totalBigrams": 102
}
```

**Counts 샘플** (일부):
```json
{
  "the": {
    "cat": 5,
    "dog": 4,
    "bird": 1,
    "mat": 2,
    "log": 1
  },
  "cat": {
    "sat": 2,
    "loves": 3
  }
}
```

**💡 핵심 인사이트**:
- 학습 = 카운팅 (자주 붙는 쌍을 세기)
- "the" 다음에 "cat"이 5번 나타남
- 이 카운트가 확률의 기초

---

## 🤖 Demo 4: 문장 생성 - 다음 토큰 예측 반복

### 기본 생성

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 10 --seed 42"

💬 텍스트 생성...
  Prompt: "the cat"

📝 생성 결과:
  the cat I love love love natural language processing natural language is

📊 Usage:
  Input:  2 tokens
  Output: 10 tokens
  Total:  12 tokens
```

**생성 과정 시각화**:
```
1. 시작: "the cat"
2. "cat" → 다음? → "I" 선택
3. "I" → 다음? → "love" 선택
4. "love" → 다음? → "love" 선택 (반복)
5. ...
6. 10개 토큰 완성
```

**💡 핵심 인사이트**:
- AI는 문장을 "한 번에 만들지 않음"
- **토큰 하나씩 골라서 붙이는 반복**
- maxTokens만큼 반복 후 종료

---

## 🎲 Demo 5: Seed와 재현성 - 확률적 선택

### 같은 Seed (42)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 5 --seed 42"

📝 생성 결과:
  the cat I love love love

$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 5 --seed 42"

📝 생성 결과:
  the cat I love love love
```

✅ **결과 동일!**

### 다른 Seed (123)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 5 --seed 123"

📝 생성 결과:
  the cat sat on the mat
```

❗ **결과 다름!**

**💡 핵심 인사이트**:
- Seed 고정 = 같은 확률적 선택 = 재현 가능
- Seed 다름 = 다른 선택 = 다양한 결과
- 이것이 **"같은 질문에 다른 답"의 원리**

---

## 📊 Demo 6: Usage 측정 - 토큰이 곧 비용

### 짧은 프롬프트

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the' --max-tokens 5"

📊 Usage:
  Input:  1 tokens
  Output: 5 tokens
  Total:  6 tokens
```

### 긴 프롬프트

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat sat on the' --max-tokens 5"

📊 Usage:
  Input:  5 tokens
  Output: 5 tokens
  Total:  10 tokens
```

### 비용 비교

| 프롬프트 | Input | Output | Total | 상대 비용 |
|---------|-------|--------|-------|----------|
| "the" | 1 | 5 | 6 | 100% |
| "the cat sat on the" | 5 | 5 | 10 | 167% |

**💡 핵심 인사이트**:
- 같은 답변 길이(5 tokens)
- 프롬프트만 길어도 Total 증가
- **Input + Output = Total = 비용**

---

## 🌡️ Demo 7: Temperature - 창의성 vs 안정성

### Low Temperature (0.1)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 10 --temperature 0.1 --seed 42"

📝 생성 결과:
  the cat sat on the mat the cat sat on the
```

특징:
- 예측 가능
- 반복적
- 안정적

### High Temperature (2.0)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the cat' --max-tokens 10 --temperature 2.0 --seed 42"

📝 생성 결과:
  the cat loves the dog sat on the wire shines at
```

특징:
- 다양함
- 창의적
- 때로 이상함

**💡 핵심 인사이트**:
- Temperature = 확률 분포의 "뾰족함" 조절
- 낮음 (0.1): 거의 항상 1등만 선택
- 높음 (2.0): 낮은 확률도 선택 가능
- **창의성과 안정성의 트레이드오프**

---

## 🎯 Demo 8: TopK - 선택지 제한

### TopK = 1 (항상 1등)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the' --max-tokens 8 --top-k 1"

📝 생성 결과:
  the cat sat on the mat the cat
```

### TopK = 50 (상위 50개 중 선택)

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'the' --max-tokens 8 --top-k 50"

📝 생성 결과:
  the dog loves the cat sat on the
```

**💡 핵심 인사이트**:
- TopK = 후보를 상위 K개로 제한
- K=1: 가장 안정적 (항상 베스트만)
- K 클수록: 더 다양한 선택 가능

---

## 🔄 Demo 9: Bigram의 한계 - 반복 패턴

### 짧은 문맥의 문제

```bash
$ ./gradlew :mini-ai-cli:run --args="run -p 'love' --max-tokens 20"

📝 생성 결과:
  love natural language is amazing I love natural language is amazing I
```

**왜 반복될까?**

```
Bigram 관점:
"love" → "natural" (가장 흔함)
"natural" → "language" (가장 흔함)
"language" → "is" (가장 흔함)
"is" → "amazing" (가장 흔함)
"amazing" → "I" (가장 흔함)
"I" → "love" (가장 흔함)
"love" → ... (다시 반복!)
```

**💡 핵심 인사이트**:
- Bigram은 **직전 1개만** 기억
- 같은 패턴에 빠지면 벗어나기 어려움
- 이것이 **Trigram이 필요한 이유**

---

## 📈 Demo 10: 프로젝트 구조

### 빌드 성공

```bash
$ ./gradlew build

BUILD SUCCESSFUL in 2s
23 actionable tasks: 9 executed, 14 up-to-date
```

### 테스트 통과

```bash
$ ./gradlew test

BigramModelTest > 기본 텍스트 생성 PASSED
BigramModelTest > Stop sequence 테스트 PASSED
BigramModelTest > Usage 측정 PASSED
BigramModelTest > TopK 필터링 테스트 PASSED
BigramModelTest > Temperature 변화 테스트 PASSED
BigramModelTest > Seed 고정 시 재현성 PASSED

BUILD SUCCESSFUL in 1s
```

---

## 🎬 Demo 11: 서버 API

### 서버 시작

```bash
$ ./gradlew :mini-ai-server:bootRun

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

Started MiniAiServerApplication in 0.527 seconds
```

### API 호출 - Train

```bash
$ curl -X POST http://localhost:8080/v1/train \
  -H "Content-Type: application/json" \
  -d '{"corpusPath": "data/sample-corpus.txt"}'

{
  "message": "Training completed",
  "metadata": {
    "modelType": "bigram",
    "vocabSize": 56,
    "totalTokens": 103,
    "totalBigrams": 102
  },
  "latencyMs": 12
}
```

### API 호출 - Generate

```bash
$ curl -X POST http://localhost:8080/v1/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "the cat",
    "maxTokens": 10,
    "temperature": 1.0,
    "topK": 50,
    "seed": 42
  }'

{
  "text": "the cat loves the dog sat on the mat the",
  "usage": {
    "inputTokens": 2,
    "outputTokens": 10,
    "totalTokens": 12
  },
  "model": "bigram-v1",
  "latencyMs": 3
}
```

---

## 📊 Demo 12: 비교표 시각화

### Bigram vs 최신 AI

| 특성 | Bigram | GPT-4 |
|------|--------|-------|
| 문맥 길이 | 1개 토큰 | ~128,000 토큰 |
| 확률 계산 | 표 (카운트) | 뉴럴 네트워크 |
| 학습 방식 | 세기 (카운팅) | 경사하강법 |
| 처리 시간 | 매우 빠름 (~1ms) | 느림 (~1s) |
| 품질 | 낮음 (반복 많음) | 높음 (자연스러움) |
| 비용 | 거의 무료 | 토큰당 과금 |
| "처음 보는 문맥" | 실패 | 일반화로 처리 |

**공통점**:
- ✅ 둘 다 토큰 단위 처리
- ✅ 둘 다 다음 토큰 예측
- ✅ 둘 다 확률적 선택
- ✅ 둘 다 Usage 측정

---

## 🎯 실습 가이드 미리보기

### 빠른 시작 스크립트

```bash
$ ./examples/빠른시작-토큰체험.sh

========================================
토큰 개념 빠른 체험
========================================

📚 실습 1: 토큰화 - 텍스트를 조각으로 나누기
----------------------------------------
입력: '오늘은 날씨가 좋다'
📌 토큰화 결과:
  원본: "오늘은 날씨가 좋다"
  토큰 수: 3
  토큰: [오늘은, 날씨가, 좋다]

📚 실습 2: 토큰 수 비교 - 띄어쓰기의 영향
----------------------------------------
...

========================================
✨ 토큰 개념 체험 완료!
========================================
```

---

## 📝 핵심 메시지 요약

### 우리가 본 것

1. ✅ **토큰화**: 텍스트 → 조각 변환
2. ✅ **학습**: 자주 붙는 쌍을 세기
3. ✅ **생성**: 다음 토큰 반복 선택
4. ✅ **비용**: Input + Output = Total
5. ✅ **다양성**: Seed, Temperature, TopK
6. ✅ **한계**: 짧은 문맥의 반복 문제

### 토큰으로 설명되는 AI의 모든 현상

- ❓ 왜 비쌀까? → **토큰 수**
- ❓ 왜 느릴까? → **토큰 처리량**
- ❓ 왜 잊을까? → **문맥 길이 한계**
- ❓ 왜 다를까? → **확률적 선택**

---

**이 문서의 모든 화면 캡처는 실제로 동작하는 시스템에서 얻은 것입니다!** ✅
