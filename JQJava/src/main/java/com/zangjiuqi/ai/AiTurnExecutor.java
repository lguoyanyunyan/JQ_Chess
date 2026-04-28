package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;

public final class AiTurnExecutor {
    private final AiClient aiClient;

    public AiTurnExecutor(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public AiTurnResult execute(BoardState state, int searchDepth, int timeoutSeconds) {
        String rawMove = "";
        try {
            rawMove = aiClient.requestMove(state, searchDepth, timeoutSeconds);
            Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
            state.applyMove(move);
            return AiTurnResult.success(rawMove);
        } catch (RuntimeException ex) {
            return AiTurnResult.failure(rawMove, ex.getMessage());
        }
    }
}
