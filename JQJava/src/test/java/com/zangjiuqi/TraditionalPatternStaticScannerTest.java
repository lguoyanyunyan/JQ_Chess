package com.zangjiuqi;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalPatternStaticMatch;
import com.zangjiuqi.core.TraditionalPatternStaticScanner;
import com.zangjiuqi.core.TraditionalPatternTemplate;
import com.zangjiuqi.core.TraditionalPatternTemplates;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraditionalPatternStaticScannerTest {
    @Test
    void recognizesCompleteDoubleDoorAndThreeDoorLhasa() {
        assertMatch("\u53cc\u95e8\u62c9\u8428", stateWithBlack(lhasaDoubleDoorPoints()),
                TraditionalPatternTemplates.lhasaTemplates());
        assertMatch("\u4e09\u95e8\u62c9\u8428", stateWithBlack(lhasaThreeDoorPoints()),
                TraditionalPatternTemplates.lhasaTemplates());
    }

    @Test
    void recognizesMirroredAndBoundaryLhasa() {
        assertMatch("\u53cc\u95e8\u62c9\u8428", stateWithBlack(List.of(
                p(5, 5), p(6, 5), p(7, 5), p(8, 5),
                p(5, 6), p(6, 6), p(8, 6),
                p(5, 7), p(7, 7), p(8, 7),
                p(5, 8), p(6, 8), p(7, 8), p(8, 8)
        )), TraditionalPatternTemplates.lhasaTemplates());
        assertMatch("\u53cc\u95e8\u62c9\u8428", stateWithBlack(shift(lhasaDoubleDoorPoints(), 7, 9)),
                TraditionalPatternTemplates.lhasaTemplates());
    }

    @Test
    void rejectsIncompleteAndMixedLhasa() {
        BoardState incomplete = stateWithBlack(lhasaDoubleDoorPoints().subList(0, 13));
        BoardState mixed = stateWithBlack(lhasaDoubleDoorPoints());
        mixed.putForTesting(p(3, 3), 1);

        assertTrue(find(incomplete, TraditionalPatternTemplates.lhasaTemplates()).isEmpty());
        assertTrue(find(mixed, TraditionalPatternTemplates.lhasaTemplates()).isEmpty());
    }

    @Test
    void recognizesCompleteRotatedMirroredAndBoundaryGoldfish() {
        List<BoardPoint> goldfish = goldfishPoints();

        assertMatch("\u91d1\u9c7c", stateWithBlack(shift(goldfish, 2, 2)),
                TraditionalPatternTemplates.goldfishTemplates());
        assertMatch("\u91d1\u9c7c", stateWithBlack(transformAndShift(goldfish, 1, 5, 5)),
                TraditionalPatternTemplates.goldfishTemplates());
        assertMatch("\u91d1\u9c7c", stateWithBlack(transformAndShift(goldfish, 4, 5, 5)),
                TraditionalPatternTemplates.goldfishTemplates());
        assertMatch("\u91d1\u9c7c", stateWithBlack(shift(goldfish, 9, 10)),
                TraditionalPatternTemplates.goldfishTemplates());
    }

    @Test
    void rejectsIncompleteAndMixedGoldfish() {
        BoardState incomplete = stateWithBlack(goldfishPoints().subList(0, 13));
        BoardState mixed = stateWithBlack(goldfishPoints());
        mixed.putForTesting(p(2, 1), 1);

        assertTrue(find(incomplete, TraditionalPatternTemplates.goldfishTemplates()).isEmpty());
        assertTrue(find(mixed, TraditionalPatternTemplates.goldfishTemplates()).isEmpty());
    }

    private static void assertMatch(
            String expectedName,
            BoardState state,
            List<TraditionalPatternTemplate> templates
    ) {
        assertEquals(expectedName, find(state, templates).orElseThrow().name());
    }

    private static Optional<TraditionalPatternStaticMatch> find(
            BoardState state,
            List<TraditionalPatternTemplate> templates
    ) {
        return TraditionalPatternStaticScanner.bestCompleteMatch(
                state.snapshot(),
                state.ruleConfig().boardSize(),
                PieceColor.BLACK,
                templates
        );
    }

    private static BoardState stateWithBlack(List<BoardPoint> points) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : points) {
            state.putForTesting(point, 2);
        }
        return state;
    }

    private static List<BoardPoint> lhasaDoubleDoorPoints() {
        return List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                p(0, 2), p(1, 2), p(3, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3));
    }

    private static List<BoardPoint> lhasaThreeDoorPoints() {
        return List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                p(0, 2), p(1, 2), p(2, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3));
    }

    private static List<BoardPoint> goldfishPoints() {
        return List.of(
                p(1, 0), p(2, 0), p(3, 0), p(4, 0),
                p(1, 1), p(2, 1), p(4, 1),
                p(0, 2), p(2, 2), p(3, 2),
                p(0, 3), p(1, 3), p(2, 3), p(3, 3)
        );
    }

    private static List<BoardPoint> shift(List<BoardPoint> points, int fileOffset, int rankOffset) {
        return points.stream()
                .map(point -> p(point.fileIndex() + fileOffset, point.rankIndex() + rankOffset))
                .toList();
    }

    private static List<BoardPoint> transformAndShift(List<BoardPoint> points, int transform, int fileOffset, int rankOffset) {
        List<RawPoint> transformed = points.stream()
                .map(point -> transform(point, transform))
                .toList();
        int minFile = transformed.stream().mapToInt(RawPoint::file).min().orElse(0);
        int minRank = transformed.stream().mapToInt(RawPoint::rank).min().orElse(0);
        return transformed.stream()
                .map(point -> p(point.file() - minFile + fileOffset, point.rank() - minRank + rankOffset))
                .toList();
    }

    private static RawPoint transform(BoardPoint point, int transform) {
        int x = point.fileIndex();
        int y = point.rankIndex();
        return switch (transform) {
            case 1 -> new RawPoint(y, -x);
            case 4 -> new RawPoint(-x, y);
            default -> throw new IllegalArgumentException("Unsupported test transform: " + transform);
        };
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private record RawPoint(int file, int rank) {
    }
}
