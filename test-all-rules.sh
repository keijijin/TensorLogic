#!/bin/bash

echo "=========================================="
echo "🧪 Tensor Logic ルール検証テスト"
echo "=========================================="
echo ""

# 色の定義
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ========================================
# 例1: 動物分類ルール
# ========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}例1: 動物分類ルール（イルカ）${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}📋 ステップ1: ルール読み込み${NC}"
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/animal-classification-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo -e "${YELLOW}📋 ステップ2: LLM検証${NC}"
echo "質問: イルカは温血動物ですか？"
echo ""
curl -X POST http://localhost:8080/api/camel/verify \
  -H 'Content-Type: application/json' \
  -d '"イルカは温血動物ですか？"' \
  2>/dev/null | jq '{
    llmAnswer: .answer[0:100] + "...",
    llmConfidence,
    isValid: .isLogicallySound,
    expectedValue: .validationDetails.expectedValue,
    actualValue: .validationDetails.actualValue,
    confidence: .validationConfidence
  }'
echo ""
echo -e "${GREEN}✓ 例1完了${NC}"
echo ""
sleep 2

# ========================================
# 例2: 年齢と資格ルール
# ========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}例2: 年齢と資格ルール（多段階推論）${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}📋 ステップ1: ルール読み込み${NC}"
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo -e "${YELLOW}📋 ステップ2: 登録された事実を確認${NC}"
curl http://localhost:8080/api/rules/inspect 2>/dev/null | \
  jq '.facts[] | select(.name | contains("taro")) | {name, value: .fullContent}'
echo ""

echo -e "${YELLOW}📋 ステップ3: LLM検証${NC}"
echo "質問: 20歳の太郎は運転免許を取得できますか？"
echo ""
curl -X POST http://localhost:8080/api/verify/simple \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "20歳の太郎は運転免許を取得できますか？",
    "ruleFile": "rules/age-qualification-rules.yaml"
  }' \
  2>/dev/null | jq '{
    llmAnswer: .llmAnswer[0:100] + "...",
    llmConfidence,
    isValid: .logicallySound,
    inferredFacts,
    validationScore
  }'
echo ""
echo -e "${GREEN}✓ 例2完了${NC}"
echo ""
sleep 2

# ========================================
# 例3: 天気と活動ルール
# ========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}例3: 天気と活動ルール（確率計算）${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

echo -e "${YELLOW}📋 ステップ1: ルール読み込み${NC}"
curl -X POST http://localhost:8080/api/rules/load-resource \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/weather-activity-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo -e "${YELLOW}📋 ステップ2: LLM検証${NC}"
echo "質問: 今日は外出に適していますか？"
echo ""
curl -X POST http://localhost:8080/api/camel/verify \
  -H 'Content-Type: application/json' \
  -d '"今日は外出に適していますか？"' \
  2>/dev/null | jq '{
    llmAnswer: .answer[0:100] + "...",
    llmConfidence,
    isValid: .isLogicallySound,
    expectedValue: .validationDetails.expectedValue,
    actualValue: .validationDetails.actualValue,
    confidence: .validationConfidence
  }'
echo ""
echo -e "${GREEN}✓ 例3完了${NC}"
echo ""

# ========================================
# まとめ
# ========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}📊 テスト結果まとめ${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo "例1: 動物分類（イルカ）"
echo "  期待値: 0.98 (1.0 × 0.98)"
echo ""
echo "例2: 年齢資格（太郎）"
echo "  期待値: taro_is_adult = 1.0, taro_can_drive = 0.95"
echo ""
echo "例3: 天気活動"
echo "  期待値: 0.765 (0.9 × 0.85)"
echo ""
echo -e "${GREEN}✅ 全テスト完了！${NC}"
echo ""

