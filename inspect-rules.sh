#!/bin/bash

# ルール確認スクリプト
# 使い方: ./inspect-rules.sh

BASE_URL="http://localhost:8080/api/rules/inspect"

# カラー設定
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}╔════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║       TensorLogic ルール確認ツール            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════╝${NC}"
echo ""

# jqがインストールされているか確認
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}警告: jq がインストールされていません。${NC}"
    echo "整形されていない出力を表示します。"
    echo "jq をインストールすると、より読みやすくなります: brew install jq"
    echo ""
    JQ_CMD="cat"
else
    JQ_CMD="jq"
fi

# 1. システム状態
echo -e "${BLUE}===== システム状態 =====${NC}"
curl -s "$BASE_URL/status" | $JQ_CMD
echo ""

# 2. ルール一覧
echo -e "${BLUE}===== ルール一覧 =====${NC}"
if command -v jq &> /dev/null; then
    curl -s "$BASE_URL/rules" | jq -r '.rules[] | "[\(.operation)] \(.name)\n  記法: \(.notation)\n"'
else
    curl -s "$BASE_URL/rules"
fi
echo ""

# 3. 事実一覧
echo -e "${BLUE}===== 事実（ファクト）一覧 =====${NC}"
if command -v jq &> /dev/null; then
    curl -s "$BASE_URL/facts" | jq -r '.facts[] | "\(.name) \(.shape)\n  値: \(.preview)\n  統計: min=\(.stats.min), max=\(.stats.max), mean=\(.stats.mean)\n"'
else
    curl -s "$BASE_URL/facts"
fi
echo ""

# 4. 特定のルールを詳しく表示（存在する場合）
echo -e "${BLUE}===== ルール詳細（最初の1つ） =====${NC}"
FIRST_RULE=$(curl -s "$BASE_URL/rules" | jq -r '.rules[0].name // empty' 2>/dev/null)
if [ -n "$FIRST_RULE" ]; then
    curl -s "$BASE_URL/rules/$FIRST_RULE" | $JQ_CMD
else
    echo "ルールが見つかりません"
fi
echo ""

# 5. 特定の事実を詳しく表示（存在する場合）
echo -e "${BLUE}===== 事実詳細（最初の1つ） =====${NC}"
FIRST_FACT=$(curl -s "$BASE_URL/facts" | jq -r '.facts[0].name // empty' 2>/dev/null)
if [ -n "$FIRST_FACT" ]; then
    curl -s "$BASE_URL/facts/$FIRST_FACT" | $JQ_CMD
else
    echo "事実が見つかりません"
fi
echo ""

echo -e "${GREEN}完了！${NC}"

