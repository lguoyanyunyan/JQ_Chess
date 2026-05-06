package com.zangjiuqi.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TraditionalPatternStaticScanner {
    private TraditionalPatternStaticScanner() {
    }

    public static Optional<TraditionalPatternStaticMatch> bestCompleteMatch(
            int[][] cells,
            int boardSize,
            PieceColor color,
            List<TraditionalPatternTemplate> templates
    ) {
        for (TraditionalPatternTemplate template : templates) {
            if (hasCompleteMatch(cells, boardSize, color, template)) {
                return Optional.of(new TraditionalPatternStaticMatch(template.name(), color, template.pointCount()));
            }
        }
        return Optional.empty();
    }

    private static boolean hasCompleteMatch(
            int[][] cells,
            int boardSize,
            PieceColor color,
            TraditionalPatternTemplate template
    ) {
        for (List<TraditionalPatternTemplate.Offset> variant : variants(template.points(), template.transformMode())) {
            int maxFile = variant.stream().mapToInt(TraditionalPatternTemplate.Offset::file).max().orElse(0);
            int maxRank = variant.stream().mapToInt(TraditionalPatternTemplate.Offset::rank).max().orElse(0);
            for (int anchorFile = 0; anchorFile < boardSize - maxFile; anchorFile++) {
                for (int anchorRank = 0; anchorRank < boardSize - maxRank; anchorRank++) {
                    if (completeAt(cells, color, variant, anchorFile, anchorRank)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean completeAt(
            int[][] cells,
            PieceColor color,
            List<TraditionalPatternTemplate.Offset> points,
            int anchorFile,
            int anchorRank
    ) {
        for (TraditionalPatternTemplate.Offset offset : points) {
            int value = cells[anchorFile + offset.file()][anchorRank + offset.rank()];
            if (value <= 0 || PieceColor.fromPieceValue(value) != color) {
                return false;
            }
        }
        return true;
    }

    private static List<List<TraditionalPatternTemplate.Offset>> variants(
            List<TraditionalPatternTemplate.Offset> points,
            PatternTransformMode transformMode
    ) {
        List<List<TraditionalPatternTemplate.Offset>> variants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int transformCount = transformMode == PatternTransformMode.ROTATE_ONLY ? 4 : 8;
        for (int transform = 0; transform < transformCount; transform++) {
            List<TraditionalPatternTemplate.Offset> transformed = new ArrayList<>(points.size());
            for (TraditionalPatternTemplate.Offset point : points) {
                transformed.add(transform(point, transform));
            }
            int minFile = transformed.stream().mapToInt(TraditionalPatternTemplate.Offset::file).min().orElse(0);
            int minRank = transformed.stream().mapToInt(TraditionalPatternTemplate.Offset::rank).min().orElse(0);
            List<TraditionalPatternTemplate.Offset> normalized = transformed.stream()
                    .map(point -> new TraditionalPatternTemplate.Offset(point.file() - minFile, point.rank() - minRank))
                    .sorted((left, right) -> {
                        int rankCompare = Integer.compare(left.rank(), right.rank());
                        if (rankCompare != 0) {
                            return rankCompare;
                        }
                        return Integer.compare(left.file(), right.file());
                    })
                    .toList();
            if (seen.add(normalized.toString())) {
                variants.add(normalized);
            }
        }
        return variants;
    }

    private static TraditionalPatternTemplate.Offset transform(TraditionalPatternTemplate.Offset point, int transform) {
        int x = point.file();
        int y = point.rank();
        return switch (transform) {
            case 0 -> new TraditionalPatternTemplate.Offset(x, y);
            case 1 -> new TraditionalPatternTemplate.Offset(y, -x);
            case 2 -> new TraditionalPatternTemplate.Offset(-x, -y);
            case 3 -> new TraditionalPatternTemplate.Offset(-y, x);
            case 4 -> new TraditionalPatternTemplate.Offset(-x, y);
            case 5 -> new TraditionalPatternTemplate.Offset(y, x);
            case 6 -> new TraditionalPatternTemplate.Offset(x, -y);
            case 7 -> new TraditionalPatternTemplate.Offset(-y, -x);
            default -> throw new IllegalArgumentException("Unknown transform: " + transform);
        };
    }
}
