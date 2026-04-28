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

    int evaluate(BoardState state, PieceColor perspective) {
        GameResult result = state.gameResult();
        if (result.finished()) {
            return result.winner()
                    .map(winner -> winner == perspective ? 1_000_000 : -1_000_000)
                    .orElse(0);
        }

        PieceColor opponent = perspective.opponent();
        int material = (pieceCount(state, perspective) - pieceCount(state, opponent)) * 500;
        int mobility = mobilityScore(state, perspective) - mobilityScore(state, opponent);
        int squarePotential = squarePotential(state, perspective) - squarePotential(state, opponent);
        int opportunity = state.ruleConfig().boardSize() <= 8
                ? immediateOpportunityScore(state, perspective) - immediateOpportunityScore(state, opponent)
                : 0;
        int center = centerScore(state, perspective) - centerScore(state, opponent);
        return material + mobility * 2 + squarePotential * 20 + opportunity + center;
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
                    .map(match -> 500 + match.captureCount() * 300 + formationNameBonus(match.name()))
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

    private int pieceCount(BoardState state, PieceColor color) {
        int count = 0;
        int[][] cells = state.snapshot();
        for (int file = 0; file < cells.length; file++) {
            for (int rank = 0; rank < cells[file].length; rank++) {
                int value = cells[file][rank];
                if (value > 0 && PieceColor.fromPieceValue(value) == color) {
                    count++;
                }
            }
        }
        return count;
    }

    private int squarePotential(BoardState state, PieceColor color) {
        int score = 0;
        int size = state.ruleConfig().boardSize();
        int[][] cells = state.snapshot();
        for (int file = 0; file < size - 1; file++) {
            for (int rank = 0; rank < size - 1; rank++) {
                int own = 0;
                int empty = 0;
                BoardPoint[] square = {
                        new BoardPoint(file, rank),
                        new BoardPoint(file + 1, rank),
                        new BoardPoint(file, rank + 1),
                        new BoardPoint(file + 1, rank + 1)
                };
                for (BoardPoint point : square) {
                    int value = cells[point.fileIndex()][point.rankIndex()];
                    if (value == 0) {
                        empty++;
                    } else if (PieceColor.fromPieceValue(value) == color) {
                        own++;
                    }
                }
                if (own == 3 && empty == 1) {
                    score += 4;
                } else if (own == 2 && empty == 2) {
                    score += 1;
                }
            }
        }
        return score;
    }

    private int centerScore(BoardState state, PieceColor color) {
        int score = 0;
        int size = state.ruleConfig().boardSize();
        double center = (size - 1) / 2.0;
        int[][] cells = state.snapshot();
        for (int file = 0; file < size; file++) {
            for (int rank = 0; rank < size; rank++) {
                int value = cells[file][rank];
                if (value > 0 && PieceColor.fromPieceValue(value) == color) {
                    double distance = Math.abs(file - center) + Math.abs(rank - center);
                    score += Math.max(0, size - (int) Math.round(distance));
                }
            }
        }
        return score;
    }

    private int formationNameBonus(String name) {
        if (name.contains("拉萨")) {
            return 350;
        }
        if (name.contains("双门")) {
            return 220;
        }
        return 120;
    }
}
