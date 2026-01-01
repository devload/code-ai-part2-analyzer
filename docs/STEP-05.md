# Step 5: Server 만들기 (Spring Boot REST API)

## 학습 포인트

**모델을 "서빙" 형태로 제공하는 구조를 익힙니다.**

---

## 구현된 API

### POST /v1/train
모델 학습

**Request**:
```json
{
  "corpusPath": "/path/to/corpus.txt",
  "outputPath": "/path/to/artifact.json"
}
```

**Response**:
```json
{
  "status": "success",
  "message": "학습 완료",
  "artifactPath": "/path/to/artifact.json",
  "vocabSize": 56,
  "latencyMs": 8
}
```

### POST /v1/generate
텍스트 생성

**Request**:
```json
{
  "prompt": "the cat",
  "maxTokens": 10,
  "temperature": 1.0,
  "topK": 50,
  "seed": 42
}
```

**Response**:
```json
{
  "generatedText": "the cat I love...",
  "usage": {
    "inputTokens": 2,
    "outputTokens": 10,
    "totalTokens": 12
  },
  "latencyMs": 0,
  "model": "bigram-v1"
}
```

### GET /v1/health
헬스 체크

---

## DoD

- ✅ Spring Boot 서버 구현
- ✅ POST /v1/train 동작
- ✅ POST /v1/generate 동작
- ✅ usage/latency/model 응답에 포함
- ✅ curl 테스트 성공

---

## 다음: Step 6 (CLI)
