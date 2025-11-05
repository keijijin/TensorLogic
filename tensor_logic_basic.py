"""
Tensor Logic 基本サンプル
=======================

Tensor Logicは、テンソル演算と論理推論を統一する新しいプログラミングパラダイムです。
このサンプルでは、基本的な概念を実装します。
"""

import numpy as np
from typing import Dict, List, Tuple


class TensorLogicBase:
    """
    Tensor Logicの基本クラス
    
    テンソル演算とアインシュタインの総和規約を用いて、
    論理推論を表現します。
    """
    
    def __init__(self):
        self.facts = {}  # 事実（ファクト）を保存
        self.rules = []  # 推論ルールを保存
    
    def add_fact(self, name: str, tensor: np.ndarray):
        """
        事実（ファクト）を追加
        
        Args:
            name: 事実の名前
            tensor: テンソル表現（0-1の値）
        """
        self.facts[name] = tensor
        print(f"事実 '{name}' を追加: shape={tensor.shape}")
    
    def add_rule(self, inputs: List[str], output: str, operation='einsum', einsum_str: str = None):
        """
        推論ルールを追加
        
        Args:
            inputs: 入力となる事実のリスト
            output: 出力される事実の名前
            operation: 演算の種類
            einsum_str: einsum記法の文字列
        """
        rule = {
            'inputs': inputs,
            'output': output,
            'operation': operation,
            'einsum_str': einsum_str
        }
        self.rules.append(rule)
        print(f"ルール追加: {inputs} -> {output}")
    
    def apply_rule(self, rule: Dict) -> Tuple[str, np.ndarray]:
        """
        ルールを適用して新しい事実を推論
        
        Args:
            rule: 適用するルール
            
        Returns:
            (出力名, 推論されたテンソル)
        """
        inputs = [self.facts[name] for name in rule['inputs']]
        
        if rule['operation'] == 'einsum':
            # アインシュタインの総和規約を使用
            result = np.einsum(rule['einsum_str'], *inputs)
        else:
            raise ValueError(f"Unknown operation: {rule['operation']}")
        
        return rule['output'], result
    
    def forward_chain(self):
        """
        前向き推論を実行
        すべてのルールを適用して新しい事実を導出
        """
        print("\n=== 前向き推論を開始 ===")
        new_facts = {}
        
        for rule in self.rules:
            # すべての入力が揃っている場合のみ実行
            if all(inp in self.facts for inp in rule['inputs']):
                output_name, result = self.apply_rule(rule)
                new_facts[output_name] = result
                print(f"推論: {rule['inputs']} -> {output_name}")
                print(f"結果の形状: {result.shape}")
        
        # 新しい事実を追加
        self.facts.update(new_facts)
        return new_facts
    
    def get_fact(self, name: str) -> np.ndarray:
        """事実を取得"""
        return self.facts.get(name)


def example1_simple_logic():
    """
    例1: 簡単な論理推論
    
    ルール: 「AならばB」かつ「BならばC」 => 「AならばC」
    """
    print("\n" + "="*60)
    print("例1: 簡単な論理推論（三段論法）")
    print("="*60)
    
    tl = TensorLogicBase()
    
    # 事実を定義
    # A: 「ソクラテスは人間である」を表す確率（1.0 = 確実）
    A = np.array([1.0])
    tl.add_fact('A', A)
    
    # 「人間 → 動物」の関係（確実な含意）
    A_implies_B = np.array([[1.0]])  # [人間][動物]
    tl.add_fact('A_implies_B', A_implies_B)
    
    # 「動物 → 生物」の関係（確実な含意）
    B_implies_C = np.array([[1.0]])  # [動物][生物]
    tl.add_fact('B_implies_C', B_implies_C)
    
    # ルール1: A と A→B から B を推論
    tl.add_rule(['A', 'A_implies_B'], 'B', 'einsum', 'i,ij->j')
    
    # ルール2: B と B→C から C を推論
    tl.add_rule(['B', 'B_implies_C'], 'C', 'einsum', 'i,ij->j')
    
    # 推論実行
    results = tl.forward_chain()
    
    print("\n結果:")
    print(f"B (ソクラテスは動物): {tl.get_fact('B')}")
    print(f"C (ソクラテスは生物): {tl.get_fact('C')}")


def example2_knowledge_graph():
    """
    例2: 知識グラフでの推論
    
    エンティティと関係をテンソルで表現し、推論を行います。
    """
    print("\n" + "="*60)
    print("例2: 知識グラフでの関係推論")
    print("="*60)
    
    tl = TensorLogicBase()
    
    # エンティティ: 人物（太郎、花子、次郎）
    # 関係: 友達、仕事仲間
    
    # 太郎と花子は友達（確率的な関係）
    # shape: [人1, 人2]
    is_friend = np.array([
        [0.0, 0.9, 0.3],  # 太郎 -> (太郎, 花子, 次郎)
        [0.9, 0.0, 0.8],  # 花子 -> (太郎, 花子, 次郎)
        [0.3, 0.8, 0.0]   # 次郎 -> (太郎, 花子, 次郎)
    ])
    tl.add_fact('is_friend', is_friend)
    
    # 推論ルール: 「友達の友達」関係を計算
    # A が B の友達 かつ B が C の友達 => A は C の友達候補
    tl.add_rule(['is_friend', 'is_friend'], 'friend_of_friend', 
                'einsum', 'ij,jk->ik')
    
    results = tl.forward_chain()
    
    print("\n元の友達関係:")
    print(is_friend)
    print("\n友達の友達（推論結果）:")
    print(tl.get_fact('friend_of_friend'))
    
    names = ['太郎', '花子', '次郎']
    print("\n解釈:")
    friend_of_friend = tl.get_fact('friend_of_friend')
    for i, name1 in enumerate(names):
        for j, name2 in enumerate(names):
            if i != j:
                score = friend_of_friend[i, j]
                print(f"{name1} -> {name2}: {score:.2f} (友達の友達スコア)")


def example3_fuzzy_logic():
    """
    例3: ファジィ論理
    
    確率的・ファジィな推論を行います。
    """
    print("\n" + "="*60)
    print("例3: ファジィ論理推論")
    print("="*60)
    
    tl = TensorLogicBase()
    
    # 条件: 天気の状態（晴れ、曇り、雨）
    # 各状態の確率
    weather = np.array([0.6, 0.3, 0.1])  # [晴れ, 曇り, 雨]
    tl.add_fact('weather', weather)
    
    # ルール: 天気による活動の適性
    # shape: [天気, 活動]
    # 活動: [ピクニック, 読書, 映画]
    weather_to_activity = np.array([
        [0.9, 0.3, 0.2],  # 晴れの場合の各活動の適性
        [0.5, 0.7, 0.6],  # 曇りの場合
        [0.1, 0.9, 0.9]   # 雨の場合
    ])
    tl.add_fact('weather_to_activity', weather_to_activity)
    
    # 推論: 天気から最適な活動を計算
    tl.add_rule(['weather', 'weather_to_activity'], 'recommended_activity',
                'einsum', 'i,ij->j')
    
    results = tl.forward_chain()
    
    print("\n天気の確率:")
    print(f"晴れ: {weather[0]:.1f}, 曇り: {weather[1]:.1f}, 雨: {weather[2]:.1f}")
    
    print("\n推奨活動スコア:")
    activities = ['ピクニック', '読書', '映画']
    recommended = tl.get_fact('recommended_activity')
    for i, activity in enumerate(activities):
        print(f"{activity}: {recommended[i]:.2f}")
    
    best_idx = np.argmax(recommended)
    print(f"\n最も推奨される活動: {activities[best_idx]}")


def example4_neural_symbolic():
    """
    例4: ニューラル・シンボリック統合
    
    学習可能な重みを持つルールを使用
    """
    print("\n" + "="*60)
    print("例4: ニューラル・シンボリック統合")
    print("="*60)
    
    tl = TensorLogicBase()
    
    # 入力: 動物の特徴（4匹）
    # 特徴: [毛がある, 鳴く, 飛ぶ]
    animal_features = np.array([
        [1.0, 1.0, 0.0],  # 犬
        [1.0, 1.0, 0.0],  # 猫
        [0.0, 1.0, 1.0],  # 鳥
        [0.0, 0.0, 1.0]   # 魚
    ])
    tl.add_fact('animal_features', animal_features)
    
    # 学習された重み: 特徴からカテゴリへの変換
    # カテゴリ: [哺乳類, 鳥類, 魚類]
    # この重みは通常、学習により最適化されます
    learned_weights = np.array([
        [0.5, 0.0, 0.0],  # 毛がある -> カテゴリ
        [0.3, 0.3, 0.0],  # 鳴く -> カテゴリ
        [0.0, 0.6, 0.4]   # 飛ぶ -> カテゴリ
    ])
    tl.add_fact('learned_weights', learned_weights)
    
    # 推論: 特徴からカテゴリを予測
    tl.add_rule(['animal_features', 'learned_weights'], 'predicted_categories',
                'einsum', 'ij,jk->ik')
    
    results = tl.forward_chain()
    
    print("\n動物の特徴:")
    animals = ['犬', '猫', '鳥', '魚']
    features = ['毛', '鳴く', '飛ぶ']
    for i, animal in enumerate(animals):
        feature_str = ', '.join([f for j, f in enumerate(features) if animal_features[i, j] > 0.5])
        print(f"{animal}: {feature_str}")
    
    print("\n予測されたカテゴリ:")
    categories = ['哺乳類', '鳥類', '魚類']
    predictions = tl.get_fact('predicted_categories')
    for i, animal in enumerate(animals):
        print(f"\n{animal}:")
        for j, category in enumerate(categories):
            print(f"  {category}: {predictions[i, j]:.2f}")


if __name__ == "__main__":
    print("=" * 60)
    print("Tensor Logic サンプル集")
    print("=" * 60)
    print("\nTensor Logicは、テンソル演算を用いて論理推論を実現する")
    print("新しいプログラミングパラダイムです。")
    
    # すべての例を実行
    example1_simple_logic()
    example2_knowledge_graph()
    example3_fuzzy_logic()
    example4_neural_symbolic()
    
    print("\n" + "="*60)
    print("すべてのサンプルが完了しました！")
    print("="*60)


