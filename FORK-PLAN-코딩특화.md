# Fork Plan: 코딩 작업 특화 버전

> **기반 프로젝트**: Mini AI Full-Stack (토큰/Bigram 교육용)
> **목표**: 코드 생성/자동완성에 특화된 실용 버전 개발

---

## 🎯 프로젝트 비전

### 현재 버전 (교육용)
```
토큰 → Bigram → 문장 생성
→ "AI 원리 배우기"
```

### Fork 버전 (코딩 특화)
```
코드 토큰 → 코드 패턴 → 코드 생성/자동완성
→ "실제 코딩 어시스턴트"
```

---

## 📋 주요 변경 사항

### 1. 프로젝트 이름 변경

```bash
# Before
mini-ai (토큰 교육용)

# After
code-ai 또는 mini-copilot
→ "코드 생성에 특화된 AI"
```

### 2. 저장소 구조

```bash
# Fork 후 구조
code-ai/
├── mini-ai-core/              # 유지 (인터페이스)
├── code-tokenizer/            # 신규! (코드 전용 토크나이저)
├── code-model-ngram/          # 확장! (코드 패턴 학습)
├── code-server/               # 확장! (코드 자동완성 API)
├── code-cli/                  # 확장! (개발자용 CLI)
├── vscode-extension/          # 신규! (VSCode 플러그인)
├── data/
│   ├── code-corpus/           # 신규! (코드 데이터)
│   │   ├── java/
│   │   ├── python/
│   │   ├── javascript/
│   │   └── ...
│   └── patterns/              # 신규! (코드 패턴)
└── examples/
    └── code-completion-demo/  # 신규! (코드 자동완성 데모)
```

---

## 🔧 핵심 기술 변경

### 1. 토크나이저: 코드 전용

**기존 (WhitespaceTokenizer)**:
```java
"hello world" → ["hello", "world"]
```

**신규 (CodeTokenizer)**:
```java
"function getName() {"
→ ["function", "getName", "(", ")", "{"]

// 특징:
// - 괄호, 세미콜론 분리
// - camelCase 유지
// - 들여쓰기 보존
// - 주석 처리
```

**구현 방향**:
```java
public class CodeTokenizer implements Tokenizer {
    // 1. 언어별 키워드 인식
    // 2. 심볼 분리 (, { } [ ] ( ) ; 등)
    // 3. 문자열/주석 처리
    // 4. 들여쓰기 레벨 토큰화
}
```

### 2. 코퍼스: 실제 코드 데이터

**수집 전략**:

**방법 1: GitHub 크롤링**
```bash
# 인기 오픈소스에서 코드 수집
data/code-corpus/java/
├── spring-framework-samples.txt
├── hibernate-examples.txt
└── common-patterns.txt
```

**방법 2: 템플릿/패턴 직접 작성**
```java
// data/code-corpus/java/patterns.txt
public class ClassName {
public void methodName() {
if (condition) {
System.out.println("message");
}
}
}

for (int i = 0; i < length; i++) {
array[i] = value;
}

try {
operation();
} catch (Exception e) {
logger.error("error", e);
}
```

**방법 3: 자주 쓰는 코드 스니펫**
```java
// data/code-corpus/java/snippets.txt
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
@Autowired private ServiceName serviceName;
public ResponseEntity<DataType> methodName(@RequestBody RequestType request) {
```

### 3. N-gram 확장: 코드 패턴 인식

**Bigram 예시**:
```
"public" → "class" (80%)
"public" → "void" (15%)
"public" → "static" (5%)

"if" → "(" (99%)
"(" → "condition" (70%)
```

**Trigram으로 확장 (더 정확)**:
```
("public", "static") → "void" (60%)
("public", "static") → "final" (40%)

("if", "(") → "null" (30%)
("if", "(") → "condition" (50%)
```

### 4. 새로운 기능: 코드 자동완성

**API 확장**:
```java
// 기존
POST /v1/generate
{
  "prompt": "the cat",
  "maxTokens": 10
}

// 신규
POST /v1/code/complete
{
  "code": "public class User {\n    private String ",
  "language": "java",
  "cursorPosition": 45,
  "maxSuggestions": 5
}

→ 응답:
{
  "suggestions": [
    "name;",
    "email;",
    "id;",
    "username;",
    "password;"
  ],
  "confidence": [0.8, 0.6, 0.5, 0.4, 0.3]
}
```

---

## 🚀 구현 로드맵

### Phase 1: Fork 및 기본 구조 (1주)

**Task 1.1: 저장소 Fork**
```bash
# 현재 프로젝트 Fork
cd ..
cp -r aimaker code-ai
cd code-ai

# Git 초기화
git remote remove origin
git remote add origin <new-repo-url>

# 브랜치 전략
git checkout -b feature/code-tokenizer
```

**Task 1.2: 이름 변경**
```bash
# 모듈 이름 변경
mv mini-ai-core code-core
mv mini-ai-server code-server
mv mini-ai-cli code-cli

# build.gradle 수정
# package 이름 변경
```

**Task 1.3: README 업데이트**
```markdown
# Code AI - 코드 생성 특화 AI

기반: Mini AI (토큰 교육용)
목적: 실제 코드 자동완성

## 차별점
- ✅ 코드 전용 토크나이저
- ✅ 실제 코드 코퍼스
- ✅ VSCode 통합
- ✅ 다중 언어 지원
```

---

### Phase 2: 코드 토크나이저 개발 (1-2주)

**Task 2.1: CodeTokenizer 기본**
```java
// code-tokenizer/src/main/java/com/codeai/tokenizer/CodeTokenizer.java

public class CodeTokenizer implements Tokenizer {

    @Override
    public List<Integer> encode(String code) {
        // 1. 언어 감지
        String language = detectLanguage(code);

        // 2. 언어별 토큰화
        List<String> tokens = tokenizeByLanguage(code, language);

        // 3. 심볼 분리
        tokens = splitSymbols(tokens);

        // 4. ID 변환
        return tokensToIds(tokens);
    }

    private List<String> tokenizeByLanguage(String code, String lang) {
        switch(lang) {
            case "java": return tokenizeJava(code);
            case "python": return tokenizePython(code);
            default: return tokenizeGeneric(code);
        }
    }

    private List<String> tokenizeJava(String code) {
        // Java 키워드 인식
        // 괄호, 세미콜론 분리
        // camelCase 유지
        // 들여쓰기 보존
    }
}
```

**Task 2.2: 테스트 작성**
```java
@Test
public void testJavaCodeTokenization() {
    String code = "public void getName() {";
    List<String> tokens = tokenizer.tokenize(code);

    assertEquals(Arrays.asList(
        "public", "void", "getName", "(", ")", "{"
    ), tokens);
}

@Test
public void testIndentationPreservation() {
    String code = "    if (true) {\n        return;";
    // 들여쓰기가 토큰에 포함되어야 함
}
```

---

### Phase 3: 코드 코퍼스 준비 (1주)

**Task 3.1: Java 코퍼스 수집**
```bash
# Spring Boot 일반 패턴
data/code-corpus/java/spring-patterns.txt
```

**내용 예시**:
```java
@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody UserRequest request) {
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}

// 반복되는 패턴들
for (int i = 0; i < list.size(); i++) {
    Item item = list.get(i);
    process(item);
}

if (value != null) {
    return value;
} else {
    return defaultValue;
}

try {
    operation();
} catch (Exception e) {
    logger.error("Failed", e);
    throw new CustomException(e);
}
```

**Task 3.2: Python 코퍼스**
```python
# data/code-corpus/python/patterns.txt

def function_name(param1, param2):
    if param1 is None:
        return None

    result = []
    for item in param2:
        if condition(item):
            result.append(item)

    return result

class ClassName:
    def __init__(self, param):
        self.param = param

    def method_name(self):
        return self.param

try:
    operation()
except Exception as e:
    logger.error(f"Error: {e}")
    raise
```

**Task 3.3: JavaScript 코퍼스**
```javascript
// data/code-corpus/javascript/patterns.txt

function functionName(param1, param2) {
    if (!param1) {
        return null;
    }

    const result = param2.filter(item => condition(item));
    return result.map(item => transform(item));
}

const Component = ({ prop1, prop2 }) => {
    const [state, setState] = useState(initialValue);

    useEffect(() => {
        fetchData();
    }, [dependency]);

    return (
        <div>
            {state.map(item => <Item key={item.id} data={item} />)}
        </div>
    );
};
```

---

### Phase 4: 코드 자동완성 API (2주)

**Task 4.1: 자동완성 엔진**
```java
public class CodeCompletionEngine {

    public List<CodeSuggestion> complete(CodeCompletionRequest request) {
        // 1. 커서 위치까지의 코드 분석
        String prefix = request.getCode().substring(0, request.getCursorPosition());

        // 2. 토큰화
        List<Integer> tokens = tokenizer.encode(prefix);

        // 3. 다음 토큰 예측 (N-gram)
        Map<Integer, Double> nextTokenProbs = model.predictNext(tokens);

        // 4. 상위 K개 선택
        List<Integer> topK = selectTopK(nextTokenProbs, request.getMaxSuggestions());

        // 5. 토큰 → 코드 변환
        return topK.stream()
            .map(tokenizer::decode)
            .map(code -> new CodeSuggestion(code, nextTokenProbs.get(code)))
            .collect(Collectors.toList());
    }
}
```

**Task 4.2: Context-aware 개선**
```java
public class ContextAwareCompletion {

    public List<CodeSuggestion> complete(CodeCompletionRequest request) {
        // 1. 현재 컨텍스트 분석
        CodeContext context = analyzeContext(request.getCode());

        // 2. 컨텍스트별 필터링
        if (context.isInsideClass()) {
            // 클래스 멤버 제안
            return suggestClassMembers();
        } else if (context.isInsideMethod()) {
            // 메서드 본문 제안
            return suggestMethodBody();
        } else if (context.isAfterImport()) {
            // import 문 제안
            return suggestImports();
        }

        // 3. 일반 제안
        return suggestGeneral(request);
    }
}
```

---

### Phase 5: VSCode Extension (2-3주)

**Task 5.1: Extension 프로젝트 생성**
```bash
# VSCode Extension 생성
cd code-ai
mkdir vscode-extension
cd vscode-extension
npm init -y
npm install --save-dev @types/vscode
```

**Task 5.2: Extension 기본 구조**
```typescript
// vscode-extension/src/extension.ts

import * as vscode from 'vscode';

export function activate(context: vscode.ExtensionContext) {
    // 자동완성 Provider 등록
    const provider = new CodeCompletionProvider();

    context.subscriptions.push(
        vscode.languages.registerCompletionItemProvider(
            ['java', 'python', 'javascript'],
            provider,
            '.' // trigger character
        )
    );
}

class CodeCompletionProvider implements vscode.CompletionItemProvider {
    async provideCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position
    ): Promise<vscode.CompletionItem[]> {

        // 1. 현재 코드 가져오기
        const code = document.getText();
        const cursorPosition = document.offsetAt(position);

        // 2. Code AI 서버에 요청
        const suggestions = await this.fetchSuggestions(code, cursorPosition);

        // 3. VSCode CompletionItem으로 변환
        return suggestions.map(s => {
            const item = new vscode.CompletionItem(s.code);
            item.kind = vscode.CompletionItemKind.Snippet;
            item.detail = `Confidence: ${s.confidence}`;
            return item;
        });
    }

    private async fetchSuggestions(code: string, position: number) {
        const response = await fetch('http://localhost:8080/v1/code/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code, cursorPosition: position })
        });
        return response.json();
    }
}
```

---

## 📊 차별화 포인트

### 1. 교육 + 실용 겸용

**교육 모드**:
```bash
# 기존 토큰 개념 실습
./examples/빠른시작-토큰체험.sh

# 코드 패턴 학습 체험
./examples/코드패턴-학습.sh
→ 어떻게 코드 패턴을 학습하는지 시연
```

**실용 모드**:
```bash
# 실제 코드 자동완성
code-ai complete --file MyClass.java --line 10
→ 실제 사용 가능한 제안
```

### 2. 경량 & 오프라인

**장점**:
- ✅ GitHub Copilot과 달리 오프라인 가능
- ✅ 로컬에서 실행 (프라이버시)
- ✅ 빠른 응답 (<100ms)
- ✅ 무료

**한계 (투명하게 공개)**:
- ⚠️ GPT/Copilot보다 품질 낮음
- ⚠️ 단순 패턴만 학습
- ⚠️ 복잡한 로직은 어려움

### 3. 커스터마이징 가능

**자신의 코드 스타일로 학습**:
```bash
# 팀의 코드베이스로 학습
code-ai train --corpus ./src/**/*.java --output team-model.json

# 팀 전용 자동완성
code-ai complete --model team-model.json
```

---

## 🎯 실사용 시나리오

### 시나리오 1: Spring Boot 개발

```java
// 사용자가 타이핑:
@RestController
public class UserController {

    @Autowired
    private |  // 커서 위치

// Code AI 제안:
UserService userService;  // 90%
UserRepository userRepository;  // 80%
```

### 시나리오 2: 반복 코드 작성

```java
// 사용자가 타이핑:
for (|  // 커서 위치

// Code AI 제안:
int i = 0; i < list.size(); i++  // 70%
User user : users  // 60%
String item : items  // 50%
```

### 시나리오 3: 에러 핸들링

```java
// 사용자가 타이핑:
try {
    operation();
} |  // 커서 위치

// Code AI 제안:
catch (Exception e) {  // 95%
finally {  // 40%
```

---

## 📝 마이그레이션 가이드

### 기존 프로젝트에서 가져올 것

✅ **유지**:
- `mini-ai-core` → `code-core` (인터페이스 구조)
- Bigram/Trigram 학습 로직
- Usage 측정
- REST API 기본 구조

✅ **확장**:
- Tokenizer → CodeTokenizer
- LanguageModel → CodeModel
- Sampler → CodeSuggester

✅ **신규 추가**:
- VSCode Extension
- 코드 코퍼스
- Context 분석
- 다중 언어 지원

---

## 🚦 다음 단계

### 즉시 시작
```bash
# 1. Fork
cp -r aimaker code-ai
cd code-ai

# 2. 첫 커밋
git init
git add .
git commit -m "Fork from mini-ai for code specialization"

# 3. 첫 이슈 생성
# - [ ] CodeTokenizer 설계
# - [ ] Java 코퍼스 수집
# - [ ] 자동완성 API 설계
```

### 1주차 목표
- [ ] Fork 완료
- [ ] 기본 CodeTokenizer 구현
- [ ] Java 패턴 100개 수집
- [ ] 테스트 작성

### 1개월 목표
- [ ] CodeTokenizer 완성
- [ ] 3개 언어 코퍼스 (Java, Python, JS)
- [ ] 자동완성 API 동작
- [ ] CLI 데모 가능

### 3개월 목표
- [ ] VSCode Extension 배포
- [ ] 실사용 가능한 품질
- [ ] 블로그/발표 자료

---

**이제 Fork해서 시작하시겠어요?** 🚀
