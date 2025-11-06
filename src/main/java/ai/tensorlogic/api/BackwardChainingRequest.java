package ai.tensorlogic.api;

/**
 * 後向き推論のリクエスト
 * 
 * @param goal 達成したい目標（事実の名前）
 * @param namespace 適用するネームスペース（nullまたは"*"の場合は全ルール）
 */
public record BackwardChainingRequest(
    String goal,
    String namespace
) {
}

