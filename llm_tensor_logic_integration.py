"""
LLMとTensor Logicの連携サンプル
====================================

LLMの推論過程をTensor Logicで構造化・検証するシステムの実装例

主要なアプローチ:
1. LLMの出力を構造化データに変換
2. Tensor Logicで論理的な検証を実行
3. ハイブリッド推論（LLMの直感 + Tensor Logicの論理）
"""

import numpy as np
from typing import Dict, List, Tuple, Optional
import json
import re


class LLMTensorLogicBridge:
    """
    LLMとTensor Logicを橋渡しするクラス
    
    LLMの出力をテンソルに変換し、論理的な推論を行います。
    """
    
    def __init__(self):
        self.facts = {}
        self.rules = []
        self.reasoning_trace = []  # 推論過程の記録
    
    def parse_llm_output_to_tensor(self, llm_output: str, entity_map: Dict[str, int]) -> np.ndarray:
        """
        LLMの出力をテンソルに変換
        
        Args:
            llm_output: LLMからのテキスト出力
            entity_map: エンティティ名とインデックスのマッピング
            
        Returns:
            テンソル表現
        """
        # 確率や確信度を抽出（例: "確率80%"、"0.8の確信度"など）
        confidence_patterns = [
            r'(\d+)%',
            r'確率[:\s]*(\d+)',
            r'(\d*\.\d+)',
        ]
        
        confidence = 0.5  # デフォルト
        for pattern in confidence_patterns:
            match = re.search(pattern, llm_output)
            if match:
                value = float(match.group(1))
                confidence = value / 100 if value > 1 else value
                break
        
        return confidence
    
    def extract_reasoning_steps(self, llm_chain_of_thought: str) -> List[Dict]:
        """
        Chain-of-Thought形式の推論ステップを抽出
        
        Args:
            llm_chain_of_thought: LLMのステップバイステップの推論
            
        Returns:
            推論ステップのリスト
        """
        steps = []
        
        # ステップを分割（番号付きリストを想定）
        step_pattern = r'(\d+)\.\s*(.+?)(?=\d+\.|$)'
        matches = re.findall(step_pattern, llm_chain_of_thought, re.DOTALL)
        
        for num, content in matches:
            steps.append({
                'step_number': int(num),
                'content': content.strip(),
                'validated': False
            })
        
        return steps
    
    def validate_reasoning_with_tensor_logic(self, 
                                            premise1: np.ndarray,
                                            premise2: np.ndarray,
                                            conclusion: np.ndarray,
                                            logic_type: str = 'modus_ponens') -> Dict:
        """
        Tensor Logicを使って推論の妥当性を検証
        
        Args:
            premise1: 前提1のテンソル
            premise2: 前提2のテンソル（含意関係など）
            conclusion: 結論のテンソル
            logic_type: 論理タイプ
            
        Returns:
            検証結果
        """
        if logic_type == 'modus_ponens':
            # A かつ (A→B) から B を導出
            expected_conclusion = np.einsum('i,ij->j', premise1, premise2)
        elif logic_type == 'conjunction':
            # A と B から (A∧B) を導出
            expected_conclusion = np.minimum(premise1, premise2)
        else:
            expected_conclusion = conclusion
        
        # 結論との差異を計算
        error = np.abs(expected_conclusion - conclusion)
        is_valid = np.all(error < 0.2)  # 許容誤差20%
        
        return {
            'is_valid': bool(is_valid),
            'expected': expected_conclusion,
            'actual': conclusion,
            'error': error,
            'confidence': float(1.0 - np.mean(error))
        }


class HybridReasoningSystem:
    """
    LLMとTensor Logicを組み合わせたハイブリッド推論システム
    
    LLMの柔軟性とTensor Logicの厳密性を両立させます。
    """
    
    def __init__(self):
        self.bridge = LLMTensorLogicBridge()
        self.knowledge_base = {}
        
    def add_knowledge(self, name: str, tensor: np.ndarray, description: str = ""):
        """知識ベースに情報を追加"""
        self.knowledge_base[name] = {
            'tensor': tensor,
            'description': description
        }
    
    def simulate_llm_reasoning(self, query: str) -> Dict:
        """
        LLMの推論をシミュレート
        
        実際のLLM API（OpenAI、Anthropicなど）と連携する場合は、
        ここで実際のAPI呼び出しを行います。
        """
        # このサンプルではシミュレーションを行います
        simulated_responses = {
            "ソクラテスは何か？": {
                "answer": "ソクラテスは人間です。",
                "confidence": 0.95,
                "reasoning": "1. ソクラテスは古代ギリシャの哲学者です。\n"
                           "2. 哲学者は人間です。\n"
                           "3. したがって、ソクラテスは人間です。"
            },
            "人間は死ぬのか？": {
                "answer": "はい、すべての人間は死にます。",
                "confidence": 0.98,
                "reasoning": "1. 人間は生物学的存在です。\n"
                           "2. すべての生物学的存在は死を迎えます。\n"
                           "3. したがって、人間は死にます。"
            },
            "ソクラテスは死ぬのか？": {
                "answer": "はい、ソクラテスは死にます。",
                "confidence": 0.90,
                "reasoning": "1. ソクラテスは人間です。\n"
                           "2. すべての人間は死にます。\n"
                           "3. したがって、ソクラテスは死にます。"
            }
        }
        
        return simulated_responses.get(query, {
            "answer": "わかりません。",
            "confidence": 0.1,
            "reasoning": "十分な情報がありません。"
        })
    
    def verify_llm_reasoning(self, llm_response: Dict) -> Dict:
        """
        LLMの推論をTensor Logicで検証
        
        Args:
            llm_response: LLMからの応答
            
        Returns:
            検証結果
        """
        # 推論ステップを抽出
        steps = self.bridge.extract_reasoning_steps(llm_response['reasoning'])
        
        print(f"\n推論ステップ数: {len(steps)}")
        for step in steps:
            print(f"  ステップ{step['step_number']}: {step['content'][:50]}...")
        
        # 簡単な例: 三段論法の検証
        # "ソクラテスは人間" → "人間は死ぬ" → "ソクラテスは死ぬ"
        
        # テンソル表現に変換
        socrates_is_human = np.array([llm_response['confidence']])
        human_is_mortal = np.array([[0.98]])  # 人間→死ぬ の確実性
        
        # Tensor Logicで検証
        validation = self.bridge.validate_reasoning_with_tensor_logic(
            socrates_is_human,
            human_is_mortal,
            np.array([llm_response['confidence']]),
            logic_type='modus_ponens'
        )
        
        return {
            'llm_answer': llm_response['answer'],
            'llm_confidence': llm_response['confidence'],
            'logic_validation': validation,
            'steps': steps,
            'is_logically_sound': validation['is_valid']
        }


def example1_llm_output_verification():
    """
    例1: LLMの出力を検証
    
    LLMが生成した推論をTensor Logicで検証します。
    """
    print("\n" + "="*70)
    print("例1: LLMの推論をTensor Logicで検証")
    print("="*70)
    
    system = HybridReasoningSystem()
    
    # LLMに質問（シミュレーション）
    query = "ソクラテスは死ぬのか？"
    print(f"\n質問: {query}")
    
    # LLMの応答を取得
    llm_response = system.simulate_llm_reasoning(query)
    print(f"\nLLMの回答: {llm_response['answer']}")
    print(f"LLMの確信度: {llm_response['confidence']:.2f}")
    print(f"\nLLMの推論過程:\n{llm_response['reasoning']}")
    
    # Tensor Logicで検証
    verification = system.verify_llm_reasoning(llm_response)
    
    print("\n" + "-"*70)
    print("Tensor Logicによる検証結果:")
    print("-"*70)
    print(f"論理的に健全: {'✓ はい' if verification['is_logically_sound'] else '✗ いいえ'}")
    print(f"検証信頼度: {verification['logic_validation']['confidence']:.2%}")
    print(f"期待される結論: {verification['logic_validation']['expected']}")
    print(f"実際のLLM出力: {verification['logic_validation']['actual']}")
    print(f"誤差: {verification['logic_validation']['error']}")


def example2_structured_reasoning():
    """
    例2: 構造化された推論
    
    LLMの自然言語推論をテンソルの推論グラフに変換します。
    """
    print("\n" + "="*70)
    print("例2: LLMの推論を構造化されたTensor Logicグラフに変換")
    print("="*70)
    
    # 推論グラフの構築
    print("\n知識ベースの構築:")
    
    # エンティティ: [ソクラテス, プラトン, アリストテレス]
    # 属性: [人間, 哲学者, ギリシャ人]
    entity_attributes = np.array([
        [1.0, 1.0, 1.0],  # ソクラテス
        [1.0, 1.0, 1.0],  # プラトン
        [1.0, 1.0, 1.0],  # アリストテレス
    ])
    
    print("  エンティティの属性行列:")
    entities = ['ソクラテス', 'プラトン', 'アリストテレス']
    attributes = ['人間', '哲学者', 'ギリシャ人']
    for i, entity in enumerate(entities):
        attrs = [attributes[j] for j in range(len(attributes)) if entity_attributes[i, j] > 0.5]
        print(f"    {entity}: {', '.join(attrs)}")
    
    # ルール: 属性から結論へ
    # [人間, 哲学者, ギリシャ人] -> [死ぬ, 賢い, 影響力がある]
    attribute_to_conclusion = np.array([
        [0.98, 0.3, 0.2],   # 人間 -> (死ぬ, 賢い, 影響力)
        [0.1, 0.9, 0.7],    # 哲学者 -> (死ぬ, 賢い, 影響力)
        [0.0, 0.2, 0.3],    # ギリシャ人 -> (死ぬ, 賢い, 影響力)
    ])
    
    # 推論実行
    conclusions = np.einsum('ij,jk->ik', entity_attributes, attribute_to_conclusion)
    
    print("\n推論結果:")
    conclusion_labels = ['死ぬ', '賢い', '影響力がある']
    for i, entity in enumerate(entities):
        print(f"\n  {entity}:")
        for j, label in enumerate(conclusion_labels):
            print(f"    {label}: {conclusions[i, j]:.2f}")


def example3_confidence_propagation():
    """
    例3: 確信度の伝播
    
    LLMの不確実性をTensor Logicで伝播させます。
    """
    print("\n" + "="*70)
    print("例3: LLMの確信度をTensor Logicで伝播")
    print("="*70)
    
    # LLMからの複数の事実（確信度付き）
    print("\nLLMが抽出した事実:")
    
    facts = {
        "太郎は東京に住んでいる": 0.9,
        "東京は日本にある": 0.99,
        "日本はアジアにある": 0.95
    }
    
    for fact, confidence in facts.items():
        print(f"  {fact} (確信度: {confidence:.0%})")
    
    # テンソル表現
    # [太郎] -> [東京] -> [日本] -> [アジア]
    taro_in_tokyo = np.array([0.9])
    tokyo_in_japan = np.array([[0.99]])
    japan_in_asia = np.array([[0.95]])
    
    # 推論の連鎖
    print("\n推論の連鎖:")
    
    # 太郎 -> 日本
    taro_in_japan = np.einsum('i,ij->j', taro_in_tokyo, tokyo_in_japan)
    print(f"  太郎は日本に住んでいる: {taro_in_japan[0]:.2%}")
    
    # 太郎 -> アジア
    taro_in_asia = np.einsum('i,ij->j', taro_in_japan, japan_in_asia)
    print(f"  太郎はアジアに住んでいる: {taro_in_asia[0]:.2%}")
    
    print("\n確信度の伝播:")
    print(f"  初期確信度: {facts['太郎は東京に住んでいる']:.0%}")
    print(f"  1ステップ後: {taro_in_japan[0]:.0%}")
    print(f"  2ステップ後: {taro_in_asia[0]:.0%}")
    print(f"  累積的な不確実性: {(1 - taro_in_asia[0]):.0%}")


def example4_contradiction_detection():
    """
    例4: 矛盾の検出
    
    LLMが生成した複数の主張の論理的矛盾を検出します。
    """
    print("\n" + "="*70)
    print("例4: LLMの出力から論理的矛盾を検出")
    print("="*70)
    
    print("\nLLMが生成した主張:")
    
    # LLMからの主張（シミュレーション）
    claims = [
        ("AはBより大きい", np.array([1.0])),
        ("BはCより大きい", np.array([1.0])),
        ("CはAより大きい", np.array([0.8])),  # 矛盾！
    ]
    
    for claim, confidence in claims:
        print(f"  {claim} (確信度: {confidence[0]:.0%})")
    
    # 推移律による検証
    # A > B かつ B > C => A > C であるべき
    A_gt_B = claims[0][1][0]
    B_gt_C = claims[1][1][0]
    C_gt_A = claims[2][1][0]
    
    # 論理積で推論
    A_gt_C_expected = min(A_gt_B, B_gt_C)  # 推移律から期待される値
    
    print("\n論理的検証:")
    print(f"  A > B かつ B > C から推論: A > C であるべき (確信度: {A_gt_C_expected:.0%})")
    print(f"  しかし、LLMは「C > A」と主張 (確信度: {C_gt_A:.0%})")
    
    # 矛盾の度合い
    contradiction_score = min(A_gt_C_expected, C_gt_A)
    print(f"\n矛盾スコア: {contradiction_score:.2f}")
    
    if contradiction_score > 0.3:
        print("  ⚠️  論理的矛盾を検出しました！")
        print("  推奨: LLMに再度質問するか、事実を確認してください。")
    else:
        print("  ✓ 論理的に一貫しています。")


def example5_neural_symbolic_reasoning():
    """
    例5: ニューラル・シンボリック推論
    
    LLMの埋め込みベクトルとTensor Logicを組み合わせます。
    """
    print("\n" + "="*70)
    print("例5: LLMの埋め込みベクトルとTensor Logicの統合")
    print("="*70)
    
    print("\n概念:")
    print("  LLMが生成する意味的埋め込みベクトルと")
    print("  Tensor Logicの論理演算を組み合わせることで、")
    print("  意味的類似性と論理的整合性の両方を考慮した推論が可能になります。")
    
    # シミュレーション: LLMの埋め込みベクトル（次元削減版）
    # 実際にはOpenAI Embeddingsなどを使用
    embeddings = {
        '犬': np.array([0.8, 0.2, 0.9, 0.1]),      # 動物、哺乳類の特徴
        '猫': np.array([0.7, 0.3, 0.85, 0.15]),    # 類似
        '魚': np.array([0.6, 0.1, 0.2, 0.8]),      # 異なる
        'ペット': np.array([0.75, 0.25, 0.8, 0.2]), # 抽象概念
    }
    
    print("\n意味的埋め込み（シミュレーション）:")
    for concept, vec in embeddings.items():
        print(f"  {concept}: {vec}")
    
    # 意味的類似度の計算
    print("\nコサイン類似度:")
    
    def cosine_similarity(v1, v2):
        return np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))
    
    dog_cat_sim = cosine_similarity(embeddings['犬'], embeddings['猫'])
    dog_fish_sim = cosine_similarity(embeddings['犬'], embeddings['魚'])
    dog_pet_sim = cosine_similarity(embeddings['犬'], embeddings['ペット'])
    
    print(f"  犬 ↔ 猫: {dog_cat_sim:.3f}")
    print(f"  犬 ↔ 魚: {dog_fish_sim:.3f}")
    print(f"  犬 ↔ ペット: {dog_pet_sim:.3f}")
    
    # 論理ルール: "犬" is_a "ペット"
    is_a_relation = np.array([[dog_pet_sim]])
    
    print("\n論理的推論:")
    print(f"  「犬はペットである」の確信度: {is_a_relation[0, 0]:.2%}")
    
    # ハイブリッド推論
    print("\nハイブリッド推論の利点:")
    print("  ✓ 意味的な柔軟性（LLMの強み）")
    print("  ✓ 論理的な厳密性（Tensor Logicの強み）")
    print("  ✓ 説明可能性（推論過程の追跡）")


def example6_realtime_fact_checking():
    """
    例6: リアルタイム事実検証
    
    LLMの生成テキストをリアルタイムで検証します。
    """
    print("\n" + "="*70)
    print("例6: LLMの生成をリアルタイムで事実検証")
    print("="*70)
    
    # 知識ベース（検証済みの事実）
    verified_facts = {
        '地球の公転周期': 365.25,
        '光の速度(km/s)': 299792.458,
        '日本の首都': '東京',
    }
    
    print("\n検証済み知識ベース:")
    for fact, value in verified_facts.items():
        print(f"  {fact}: {value}")
    
    # LLMが生成した文章（シミュレーション）
    llm_generated_text = """
    地球は太陽の周りを約365日で公転します。
    光の速度は秒速約30万キロメートルです。
    """
    
    print(f"\nLLMが生成したテキスト:")
    print(f'  "{llm_generated_text.strip()}"')
    
    # 数値の抽出と検証
    print("\n事実検証:")
    
    # 365日の検証
    expected = verified_facts['地球の公転周期']
    extracted = 365.0
    error = abs(expected - extracted) / expected
    
    print(f"  地球の公転周期:")
    print(f"    LLMの主張: {extracted}日")
    print(f"    正確な値: {expected}日")
    print(f"    誤差: {error:.1%}")
    print(f"    判定: {'✓ 許容範囲' if error < 0.01 else '⚠️ 要確認'}")
    
    # 光速の検証
    expected = verified_facts['光の速度(km/s)']
    extracted = 300000.0
    error = abs(expected - extracted) / expected
    
    print(f"\n  光の速度:")
    print(f"    LLMの主張: {extracted:.0f} km/s")
    print(f"    正確な値: {expected:.3f} km/s")
    print(f"    誤差: {error:.2%}")
    print(f"    判定: {'✓ 許容範囲' if error < 0.01 else '⚠️ 要確認'}")


if __name__ == "__main__":
    print("="*70)
    print("LLMとTensor Logic統合サンプル")
    print("="*70)
    print("\nLLMの柔軟な推論とTensor Logicの論理的厳密性を組み合わせた")
    print("ハイブリッドAIシステムの実装例です。")
    
    # すべての例を実行
    example1_llm_output_verification()
    example2_structured_reasoning()
    example3_confidence_propagation()
    example4_contradiction_detection()
    example5_neural_symbolic_reasoning()
    example6_realtime_fact_checking()
    
    print("\n" + "="*70)
    print("まとめ: LLM × Tensor Logicの可能性")
    print("="*70)
    print("""
LLMとTensor Logicの統合により、以下が実現可能です:

1. 推論の検証: LLMの出力を論理的に検証
2. 構造化: 自然言語の推論をテンソルグラフに変換
3. 確信度の伝播: 不確実性を数学的に扱う
4. 矛盾検出: 論理的な矛盾を自動検出
5. ハイブリッド推論: 意味理解と論理推論の融合
6. リアルタイム検証: 事実に基づいた検証

これにより、より信頼性が高く説明可能なAIシステムが構築できます。
    """)
    
    print("\n次のステップ:")
    print("  - 実際のLLM API（OpenAI、Anthropicなど）との連携")
    print("  - 大規模知識グラフとの統合")
    print("  - リアルタイムストリーム処理")
    print("  - マルチモーダル推論（テキスト + 画像）")
    print("="*70)

