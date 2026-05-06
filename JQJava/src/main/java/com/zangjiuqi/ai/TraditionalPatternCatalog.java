package com.zangjiuqi.ai;

import com.zangjiuqi.core.TraditionalPatternTemplate;
import com.zangjiuqi.core.TraditionalPatternTemplates;
import com.zangjiuqi.core.TraditionalWinningPattern;

import java.util.List;

final class TraditionalPatternCatalog {
    private TraditionalPatternCatalog() {
    }

    static List<TraditionalPatternHeuristic> defaultHeuristics() {
        return List.of();
    }

    static List<PatternShape> verifiedTargetShapes() {
        return java.util.stream.Stream.concat(lhasaShapes().stream(), goldfishShapes().stream())
                .toList();
    }

    static List<TraditionalPatternHeuristic> heuristicsFor(TraditionalWinningPattern pattern) {
        return switch (pattern) {
            case LHASA -> List.of(new LhasaPatternHeuristic(lhasaShapes()));
            case GOLDFISH -> List.of(new TargetPatternHeuristic(goldfishShapes()));
            case OFF -> List.of();
        };
    }

    static List<PatternShape> lhasaShapes() {
        return TraditionalPatternTemplates.lhasaTemplates().stream()
                .map(TraditionalPatternCatalog::shape)
                .toList();
    }

    static List<PatternShape> goldfishShapes() {
        return TraditionalPatternTemplates.goldfishTemplates().stream()
                .map(TraditionalPatternCatalog::shape)
                .toList();
    }

    private static PatternShape shape(TraditionalPatternTemplate template) {
        return new PatternShape(
                template.name(),
                template.points().stream()
                        .map(point -> PatternShape.p(point.file(), point.rank()))
                        .toList(),
                template.transformMode(),
                140,
                3,
                4_000,
                20_000,
                8,
                20,
                500,
                8,
                120
        );
    }
}
