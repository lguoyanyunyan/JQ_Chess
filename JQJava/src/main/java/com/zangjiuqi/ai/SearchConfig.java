package com.zangjiuqi.ai;

public final class SearchConfig {
    private final int depth;
    private final int timeoutSeconds;

    public SearchConfig(int depth, int timeoutSeconds) {
        this.depth = Math.max(1, depth);
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
    }

    public int depth() {
        return depth;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }
}
