package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

interface TraditionalPatternHeuristic {
    int boardScore(BoardState state, PieceColor color);

    default int moveScore(BoardState state, Move move, PieceColor color) {
        BoardState next = AiScoringSupport.simulate(state, move);
        if (next == null) {
            return 0;
        }
        return boardScore(next, color) - boardScore(state, color);
    }
}
