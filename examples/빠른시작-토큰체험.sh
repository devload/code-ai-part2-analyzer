#!/bin/bash

# 토큰 개념 빠른 체험 스크립트
# ../doc/토큰을 모르면 AI는 늘 신기한 상자로 남아요.md의 개념들을 실습합니다

echo "========================================"
echo "토큰 개념 빠른 체험"
echo "========================================"
echo ""

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

cd "$(dirname "$0")/.."

echo -e "${BLUE}📚 실습 1: 토큰화 - 텍스트를 조각으로 나누기${NC}"
echo "----------------------------------------"
echo "입력: '오늘은 날씨가 좋다'"
./gradlew :mini-ai-cli:run --args="tokenize '오늘은 날씨가 좋다'" --quiet
echo ""

echo -e "${BLUE}📚 실습 2: 토큰 수 비교 - 띄어쓰기의 영향${NC}"
echo "----------------------------------------"
echo "입력 1: '할 수 있다' (띄어쓰기 O)"
./gradlew :mini-ai-cli:run --args="tokenize '할 수 있다'" --quiet
echo ""
echo "입력 2: '할수있다' (띄어쓰기 X)"
./gradlew :mini-ai-cli:run --args="tokenize '할수있다'" --quiet
echo ""
echo -e "${YELLOW}💡 교육 포인트: 띄어쓰기에 따라 토큰 수가 달라집니다 = 비용이 달라집니다${NC}"
echo ""

echo -e "${BLUE}📚 실습 3: Bigram 학습 - '자주 붙는 쌍을 센다'${NC}"
echo "----------------------------------------"
if [ ! -f "data/교육-bigram.json" ]; then
    echo "교육용 데이터로 학습 중..."
    ./gradlew :mini-ai-cli:run --args="train --corpus data/교육용-토큰개념.txt --output data/교육-bigram.json" --quiet
    echo ""
fi
echo -e "${GREEN}✅ 학습 완료!${NC}"
echo "생성된 artifact 일부:"
cat data/교육-bigram.json | grep -A 5 '"metadata"' | head -10
echo ""
echo -e "${YELLOW}💡 교육 포인트: 학습 = 카운팅 (자주 나온 쌍을 세는 것)${NC}"
echo ""

echo -e "${BLUE}📚 실습 4: 다음 토큰 예측 - 문장 생성하기${NC}"
echo "----------------------------------------"
echo "프롬프트: '오늘은'"
echo "생성 결과:"
./gradlew :mini-ai-cli:run --args="run -p '오늘은' --max-tokens 10 --seed 42" --quiet
echo ""
echo -e "${YELLOW}💡 교육 포인트: AI는 '다음 토큰을 하나씩 골라서 붙이는' 방식으로 문장을 만듭니다${NC}"
echo ""

echo -e "${BLUE}📚 실습 5: Seed로 재현성 확인${NC}"
echo "----------------------------------------"
echo "같은 seed (42) 사용:"
echo "결과 1:"
./gradlew :mini-ai-cli:run --args="run -p '날씨가' --max-tokens 5 --seed 42" --quiet
echo ""
echo "결과 2:"
./gradlew :mini-ai-cli:run --args="run -p '날씨가' --max-tokens 5 --seed 42" --quiet
echo ""
echo "다른 seed (123) 사용:"
./gradlew :mini-ai-cli:run --args="run -p '날씨가' --max-tokens 5 --seed 123" --quiet
echo ""
echo -e "${YELLOW}💡 교육 포인트: 확률적 선택 - 이것이 '같은 질문에 다른 답'이 나오는 이유입니다${NC}"
echo ""

echo -e "${BLUE}📚 실습 6: Usage 측정 - 토큰 = 비용${NC}"
echo "----------------------------------------"
echo "짧은 프롬프트:"
./gradlew :mini-ai-cli:run --args="run -p '좋다' --max-tokens 3" --quiet | grep -A 3 "Usage:"
echo ""
echo "긴 프롬프트:"
./gradlew :mini-ai-cli:run --args="run -p '오늘은 날씨가 정말 좋다' --max-tokens 3" --quiet | grep -A 3 "Usage:"
echo ""
echo -e "${YELLOW}💡 교육 포인트: Input이 길수록, Output이 길수록 Total tokens 증가 = 비용 증가${NC}"
echo ""

echo -e "${BLUE}📚 실습 7: Temperature 차이 체험${NC}"
echo "----------------------------------------"
echo "낮은 Temperature (0.1) - 안정적:"
./gradlew :mini-ai-cli:run --args="run -p '오늘은' --max-tokens 8 --temperature 0.1 --seed 42" --quiet
echo ""
echo "높은 Temperature (2.0) - 창의적:"
./gradlew :mini-ai-cli:run --args="run -p '오늘은' --max-tokens 8 --temperature 2.0 --seed 42" --quiet
echo ""
echo -e "${YELLOW}💡 교육 포인트: Temperature = 답변의 창의성 vs 안정성 조절${NC}"
echo ""

echo "========================================"
echo -e "${GREEN}✨ 토큰 개념 체험 완료!${NC}"
echo "========================================"
echo ""
echo "📖 더 자세한 실습은 다음 문서를 참고하세요:"
echo "   - examples/토큰-개념-실습.md"
echo "   - ../doc/토큰을 모르면 AI는 늘 신기한 상자로 남아요.md"
echo ""
echo "🎯 핵심 개념:"
echo "   1. 토큰 = AI가 다루는 조각 단위"
echo "   2. 학습 = 자주 붙는 쌍을 세기"
echo "   3. 생성 = 다음 토큰을 하나씩 예측"
echo "   4. 비용 = 토큰 수에 비례"
echo "   5. 다양성 = 확률적 선택"
echo ""
