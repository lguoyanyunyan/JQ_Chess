package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;

public final class JavaAiClient implements AiClient {
    private final JavaAiEngine engine = new JavaAiEngine();

    @Override
    public String requestMove(BoardState state, int searchDepth, int timeoutSeconds) {
        if (state.phase() != BoardPhase.EMBATTLE && state.phase() != BoardPhase.MOVE) {
            throw new IllegalStateException("Java AI can only move during embattle or move phase.");
        }
        SearchResult result = engine.search(state.copyForSimulation(), new SearchConfig(searchDepth, timeoutSeconds));
        Move move = result.bestMove();
        BoardState validation = state.copyForSimulation();
        validation.applyMove(move);
        return AiMoveFormatter.toAiNotation(move);
    }

    @Override
    public void destroyHashtable() {
        engine.clearCache();
    }
}
