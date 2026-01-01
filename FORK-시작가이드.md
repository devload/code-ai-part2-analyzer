# Fork 빠른 시작 가이드

> 코딩 특화 버전으로 Fork하는 첫 단계

---

## 🚀 5분 안에 Fork 시작하기

### Step 1: Fork 생성 (1분)

```bash
# 현재 위치: aimaker 프로젝트 상위 폴더
cd /Users/devload

# Fork (복사)
cp -r aimaker code-ai
cd code-ai

# 확인
ls -la
```

### Step 2: Git 초기화 (1분)

```bash
# 기존 git 히스토리 제거 (선택)
rm -rf .git

# 새로 시작
git init
git add .
git commit -m "Initial commit: Fork from mini-ai for code specialization

Base project: Mini AI (토큰 교육용)
Target: Code-specialized AI assistant
Focus: Code completion, code generation
"

# 원격 저장소 연결 (본인 GitHub)
git remote add origin https://github.com/YOUR_USERNAME/code-ai.git
git branch -M main
git push -u origin main
```

### Step 3: 첫 변경 - README 업데이트 (2분)

```bash
# README.md 맨 위에 추가
cat > README-NEW.md << 'EOF'
# Code AI - 코드 생성 특화 AI 어시스턴트

> **기반**: [Mini AI](https://github.com/ORIGINAL/mini-ai) - 토큰/Bigram 교육용 프로젝트
> **목적**: 실제 코드 자동완성 및 생성에 특화

## 🎯 차별점

| 특징 | Mini AI (기반) | Code AI (이 프로젝트) |
|------|---------------|-------------------|
| 목적 | 교육 (토큰 이해) | 실용 (코드 생성) |
| 토크나이저 | Whitespace | Code-aware |
| 코퍼스 | 일반 문장 | 코드 패턴 |
| 출력 | 텍스트 | 코드 |
| 통합 | CLI/API | VSCode Extension |

## 🚀 빠른 시작

\`\`\`bash
# 코드 패턴 학습
./gradlew :code-cli:run --args="train --corpus data/code-corpus/java/patterns.txt"

# 코드 자동완성
./gradlew :code-cli:run --args="complete --code 'public class User {'"
\`\`\`

## 📚 로드맵

- [x] Phase 0: Fork from Mini AI
- [ ] Phase 1: CodeTokenizer 개발
- [ ] Phase 2: 코드 코퍼스 수집
- [ ] Phase 3: 자동완성 API
- [ ] Phase 4: VSCode Extension

자세한 계획: [FORK-PLAN-코딩특화.md](FORK-PLAN-코딩특화.md)

---

EOF

# 기존 README 백업하고 새로 작성
mv README.md README-ORIGINAL.md
mv README-NEW.md README.md

git add .
git commit -m "Update README for code-ai fork"
```

### Step 4: 첫 이슈 생성 (1분)

GitHub에서 Issues 생성:

```markdown
## Phase 1: CodeTokenizer 개발

### 목표
코드를 올바르게 토큰화하는 CodeTokenizer 구현

### Tasks
- [ ] 기본 CodeTokenizer 인터페이스 설계
- [ ] Java 토큰화 구현
- [ ] 괄호/세미콜론 분리
- [ ] 테스트 작성
- [ ] 문서화

### 예상 소요 시간
1-2주

### 참고
- [FORK-PLAN-코딩특화.md](FORK-PLAN-코딩특화.md) - Phase 2 참조
```

---

## 📁 초기 폴더 구조 만들기

### 코드 코퍼스 폴더 생성

```bash
# data 폴더에 코드 전용 디렉토리 추가
mkdir -p data/code-corpus/{java,python,javascript}
mkdir -p data/code-patterns/{common,framework-specific}

# 초기 Java 패턴 파일 생성
cat > data/code-corpus/java/basic-patterns.txt << 'EOF'
public class ClassName {
private String fieldName;

public ClassName(String fieldName) {
this.fieldName = fieldName;
}

public String getFieldName() {
return fieldName;
}

public void setFieldName(String fieldName) {
this.fieldName = fieldName;
}
}

for (int i = 0; i < array.length; i++) {
System.out.println(array[i]);
}

if (condition) {
return true;
} else {
return false;
}

try {
operation();
} catch (Exception e) {
e.printStackTrace();
}
EOF

git add data/
git commit -m "Add initial code corpus structure"
```

---

## 🎯 첫 주 작업 가이드

### Day 1: 환경 설정
- [x] Fork 완료
- [x] Git 설정
- [x] README 업데이트
- [ ] 팀원 초대 (협업 시)

### Day 2-3: 설계
- [ ] CodeTokenizer 인터페이스 설계
- [ ] 코드 코퍼스 구조 설계
- [ ] API 엔드포인트 설계

### Day 4-5: 구현 시작
- [ ] CodeTokenizer 기본 구현
- [ ] Java 토큰화 로직
- [ ] 테스트 작성

### Day 6-7: 데이터 준비
- [ ] Java 패턴 100개 수집
- [ ] 학습 실행
- [ ] 결과 확인

---

## 💡 즉시 실험해볼 것

### 실험 1: 기존 시스템으로 코드 학습

```bash
# 현재 시스템으로 Java 코드를 학습하면?
cat > /tmp/test-code.txt << 'EOF'
public class User {
public String name;
public String email;
}
public void getName() {
return name;
}
public void setName(String name) {
this.name = name;
}
EOF

# 학습
./gradlew :mini-ai-cli:run --args="train --corpus /tmp/test-code.txt --output /tmp/code-test.json"

# 생성 시도
./gradlew :mini-ai-cli:run --args="run -p 'public' --max-tokens 5"
```

**결과 예상:**
```
public class User {
public void getName
```

**문제점 발견:**
- 괄호가 분리 안 됨
- 들여쓰기 무시
- 코드 구조 이해 못함

→ **CodeTokenizer 필요성 확인!**

### 실험 2: 어떤 패턴이 학습되는지 확인

```bash
# Artifact 확인
cat /tmp/code-test.json | jq '.counts' | head -20

# 어떤 Bigram이 생성되었는지 확인
# "public" → "class" 몇 번?
# "String" → "name" 몇 번?
```

→ **어떤 데이터가 필요한지 감 잡기**

---

## 📋 체크리스트

### Fork 완료 확인
- [ ] code-ai 폴더 생성됨
- [ ] Git 초기화 완료
- [ ] README 업데이트
- [ ] 원격 저장소 연결 (GitHub)

### 초기 구조 확인
- [ ] data/code-corpus/ 폴더 생성
- [ ] 초기 Java 패턴 파일 생성
- [ ] FORK-PLAN-코딩특화.md 존재
- [ ] 이 가이드 (FORK-시작가이드.md) 존재

### 다음 단계 준비
- [ ] GitHub Issues 생성
- [ ] Phase 1 계획 확인
- [ ] 팀원과 공유 (협업 시)

---

## 🔗 유용한 링크

### 참고 문서
- [FORK-PLAN-코딩특화.md](FORK-PLAN-코딩특화.md) - 전체 계획
- [README-ORIGINAL.md](README-ORIGINAL.md) - 원본 프로젝트 문서

### 기반 프로젝트
- [Mini AI GitHub](#) - 원본 저장소
- [NotebookLM-소스-토큰과AI.md](NotebookLM-소스-토큰과AI.md) - 토큰 개념 설명

### 영감
- GitHub Copilot
- TabNine
- Kite (discontinued but good reference)

---

**Fork 준비 완료! 이제 코딩 특화 AI를 만들어봅시다!** 🚀
