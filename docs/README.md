# 📚 TensorLogic ドキュメント

このディレクトリには、TensorLogicプロジェクトの全ドキュメントが整理されています。

---

## 📂 **ディレクトリ構成**

### 📖 **[guides/](./guides/)** - ガイド・チュートリアル

システムの使い方、機能の詳細、設定方法などのガイドドキュメント。

| ファイル | 内容 |
|---------|------|
| **QUICKSTART.md** | クイックスタートガイド |
| **TENSOR_LOGIC_ENGINE_GUIDE.md** | Tensor Logicエンジンの完全ガイド |
| **BACKWARD_CHAINING_GUIDE.md** | 後向き推論の実装と使い方 |
| **LLM_TO_TENSOR_LOGIC_GUIDE.md** | LLM推論のTensor Logic化ガイド |
| **GENERIC_LLM_VERIFICATION_GUIDE.md** | 汎用LLM検証システムのガイド |
| **YAML_RULE_CREATION_GUIDE.md** | YAMLルールファイルの作成方法 |
| **RULE_AND_TENSOR_GUIDE.md** | ルールとテンソルの完全ガイド |
| **RULE_DSL_GUIDE.md** | ルールDSL（Domain Specific Language）ガイド |
| **RULE_INSPECTION_GUIDE.md** | ルール検証APIの使い方 |
| **RULE_AUTO_LOAD_GUIDE.md** | ルール自動読み込みガイド |
| **NAMESPACE_GUIDE.md** | ネームスペース機能のガイド |
| **DRD_TO_TENSOR_LOGIC_GUIDE.md** | Decision Requirement DiagramからTensor Logicへの変換 |
| **VALIDATION_OUTPUT_GUIDE.md** | LLM検証出力の説明 |
| **OPENAI_API_KEY_SETUP.md** | OpenAI APIキーの設定方法 |

### 📊 **[reports/](./reports/)** - 検証・テストレポート

実験結果、検証レポート、テスト結果のサマリー。

| ファイル | 内容 |
|---------|------|
| **LLM_REASONING_VERIFICATION_REPORT.md** | LLM推論のTensor Logic化と検証の実証レポート |
| **LLM_REASONING_TEST_REPORT.md** | LLM推論Tensor Logic化機能のテストレポート（22テスト） |
| **VERIFICATION_REPORT_LOAN_APPROVAL.md** | 融資審査ルールの検証レポート |
| **FINAL_TEST_REPORT.md** | 最終テスト結果レポート |
| **TEST_RESULT_SUMMARY.md** | テスト結果サマリー |

### 🧪 **[testing/](./testing/)** - テスト関連

テストの実行方法、テストケース、検証手順。

| ファイル | 内容 |
|---------|------|
| **TEST_GUIDE.md** | テストの実行方法とガイド |
| **TEST_GENERIC_VERIFICATION.md** | 汎用LLM検証のテスト手順 |
| **TEST_INSPECTION.md** | テスト検証手順 |

### 🔧 **[implementation/](./implementation/)** - 実装詳細

システムの内部実装、アーキテクチャ、技術的詳細。

| ファイル | 内容 |
|---------|------|
| **JAVA_IMPLEMENTATION.md** | Java実装の詳細 |
| **LLM_INTEGRATION.md** | LLM統合の実装詳細 |
| **BACKWARD_CHAINING_IMPLEMENTATION_SUMMARY.md** | 後向き推論の実装サマリー |
| **NAMESPACE_IMPLEMENTATION_SUMMARY.md** | ネームスペースの実装サマリー |

### 🚀 **[deployment/](./deployment/)** - デプロイ関連

デプロイ手順、環境構築、運用ガイド。

| ファイル | 内容 |
|---------|------|
| **gdrive_push_deploy_guide.md** | Google Driveへのデプロイガイド |

### 📝 **[misc/](./misc/)** - その他

その他の参考資料、補足説明。

| ファイル | 内容 |
|---------|------|
| **DEMO_MODE_EXPLANATION.md** | デモモードの説明 |

---

## 🚀 **はじめに**

### **初めての方**
1. まず **[guides/QUICKSTART.md](./guides/QUICKSTART.md)** でシステムの概要を把握
2. **[guides/TENSOR_LOGIC_ENGINE_GUIDE.md](./guides/TENSOR_LOGIC_ENGINE_GUIDE.md)** でコア機能を理解
3. **[guides/YAML_RULE_CREATION_GUIDE.md](./guides/YAML_RULE_CREATION_GUIDE.md)** でルール作成方法を学習

### **実装者の方**
1. **[implementation/JAVA_IMPLEMENTATION.md](./implementation/JAVA_IMPLEMENTATION.md)** で実装詳細を確認
2. **[testing/TEST_GUIDE.md](./testing/TEST_GUIDE.md)** でテスト方法を理解
3. **[guides/RULE_AUTO_LOAD_GUIDE.md](./guides/RULE_AUTO_LOAD_GUIDE.md)** でルール管理を学習

### **研究者の方**
1. **[reports/LLM_REASONING_VERIFICATION_REPORT.md](./reports/LLM_REASONING_VERIFICATION_REPORT.md)** で実証結果を確認
2. **[guides/LLM_TO_TENSOR_LOGIC_GUIDE.md](./guides/LLM_TO_TENSOR_LOGIC_GUIDE.md)** で理論的背景を理解
3. **[reports/VERIFICATION_REPORT_LOAN_APPROVAL.md](./reports/VERIFICATION_REPORT_LOAN_APPROVAL.md)** で応用例を参照

---

## 🔍 **ドキュメント検索ガイド**

### **機能別**

| やりたいこと | 参照ドキュメント |
|-------------|-----------------|
| システムを起動したい | guides/QUICKSTART.md |
| ルールを作成したい | guides/YAML_RULE_CREATION_GUIDE.md |
| LLMと連携したい | guides/LLM_TO_TENSOR_LOGIC_GUIDE.md |
| 後向き推論を使いたい | guides/BACKWARD_CHAINING_GUIDE.md |
| テストを実行したい | testing/TEST_GUIDE.md |
| OpenAI APIを設定したい | guides/OPENAI_API_KEY_SETUP.md |
| ネームスペースを使いたい | guides/NAMESPACE_GUIDE.md |
| ルールを自動ロードしたい | guides/RULE_AUTO_LOAD_GUIDE.md |
| 実装詳細を知りたい | implementation/JAVA_IMPLEMENTATION.md |
| DRDから変換したい | guides/DRD_TO_TENSOR_LOGIC_GUIDE.md |

### **役割別**

| 役割 | おすすめドキュメント |
|------|---------------------|
| **初心者** | guides/QUICKSTART.md → guides/YAML_RULE_CREATION_GUIDE.md |
| **開発者** | implementation/ → testing/TEST_GUIDE.md |
| **研究者** | reports/ → guides/LLM_TO_TENSOR_LOGIC_GUIDE.md |
| **運用者** | guides/RULE_AUTO_LOAD_GUIDE.md → deployment/ |
| **ビジネス** | reports/LLM_REASONING_VERIFICATION_REPORT.md |

---

## 📈 **ドキュメント統計**

- **総ドキュメント数**: 28
- **ガイド**: 14
- **レポート**: 5
- **テスト**: 3
- **実装**: 4
- **デプロイ**: 1
- **その他**: 1

---

## 🤝 **貢献**

ドキュメントの改善提案やバグ報告は、プロジェクトのIssueトラッカーまでお願いします。

---

## 📜 **ライセンス**

このドキュメントは、TensorLogicプロジェクトのライセンスに従います。

---

**最終更新**: 2025年11月7日  
**プロジェクト**: TensorLogic 1.0.0

