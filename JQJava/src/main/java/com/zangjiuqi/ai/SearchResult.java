package com.zangjiuqi.ai;

import com.zangjiuqi.core.Move;

public final class SearchResult {
    private final Move bestMove;
    private final int completedDepth;
    private final long nodes;
    private final boolean timedOut;

    public SearchResult(Move bestMove, int completedDepth, long nodes, boolean timedOut) {
        this.bestMove = bestMove;
        this.completedDepth = completedDepth;
        this.nodes = nodes;
        this.timedOut = timedOut;
    }

    public Move bestMove() {
        return bestMove;
    }

    public int completedDepth() {
        return completedDepth;
    }

    public long nodes() {
        return nodes;
    }

    public boolean timedOut() {
        return timedOut;
    }
}
