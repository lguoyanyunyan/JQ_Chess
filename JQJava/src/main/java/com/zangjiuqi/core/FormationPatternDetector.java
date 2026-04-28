package com.zangjiuqi.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class FormationPatternDetector {
    private static final List<PatternTemplate> TEMPLATES = List.of(
            new PatternTemplate(
                    "平门褡裢",
                    1,
                    List.of(p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3), p(1, 3)),
                    p(1, 1),
                    p(1, 2)
            ),
            new PatternTemplate(
                    "斜门褡裢",
                    1,
                    List.of(p(0, 0), p(1, 0), p(0, 1), p(1, 1), p(3, 1), p(2, 2), p(3, 2)),
                    p(2, 1),
                    p(1, 1)
            ),
            new PatternTemplate(
                    "平口双门褡裢",
                    2,
                    List.of(p(0, 0), p(1, 0), p(2, 0), p(0, 1), p(0, 2), p(1, 2),
                            p(2, 2), p(0, 3), p(1, 3), p(2, 3)),
                    p(1, 1),
                    p(1, 2)
            ),
            new PatternTemplate(
                    "斜口双门褡裢",
                    2,
                    List.of(p(0, 0), p(1, 0), p(0, 1), p(1, 1), p(3, 1), p(4, 1),
                            p(2, 2), p(3, 2), p(4, 2), p(3, 3)),
                    p(2, 1),
                    p(1, 1)
            ),
            new PatternTemplate(
                    "双门拉萨",
                    2,
                    List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                            p(0, 2), p(1, 2), p(3, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3)),
                    p(1, 1),
                    p(1, 2)
            ),
            new PatternTemplate(
                    "三门拉萨",
                    3,
                    List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                            p(0, 2), p(1, 2), p(2, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3)),
                    p(1, 1),
                    p(1, 2)
            )
    );

    private FormationPatternDetector() {
    }

    public static List<FormationMatch> findMatches(
            int[][] cells,
            int boardSize,
            PieceColor color,
            BoardPoint movedFrom,
            BoardPoint movedTo
    ) {
        List<FormationMatch> matches = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (PatternTemplate template : TEMPLATES) {
            for (PatternTemplate variant : template.variants()) {
                int anchorFile = movedFrom.fileIndex() - variant.from().file();
                int anchorRank = movedFrom.rankIndex() - variant.from().rank();
                if (anchorFile < 0 || anchorRank < 0
                        || anchorFile + variant.width() > boardSize
                        || anchorRank + variant.height() > boardSize) {
                    continue;
                }
                FormationMatch match = matchAt(cells, boardSize, color, movedFrom, movedTo, variant, anchorFile, anchorRank);
                if (match == null) {
                    continue;
                }
                String key = match.name() + "|" + match.triggerPoint() + "|" + match.points();
                if (seen.add(key)) {
                    matches.add(match);
                }
            }
        }
        matches.sort(Comparator.comparingInt(FormationMatch::captureCount).reversed()
                .thenComparing(FormationMatch::name));
        return matches;
    }

    public static Optional<FormationMatch> bestMatch(
            int[][] cells,
            int boardSize,
            PieceColor color,
            BoardPoint movedFrom,
            BoardPoint movedTo
    ) {
        List<FormationMatch> matches = findMatches(cells, boardSize, color, movedFrom, movedTo);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private static FormationMatch matchAt(
            int[][] cells,
            int boardSize,
            PieceColor color,
            BoardPoint movedFrom,
            BoardPoint movedTo,
            PatternTemplate template,
            int anchorFile,
            int anchorRank
    ) {
        BoardPoint from = translate(template.from(), anchorFile, anchorRank);
        BoardPoint to = translate(template.to(), anchorFile, anchorRank);
        if (!from.equals(movedFrom) || !to.equals(movedTo)) {
            return null;
        }
        if (!from.isInside(boardSize) || !to.isInside(boardSize)) {
            return null;
        }
        if (cells[from.fileIndex()][from.rankIndex()] != 0) {
            return null;
        }
        if (!sameColor(cells, to, color)) {
            return null;
        }

        List<BoardPoint> points = new ArrayList<>(template.points().size());
        for (Offset point : template.points()) {
            BoardPoint boardPoint = translate(point, anchorFile, anchorRank);
            if (!boardPoint.isInside(boardSize) || !sameColor(cells, boardPoint, color)) {
                return null;
            }
            points.add(boardPoint);
        }
        if (!hasContinuousSquarePotential(cells, boardSize, color, from, to)) {
            return null;
        }
        return new FormationMatch(template.name(), to, color, template.captureCount(), points);
    }

    private static boolean hasContinuousSquarePotential(
            int[][] cells,
            int boardSize,
            PieceColor color,
            BoardPoint from,
            BoardPoint to
    ) {
        if (squareCountAt(cells, boardSize, color, to) == 0) {
            return false;
        }

        int movedPiece = cells[to.fileIndex()][to.rankIndex()];
        cells[to.fileIndex()][to.rankIndex()] = 0;
        cells[from.fileIndex()][from.rankIndex()] = movedPiece;
        boolean canMoveBackToSquare = squareCountAt(cells, boardSize, color, from) > 0;
        cells[from.fileIndex()][from.rankIndex()] = 0;
        cells[to.fileIndex()][to.rankIndex()] = movedPiece;
        return canMoveBackToSquare;
    }

    private static int squareCountAt(int[][] cells, int boardSize, PieceColor color, BoardPoint point) {
        int count = 0;
        int[] xInc = {1, -1, -1, 1};
        int[] yInc = {-1, -1, 1, 1};
        for (int i = 0; i < xInc.length; i++) {
            int diagonalFile = point.fileIndex() + xInc[i];
            int diagonalRank = point.rankIndex() + yInc[i];
            int horizontalFile = point.fileIndex() + xInc[i];
            int horizontalRank = point.rankIndex();
            int verticalFile = point.fileIndex();
            int verticalRank = point.rankIndex() + yInc[i];
            if (isInside(boardSize, diagonalFile, diagonalRank)
                    && isInside(boardSize, horizontalFile, horizontalRank)
                    && isInside(boardSize, verticalFile, verticalRank)
                    && sameColor(cells, new BoardPoint(diagonalFile, diagonalRank), color)
                    && sameColor(cells, new BoardPoint(horizontalFile, horizontalRank), color)
                    && sameColor(cells, new BoardPoint(verticalFile, verticalRank), color)) {
                count++;
            }
        }
        return count;
    }

    private static boolean sameColor(int[][] cells, BoardPoint point, PieceColor color) {
        int value = cells[point.fileIndex()][point.rankIndex()];
        return value > 0 && PieceColor.fromPieceValue(value) == color;
    }

    private static boolean isInside(int boardSize, int file, int rank) {
        return file >= 0 && file < boardSize && rank >= 0 && rank < boardSize;
    }

    private static BoardPoint translate(Offset offset, int anchorFile, int anchorRank) {
        return new BoardPoint(anchorFile + offset.file(), anchorRank + offset.rank());
    }

    private static Offset p(int file, int rank) {
        return new Offset(file, rank);
    }

    private static final class PatternTemplate {
        private final String name;
        private final int captureCount;
        private final List<Offset> points;
        private final Offset from;
        private final Offset to;

        private PatternTemplate(String name, int captureCount, List<Offset> points, Offset from, Offset to) {
            this.name = name;
            this.captureCount = captureCount;
            this.points = List.copyOf(points);
            this.from = from;
            this.to = to;
        }

        private String name() {
            return name;
        }

        private int captureCount() {
            return captureCount;
        }

        private List<Offset> points() {
            return points;
        }

        private Offset from() {
            return from;
        }

        private Offset to() {
            return to;
        }

        private int width() {
            return pointsWithEndpoints().stream().mapToInt(Offset::file).max().orElse(0) + 1;
        }

        private int height() {
            return pointsWithEndpoints().stream().mapToInt(Offset::rank).max().orElse(0) + 1;
        }

        private List<PatternTemplate> variants() {
            List<PatternTemplate> variants = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (int transform = 0; transform < 8; transform++) {
                PatternTemplate variant = transformed(transform);
                String key = variant.points + "|" + variant.from + "|" + variant.to;
                if (seen.add(key)) {
                    variants.add(variant);
                }
            }
            return variants;
        }

        private PatternTemplate transformed(int transform) {
            List<Offset> all = new ArrayList<>(pointsWithEndpoints());
            List<Offset> transformed = new ArrayList<>(all.size());
            for (Offset point : all) {
                transformed.add(transform(point, transform));
            }
            int minFile = transformed.stream().mapToInt(Offset::file).min().orElse(0);
            int minRank = transformed.stream().mapToInt(Offset::rank).min().orElse(0);

            List<Offset> normalizedPoints = new ArrayList<>(points.size());
            for (Offset point : points) {
                Offset transformedPoint = transform(point, transform);
                normalizedPoints.add(new Offset(transformedPoint.file() - minFile, transformedPoint.rank() - minRank));
            }
            Offset transformedFrom = transform(from, transform);
            Offset transformedTo = transform(to, transform);
            return new PatternTemplate(
                    name,
                    captureCount,
                    normalizedPoints,
                    new Offset(transformedFrom.file() - minFile, transformedFrom.rank() - minRank),
                    new Offset(transformedTo.file() - minFile, transformedTo.rank() - minRank)
            );
        }

        private List<Offset> pointsWithEndpoints() {
            List<Offset> all = new ArrayList<>(points.size() + 2);
            all.addAll(points);
            all.add(from);
            all.add(to);
            return all;
        }

        private static Offset transform(Offset point, int transform) {
            int x = point.file();
            int y = point.rank();
            switch (transform) {
                case 0:
                    return new Offset(x, y);
                case 1:
                    return new Offset(x, -y);
                case 2:
                    return new Offset(-x, y);
                case 3:
                    return new Offset(-x, -y);
                case 4:
                    return new Offset(y, x);
                case 5:
                    return new Offset(y, -x);
                case 6:
                    return new Offset(-y, x);
                case 7:
                    return new Offset(-y, -x);
                default:
                    throw new IllegalArgumentException("Unknown transform: " + transform);
            }
        }
    }

    private static final class Offset {
        private final int file;
        private final int rank;

        private Offset(int file, int rank) {
            this.file = file;
            this.rank = rank;
        }

        private int file() {
            return file;
        }

        private int rank() {
            return rank;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Offset)) {
                return false;
            }
            Offset other = (Offset) obj;
            return file == other.file && rank == other.rank;
        }

        @Override
        public int hashCode() {
            return 31 * file + rank;
        }

        @Override
        public String toString() {
            return file + "," + rank;
        }
    }
}
