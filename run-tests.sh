#!/bin/bash

# ========================================
# Tensor Logic テスト実行スクリプト
# ========================================

set -e  # エラー時に終了

# カラー出力
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}  Tensor Logic テスト実行${NC}"
echo -e "${BLUE}=====================================${NC}"
echo ""

# プロジェクトディレクトリに移動
cd "$(dirname "$0")"

# 引数チェック
TEST_TYPE=${1:-all}

case $TEST_TYPE in
  "all")
    echo -e "${GREEN}✓ 全テストを実行します...${NC}"
    mvn clean test
    ;;
  
  "engine")
    echo -e "${GREEN}✓ TensorLogicEngine のテストを実行します...${NC}"
    mvn test -Dtest=TensorLogicEngineTest
    ;;
  
  "parser")
    echo -e "${GREEN}✓ RuleParser のテストを実行します...${NC}"
    mvn test -Dtest=RuleParserTest
    ;;
  
  "routes")
    echo -e "${GREEN}✓ Camel Routes のテストを実行します...${NC}"
    mvn test -Dtest=TensorLogicRoutesTest
    ;;
  
  "api")
    echo -e "${GREEN}✓ REST API のテストを実行します...${NC}"
    mvn test -Dtest=TensorLogicResourceTest
    ;;
  
  "llm")
    echo -e "${GREEN}✓ LLM Service のテストを実行します...${NC}"
    mvn test -Dtest=LLMServiceTest
    ;;
  
  "integration")
    echo -e "${GREEN}✓ 統合テストを実行します...${NC}"
    mvn test -Dtest=IntegrationTest
    ;;
  
  "coverage")
    echo -e "${GREEN}✓ カバレッジレポートを生成します...${NC}"
    mvn clean test jacoco:report
    echo ""
    echo -e "${YELLOW}カバレッジレポート: ${NC}target/site/jacoco/index.html"
    
    # macOSの場合、自動でブラウザを開く
    if [[ "$OSTYPE" == "darwin"* ]]; then
      echo -e "${GREEN}✓ ブラウザでレポートを開きます...${NC}"
      open target/site/jacoco/index.html
    fi
    ;;
  
  "unit")
    echo -e "${GREEN}✓ ユニットテストのみ実行します...${NC}"
    mvn test -Dtest=TensorLogicEngineTest,RuleParserTest,LLMServiceTest
    ;;
  
  "quick")
    echo -e "${GREEN}✓ クイックテスト（コンパイルなし）を実行します...${NC}"
    mvn surefire:test
    ;;
  
  "watch")
    echo -e "${GREEN}✓ 継続的テストモード（Quarkus Dev Mode）を起動します...${NC}"
    echo -e "${YELLOW}  ターミナルで 'r' を押すとテストが実行されます${NC}"
    mvn quarkus:dev
    ;;
  
  "help")
    echo "使用方法: ./run-tests.sh [TEST_TYPE]"
    echo ""
    echo "TEST_TYPE:"
    echo "  all         - 全テストを実行（デフォルト）"
    echo "  engine      - TensorLogicEngineのテストのみ"
    echo "  parser      - RuleParserのテストのみ"
    echo "  routes      - Camel Routesのテストのみ"
    echo "  api         - REST APIのテストのみ"
    echo "  llm         - LLM Serviceのテストのみ"
    echo "  integration - 統合テストのみ"
    echo "  unit        - 全ユニットテスト"
    echo "  coverage    - カバレッジレポート生成"
    echo "  quick       - クイックテスト（コンパイルスキップ）"
    echo "  watch       - 継続的テストモード"
    echo "  help        - このヘルプを表示"
    echo ""
    echo "例:"
    echo "  ./run-tests.sh all"
    echo "  ./run-tests.sh engine"
    echo "  ./run-tests.sh coverage"
    exit 0
    ;;
  
  *)
    echo -e "${RED}✗ 不明なテストタイプ: $TEST_TYPE${NC}"
    echo "使用方法: ./run-tests.sh [all|engine|parser|routes|api|llm|integration|unit|coverage|quick|watch|help]"
    exit 1
    ;;
esac

# 結果の表示
if [ $? -eq 0 ]; then
  echo ""
  echo -e "${GREEN}=====================================${NC}"
  echo -e "${GREEN}  ✓ テストが成功しました！${NC}"
  echo -e "${GREEN}=====================================${NC}"
else
  echo ""
  echo -e "${RED}=====================================${NC}"
  echo -e "${RED}  ✗ テストが失敗しました${NC}"
  echo -e "${RED}=====================================${NC}"
  exit 1
fi

