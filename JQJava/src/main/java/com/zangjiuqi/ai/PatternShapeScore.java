package com.zangjiuqi.ai;

record PatternShapeScore(
        String shapeName,
        int score,
        int ownCount,
        int emptyCount,
        int blockedCount,
        int pointCount,
        boolean complete,
        boolean oneMoveAway
) {
    static PatternShapeScore empty() {
        return new PatternShapeScore("", 0, 0, 0, 0, 0, false, false);
    }

    boolean betterThan(PatternShapeScore other) {
        if (score != other.score()) {
            return score > other.score();
        }
        if (blockedCount != other.blockedCount()) {
            return blockedCount < other.blockedCount();
        }
        return ownCount > other.ownCount();
    }
}
