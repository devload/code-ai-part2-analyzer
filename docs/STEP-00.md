# Step 0: 뼈대 만들기 (프로젝트 골격 + 학습 목표 세팅)

## 학습 포인트

이 단계에서는 **"교체 가능한 구조"** 가 무엇인지 감을 잡습니다.

핵심은 **인터페이스 우선 설계**입니다:
- 모든 주요 컴포넌트를 인터페이스로 먼저 정의
- 구현체는 나중에 추가하되, 언제든 교체 가능
- `mini-ai-core`는 **외부 의존성 0** (순수 Java만)

이렇게 설계하면:
- Tokenizer를 Whitespace → BPE → SentencePiece로 교체 가능
- Model을 Bigram → Trigram → Transformer로 확장 가능
- 각 모듈이 독립적으로 테스트/개발 가능

---

## 전체 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│                    Mini AI Full-Stack                        │
│                  (과정 중심 교육 자료)                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  mini-ai-cli    │  사용자가 사용하는 명령줄 도구
│                 │  - train, run, chat, tokenize
└────────┬────────┘
         │ HTTP
         ↓
┌─────────────────┐
│ mini-ai-server  │  Spring Boot REST API
│                 │  - POST /v1/train
│                 │  - POST /v1/generate
└────────┬────────┘
         │ depends
         ↓
┌─────────────────────────────────────────────────────────────┐
│                  mini-ai-model-ngram                         │
│  - BigramTrainer (Step 2)   학습 = 카운트 테이블 생성        │
│  - BigramModel (Step 3)     생성 = 다음 토큰 예측 루프       │
│  - Sampler (topK, temperature)                               │
└────────┬────────────────────────────────────────────────────┘
         │ depends
         ↓
┌─────────────────────────────────────────────────────────────┐
│             mini-ai-tokenizer-simple                         │
│  - WhitespaceTokenizer (Step 1)  텍스트 → 토큰 ID           │
└────────┬────────────────────────────────────────────────────┘
         │ depends
         ↓
┌─────────────────────────────────────────────────────────────┐
│                    mini-ai-core                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Tokenizer 인터페이스                                │   │
│  │   - encode(text) → List<Integer>                     │   │
│  │   - decode(tokens) → String                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  LanguageModel 인터페이스                            │   │
│  │   - generate(request) → response                     │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Trainer 인터페이스                                  │   │
│  │   - train(corpusPath, outputPath)                    │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  DTO (Data Transfer Objects)                         │   │
│  │   - Usage (inputTokens, outputTokens, totalTokens)   │   │
│  │   - GenerateRequest (prompt, maxTokens, ...)         │   │
│  │   - GenerateResponse (text, usage, latency, ...)     │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  외부 의존성 없음 - 순수 Java 인터페이스와 타입만            │
└─────────────────────────────────────────────────────────────┘
```

---

## 모듈별 역할

| 모듈 | 역할 | 외부 의존성 |
|------|------|------------|
| **mini-ai-core** | 인터페이스/DTO 정의 | 없음 (순수 Java) |
| **mini-ai-tokenizer-simple** | Tokenizer 구현 | core |
| **mini-ai-model-ngram** | N-gram 모델/학습 구현 | core, tokenizer, gson |
| **mini-ai-server** | REST API 제공 | core, tokenizer, model, Spring Boot |
| **mini-ai-cli** | 사용자 CLI | core, picocli, okhttp |

---

## 구현된 인터페이스/타입

### 1. Tokenizer 인터페이스
```java
public interface Tokenizer {
    List<Integer> encode(String text);
    String decode(List<Integer> tokens);
    int vocabSize();
}
```

**왜 필요한가?**
- 모델은 텍스트를 직접 이해하지 못함
- 텍스트를 "조각(토큰)"으로 나누고, 각 조각에 숫자(ID)를 부여
- encode/decode는 양방향 변환 보장

### 2. LanguageModel 인터페이스
```java
public interface LanguageModel {
    GenerateResponse generate(GenerateRequest request);
    String modelName();
}
```

**왜 필요한가?**
- 언어 모델의 핵심 = "다음 토큰 예측"
- generate()는 이 예측을 반복하여 텍스트 생성
- 인터페이스로 정의하면 Bigram ↔ Trigram ↔ GPT로 교체 가능

### 3. Trainer 인터페이스
```java
public interface Trainer {
    void train(Path corpusPath, Path outputPath);
    String trainerName();
}
```

**왜 필요한가?**
- 학습 = 데이터에서 패턴 추출
- N-gram의 경우: 학습 = 카운트 테이블 생성
- 학습 결과(artifact)를 파일로 저장하여 재사용

### 4. Usage 타입
```java
public class Usage {
    private final int inputTokens;   // 프롬프트 토큰 수
    private final int outputTokens;  // 생성된 토큰 수
    private final int totalTokens;   // 전체 토큰 수
}
```

**왜 필요한가?**
- 토큰은 AI 서비스의 **비용 단위**
- OpenAI, Claude 등 모든 상용 LLM은 토큰으로 과금
- 사용량을 추적해야 비용 예측 가능

### 5. GenerateRequest/Response
```java
public class GenerateRequest {
    private final String prompt;          // 생성 시작점
    private final int maxTokens;          // 최대 생성 길이
    private final double temperature;     // 창의성 (0.0 ~ 2.0)
    private final int topK;               // 후보 제한
    private final Long seed;              // 재현성 보장
    private final List<String> stopSequences;
}

public class GenerateResponse {
    private final String generatedText;   // 생성 결과
    private final Usage usage;            // 토큰 사용량
    private final long latencyMs;         // 응답 시간
    private final String model;           // 사용된 모델
}
```

**왜 필요한가?**
- Request: 생성 옵션을 구조화 (temperature, topK는 Step 3에서 사용)
- Response: 결과뿐 아니라 메타정보(usage, latency)도 제공
- 실제 LLM API와 동일한 구조 (OpenAI, Anthropic 스타일)

---

## 디렉토리 구조

```
mini-ai/
├── mission.md                  # 전체 Step 0~7 미션 정의
├── ANALYSIS.md                 # 미션 분석 문서
├── README.md                   # Step별 실행 가이드
├── build.gradle                # 루트 빌드 설정
├── settings.gradle             # 멀티모듈 설정
├── gradlew                     # Gradle wrapper
├── docs/
│   ├── STEP-00.md             # 이 문서
│   └── demo/
│       └── STEP-00.log        # 빌드 성공 로그
├── mini-ai-core/
│   ├── build.gradle
│   └── src/main/java/com/miniai/core/
│       ├── tokenizer/
│       │   └── Tokenizer.java
│       ├── model/
│       │   ├── LanguageModel.java
│       │   └── Trainer.java
│       └── types/
│           ├── Usage.java
│           ├── GenerateRequest.java
│           └── GenerateResponse.java
├── mini-ai-tokenizer-simple/
│   ├── build.gradle
│   └── src/main/java/...      # Step 1에서 구현
├── mini-ai-model-ngram/
│   ├── build.gradle
│   └── src/main/java/...      # Step 2, 3에서 구현
├── mini-ai-server/
│   ├── build.gradle
│   └── src/main/java/...      # Step 5에서 구현
└── mini-ai-cli/
    ├── build.gradle
    └── src/main/java/...      # Step 6에서 구현
```

---

## 빌드 검증

```bash
./gradlew build
```

결과:
- ✅ 모든 모듈 컴파일 성공
- ✅ 인터페이스만 정의되어 있고 구현체는 없음 (의도된 상태)
- ✅ 외부 의존성 다운로드 완료

---

## 왜 이렇게 했는가?

### 인터페이스 우선 설계의 장점

1. **명확한 계약(Contract)**
   - 각 컴포넌트가 무엇을 해야 하는지 먼저 정의
   - 구현 전에 전체 구조를 파악 가능

2. **교체 가능성(Pluggability)**
   - WhitespaceTokenizer → BPETokenizer로 교체 시, 인터페이스만 준수하면 OK
   - BigramModel → TrigramModel로 확장 시, LanguageModel 인터페이스 재사용

3. **테스트 용이성**
   - Mock 구현체로 단위 테스트 가능
   - 각 모듈을 독립적으로 테스트

4. **교육 효과**
   - "왜 이 메서드가 필요한지"를 먼저 이해
   - 구현은 나중에 하나씩 추가하면서 학습

### 과정 중심 접근

이 프로젝트는 **"완성품"보다 "학습 경험"** 을 우선합니다:
- 한 번에 다 만들지 않음
- 각 Step에서 하나의 개념만 집중
- Step 0: 구조 이해 → Step 1: 토큰화 → Step 2: 학습 → ...

---

## 다음 단계: Step 1

**목표**: Tokenizer 만들기 (가장 작은 성공)

구현할 것:
- `WhitespaceTokenizer` (공백 기준 분리)
- encode/decode 구현
- JUnit 테스트 작성

학습할 것:
- 텍스트가 "조각"으로 바뀌는 순간을 직접 만들기
- 토큰화의 한계 이해 (공백 토크나이저의 문제점)

---

## DoD (Definition of Done) 체크리스트

- [x] Gradle 멀티모듈 프로젝트 생성
- [x] `./gradlew build` 성공
- [x] mini-ai-core에 인터페이스/DTO 정의
- [x] 외부 의존성 없는 순수 Java 코드
- [x] docs/STEP-00.md 작성
- [x] 아키텍처 다이어그램 포함
- [ ] docs/demo/STEP-00.log 생성 (다음 단계)
- [ ] Git 커밋 및 step-00 태그 (다음 단계)
