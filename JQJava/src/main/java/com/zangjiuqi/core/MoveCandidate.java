package com.zangjiuqi.core;

import java.util.Objects;
import java.util.Optional;

public final class MoveCandidate {
    private final BoardPoint target;
    private final BoardPoint capturedPoint;

    public MoveCandidate(BoardPoint target, BoardPoint capturedPoint) {
        this.target = Objects.requireNonNull(target, "target");
        this.capturedPoint = capturedPoint;
    }

    public BoardPoint target() {
        return target;
    }

    public boolean jump() {
        return capturedPoint != null;
    }

    public Optional<BoardPoint> capturedPoint() {
        return Optional.ofNullable(capturedPoint);
    }
}
