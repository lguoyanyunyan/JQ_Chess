package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.GameResult;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

import java.util.List;

final class AiEvaluator {
    private static final int MAX_OPPORTUNITY_MOVES = 96;

    private final AiMoveGenerator generator;

    AiEvaluator(AiMoveGenerator generator) {
        this.generator = generator;
    }

    int evaluateCompetitive(BoardState state, PieceColor perspective) {
        GameResult result = state.gameResult();
        if (result.finished()) {
            return result.winner()
                    .map(winner -> winner == perspective ? 1_000_000 : -1_000_000)
                    .orElse(0);
        }

        PieceColor opponent = perspective.opponent();
        int material = (AiScoringSupport.pieceCount(state, perspective) - AiScoringSupport.pieceCount(state, opponent)) * 500;
        int mobility = mobilityScore(state, perspective) - mobilityScore(state, opponent);
        int squarePotential = AiScoringSupport.squarePotential(state, perspective) - AiScoringSupport.squarePotential(state, opponent);
        int opportunity = state.ruleConfig().boardSize() <= 8
                ? immediateOpportunityScore(state, perspective) - immediateOpportunityScore(state, opponent)
                : 0;
        int center = AiScoringSupport.centerScore(state, perspective) - AiScoringSupport.centerScore(state, opponent);
        return material + mobility * 2 + squarePotential * 20 + opportunity + center;
    }

    int evaluateTraditionalBase(BoardState state, PieceColor perspective) {
        GameResult result = state.gameResult();
        if (result.finished()) {
            return result.winner()
                    .map(winner -> winner == perspective ? 1_000_000 : -1_000_000)
                    .orElse(0);
        }

        PieceColor opponent = perspective.opponent();
        int material = (AiScoringSupport.pieceCount(state, perspective) - AiScoringSupport.pieceCount(state, opponent)) * 220;
        int mobility = mobilityScore(state, perspective) - mobilityScore(state, opponent);
        int squarePotential = AiScoringSupport.squarePotential(state, perspective) - AiScoringSupport.squarePotential(state, opponent);
        int center = AiScoringSupport.centerScore(state, perspective) - AiScoringSupport.centerScore(state, opponent);
        return material + mobility + squarePotential * 12 + center;
    }

    private int mobilityScore(BoardState state, PieceColor color) {
        BoardState moveState = stateForColor(state, color);
        if (moveState == null) {
            return 0;
        }
        return generator.generateLegalMoves(moveState).size();
    }

    int immediateOpportunityScore(BoardState state, PieceColor color) {
        BoardState moveState = stateForColor(state, color);
        if (moveState == null) {
            return 0;
        }

        List<Move> moves = generator.generateLegalMoves(moveState);
        int best = 0;
        int inspected = 0;
        for (Move move : moves) {
            inspected++;
            if (inspected > MAX_OPPORTUNITY_MOVES) {
                break;
            }
            int score = move.jumpCaptures().size() * 80 + move.squareCaptures().size() * 140;
            BoardState probe = moveState.copyForSimulation();
            try {
                probe.applyMove(move);
            } catch (RuntimeException ignored) {
                continue;
            }
            if (probe.gameResult().finished()) {
                score += 20_000;
            }
            score += probe.lastFormationMatch()
                    .map(match -> 500 + match.captureCount() * 300 + AiScoringSupport.formationNameBonus(match.name()) / 20)
                    .orElse(0);
            if (probe.phase() == BoardPhase.SQUARE_CAPTURE) {
                score += probe.pendingCaptureCount() * 150;
            }
            best = Math.max(best, score);
        }
        return best;
    }

    private BoardState stateForColor(BoardState state, PieceColor color) {
        if (state.gameResult().finished()) {
            return null;
        }
        if (state.phase() == BoardPhase.EMBATTLE) {
            return state.currentTurnColor() == color ? state : null;
        }
        if (state.currentTurnColor() == color) {
            return state;
        }
        BoardState copy = state.copyForSimulation();
        try {
            copy.enterMovePhase(color);
            return copy;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

}
