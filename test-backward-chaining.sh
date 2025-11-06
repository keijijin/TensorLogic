#!/bin/bash

# ================================================
# Backward Chaining テストスクリプト
# ================================================
# 後向き推論の動作を確認するテストスクリプト
#
# 使用方法:
#   chmod +x test-backward-chaining.sh
#   ./test-backward-chaining.sh

echo "======================================"
echo "  Backward Chaining テスト開始"
echo "======================================"
echo ""

# 色の定義
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# ベースURL
BASE_URL="http://localhost:8080"

# ==================================================
# テスト 1: 融資承認の後向き推論
# ==================================================
echo -e "${BLUE}テスト 1: 融資承認の後向き推論${NC}"
echo "目標: loan_approved"
echo ""

echo "ステップ 1: ルールをロード"
curl -X POST "${BASE_URL}/api/rules/load-resource" \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo "ステップ 2: Backward Chaining を実行"
curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{"goal": "loan_approved"}' \
  2>/dev/null | jq '.'
echo ""
echo -e "${GREEN}✓ テスト 1 完了${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 2: 成人判定の後向き推論
# ==================================================
echo -e "${BLUE}テスト 2: 成人判定の後向き推論${NC}"
echo "目標: is_adult"
echo ""

echo "Backward Chaining を実行"
curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{"goal": "is_adult"}' \
  2>/dev/null | jq '.'
echo ""
echo -e "${GREEN}✓ テスト 2 完了${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 3: 財務適格性の後向き推論
# ==================================================
echo -e "${BLUE}テスト 3: 財務適格性の後向き推論${NC}"
echo "目標: financially_eligible"
echo ""

echo "Backward Chaining を実行"
curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{"goal": "financially_eligible"}' \
  2>/dev/null | jq '.'
echo ""
echo -e "${GREEN}✓ テスト 3 完了${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 4: 年齢と資格の後向き推論
# ==================================================
echo -e "${BLUE}テスト 4: 年齢と資格の後向き推論${NC}"
echo "目標: taro_can_drive"
echo ""

echo "ステップ 1: ルールをロード"
curl -X POST "${BASE_URL}/api/rules/load-resource" \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo "ステップ 2: Backward Chaining を実行"
curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{"goal": "taro_can_drive"}' \
  2>/dev/null | jq '.'
echo ""
echo -e "${GREEN}✓ テスト 4 完了${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 5: 存在しない目標（失敗ケース）
# ==================================================
echo -e "${BLUE}テスト 5: 存在しない目標（失敗ケース）${NC}"
echo "目標: nonexistent_goal"
echo ""

echo "Backward Chaining を実行"
curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{"goal": "nonexistent_goal"}' \
  2>/dev/null | jq '.'
echo ""
echo -e "${YELLOW}⚠ テスト 5 完了（期待通りの失敗）${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# サマリー
# ==================================================
echo ""
echo "======================================"
echo -e "${GREEN}  すべてのテスト完了  ${NC}"
echo "======================================"
echo ""
echo "結果の確認ポイント:"
echo "  1. success: true/false"
echo "  2. goalConfidence: 0.0-1.0"
echo "  3. reasoningPath: 推論パスの配列"
echo "  4. requiredFacts: 必要な事実とその値"
echo ""
echo "詳細なドキュメント:"
echo "  - BACKWARD_CHAINING_GUIDE.md"
echo "  - TENSOR_LOGIC_ENGINE_GUIDE.md"
echo ""

