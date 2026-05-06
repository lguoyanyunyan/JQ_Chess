package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.TraditionalWinningPattern;

import java.util.List;

final class TraditionalAiProfile implements AiStrategyProfile {
    private final AiEvaluator evaluator;
    private final List<TraditionalPatternHeuristic> patternHeuristics;

    TraditionalAiProfile(AiEvaluator evaluator, List<TraditionalPatternHeuristic> patternHeuristics) {
        this.evaluator = evaluator;
        this.patternHeuristics = List.copyOf(patternHeuristics);
    }

    static TraditionalAiProfile withDefaultHeuristics(AiEvaluator evaluator) {
        return new TraditionalAiProfile(evaluator, TraditionalPatternCatalog.defaultHeuristics());
    }

    @Override
    public int evaluate(BoardState state, PieceColor perspective) {
        List<TraditionalPatternHeuristic> activeHeuristics = activeHeuristics(state);
        if (state.ruleConfig().traditionalWinningPattern() == TraditionalWinningPattern.OFF) {
            return evaluator.evaluateTraditionalBase(state, perspective);
        }

        PieceColor opponent = perspective.opponent();
        int opponentUrgency = AiScoringSupport.flyUrgency(state, opponent);
        int ownUrgency = AiScoringSupport.flyUrgency(state, perspective);
        int score = evaluator.evaluateTraditionalBase(state, perspective);
        score += formationStructureScore(state, perspective, activeHeuristics) * (2 + opponentUrgency);
        score -= formationStructureScore(state, opponent, activeHeuristics) * (1 + ownUrgency);
        score += gateScore(state, perspective, opponentUrgency);
        score -= gateScore(state, opponent, ownUrgency);
        return score;
    }

    @Override
    public int moveOrderScore(BoardState state, Move move) {
        int score = move.squareCaptures().size() * 4_000 + move.jumpCaptures().size() * 2_500;
        score += Math.max(0, move.path().size() - 1) * 80;
        for (BoardPoint capture : move.squareCaptures()) {
            score += AiScoringSupport.captureTargetScore(state, capture);
        }
        score += AiScoringSupport.formationBonus(state, move, 14_000, 5_000);
        score += patternMoveScore(state, move);
        score += gateMoveScore(state, move);
        return score;
    }

    @Override
    public int rootTacticalBonus(BoardState state, Move move) {
        int score = move.squareCaptures().size() * 500
                + move.jumpCaptures().size() * 350
                + Math.max(0, move.path().size() - 1) * 8;
        score += AiScoringSupport.formationBonus(state, move, 7_000, 2_500);
        score += patternMoveScore(state, move) / 4;
        score += gateMoveScore(state, move) / 5;
        return score;
    }

    private int formationStructureScore(
            BoardState state,
            PieceColor color,
            List<TraditionalPatternHeuristic> activeHeuristics
    ) {
        int best = 0;
        for (TraditionalPatternHeuristic heuristic : activeHeuristics) {
            best = Math.max(best, heuristic.boardScore(state, color));
        }
        return best;
    }

    private int patternMoveScore(BoardState state, Move move) {
        if (state.ruleConfig().traditionalWinningPattern() == TraditionalWinningPattern.OFF) {
            return 0;
        }
        PieceColor color = movingColor(state, move);
        if (color == null) {
            return 0;
        }
        List<TraditionalPatternHeuristic> activeHeuristics = activeHeuristics(state);
        int best = 0;
        for (TraditionalPatternHeuristic heuristic : activeHeuristics) {
            best = Math.max(best, heuristic.moveScore(state, move, color));
        }
        int urgency = AiScoringSupport.flyUrgency(state, color.opponent());
        return Math.max(0, best) * (2 + urgency);
    }

    private List<TraditionalPatternHeuristic> activeHeuristics(BoardState state) {
        if (!patternHeuristics.isEmpty()) {
            return patternHeuristics;
        }
        return TraditionalPatternCatalog.heuristicsFor(state.ruleConfig().traditionalWinningPattern());
    }

    private int gateMoveScore(BoardState state, Move move) {
        PieceColor color = movingColor(state, move);
        if (color == null) {
            return 0;
        }
        PieceColor opponent = color.opponent();
        int before = AiScoringSupport.squareGateCount(state, opponent);
        BoardState next = AiScoringSupport.simulate(state, move);
        if (next == null) {
            return 0;
        }
        if (next.gameResult().finished()) {
            return next.gameResult().winner()
                    .map(winner -> winner == color ? 200_000 : -200_000)
                    .orElse(0);
        }
        int after = AiScoringSupport.squareGateCount(next, opponent);
        int urgency = AiScoringSupport.flyUrgency(state, opponent);
        int reduction = Math.max(0, before - after);
        int score = reduction * 16_000 * (1 + urgency);
        if (before > 0 && after == 0) {
            score += 45_000 * (1 + urgency);
        }
        if (after > 0 && AiScoringSupport.pieceCount(next, opponent) <= state.ruleConfig().flyPieceThreshold()) {
            score -= 80_000;
        }
        return score;
    }

    private int gateScore(BoardState state, PieceColor color, int opponentUrgency) {
        PieceColor opponent = color.opponent();
        int ownGates = AiScoringSupport.squareGateCount(state, color);
        int opponentGates = AiScoringSupport.squareGateCount(state, opponent);
        int score = ownGates * 120;
        score -= opponentGates * 420 * (1 + opponentUrgency);
        if (opponentGates == 0 && AiScoringSupport.pieceCount(state, color) > AiScoringSupport.pieceCount(state, opponent)) {
            score += 3_000 * (1 + opponentUrgency);
        }
        return score;
    }

    private PieceColor movingColor(BoardState state, Move move) {
        if (move.isPlacement()) {
            return state.currentTurnColor();
        }
        BoardPoint from = move.path().get(0);
        int piece = state.get(from);
        if (piece <= 0) {
            return null;
        }
        return PieceColor.fromPieceValue(piece);
    }
}
