package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

interface AiStrategyProfile {
    int evaluate(BoardState state, PieceColor perspective);

    int moveOrderScore(BoardState state, Move move);

    int rootTacticalBonus(BoardState state, Move move);
}
