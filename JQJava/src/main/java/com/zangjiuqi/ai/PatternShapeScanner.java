package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PatternTransformMode;
import com.zangjiuqi.core.PieceColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PatternShapeScanner {
    PatternShapeScore bestScore(BoardState state, PieceColor color, List<PatternShape> shapes) {
        PatternShapeScore best = PatternShapeScore.empty();
        for (PatternShape shape : shapes) {
            PatternShapeScore candidate = bestScore(state, color, shape);
            if (candidate.betterThan(best)) {
                best = candidate;
            }
        }
        return best;
    }

    PatternShapeScore bestScore(BoardState state, PieceColor color, PatternShape shape) {
        int boardSize = state.ruleConfig().boardSize();
        int[][] cells = state.snapshot();
        PatternShapeScore best = PatternShapeScore.empty();
        for (List<PatternShape.Offset> variant : variants(shape.points(), shape.transformMode())) {
            int maxFile = variant.stream().mapToInt(PatternShape.Offset::file).max().orElse(0);
            int maxRank = variant.stream().mapToInt(PatternShape.Offset::rank).max().orElse(0);
            for (int anchorFile = 0; anchorFile < boardSize - maxFile; anchorFile++) {
                for (int anchorRank = 0; anchorRank < boardSize - maxRank; anchorRank++) {
                    PatternShapeScore candidate = scoreAt(cells, color, shape, variant, anchorFile, anchorRank);
                    if (candidate.betterThan(best)) {
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private PatternShapeScore scoreAt(
            int[][] cells,
            PieceColor color,
            PatternShape shape,
            List<PatternShape.Offset> points,
            int anchorFile,
            int anchorRank
    ) {
        int own = 0;
        int empty = 0;
        int blocked = 0;
        for (PatternShape.Offset offset : points) {
            BoardPoint point = new BoardPoint(anchorFile + offset.file(), anchorRank + offset.rank());
            int value = cells[point.fileIndex()][point.rankIndex()];
            if (value == 0) {
                empty++;
            } else if (PieceColor.fromPieceValue(value) == color) {
                own++;
            } else {
                blocked++;
            }
        }
        int score = shape.score(own, empty, blocked);
        return new PatternShapeScore(
                shape.name(),
                score,
                own,
                empty,
                blocked,
                shape.pointCount(),
                blocked == 0 && empty == 0,
                blocked == 0 && empty == 1
        );
    }

    private List<List<PatternShape.Offset>> variants(List<PatternShape.Offset> points, PatternTransformMode transformMode) {
        List<List<PatternShape.Offset>> variants = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int transformCount = transformMode == PatternTransformMode.ROTATE_ONLY ? 4 : 8;
        for (int transform = 0; transform < transformCount; transform++) {
            List<PatternShape.Offset> transformed = new ArrayList<>(points.size());
            for (PatternShape.Offset point : points) {
                transformed.add(transform(point, transform));
            }
            int minFile = transformed.stream().mapToInt(PatternShape.Offset::file).min().orElse(0);
            int minRank = transformed.stream().mapToInt(PatternShape.Offset::rank).min().orElse(0);
            List<PatternShape.Offset> normalized = transformed.stream()
                    .map(point -> new PatternShape.Offset(point.file() - minFile, point.rank() - minRank))
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

    private static PatternShape.Offset transform(PatternShape.Offset point, int transform) {
        int x = point.file();
        int y = point.rank();
        return switch (transform) {
            case 0 -> new PatternShape.Offset(x, y);
            case 1 -> new PatternShape.Offset(y, -x);
            case 2 -> new PatternShape.Offset(-x, -y);
            case 3 -> new PatternShape.Offset(-y, x);
            case 4 -> new PatternShape.Offset(-x, y);
            case 5 -> new PatternShape.Offset(y, x);
            case 6 -> new PatternShape.Offset(x, -y);
            case 7 -> new PatternShape.Offset(-y, -x);
            default -> throw new IllegalArgumentException("Unknown transform: " + transform);
        };
    }
}
