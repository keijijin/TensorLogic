#!/bin/bash

# ================================================
# Namespace テストスクリプト
# ================================================
# ネームスペース機能の動作を確認するテストスクリプト
#
# 使用方法:
#   chmod +x test-namespace.sh
#   ./test-namespace.sh

echo "======================================"
echo "  Namespace テスト開始"
echo "======================================"
echo ""

# 色の定義
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# ベースURL
BASE_URL="http://localhost:8080"

# ==================================================
# セットアップ: 複数のルールセットをロード
# ==================================================
echo -e "${BLUE}セットアップ: ルールセットのロード${NC}"
echo ""

echo "ルール1: loan-approval をロード"
curl -X POST "${BASE_URL}/api/rules/load-resource" \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/loan-approval-from-drd.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo "ルール2: age-qualification をロード"
curl -X POST "${BASE_URL}/api/rules/load-resource" \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/age-qualification-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo "ルール3: bird-contradiction をロード"
curl -X POST "${BASE_URL}/api/rules/load-resource" \
  -H 'Content-Type: application/json' \
  -d '{"resourcePath": "rules/bird-contradiction-rules.yaml"}' \
  2>/dev/null | jq '.'
echo ""

echo "ルール確認"
curl "${BASE_URL}/api/rules/inspect" 2>/dev/null | jq '.count, .namespaces'
echo ""

echo -e "${GREEN}✓ セットアップ完了${NC}"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 1: 特定のネームスペースのみ (loan-approval)
# ==================================================
echo -e "${BLUE}テスト 1: loan-approval ネームスペースのみ${NC}"
echo ""

curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved",
    "namespace": "loan-approval"
  }' \
  2>/dev/null | jq '{
    success,
    goal,
    goalConfidence,
    reasoningPath: .reasoningPath[:5]
  }'
echo ""
echo -e "${GREEN}✓ テスト 1 完了${NC}"
echo "→ 推論パスに '(ns: loan-approval)' が表示されること"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 2: 特定のネームスペースのみ (age-qualification)
# ==================================================
echo -e "${BLUE}テスト 2: age-qualification ネームスペースのみ${NC}"
echo ""

curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "taro_can_drive",
    "namespace": "age-qualification"
  }' \
  2>/dev/null | jq '{
    success,
    goal,
    goalConfidence,
    reasoningPath
  }'
echo ""
echo -e "${GREEN}✓ テスト 2 完了${NC}"
echo "→ 推論パスに '(ns: age-qualification)' が表示されること"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 3: 全ネームスペース (ワイルドカード "*")
# ==================================================
echo -e "${BLUE}テスト 3: 全ネームスペース (ワイルドカード)${NC}"
echo ""

curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "is_adult",
    "namespace": "*"
  }' \
  2>/dev/null | jq '{
    success,
    goal,
    goalConfidence,
    reasoningPath
  }'
echo ""
echo -e "${GREEN}✓ テスト 3 完了${NC}"
echo "→ 複数のネームスペースからルールが適用される可能性"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 4: ネームスペース指定なし（デフォルト = 全ルール）
# ==================================================
echo -e "${BLUE}テスト 4: ネームスペース指定なし（デフォルト）${NC}"
echo ""

curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved"
  }' \
  2>/dev/null | jq '{
    success,
    goal,
    goalConfidence,
    reasoningPath: .reasoningPath[:5]
  }'
echo ""
echo -e "${GREEN}✓ テスト 4 完了${NC}"
echo "→ 全ネームスペースのルールが適用される"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 5: 存在しないネームスペース（失敗ケース）
# ==================================================
echo -e "${BLUE}テスト 5: 存在しないネームスペース（失敗ケース）${NC}"
echo ""

curl -X POST "${BASE_URL}/api/tensor-logic/backward-chain" \
  -H 'Content-Type: application/json' \
  -d '{
    "goal": "loan_approved",
    "namespace": "nonexistent-namespace"
  }' \
  2>/dev/null | jq '{
    success,
    goal,
    goalConfidence
  }'
echo ""
echo -e "${YELLOW}⚠ テスト 5 完了（期待通りの失敗）${NC}"
echo "→ success: false となるはず"
echo ""
echo "======================================"
echo ""

# ==================================================
# テスト 6: Forward Chaining でネームスペース指定
# ==================================================
echo -e "${BLUE}テスト 6: Forward Chaining でネームスペース指定${NC}"
echo ""

# まずルールをクリアするためアプリケーションを再起動が必要だが、
# テストではそのまま実行
echo "loan-approval ネームスペースのみで Forward Chaining"
curl -X POST "${BASE_URL}/api/verify/simple" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "18歳で年収300万円以上、信用スコアが良好な申請者は融資を受けられますか？",
    "ruleFile": "rules/loan-approval-from-drd.yaml",
    "namespace": "loan-approval"
  }' \
  2>/dev/null | jq '{
    success,
    logicallySound,
    validationScore,
    inferredFacts: (.inferredFacts | keys)
  }'
echo ""
echo -e "${GREEN}✓ テスト 6 完了${NC}"
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
echo "検証ポイント:"
echo "  1. 推論パスに '(ns: namespace-name)' が表示される"
echo "  2. 特定のネームスペース指定で、そのルールのみ適用"
echo "  3. ワイルドカード(*) または省略で全ルール適用"
echo "  4. 存在しないネームスペースは失敗"
echo ""
echo "詳細なドキュメント:"
echo "  - NAMESPACE_GUIDE.md"
echo ""

