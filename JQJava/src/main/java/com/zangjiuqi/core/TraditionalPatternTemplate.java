package com.zangjiuqi.core;

import java.util.List;

public final class TraditionalPatternTemplate {
    private final String name;
    private final List<Offset> points;
    private final PatternTransformMode transformMode;

    TraditionalPatternTemplate(String name, List<Offset> points) {
        this(name, points, PatternTransformMode.ROTATE_AND_MIRROR);
    }

    TraditionalPatternTemplate(String name, List<Offset> points, PatternTransformMode transformMode) {
        this.name = name;
        this.points = List.copyOf(points);
        this.transformMode = transformMode;
    }

    public String name() {
        return name;
    }

    public List<Offset> points() {
        return points;
    }

    public int pointCount() {
        return points.size();
    }

    public PatternTransformMode transformMode() {
        return transformMode;
    }

    static Offset p(int file, int rank) {
        return new Offset(file, rank);
    }

    public record Offset(int file, int rank) {
    }
}
