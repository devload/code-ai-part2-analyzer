# Mission: Mini AI Full-Stack (Java) — 과정 중심 개발 (교육자료용)

## 최종 목표(변하지 않는 큰 그림)
자바로 "토큰화 → n-gram(Bigram) 학습/서빙 → REST API → CLI"를 직접 구현하면서,
각 단계마다 문서/데모/커밋을 남겨 교육 자료로 쓸 수 있게 만든다.

## 핵심 원칙
- 한 번에 완성하지 않는다. 반드시 단계별로 만든다.
- 매 단계마다 다음 산출물이 남아야 한다:
  - (1) 문서: docs/STEP-XX.md
  - (2) 코드: 커밋 단위
  - (3) 데모 로그: docs/demo/STEP-XX.log (실행 커맨드 + 출력)
- 매 단계 끝에서 "왜 이렇게 했는지"를 한 문단으로 기록한다.
- 설계는 교체 가능하도록(Tokenizer/Model/Trainer 인터페이스) 처음부터 잡는다.
- MVP 모델은 Bigram만 구현한다. Trigram은 "설계 자리만" 확보한다.

---

## Repository 규칙
- Gradle 멀티모듈
- 모듈:
  - mini-ai-core
  - mini-ai-tokenizer-simple
  - mini-ai-model-ngram
  - mini-ai-server
  - mini-ai-cli
- 각 스텝 끝에 tag: step-01, step-02 … 로 남긴다.

---

# Step 0. 뼈대 만들기 (프로젝트 골격 + 학습 목표 세팅)
## 학습 포인트
- "교체 가능한 구조"가 무엇인지 감을 잡는다.

## 구현 범위
- Gradle 멀티모듈 생성
- mini-ai-core에 인터페이스/DTO만 정의 (구현 X)
  - Tokenizer
  - LanguageModel (or NGramModel)
  - Trainer
  - Usage 타입
  - GenerateRequest/Response

## 산출물
- docs/STEP-00.md: 전체 아키텍처 개요(그림 1장 ASCII로라도)
- README.md: Step별 실행 흐름 목차만 작성
- docs/demo/STEP-00.log: gradle 빌드 성공 로그

## DoD
- ./gradlew build 성공
- core 모듈에 "구현 없는 인터페이스"만 존재

---

# Step 1. Tokenizer 만들기 (가장 작은 성공)
## 학습 포인트
- 텍스트가 "조각"으로 바뀌는 순간을 직접 만든다.

## 구현 범위
- WhitespaceTokenizer 구현 (encode/decode)
- tokenize 명령(로컬 테스트용)까지는 아직 CLI가 아니라 JUnit 테스트로 확인

## 산출물
- docs/STEP-01.md: 토큰이 무엇인지 + 공백 토크나이저 한계
- docs/demo/STEP-01.log: 단위 테스트 실행 로그(입력/출력 예시 포함)

## DoD
- encode/decode round-trip 테스트 통과
- "오늘은 날씨가 좋다" 토큰 분해 예시가 문서에 포함

---

# Step 2. Bigram 학습(Train) 구현 (학습=세기)
## 학습 포인트
- 학습은 "카운트 테이블 만들기"라는 감각을 잡는다.

## 구현 범위
- BigramTrainer 구현
  - 입력: 텍스트 파일 경로
  - 처리: 토큰화 후 prev->next 카운트 생성
  - 출력: BigramArtifact(JSON) 저장
- artifact 형식 확정 (counts, meta, vocab 등)

## 산출물
- docs/STEP-02.md: bigram이 무엇인지 + 카운트 예시 표(간단히)
- docs/demo/STEP-02.log:
  - corpus 샘플 파일 생성
  - trainer 실행 커맨드
  - artifact 일부 출력(상위 5개 정도)

## DoD
- artifact 파일이 생성되고, counts가 JSON으로 저장됨
- corpus가 달라도 재학습 가능

---

# Step 3. Bigram 생성(Generate) 구현 (서빙=다음 토큰 선택)
## 학습 포인트
- "다음 토큰 예측 루프"를 직접 손으로 만든다.

## 구현 범위
- BigramModel (artifact 로드)
- Sampler 구현
  - topK
  - temperature
  - 랜덤 시드 옵션(재현성)
- generate(req) 구현
  - prompt 토큰화
  - maxTokens만큼 반복 생성
  - stopSequences(간단 구현)

## 산출물
- docs/STEP-03.md: 생성 루프 설명(의사코드 포함)
- docs/demo/STEP-03.log:
  - 동일 prompt에 seed 고정 시 결과 동일
  - temperature/topK 바꿨을 때 결과 변화 비교

## DoD
- 모델이 실제 텍스트를 생성함(품질 상관 X)
- seed 고정 시 재현됨

---

# Step 4. Usage(토큰 카운팅) 붙이기 (비용 감각 만들기)
## 학습 포인트
- "왜 토큰이 비용 단위인지"를 시스템이 직접 보여주게 만든다.

## 구현 범위
- UsageMeter 구현
  - inputTokens = encode(prompt).size
  - outputTokens = 생성된 토큰 수
- GenerateResponse에 usage 포함

## 산출물
- docs/STEP-04.md: 토큰 카운트가 왜 중요한지(대화 길이/비용/제한)
- docs/demo/STEP-04.log:
  - 같은 prompt에 maxTokens 바꿔서 totalTokens 변화 확인

## DoD
- 응답에 usage가 항상 포함
- input/output/total이 일관됨

---

# Step 5. Server 만들기 (Spring Boot API로 감싸기)
## 학습 포인트
- 모델을 "서빙" 형태로 제공하는 구조를 익힌다.

## 구현 범위
- mini-ai-server Spring Boot
- POST /v1/train
- POST /v1/generate
- latencyMs 측정하여 포함
- artifact 경로 지정 방식(기본값 포함)

## 산출물
- docs/STEP-05.md: API 설계 이유 + 요청/응답 예시
- docs/demo/STEP-05.log:
  - curl로 train/generate 호출
  - 응답 JSON 캡처

## DoD
- 서버 실행 후 curl로 generate 동작
- usage/latency/model 정보가 응답에 포함

---

# Step 6. CLI 만들기 (서버를 쓰는 사용자 경험)
## 학습 포인트
- "CLI로 AI를 쓰는 흐름"을 직접 만든다.

## 구현 범위
- picocli 기반 mini-ai-cli
- 명령어:
  - mini-ai train --corpus <path>
  - mini-ai run -p "<prompt>" ...
  - mini-ai chat (REPL)
  - mini-ai tokenize "<text>"
- CLI는 HTTP로 server 호출

## 산출물
- docs/STEP-06.md: CLI UX 설계(왜 이 명령어들인지)
- docs/demo/STEP-06.log:
  - train -> run -> chat 시나리오 실행 기록

## DoD
- CLI만으로 학습/생성/채팅이 가능
- tokenize 결과가 즉시 확인됨

---

# Step 7. 확장 설계 자리 확보 (Trigram 훅만)
## 학습 포인트
- "교체 가능한 구조"가 실제로 어떻게 확장되는지 보여준다.

## 구현 범위
- TrigramModel/Trainer는 구현하지 않아도 됨
- 다만 인터페이스와 artifact 확장 포인트 문서화
  - backoff, interpolation 옵션이 들어갈 위치 표시

## 산출물
- docs/STEP-07.md: trigram이 왜 중요한지(문장성/희소성) + 다음 확장 로드맵(코드 자리 기준)
- docs/demo/STEP-07.log: N/A (문서 중심)

## DoD
- 코드에 "Trigram을 끼울 자리"가 실제로 존재
- artifact 포맷이 확장 가능한 형태로 설명됨

---

## 전체 완료 기준
- step-00 ~ step-07 태그가 존재
- docs/STEP-XX.md + docs/demo/STEP-XX.log가 모두 존재
- README에 "이 저장소는 과정 중심이며, step을 따라가면 된다"가 명확히 적혀 있음
