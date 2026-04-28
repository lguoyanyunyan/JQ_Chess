package com.zangjiuqi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FormationMatch {
    private final String name;
    private final BoardPoint triggerPoint;
    private final PieceColor color;
    private final int captureCount;
    private final List<BoardPoint> points;

    FormationMatch(String name, BoardPoint triggerPoint, PieceColor color, int captureCount, List<BoardPoint> points) {
        this.name = Objects.requireNonNull(name, "name");
        this.triggerPoint = Objects.requireNonNull(triggerPoint, "triggerPoint");
        this.color = Objects.requireNonNull(color, "color");
        this.captureCount = captureCount;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    public String name() {
        return name;
    }

    public BoardPoint triggerPoint() {
        return triggerPoint;
    }

    public PieceColor color() {
        return color;
    }

    public int captureCount() {
        return captureCount;
    }

    public List<BoardPoint> points() {
        return points;
    }
}
