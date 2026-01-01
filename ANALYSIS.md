# 미션 분석 및 개발 준비

## 📊 프로젝트 현황
- **프로젝트명**: Mini AI Full-Stack (Java)
- **목적**: 과정 중심 교육 자료 (Step별 학습 경험)
- **현재 상태**: 빈 디렉토리 (mission.md만 존재)
- **Git 상태**: 미초기화 (Step 0에서 초기화 예정)

---

## 🏗 전체 아키텍처

### 모듈 구조
```
mini-ai/
├── mini-ai-core              # 핵심 인터페이스/DTO (무의존성)
│   └── Tokenizer, LanguageModel, Trainer, Usage, Request/Response
│
├── mini-ai-tokenizer-simple  # Tokenizer 구현체
│   └── WhitespaceTokenizer (Step 1)
│
├── mini-ai-model-ngram       # N-gram 모델 구현체
│   ├── BigramTrainer (Step 2)
│   ├── BigramModel (Step 3)
│   └── Sampler (topK, temperature)
│
├── mini-ai-server            # Spring Boot REST API
│   ├── POST /v1/train
│   └── POST /v1/generate
│
└── mini-ai-cli               # 사용자 CLI
    ├── train, run, chat, tokenize
    └── HTTP client (서버 호출)
```

### 의존성 흐름
```
cli ──HTTP──> server ──depends──> model ──depends──> tokenizer ──depends──> core
                                                                                ↑
                                                                            (인터페이스만)
```

---

## 🎯 Step별 목표 및 산출물

| Step | 제목 | 핵심 학습 | 주요 산출물 | DoD |
|------|------|-----------|-------------|-----|
| **0** | 뼈대 만들기 | 교체 가능 설계 | 인터페이스, 빌드 성공 | ./gradlew build ✓ |
| **1** | Tokenizer | 토큰화 개념 | encode/decode 테스트 | round-trip 통과 |
| **2** | Bigram 학습 | 학습=카운트 테이블 | BigramArtifact JSON | counts 저장됨 |
| **3** | Bigram 생성 | 다음 토큰 예측 루프 | 텍스트 생성 데모 | seed 재현성 ✓ |
| **4** | Usage 측정 | 토큰=비용 단위 | usage 포함 응답 | input/output 일관 |
| **5** | REST API | 모델 서빙 | curl 테스트 로그 | generate 동작 |
| **6** | CLI | AI 사용 경험 | train→run→chat | CLI로 전체 흐름 |
| **7** | 확장 설계 | 모델 교체 가능성 | Trigram 훅 문서 | 확장 자리 존재 |

---

## 📁 Step 0 실행 계획

### 목표
프로젝트 골격을 만들고 인터페이스를 정의하여 "교체 가능한 구조"의 기반을 만든다.

### 작업 순서

#### 1. Git 저장소 초기화
```bash
git init
echo "build/" > .gitignore
echo ".gradle/" >> .gitignore
echo "*.log" >> .gitignore
```

#### 2. Gradle 멀티모듈 프로젝트 생성
- `settings.gradle` : 5개 모듈 포함
- 루트 `build.gradle` : Java 17, 공통 의존성
- 각 모듈 `build.gradle` : 의존 관계

#### 3. mini-ai-core 인터페이스 정의
```
mini-ai-core/src/main/java/com/miniai/core/
├── tokenizer/
│   └── Tokenizer.java
├── model/
│   ├── LanguageModel.java
│   └── Trainer.java
├── types/
│   ├── Usage.java
│   ├── GenerateRequest.java
│   └── GenerateResponse.java
```

#### 4. 문서 작성
- `README.md` : Step별 흐름 목차
- `docs/STEP-00.md` : 아키텍처 개요 + ASCII 다이어그램
- `docs/demo/` : 데모 로그용 디렉토리

#### 5. 빌드 검증
```bash
./gradlew build > docs/demo/STEP-00.log 2>&1
```

#### 6. Git 커밋 & 태그
```bash
git add .
git commit -m "Step 0: 프로젝트 뼈대 및 인터페이스 정의"
git tag step-00
```

### DoD 체크리스트
- [ ] `./gradlew build` 성공
- [ ] mini-ai-core에 "구현 없는 인터페이스"만 존재
- [ ] docs/STEP-00.md 존재
- [ ] README.md에 Step별 목차 존재
- [ ] docs/demo/STEP-00.log 존재
- [ ] Git 태그 step-00 생성

---

## 🔑 핵심 설계 원칙 (전체 프로젝트)

### 1. 인터페이스 우선 설계
- 모든 주요 컴포넌트는 인터페이스로 정의
- 구현체는 언제든 교체 가능
- mini-ai-core는 **순수 Java** (외부 의존성 0)

### 2. 과정 중심 산출물
매 Step마다:
- **문서** (docs/STEP-XX.md) : 왜 이렇게 했는지
- **데모** (docs/demo/STEP-XX.log) : 실행 결과
- **코드** : 커밋 단위
- **태그** : step-XX

### 3. MVP 우선, 확장 대비
- Bigram만 완전 구현
- Trigram은 "확장 포인트"만 확보
- 품질보다 "학습 경험"에 집중

### 4. 재현 가능성
- Random seed 옵션
- 테스트는 결정적(deterministic) 결과
- 모든 명령어는 문서에 기록

---

## 🚀 다음 액션

**지금 바로 Step 0 시작 가능**

다음 명령어로 Step 0를 진행하시겠습니까?
```
Claude Code에게 "Step 0 시작해줘" 요청
```

또는 수동으로 진행하려면:
1. Git 초기화
2. Gradle 프로젝트 생성
3. 인터페이스 정의
4. 문서 작성
5. 빌드 검증
6. 커밋 & 태그
