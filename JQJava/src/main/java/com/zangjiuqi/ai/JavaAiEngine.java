package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.GameResult;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;

import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JavaAiEngine {
    private static final int WIN_SCORE = 1_000_000;
    private static final int INF = 2_000_000;
    private static final int MAX_QUIET_BRANCHING = 48;
    private static final int TACTICAL_MOVE_SCORE = 10_000;

    private final AiMoveGenerator generator = new AiMoveGenerator();
    private final AiStrategyProfiles profiles = new AiStrategyProfiles(generator);
    private final Map<String, TableEntry> transpositionTable = new HashMap<>();

    private long deadlineNanos;
    private long nodes;
    private boolean timedOut;

    public SearchResult search(BoardState state, SearchConfig config) {
        this.deadlineNanos = System.nanoTime() + config.timeoutSeconds() * 1_000_000_000L;
        this.nodes = 0;
        this.timedOut = false;

        List<Move> legalMoves = orderedMoves(state, generator.generateLegalMoves(state), null);
        if (legalMoves.isEmpty()) {
            throw new IllegalStateException("No legal move is available.");
        }

        Move bestMove = legalMoves.get(0);
        int completedDepth = 0;
        for (int depth = 1; depth <= config.depth(); depth++) {
            legalMoves = orderedMoves(state, legalMoves, bestMove);
            SearchWindow window = searchRoot(state, legalMoves, depth);
            if (timedOut) {
                break;
            }
            bestMove = window.bestMove();
            completedDepth = depth;
        }
        return new SearchResult(bestMove, completedDepth, nodes, timedOut);
    }

    public void clearCache() {
        transpositionTable.clear();
    }

    private SearchWindow searchRoot(BoardState state, List<Move> legalMoves, int depth) {
        AiStrategyProfile profile = profiles.forState(state);
        Move bestMove = legalMoves.get(0);
        int bestScore = -INF;
        int alpha = -INF;
        int beta = INF;
        for (Move move : legalMoves) {
            if (timeoutReached()) {
                timedOut = true;
                return new SearchWindow(bestMove, bestScore);
            }
            BoardState next = state.copyForSimulation();
            next.applyMove(move);
            int score = -negamax(next, depth - 1, -beta, -alpha) + profile.rootTacticalBonus(state, move);
            if (timedOut) {
                return new SearchWindow(bestMove, bestScore);
            }
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestScore);
        }
        return new SearchWindow(bestMove, bestScore);
    }

    private int negamax(BoardState state, int depth, int alpha, int beta) {
        if (timeoutReached()) {
            timedOut = true;
            return profiles.forState(state).evaluate(state, state.currentTurnColor());
        }
        nodes++;

        GameResult result = state.gameResult();
        if (result.finished()) {
            return terminalScore(result, state.currentTurnColor());
        }
        if (depth <= 0) {
            return profiles.forState(state).evaluate(state, state.currentTurnColor());
        }

        String key = cacheKey(state, depth);
        TableEntry cached = transpositionTable.get(key);
        if (cached != null && cached.depth() >= depth) {
            if (cached.bound() == Bound.EXACT) {
                return cached.score();
            }
            if (cached.bound() == Bound.LOWER) {
                alpha = Math.max(alpha, cached.score());
            } else if (cached.bound() == Bound.UPPER) {
                beta = Math.min(beta, cached.score());
            }
            if (alpha >= beta) {
                return cached.score();
            }
        }

        int originalAlpha = alpha;
        int originalBeta = beta;
        List<Move> moves = orderedMoves(state, generator.generateLegalMoves(state), null);
        if (moves.isEmpty()) {
            return -WIN_SCORE + depth;
        }

        int best = -INF;
        int quietSearched = 0;
        AiStrategyProfile profile = profiles.forState(state);
        for (Move move : moves) {
            int orderScore = profile.moveOrderScore(state, move);
            if (orderScore < TACTICAL_MOVE_SCORE && quietSearched >= MAX_QUIET_BRANCHING) {
                break;
            }
            if (orderScore < TACTICAL_MOVE_SCORE) {
                quietSearched++;
            }
            BoardState next = state.copyForSimulation();
            next.applyMove(move);
            int score = -negamax(next, depth - 1, -beta, -alpha);
            if (timedOut) {
                return best == -INF ? profile.evaluate(state, state.currentTurnColor()) : best;
            }
            best = Math.max(best, score);
            alpha = Math.max(alpha, score);
            if (alpha >= beta) {
                break;
            }
        }
        Bound bound = Bound.EXACT;
        if (best <= originalAlpha) {
            bound = Bound.UPPER;
        } else if (best >= originalBeta) {
            bound = Bound.LOWER;
        }
        transpositionTable.put(key, new TableEntry(depth, best, bound));
        return best;
    }

    private int terminalScore(GameResult result, PieceColor sideToMove) {
        return result.winner()
                .map(winner -> winner == sideToMove ? WIN_SCORE : -WIN_SCORE)
                .orElse(0);
    }

    private List<Move> orderedMoves(BoardState state, List<Move> moves, Move preferredMove) {
        AiStrategyProfile profile = profiles.forState(state);
        List<Move> ordered = new ArrayList<>(moves);
        ordered.sort(Comparator.comparingInt((Move move) -> preferredMove != null && sameMove(move, preferredMove)
                                ? Integer.MAX_VALUE
                                : profile.moveOrderScore(state, move))
                        .reversed()
                .thenComparing(AiMoveFormatter::toAiNotation));
        return ordered;
    }

    private boolean sameMove(Move left, Move right) {
        return AiMoveFormatter.toAiNotation(left).equals(AiMoveFormatter.toAiNotation(right));
    }

    private boolean timeoutReached() {
        return System.nanoTime() >= deadlineNanos;
    }

    private String cacheKey(BoardState state, int depth) {
        StringBuilder key = new StringBuilder(state.ruleConfig().boardPointCount() * 3 + 80);
        key.append(depth).append('|');
        key.append(state.ruleConfig().boardSize()).append('|');
        key.append(state.ruleConfig().mode().name()).append('|');
        key.append(state.ruleConfig().traditionalWinningPattern().name()).append('|');
        key.append(state.phase().name()).append('|');
        key.append(state.currentTurnColor().name()).append('|');
        key.append(state.currentAiState().name()).append('|');
        key.append(state.pendingCaptureCount()).append('|');
        int[][] cells = state.snapshot();
        for (int rank = 0; rank < state.ruleConfig().boardSize(); rank++) {
            for (int file = 0; file < state.ruleConfig().boardSize(); file++) {
                int value = cells[file][rank];
                key.append(value).append(',');
            }
        }
        if (state.phase() == BoardPhase.MOVE) {
            state.selectedPoint().ifPresent(point -> key.append('|').append(point));
        }
        return key.toString();
    }

    private enum Bound {
        EXACT,
        LOWER,
        UPPER
    }

    private static final class SearchWindow {
        private final Move bestMove;
        private final int score;

        private SearchWindow(Move bestMove, int score) {
            this.bestMove = bestMove;
            this.score = score;
        }

        private Move bestMove() {
            return bestMove;
        }

        private int score() {
            return score;
        }
    }

    private static final class TableEntry {
        private final int depth;
        private final int score;
        private final Bound bound;

        private TableEntry(int depth, int score, Bound bound) {
            this.depth = depth;
            this.score = score;
            this.bound = bound;
        }

        private int depth() {
            return depth;
        }

        private int score() {
            return score;
        }

        private Bound bound() {
            return bound;
        }
    }
}
