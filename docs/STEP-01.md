# Step 1: Tokenizer 만들기 (가장 작은 성공)

## 학습 포인트

**텍스트가 "조각"으로 바뀌는 순간을 직접 만듭니다.**

핵심 질문:
- 왜 모델은 텍스트를 직접 이해하지 못할까?
- 토큰이란 무엇이고, 왜 필요한가?
- encode/decode는 무엇을 보장해야 하는가?

답:
- 모델은 **숫자만 이해**합니다 (벡터 연산 기반)
- 토큰 = 텍스트를 모델이 이해할 수 있는 "조각 단위"
- encode/decode는 **양방향 변환**을 보장해야 합니다 (정보 손실 최소화)

---

## 토큰이란?

### 정의

**토큰(Token)** = 텍스트를 처리하는 최소 단위

- 문자(character) 단위: `['H', 'e', 'l', 'l', 'o']`
- 단어(word) 단위: `['Hello', 'world']`
- 서브워드(subword) 단위: `['Hel', 'lo', 'world']` (BPE, WordPiece)

### 왜 필요한가?

```
텍스트: "Hello world"
     ↓ (encode)
토큰 ID: [42, 123]
     ↓ (모델 처리)
벡터: [[0.1, 0.5, ...], [0.3, 0.7, ...]]
     ↓ (생성)
새 토큰 ID: [42, 123, 89]
     ↓ (decode)
텍스트: "Hello world !"
```

모델은 숫자(벡터) 연산만 수행하므로, 텍스트를 숫자로 변환하는 과정이 필수입니다.

---

## WhitespaceTokenizer 구현

### 설계

**가장 단순한 토크나이저**: 공백으로 단어 분리

```java
public interface Tokenizer {
    List<Integer> encode(String text);    // 텍스트 → 토큰 ID
    String decode(List<Integer> tokens);  // 토큰 ID → 텍스트
    int vocabSize();                      // 어휘 크기
}
```

### 핵심 구성 요소

1. **Vocabulary (어휘 사전)**
   ```java
   Map<String, Integer> wordToId;  // "hello" -> 42
   Map<Integer, String> idToWord;  // 42 -> "hello"
   ```

2. **특수 토큰**
   ```java
   [UNK] = 0  // Unknown (어휘에 없는 단어)
   ```

3. **Encode/Decode**
   ```java
   encode("hello world")  →  [42, 123]
   decode([42, 123])      →  "hello world"
   ```

---

## 토큰화 예시

### 예시 1: 한글 텍스트

**입력**: "오늘은 날씨가 좋다"

**처리 과정**:
```
1. 공백으로 분리: ["오늘은", "날씨가", "좋다"]
2. 각 단어를 ID로 변환:
   - "오늘은" -> 1
   - "날씨가" -> 2
   - "좋다"   -> 3
3. 결과: [1, 2, 3]
```

**Vocabulary**:
```
[UNK]   -> 0
오늘은  -> 1
날씨가  -> 2
좋다    -> 3
내일도  -> 4
좋을까  -> 5
```

**복원 (Decode)**:
```
[1, 2, 3] -> "오늘은 날씨가 좋다"
```

### 예시 2: 영어 텍스트

**입력**: "the quick brown fox"

**Vocabulary**:
```
[UNK]  -> 0
the    -> 1
quick  -> 2
brown  -> 3
fox    -> 4
jumps  -> 5
over   -> 6
```

**Encode**:
```
"the quick brown fox" -> [1, 2, 3, 4]
```

**Decode**:
```
[1, 2, 3, 4] -> "the quick brown fox"
```

### 예시 3: Unknown 토큰 처리

**Vocabulary**: `{[UNK], hello, world}`

**입력**: "hello unknown world"

**Encode**:
```
"hello"   -> 1 (어휘에 있음)
"unknown" -> 0 (어휘에 없음 = UNK)
"world"   -> 2 (어휘에 있음)

결과: [1, 0, 2]
```

**Decode**:
```
[1, 0, 2] -> "hello [UNK] world"
```

---

## 공백 토크나이저의 한계

### 1. 구두점 처리 안 됨

```
입력: "Hello, world!"
분리: ["Hello,", "world!"]
```

- `"Hello"`와 `"Hello,"`는 **다른 토큰**
- `"world"`와 `"world!"`도 **다른 토큰**
- 어휘가 불필요하게 커짐 (vocabulary explosion)

**해결 방법**:
- 구두점을 별도 토큰으로 분리
- 또는 정규화(normalization) 적용

### 2. 대소문자 구분

```
입력: "Hello hello HELLO"
분리: ["Hello", "hello", "HELLO"]
```

- 의미는 같지만 **3개의 다른 토큰**
- 어휘 낭비

**해결 방법**:
- 소문자로 정규화 (lowercasing)
- 또는 casing을 별도 정보로 저장

### 3. 희소성 문제 (Rare words)

```
Vocabulary 크기: 10,000 단어
실제 사용 빈도:
  - 상위 100개 단어 = 전체 텍스트의 50%
  - 하위 9,000개 단어 = 전체 텍스트의 5%
```

- 드문 단어들이 어휘의 대부분을 차지
- 메모리 낭비

**해결 방법**:
- 빈도 기반 필터링
- 서브워드 토크나이저 (BPE, WordPiece)

### 4. 어휘에 없는 단어 (OOV: Out-of-Vocabulary)

```
학습 시: "I love cats"
추론 시: "I love dogs"

"dogs"는 어휘에 없음 → [UNK]
```

- 정보 손실 발생
- 모델이 의미를 이해하지 못함

**해결 방법**:
- 서브워드 토크나이저 (단어를 조각으로 분해)
- 예: `"dogs"` → `["dog", "##s"]`

### 5. 언어 의존성

```
한국어: "안녕하세요" (띄어쓰기 있음)
중국어: "你好世界" (띄어쓰기 없음)
```

- 공백 기준은 **언어에 따라 동작 안 할 수 있음**

**해결 방법**:
- 언어별 토크나이저
- 문자 단위 또는 서브워드 토크나이저

---

## 테스트 결과

### Round-trip 테스트

```java
String text = "오늘은 날씨가 좋다";
List<Integer> tokens = tokenizer.encode(text);
String decoded = tokenizer.decode(tokens);

assertEquals(text, decoded);  // ✅ 통과
```

**의미**: encode/decode가 **양방향 변환**을 제대로 수행

### Unknown 토큰 테스트

```java
String corpus = "hello world";
WhitespaceTokenizer tokenizer = WhitespaceTokenizer.fromText(corpus);

String text = "hello unknown";
List<Integer> tokens = tokenizer.encode(text);
// [1, 0]  (1=hello, 0=UNK)

String decoded = tokenizer.decode(tokens);
// "hello [UNK]"
```

**의미**: 어휘에 없는 단어를 **UNK로 안전하게 처리**

### 대소문자/구두점 테스트 (한계 확인)

```java
// 대소문자
encode("Hello") != encode("hello")  // ⚠️ 다름

// 구두점
encode("Hello") != encode("Hello,") // ⚠️ 다름
```

**의미**: 공백 토크나이저의 **한계를 명확히 이해**

---

## 코드 구조

```
mini-ai-tokenizer-simple/
└── src/
    ├── main/java/com/miniai/tokenizer/
    │   └── WhitespaceTokenizer.java      # 구현
    └── test/java/com/miniai/tokenizer/
        └── WhitespaceTokenizerTest.java  # 테스트
```

### 주요 메서드

```java
// Vocabulary 생성
WhitespaceTokenizer.fromText(corpus)

// Encode/Decode
List<Integer> tokens = tokenizer.encode("hello world");
String text = tokenizer.decode(tokens);

// Vocabulary 조회
int id = tokenizer.getTokenId("hello");
String word = tokenizer.getToken(id);
int size = tokenizer.vocabSize();
```

---

## 실전 적용

### OpenAI GPT-3의 토크나이저

- **BPE (Byte Pair Encoding)** 사용
- Vocabulary 크기: **50,257**
- 서브워드 단위로 분해하여 OOV 문제 해결

예시:
```
"ChatGPT" → ["Chat", "G", "PT"]
"tokenizer" → ["token", "izer"]
```

### Claude의 토크나이저

- 다국어 지원 최적화
- 한국어/중국어/일본어 등 효율적 처리
- Vocabulary 크기: **100,000+**

### 왜 토큰이 비용 단위인가?

```
OpenAI GPT-4 가격 (2024):
- Input:  $0.03 / 1,000 tokens
- Output: $0.06 / 1,000 tokens
```

- 토큰 수 = 모델 처리량
- 처리량 = 계산 비용
- **따라서 토큰이 과금 단위**

---

## 왜 이렇게 했는가?

### 공백 토크나이저를 선택한 이유

1. **학습 목적**
   - 토큰화의 **핵심 개념** 이해
   - encode/decode의 **양방향성** 체감
   - Unknown 토큰 처리의 **필요성** 학습

2. **단순성**
   - 100줄 미만의 코드
   - 외부 라이브러리 불필요
   - 디버깅 쉬움

3. **한계 이해**
   - 구두점, 대소문자, OOV 문제를 **직접 경험**
   - 다음 단계(BPE, SentencePiece)의 **동기 부여**

### 실전에서는?

실제 프로덕션에서는 다음을 사용:
- **BPE** (GPT 계열)
- **WordPiece** (BERT 계열)
- **SentencePiece** (다국어)
- **Tiktoken** (OpenAI의 최신 토크나이저)

하지만 **핵심 원리는 동일**:
```
encode: 텍스트 → 숫자 (토큰 ID)
decode: 숫자 → 텍스트
```

---

## 다음 단계: Step 2

**목표**: Bigram 학습 구현 (학습 = 카운트 테이블 만들기)

구현할 것:
- BigramTrainer (토큰 쌍 카운트)
- BigramArtifact (JSON 저장)
- Corpus 처리

학습할 것:
- 학습이란? = 데이터에서 **패턴 추출**
- N-gram의 핵심 = **"다음에 무엇이 올까?" 카운트**

---

## DoD (Definition of Done) 체크리스트

- [x] WhitespaceTokenizer 구현
- [x] encode/decode 메서드 구현
- [x] Unknown 토큰 처리
- [x] Vocabulary 관리
- [x] JUnit 테스트 작성
- [x] Round-trip 테스트 통과
- [x] "오늘은 날씨가 좋다" 예시 포함
- [x] 한계 이해 (구두점, 대소문자, OOV)
- [x] docs/STEP-01.md 작성
- [ ] docs/demo/STEP-01.log 생성 (다음 단계)
- [ ] Git 커밋 및 step-01 태그 (다음 단계)

---

## 참고: 실제 토크나이저 라이브러리

- **Hugging Face Tokenizers**: https://github.com/huggingface/tokenizers
- **SentencePiece**: https://github.com/google/sentencepiece
- **Tiktoken** (OpenAI): https://github.com/openai/tiktoken

**하지만 이 단계에서는 직접 만들어보며 원리를 이해하는 것이 핵심입니다!**
