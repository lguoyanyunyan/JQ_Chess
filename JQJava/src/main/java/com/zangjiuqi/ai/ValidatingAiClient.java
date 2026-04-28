package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;

public final class ValidatingAiClient implements AiClient {
    private final AiClient primary;
    private final AiClient fallback;

    public ValidatingAiClient(AiClient primary, AiClient fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public String requestMove(BoardState state, int searchDepth, int timeoutSeconds) {
        String primaryMove = "";
        RuntimeException primaryFailure = null;
        try {
            primaryMove = primary.requestMove(state, searchDepth, timeoutSeconds);
            if (isLegal(state, primaryMove)) {
                return primaryMove;
            }
            primaryFailure = new IllegalArgumentException("Primary AI returned an illegal move: " + primaryMove);
        } catch (RuntimeException ex) {
            primaryFailure = ex;
        }

        String fallbackMove = fallback.requestMove(state, searchDepth, timeoutSeconds);
        if (isLegal(state, fallbackMove)) {
            return fallbackMove;
        }

        IllegalStateException failure = new IllegalStateException(
                "Both AI backends returned illegal moves. primary=" + primaryMove + ", fallback=" + fallbackMove
        );
        if (primaryFailure != null) {
            failure.addSuppressed(primaryFailure);
        }
        return throwFailure(failure);
    }

    @Override
    public void destroyHashtable() {
        primary.destroyHashtable();
        fallback.destroyHashtable();
    }

    private static boolean isLegal(BoardState state, String rawMove) {
        try {
            Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
            BoardState copy = state.copyForSimulation();
            copy.applyMove(move);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String throwFailure(RuntimeException failure) {
        throw failure;
    }
}
