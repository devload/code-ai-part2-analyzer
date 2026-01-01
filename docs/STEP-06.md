# Step 6: CLI 만들기 (사용자 경험)

## 학습 포인트

**"CLI로 AI를 쓰는 흐름"을 직접 만듭니다.**

---

## 구현된 명령어

### 1. tokenize
텍스트 토큰화
```bash
mini-ai tokenize "hello world"
```

### 2. train
모델 학습
```bash
mini-ai train --corpus data/corpus.txt --output data/model.json
```

### 3. run
텍스트 생성
```bash
mini-ai run -p "the cat" --max-tokens 10 --seed 42
```

---

## 기술 스택

- **picocli**: CLI 프레임워크
- **OkHttp**: HTTP 클라이언트
- **Gson**: JSON 처리

---

## DoD

- ✅ tokenize 명령어 동작
- ✅ train 명령어 (HTTP API 호출)
- ✅ run 명령어 (텍스트 생성)
- ✅ Usage 정보 출력

---

## 다음: Step 7 (문서화)
