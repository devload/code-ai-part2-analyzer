# Code AI - 코드 생성 특화 AI 어시스턴트

> **기반 프로젝트**: [Mini AI](../aimaker) - 토큰/Bigram 교육용 프로젝트
> **목적**: 실제 코드 자동완성 및 생성에 특화된 AI 시스템

---

## 주요 기능

- **CodeTokenizer**: 코드 문법 인식 토크나이저
- **Trigram 모델**: 2-토큰 문맥으로 정확한 예측
- **Backoff 지원**: Trigram 없으면 Bigram으로 대체
- **1,300+ 줄 Java 패턴**: 실용적인 코드 코퍼스

---

## 빠른 시작

### 1. 빌드
```bash
./gradlew build
```

### 2. 서버 실행
```bash
./gradlew :mini-ai-server:bootRun
```

### 3. Trigram 모델 학습
```bash
# CLI로 학습 (Trigram + CodeTokenizer)
./gradlew :mini-ai-cli:run --args="train --corpus /path/to/code-ai/data/code-corpus/java-combined.txt --model trigram --tokenizer code"

# 또는 curl로
curl -X POST http://localhost:8080/v1/train -H "Content-Type: application/json" \
  -d '{"corpusPath": "/path/to/code.txt", "outputPath": "data/model.json", "modelType": "trigram", "tokenizerType": "code"}'
```

### 4. 코드 자동완성
```bash
# CLI 사용
./gradlew :mini-ai-cli:run --args='complete "public class User {"'

# curl 사용
curl -X POST http://localhost:8080/v1/generate -H "Content-Type: application/json" \
  -d '{"prompt": "public class User {", "maxTokens": 10}'
```

---

## CLI 명령어

```bash
# 도움말
./gradlew :mini-ai-cli:run

# 학습 (기본: Trigram + CodeTokenizer)
./gradlew :mini-ai-cli:run --args="train --corpus data/code.txt --model trigram --tokenizer code"

# 코드 자동완성 (3개 후보)
./gradlew :mini-ai-cli:run --args='complete "for (int i" -n 3'

# 텍스트 생성
./gradlew :mini-ai-cli:run --args='run -p "public static" --max-tokens 15'
```

---

## 완료된 기능

### Phase 0: Fork ✅
- [x] 저장소 Fork
- [x] Git 초기화

### Phase 1: CodeTokenizer ✅
- [x] 들여쓰기 압축 (`    ` → `INDENT_1`)
- [x] 심볼 분리 (`getName()` → `getName`, `(`, `)`)
- [x] Java 키워드 인식
- [x] 10개 테스트 통과

### Phase 2: 코드 코퍼스 ✅
- [x] 1,323줄 Java 패턴
- [x] Design patterns, Stream API, Spring Boot
- [x] Optional, Test patterns, Utilities

### Phase 3: Trigram 모델 ✅
- [x] TrigramArtifact, TrigramTrainer, TrigramModel
- [x] Bigram Backoff (희소성 해결)
- [x] Interpolation 가중치 설정
- [x] 7개 테스트 통과

### Phase 4: CLI 개선 ✅
- [x] `complete` 명령어 추가
- [x] `--model`, `--tokenizer` 옵션
- [x] 다중 후보 생성 (`-n`)

---

## API Reference

### POST /v1/train
```json
{
  "corpusPath": "/path/to/corpus.txt",
  "outputPath": "/path/to/model.json",
  "modelType": "trigram",      // "bigram" or "trigram"
  "tokenizerType": "code"      // "whitespace" or "code"
}
```

### POST /v1/generate
```json
{
  "prompt": "public class User {",
  "maxTokens": 10,
  "temperature": 1.0,
  "topK": 10,
  "seed": 42
}
```

---

## 프로젝트 구조

```
code-ai/
├── mini-ai-core/              # 인터페이스/DTO
├── mini-ai-tokenizer-simple/  # WhitespaceTokenizer
├── code-ai-tokenizer/         # CodeTokenizer ⭐
├── mini-ai-model-ngram/       # Bigram + Trigram ⭐
├── mini-ai-server/            # REST API
├── mini-ai-cli/               # CLI 도구 ⭐
└── data/
    ├── code-corpus/java/      # Java 패턴 파일들
    ├── code-trigram.json      # 학습된 Trigram 모델
    └── code-bigram-v3.json    # 학습된 Bigram 모델
```

---

## 기술 스택

- **Java 17**
- **Gradle 8.5**
- **Spring Boot 3.2.0**
- **picocli 4.7.5** (CLI)
- **Gson 2.10.1** (JSON)
- **OkHttp 4.12.0** (HTTP Client)

---

## 참고 문서

- [FORK-PLAN-코딩특화.md](FORK-PLAN-코딩특화.md) - 개발 계획
- [연구자료-코드LLM-핵심인사이트.md](연구자료-코드LLM-핵심인사이트.md) - GPT 연구 분석

---

## 라이선스

MIT License
