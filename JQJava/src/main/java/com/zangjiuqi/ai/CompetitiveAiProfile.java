package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

final class CompetitiveAiProfile implements AiStrategyProfile {
    private final AiEvaluator evaluator;

    CompetitiveAiProfile(AiEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public int evaluate(BoardState state, PieceColor perspective) {
        return evaluator.evaluateCompetitive(state, perspective);
    }

    @Override
    public int moveOrderScore(BoardState state, Move move) {
        int score = move.squareCaptures().size() * 8_000 + move.jumpCaptures().size() * 5_000;
        score += Math.max(0, move.path().size() - 1) * 100;
        for (BoardPoint capture : move.squareCaptures()) {
            score += AiScoringSupport.captureTargetScore(state, capture);
        }
        score += centerMoveScore(state, move);
        score += AiScoringSupport.formationBonus(state, move, 12_000, 4_000);
        return score;
    }

    @Override
    public int rootTacticalBonus(BoardState state, Move move) {
        int score = move.squareCaptures().size() * 900
                + move.jumpCaptures().size() * 700
                + Math.max(0, move.path().size() - 1) * 10;
        return score + AiScoringSupport.formationBonus(state, move, 6_000, 2_000);
    }

    private int centerMoveScore(BoardState state, Move move) {
        BoardPoint target = move.path().get(move.path().size() - 1);
        int centerA = state.ruleConfig().centerPointA();
        int centerB = state.ruleConfig().centerPointB();
        int distanceA = Math.abs(target.fileIndex() - centerA) + Math.abs(target.rankIndex() - centerA);
        int distanceB = Math.abs(target.fileIndex() - centerB) + Math.abs(target.rankIndex() - centerB);
        return -Math.min(distanceA, distanceB);
    }
}
