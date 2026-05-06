package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.FormationPatternDetector;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

import java.util.Optional;

final class AiScoringSupport {
    private AiScoringSupport() {
    }

    static int pieceCount(BoardState state, PieceColor color) {
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

    static int squarePotential(BoardState state, PieceColor color) {
        int score = 0;
        int size = state.ruleConfig().boardSize();
        int[][] cells = state.snapshot();
        for (int file = 0; file < size - 1; file++) {
            for (int rank = 0; rank < size - 1; rank++) {
                SquareCounts counts = squareCounts(cells, color, file, rank);
                if (counts.own() == 3 && counts.empty() == 1) {
                    score += 4;
                } else if (counts.own() == 2 && counts.empty() == 2) {
                    score += 1;
                }
            }
        }
        return score;
    }

    static int squareGateCount(BoardState state, PieceColor color) {
        int count = 0;
        int size = state.ruleConfig().boardSize();
        int[][] cells = state.snapshot();
        for (int file = 0; file < size - 1; file++) {
            for (int rank = 0; rank < size - 1; rank++) {
                SquareCounts counts = squareCounts(cells, color, file, rank);
                if (counts.own() == 3 && counts.empty() == 1) {
                    count++;
                }
            }
        }
        return count;
    }

    static int centerScore(BoardState state, PieceColor color) {
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

    static int captureTargetScore(BoardState state, BoardPoint point) {
        int value = state.get(point);
        if (value <= 0) {
            return 0;
        }
        int centerA = state.ruleConfig().centerPointA();
        int centerB = state.ruleConfig().centerPointB();
        int distanceA = Math.abs(point.fileIndex() - centerA) + Math.abs(point.rankIndex() - centerA);
        int distanceB = Math.abs(point.fileIndex() - centerB) + Math.abs(point.rankIndex() - centerB);
        return value * 20 + Math.max(0, state.ruleConfig().boardSize() - Math.min(distanceA, distanceB)) * 30;
    }

    static int formationBonus(BoardState state, Move move, int base, int perCapture) {
        return formationName(state, move)
                .map(name -> base + formationCaptureCount(state, move) * perCapture + formationNameBonus(name))
                .orElse(0);
    }

    static Optional<String> formationName(BoardState state, Move move) {
        if (!state.ruleConfig().formationCapturesEnabled()
                || state.phase() != BoardPhase.MOVE
                || move.path().size() < 2) {
            return Optional.empty();
        }
        BoardPoint from = move.path().get(0);
        BoardPoint to = move.path().get(move.path().size() - 1);
        int piece = state.get(from);
        if (piece <= 0) {
            return Optional.empty();
        }
        int[][] cells = simulatedMoveCells(state, move, piece, from, to);
        PieceColor color = PieceColor.fromPieceValue(piece);
        return FormationPatternDetector.bestMatch(cells, state.ruleConfig().boardSize(), color, from, to)
                .map(match -> match.name());
    }

    static BoardState simulate(BoardState state, Move move) {
        BoardState next = state.copyForSimulation();
        try {
            next.applyMove(move);
            return next;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static int flyUrgency(BoardState state, PieceColor color) {
        int count = pieceCount(state, color);
        int threshold = state.ruleConfig().flyPieceThreshold();
        if (count <= 0 || count > threshold + 4) {
            return 0;
        }
        if (count <= threshold) {
            return 5;
        }
        return threshold + 5 - count;
    }

    static int formationNameBonus(String name) {
        if (name.contains("煞")) {
            return 9_000;
        }
        if (name.contains("枪")) {
            return 7_000;
        }
        if (name.contains("拉萨")) {
            return 6_000;
        }
        if (name.contains("双门")) {
            return 4_000;
        }
        return 2_000;
    }

    private static int formationCaptureCount(BoardState state, Move move) {
        BoardPoint from = move.path().get(0);
        BoardPoint to = move.path().get(move.path().size() - 1);
        int piece = state.get(from);
        int[][] cells = simulatedMoveCells(state, move, piece, from, to);
        PieceColor color = PieceColor.fromPieceValue(piece);
        return FormationPatternDetector.bestMatch(cells, state.ruleConfig().boardSize(), color, from, to)
                .map(match -> match.captureCount())
                .orElse(0);
    }

    private static int[][] simulatedMoveCells(BoardState state, Move move, int piece, BoardPoint from, BoardPoint to) {
        int[][] cells = copyCells(state.snapshot());
        cells[from.fileIndex()][from.rankIndex()] = 0;
        for (BoardPoint captured : move.jumpCaptures()) {
            cells[captured.fileIndex()][captured.rankIndex()] = 0;
        }
        cells[to.fileIndex()][to.rankIndex()] = piece;
        return cells;
    }

    private static int[][] copyCells(int[][] source) {
        int[][] copy = new int[source.length][source.length];
        for (int file = 0; file < source.length; file++) {
            System.arraycopy(source[file], 0, copy[file], 0, source[file].length);
        }
        return copy;
    }

    private static SquareCounts squareCounts(int[][] cells, PieceColor color, int file, int rank) {
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
        return new SquareCounts(own, empty);
    }

    private record SquareCounts(int own, int empty) {
    }
}
