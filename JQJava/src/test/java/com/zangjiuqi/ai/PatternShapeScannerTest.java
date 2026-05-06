package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternShapeScannerTest {
    private final PatternShapeScanner scanner = new PatternShapeScanner();

    @Test
    void recognizesOriginalRotatedAndMirroredShapes() {
        PatternShape shape = testShape(List.of(
                PatternShape.p(0, 0), PatternShape.p(1, 0),
                PatternShape.p(0, 1)
        ));
        BoardState original = stateWithBlack(List.of(p(3, 3), p(4, 3), p(3, 4)));
        BoardState rotated = stateWithBlack(List.of(p(6, 6), p(6, 7), p(7, 7)));
        BoardState mirrored = stateWithBlack(List.of(p(8, 8), p(9, 8), p(9, 9)));

        assertTrue(scanner.bestScore(original, PieceColor.BLACK, shape).complete());
        assertTrue(scanner.bestScore(rotated, PieceColor.BLACK, shape).complete());
        assertTrue(scanner.bestScore(mirrored, PieceColor.BLACK, shape).complete());
    }

    @Test
    void scansBoundaryButIgnoresShapesTooLargeForBoard() {
        PatternShape edgeShape = testShape(List.of(
                PatternShape.p(0, 0), PatternShape.p(1, 0),
                PatternShape.p(0, 1), PatternShape.p(1, 1)
        ));
        BoardState edge = stateWithBlack(List.of(p(12, 12), p(13, 12), p(12, 13), p(13, 13)));

        PatternShape tooLarge = testShape(allBoardPointsPlusOne());

        assertTrue(scanner.bestScore(edge, PieceColor.BLACK, edgeShape).complete());
        assertEquals(0, scanner.bestScore(edge, PieceColor.BLACK, tooLarge).score());
    }

    @Test
    void distinguishesCompleteOneMoveAwayProgressAndBlocked() {
        PatternShape shape = testShape(List.of(
                PatternShape.p(0, 0), PatternShape.p(1, 0),
                PatternShape.p(0, 1), PatternShape.p(1, 1)
        ));
        BoardState low = stateWithBlack(List.of(p(1, 1), p(2, 1)));
        BoardState oneMoveAway = stateWithBlack(List.of(p(1, 1), p(2, 1), p(1, 2)));
        BoardState complete = stateWithBlack(List.of(p(1, 1), p(2, 1), p(1, 2), p(2, 2)));
        BoardState blocked = stateWithBlack(List.of(p(1, 1), p(2, 1), p(1, 2)));
        blocked.putForTesting(p(2, 2), 1);

        PatternShapeScore lowScore = scanner.bestScore(low, PieceColor.BLACK, shape);
        PatternShapeScore oneMoveAwayScore = scanner.bestScore(oneMoveAway, PieceColor.BLACK, shape);
        PatternShapeScore completeScore = scanner.bestScore(complete, PieceColor.BLACK, shape);
        PatternShapeScore blockedScore = scanner.bestScore(blocked, PieceColor.BLACK, shape);

        assertFalse(lowScore.oneMoveAway());
        assertTrue(oneMoveAwayScore.oneMoveAway());
        assertTrue(completeScore.complete());
        assertEquals(1, blockedScore.blockedCount());
        assertTrue(oneMoveAwayScore.score() > lowScore.score());
        assertTrue(completeScore.score() > oneMoveAwayScore.score());
    }

    @Test
    void returnsBestProgressAcrossCandidates() {
        PatternShape shape = testShape(List.of(
                PatternShape.p(0, 0), PatternShape.p(1, 0),
                PatternShape.p(0, 1), PatternShape.p(1, 1)
        ));
        BoardState state = stateWithBlack(List.of(
                p(1, 1), p(2, 1),
                p(8, 8), p(9, 8), p(8, 9)
        ));

        PatternShapeScore best = scanner.bestScore(state, PieceColor.BLACK, shape);

        assertEquals(3, best.ownCount());
        assertTrue(best.oneMoveAway());
    }

    @Test
    void scansCatalogLhasaTemplates() {
        BoardState state = stateWithBlack(List.of(
                p(3, 1), p(4, 1), p(5, 1), p(6, 1),
                p(3, 2), p(5, 2), p(6, 2),
                p(3, 3), p(4, 3), p(6, 3),
                p(3, 4), p(4, 4), p(5, 4), p(6, 4)
        ));

        PatternShapeScore best = scanner.bestScore(state, PieceColor.BLACK, TraditionalPatternCatalog.lhasaShapes());

        assertEquals("双门拉萨", best.shapeName());
        assertEquals(14, best.ownCount());
        assertTrue(best.complete());
    }

    @Test
    void scansCatalogGoldfishTemplate() {
        BoardState state = stateWithBlack(List.of(
                p(4, 1), p(5, 1), p(6, 1), p(7, 1),
                p(4, 2), p(5, 2), p(7, 2),
                p(3, 3), p(5, 3), p(6, 3),
                p(3, 4), p(4, 4), p(5, 4), p(6, 4)
        ));

        PatternShapeScore best = scanner.bestScore(state, PieceColor.BLACK, TraditionalPatternCatalog.goldfishShapes());

        assertEquals("\u91d1\u9c7c", best.shapeName());
        assertEquals(14, best.ownCount());
        assertTrue(best.complete());
    }

    private static PatternShape testShape(List<PatternShape.Offset> points) {
        return new PatternShape("test", points, 100, 1, 1_000, 5_000, 1, 100, 100, 1, 10);
    }

    private static List<PatternShape.Offset> allBoardPointsPlusOne() {
        List<PatternShape.Offset> points = new ArrayList<>();
        for (int file = 0; file < 15; file++) {
            for (int rank = 0; rank < 15; rank++) {
                points.add(PatternShape.p(file, rank));
            }
        }
        return points;
    }

    private static BoardState stateWithBlack(List<BoardPoint> points) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : points) {
            state.putForTesting(point, 2);
        }
        return state;
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
