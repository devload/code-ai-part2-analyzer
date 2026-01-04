# AI Process - Part 2: 코드 이해 프로세스

> **AI가 코드를 분석하고 이해하는 과정**을 프로세스 관점에서 학습하는 교육용 프로젝트
>
> 이 프로젝트는 [Part 1: 기초 프로세스](https://github.com/devload/code-ai-part1-basics)를 기반으로 합니다.

---

## 학습 목표

**AI가 코드를 분석하고 품질을 평가하기까지의 내부 과정을 이해합니다**

```
입력: Java 코드
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 7: 파싱 (Parsing)                                   │
│ 텍스트 → 구조화된 데이터 (CompilationUnit)                │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 8: AST 분석 (Abstract Syntax Tree)                  │
│ 코드 구조 → 메트릭 (복잡도, 중첩 깊이, 라인 수)           │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 9: 의미 분석 (Semantic Analysis)                    │
│ 변수 사용 추적, 타입 분석, 메서드 호출 관계               │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 10: 패턴 매칭 (Pattern Matching)                    │
│ 코드 스멜 탐지 (System.out, 빈 catch, 매직 넘버)          │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 11: 이슈 탐지 (Issue Detection)                     │
│ 보안 취약점 탐지 (SQL Injection, 하드코딩 비밀)           │
└─────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────┐
│ STEP 12: 점수화 (Scoring)                                │
│ 6차원 품질 점수 → A~F 등급                               │
└─────────────────────────────────────────────────────────┘
        │
        ▼
출력: 📊 78/100점 (등급: B)
```

---

## 프로젝트 구조

```
ai-process-part2/
├── step7-parsing/             # STEP 7: 파싱
│   ├── CodeParser.java
│   └── ParsingDemo.java
│
├── step8-ast/                 # STEP 8: AST 분석
│   ├── ASTAnalyzer.java
│   └── ASTDemo.java
│
├── step9-semantics/           # STEP 9: 의미 분석
│   ├── SemanticAnalyzer.java
│   └── SemanticsDemo.java
│
├── step10-pattern/            # STEP 10: 패턴 매칭
│   ├── PatternMatcher.java
│   └── PatternDemo.java
│
├── step11-detection/          # STEP 11: 이슈 탐지
│   ├── IssueDetector.java
│   └── DetectionDemo.java
│
├── step12-scoring/            # STEP 12: 점수화
│   ├── CodeScorer.java
│   └── ScoringDemo.java
│
├── code-pipeline/             # 코드 분석 파이프라인 통합
│   └── CodePipelineDemo.java
│
├── code-ai-analyzer/          # 기존 코드 분석 엔진
├── (Part 1 모듈들)
│
└── docs/                      # 문서
```

---

## 학습 단계

| STEP | 제목 | 핵심 질문 | 파일 |
|------|------|----------|------|
| 7 | 파싱 | 코드를 어떻게 읽는가? | `step7-parsing/` |
| 8 | AST 분석 | 코드의 구조를 어떻게 파악하는가? | `step8-ast/` |
| 9 | 의미 분석 | 변수/타입을 어떻게 추적하는가? | `step9-semantics/` |
| 10 | 패턴 매칭 | 나쁜 코드를 어떻게 찾는가? | `step10-pattern/` |
| 11 | 이슈 탐지 | 버그/보안 문제를 어떻게 발견하는가? | `step11-detection/` |
| 12 | 점수화 | 코드 품질을 어떻게 측정하는가? | `step12-scoring/` |

---

## 빠른 시작

### 빌드
```bash
./gradlew build
```

### 각 단계 데모 실행

```bash
# STEP 7: 파싱 데모
./gradlew :step7-parsing:run

# STEP 8: AST 분석 데모
./gradlew :step8-ast:run

# STEP 9: 의미 분석 데모
./gradlew :step9-semantics:run

# STEP 10: 패턴 매칭 데모
./gradlew :step10-pattern:run

# STEP 11: 이슈 탐지 데모
./gradlew :step11-detection:run

# STEP 12: 점수화 데모
./gradlew :step12-scoring:run

# 전체 코드 분석 파이프라인
./gradlew :code-pipeline:run
```

---

## 탐지 가능한 이슈

### 코드 스멜 (STEP 10)
| 이슈 | 설명 | 심각도 |
|------|------|--------|
| SYSTEM_OUT | System.out.println 사용 | WARNING |
| EMPTY_CATCH | 빈 catch 블록 | CRITICAL |
| MAGIC_NUMBER | 매직 넘버 사용 | WARNING |
| LONG_METHOD | 20줄 이상 메서드 | WARNING |
| MISSING_BRACES | if문 중괄호 누락 | INFO |

### 보안 이슈 (STEP 11)
| 이슈 | 설명 | 심각도 |
|------|------|--------|
| SQL_INJECTION | 문자열 연결 SQL | CRITICAL |
| HARDCODED_SECRET | 하드코딩된 비밀번호 | CRITICAL |
| WEAK_CRYPTO | MD5/SHA1 사용 | WARNING |

---

## 점수 체계 (STEP 12)

```
┌─────────────────────────────────────────────────────┐
│               코드 품질 점수 (100점 만점)             │
├─────────────┬───────────────────────────────────────┤
│ 카테고리     │ 배점                                  │
├─────────────┼───────────────────────────────────────┤
│ 구조        │ 20점                                  │
│ 가독성      │ 20점                                  │
│ 유지보수성  │ 20점                                  │
│ 신뢰성      │ 15점                                  │
│ 보안        │ 15점                                  │
│ 성능        │ 10점                                  │
└─────────────┴───────────────────────────────────────┘

등급: A(90+), B(80+), C(70+), D(60+), F(<60)
```

---

## 시리즈 구성

```
Part 1: AI 기초 프로세스
├── 토큰화 → 컨텍스트 → 확률계산 → 샘플링 → 생성루프 → 후처리
│
Part 2: 코드 이해 프로세스 (현재)
├── 파싱 → AST → 의미분석 → 패턴매칭 → 이슈탐지 → 점수화
│
└── Part 3: AI 서비스 프로세스
    └── API호출 → 프롬프트구성 → LLM처리 → 응답파싱 → 액션실행
```

---

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| JavaParser | 3.25.8 | AST 분석 |
| Symbol Solver | 3.25.8 | 타입 해석 |

---

## 이전 / 다음 단계

👈 [Part 1: 기초 프로세스](https://github.com/devload/code-ai-part1-basics)

👉 [Part 3: 서비스화편](https://github.com/devload/code-ai-part3-service)

---

## 라이선스

MIT License

---

**Version**: 2.0.0 | **Focus**: Code Analysis Process Education
