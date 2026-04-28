package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.FormationMatch;
import com.zangjiuqi.core.FormationPatternDetector;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationPatternDetectorTest {
    @Test
    void detectsBasicFormationTemplates() {
        assertDetects("平门褡裢", 1,
                List.of(p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3), p(1, 3)),
                p(1, 1), p(1, 2));
        assertDetects("斜门褡裢", 1,
                List.of(p(0, 0), p(1, 0), p(0, 1), p(1, 1), p(3, 1), p(2, 2), p(3, 2)),
                p(2, 1), p(1, 1));
        assertDetects("平口双门褡裢", 2,
                List.of(p(0, 0), p(1, 0), p(2, 0), p(0, 1), p(0, 2), p(1, 2),
                        p(2, 2), p(0, 3), p(1, 3), p(2, 3)),
                p(1, 1), p(1, 2));
        assertDetects("斜口双门褡裢", 2,
                List.of(p(0, 0), p(1, 0), p(0, 1), p(1, 1), p(3, 1), p(4, 1),
                        p(2, 2), p(3, 2), p(4, 2), p(3, 3)),
                p(2, 1), p(1, 1));
        assertDetects("双门拉萨", 2,
                List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                        p(0, 2), p(1, 2), p(3, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3)),
                p(1, 1), p(1, 2));
        assertDetects("三门拉萨", 3,
                List.of(p(0, 0), p(1, 0), p(2, 0), p(3, 0), p(0, 1), p(2, 1), p(3, 1),
                        p(0, 2), p(1, 2), p(2, 2), p(0, 3), p(1, 3), p(2, 3), p(3, 3)),
                p(1, 1), p(1, 2));
    }

    @Test
    void detectsTranslatedRotatedAndMirroredFormation() {
        int[][] cells = new int[14][14];
        List<BoardPoint> points = List.of(p(5, 5), p(5, 6), p(6, 5), p(7, 5), p(7, 6), p(8, 5), p(8, 6));
        for (BoardPoint point : points) {
            cells[point.fileIndex()][point.rankIndex()] = 2;
        }

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(6, 6),
                p(7, 6)
        );

        assertFalse(matches.isEmpty());
        assertEquals("平门褡裢", matches.get(0).name());
    }

    @Test
    void traditionalMoveAddsFormationCaptureToSquareCapture() {
        BoardState state = flatDalianMoveState(RuleMode.TRADITIONAL_BASIC);

        state.handlePrimaryClick(p(4, 2));
        state.handlePrimaryClick(p(4, 3));

        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertEquals(2, state.pendingCaptureCount());
        assertEquals("平门褡裢", state.lastFormationMatch().orElseThrow().name());
    }

    @Test
    void competitiveModeKeepsOnlyOrdinarySquareCapture() {
        BoardState state = flatDalianMoveState(RuleMode.COMPETITIVE);

        state.handlePrimaryClick(p(4, 2));
        state.handlePrimaryClick(p(4, 3));

        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertEquals(1, state.pendingCaptureCount());
        assertTrue(state.lastFormationMatch().isEmpty());
    }

    private static void assertDetects(String name, int captureCount, List<BoardPoint> points, BoardPoint from, BoardPoint to) {
        int[][] cells = new int[14][14];
        for (BoardPoint point : points) {
            cells[point.fileIndex()][point.rankIndex()] = 2;
        }

        List<FormationMatch> matches = FormationPatternDetector.findMatches(cells, 14, PieceColor.BLACK, from, to);

        assertFalse(matches.isEmpty(), name);
        assertEquals(name, matches.get(0).name());
        assertEquals(captureCount, matches.get(0).captureCount());
    }

    private static BoardState flatDalianMoveState(RuleMode mode) {
        BoardState state = new BoardState(mode);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : List.of(
                p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(3, 4), p(4, 4), p(4, 2)
        )) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6))) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
