package ai.tensorlogic.api;

/**
 * 後向き推論のリクエスト
 * 
 * @param goal 達成したい目標（事実の名前）
 */
public record BackwardChainingRequest(
    String goal
) {
}

