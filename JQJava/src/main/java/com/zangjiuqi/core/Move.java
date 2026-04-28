package com.zangjiuqi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Move {
    private final List<BoardPoint> path;
    private final List<BoardPoint> jumpCaptures;
    private final List<BoardPoint> squareCaptures;

    public Move(List<BoardPoint> path, List<BoardPoint> jumpCaptures, List<BoardPoint> squareCaptures) {
        this.path = immutableCopy(path);
        this.jumpCaptures = immutableCopy(jumpCaptures);
        this.squareCaptures = immutableCopy(squareCaptures);
        if (this.path.isEmpty()) {
            throw new IllegalArgumentException("Move path must not be empty.");
        }
    }

    public List<BoardPoint> path() {
        return path;
    }

    public List<BoardPoint> jumpCaptures() {
        return jumpCaptures;
    }

    public List<BoardPoint> squareCaptures() {
        return squareCaptures;
    }

    public boolean isPlacement() {
        return path.size() == 1;
    }

    private static List<BoardPoint> immutableCopy(List<BoardPoint> points) {
        if (points == null || points.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(points));
    }
}
