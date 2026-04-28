package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.MoveCandidate;
import com.zangjiuqi.core.PieceColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AiMoveGenerator {
    private static final int MAX_CAPTURE_COMBINATIONS = 64;

    List<Move> generateLegalMoves(BoardState state) {
        List<Move> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (state.phase() == BoardPhase.EMBATTLE) {
            generatePlacements(state, result, seen);
        } else if (state.phase() == BoardPhase.MOVE) {
            generateMovePhaseMoves(state, result, seen);
        } else if (state.phase() == BoardPhase.SQUARE_CAPTURE) {
            appendCaptureMoves(state, state, List.of(state.selectedPoint().orElseThrow()), List.of(), result, seen);
        }
        return result;
    }

    private void generatePlacements(BoardState state, List<Move> result, Set<String> seen) {
        int size = state.ruleConfig().boardSize();
        List<BoardPoint> points = new ArrayList<>();
        for (int file = 0; file < size; file++) {
            for (int rank = 0; rank < size; rank++) {
                BoardPoint point = new BoardPoint(file, rank);
                if (state.get(point) == 0) {
                    points.add(point);
                }
            }
        }
        points.sort(Comparator.comparingInt(point -> placementDistance(state, point)));
        for (BoardPoint point : points) {
            addIfLegal(state, new Move(List.of(point), List.of(), List.of()), result, seen);
        }
    }

    private int placementDistance(BoardState state, BoardPoint point) {
        int centerA = state.ruleConfig().centerPointA();
        int centerB = state.ruleConfig().centerPointB();
        int da = Math.abs(point.fileIndex() - centerA) + Math.abs(point.rankIndex() - centerA);
        int db = Math.abs(point.fileIndex() - centerB) + Math.abs(point.rankIndex() - centerB);
        return Math.min(da, db);
    }

    private void generateMovePhaseMoves(BoardState state, List<Move> result, Set<String> seen) {
        int[][] cells = state.snapshot();
        int size = state.ruleConfig().boardSize();
        PieceColor color = state.currentTurnColor();
        for (int file = 0; file < size; file++) {
            for (int rank = 0; rank < size; rank++) {
                int value = cells[file][rank];
                if (value <= 0 || PieceColor.fromPieceValue(value) != color) {
                    continue;
                }
                BoardPoint from = new BoardPoint(file, rank);
                BoardState selected = state.copyForSimulation();
                try {
                    selected.handlePrimaryClick(from);
                } catch (RuntimeException ex) {
                    continue;
                }
                exploreSelectedPiece(state, selected, new ArrayList<>(List.of(from)), new ArrayList<>(), result, seen);
            }
        }
    }

    private void exploreSelectedPiece(
            BoardState root,
            BoardState selected,
            List<BoardPoint> path,
            List<BoardPoint> jumpCaptures,
            List<Move> result,
            Set<String> seen
    ) {
        if (path.size() > 1) {
            BoardState finishProbe = selected.copyForSimulation();
            try {
                finishProbe.handleSecondaryClick();
                appendFinishedMove(root, finishProbe, path, jumpCaptures, result, seen);
            } catch (RuntimeException ignored) {
                // Some rule sets require another jump, so this partial path is not a legal final move.
            }
        }

        List<MoveCandidate> candidates = new ArrayList<>(selected.candidates());
        for (MoveCandidate candidate : candidates) {
            BoardState next = selected.copyForSimulation();
            List<BoardPoint> nextPath = new ArrayList<>(path);
            List<BoardPoint> nextJumpCaptures = new ArrayList<>(jumpCaptures);
            candidate.capturedPoint().ifPresent(nextJumpCaptures::add);
            nextPath.add(candidate.target());
            try {
                next.handlePrimaryClick(candidate.target());
            } catch (RuntimeException ex) {
                continue;
            }

            if (next.phase() == BoardPhase.SQUARE_CAPTURE) {
                appendCaptureMoves(root, next, nextPath, nextJumpCaptures, result, seen);
            } else if (next.phase() == BoardPhase.MOVE && next.selectedPoint().isPresent() && !next.candidates().isEmpty()) {
                exploreSelectedPiece(root, next, nextPath, nextJumpCaptures, result, seen);
            } else {
                appendFinishedMove(root, next, nextPath, nextJumpCaptures, result, seen);
            }
        }
    }

    private void appendFinishedMove(
            BoardState root,
            BoardState finished,
            List<BoardPoint> path,
            List<BoardPoint> jumpCaptures,
            List<Move> result,
            Set<String> seen
    ) {
        if (finished.phase() == BoardPhase.SQUARE_CAPTURE) {
            appendCaptureMoves(root, finished, path, jumpCaptures, result, seen);
            return;
        }
        addIfLegal(root, new Move(path, jumpCaptures, List.of()), result, seen);
    }

    private void appendCaptureMoves(
            BoardState root,
            BoardState captureState,
            List<BoardPoint> path,
            List<BoardPoint> jumpCaptures,
            List<Move> result,
            Set<String> seen
    ) {
        int needed = captureState.pendingCaptureCount();
        if (needed <= 0) {
            addIfLegal(root, new Move(path, jumpCaptures, List.of()), result, seen);
            return;
        }

        List<BoardPoint> opponents = opponentPieces(captureState);
        opponents.sort(Comparator.comparingInt((BoardPoint point) -> captureTargetScore(captureState, point)).reversed()
                .thenComparing(BoardPoint::toString));
        List<List<BoardPoint>> combinations = new ArrayList<>();
        collectCaptureCombinations(opponents, needed, 0, new ArrayList<>(), combinations);
        for (List<BoardPoint> captures : combinations) {
            addIfLegal(root, new Move(path, jumpCaptures, captures), result, seen);
        }
    }

    private List<BoardPoint> opponentPieces(BoardState state) {
        List<BoardPoint> result = new ArrayList<>();
        int size = state.ruleConfig().boardSize();
        PieceColor opponent = state.currentTurnColor().opponent();
        for (int file = 0; file < size; file++) {
            for (int rank = 0; rank < size; rank++) {
                BoardPoint point = new BoardPoint(file, rank);
                int value = state.get(point);
                if (value > 0 && PieceColor.fromPieceValue(value) == opponent) {
                    result.add(point);
                }
            }
        }
        return result;
    }

    private int captureTargetScore(BoardState state, BoardPoint point) {
        int value = state.get(point);
        if (value <= 0) {
            return 0;
        }
        PieceColor color = PieceColor.fromPieceValue(value);
        int score = value;
        score += squareMembershipScore(state, color, point) * 300;
        int centerA = state.ruleConfig().centerPointA();
        int centerB = state.ruleConfig().centerPointB();
        int distanceA = Math.abs(point.fileIndex() - centerA) + Math.abs(point.rankIndex() - centerA);
        int distanceB = Math.abs(point.fileIndex() - centerB) + Math.abs(point.rankIndex() - centerB);
        score += Math.max(0, state.ruleConfig().boardSize() - Math.min(distanceA, distanceB)) * 10;
        return score;
    }

    private int squareMembershipScore(BoardState state, PieceColor color, BoardPoint point) {
        int score = 0;
        int size = state.ruleConfig().boardSize();
        int[][] cells = state.snapshot();
        for (int file = Math.max(0, point.fileIndex() - 1); file <= Math.min(size - 2, point.fileIndex()); file++) {
            for (int rank = Math.max(0, point.rankIndex() - 1); rank <= Math.min(size - 2, point.rankIndex()); rank++) {
                int own = 0;
                int empty = 0;
                BoardPoint[] square = {
                        new BoardPoint(file, rank),
                        new BoardPoint(file + 1, rank),
                        new BoardPoint(file, rank + 1),
                        new BoardPoint(file + 1, rank + 1)
                };
                for (BoardPoint squarePoint : square) {
                    int value = cells[squarePoint.fileIndex()][squarePoint.rankIndex()];
                    if (value == 0) {
                        empty++;
                    } else if (PieceColor.fromPieceValue(value) == color) {
                        own++;
                    }
                }
                if (own == 4) {
                    score += 6;
                } else if (own == 3 && empty == 1) {
                    score += 3;
                }
            }
        }
        return score;
    }

    private void collectCaptureCombinations(
            List<BoardPoint> points,
            int needed,
            int start,
            List<BoardPoint> current,
            List<List<BoardPoint>> result
    ) {
        if (result.size() >= MAX_CAPTURE_COMBINATIONS) {
            return;
        }
        if (current.size() == needed) {
            result.add(List.copyOf(current));
            return;
        }
        for (int i = start; i < points.size(); i++) {
            current.add(points.get(i));
            collectCaptureCombinations(points, needed, i + 1, current, result);
            current.remove(current.size() - 1);
            if (result.size() >= MAX_CAPTURE_COMBINATIONS) {
                return;
            }
        }
    }

    private void addIfLegal(BoardState root, Move move, List<Move> result, Set<String> seen) {
        String notation = AiMoveFormatter.toAiNotation(move);
        if (!seen.add(notation)) {
            return;
        }
        if (root.phase() == BoardPhase.EMBATTLE) {
            BoardState probe = root.copyForSimulation();
            try {
                probe.applyMove(move);
            } catch (RuntimeException ignored) {
                return;
            }
        }
        result.add(move);
    }
}
