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
        for (FormationCase formation : basicFormationCases()) {
            assertDetects(formation.name(), formation.formationCaptures(), formation.points(), formation.from(), formation.to());
        }
    }

    @Test
    void traditionalModeTriggersEveryBasicFormationAsCaptureState() {
        for (FormationCase formation : basicFormationCases()) {
            BoardState state = formationMoveState(RuleMode.TRADITIONAL_BASIC, formation);

            state.handlePrimaryClick(formation.from());
            state.handlePrimaryClick(formation.to());

            assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase(), formation.name());
            assertEquals(formation.totalCaptures(), state.pendingCaptureCount(), formation.name());
            assertEquals(formation.name(), state.lastFormationMatch().orElseThrow().name());
        }
    }

    @Test
    void competitiveModeDoesNotAddBasicFormationCaptures() {
        for (FormationCase formation : basicFormationCases()) {
            BoardState state = formationMoveState(RuleMode.COMPETITIVE, formation);

            state.handlePrimaryClick(formation.from());
            state.handlePrimaryClick(formation.to());

            assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase(), formation.name());
            assertEquals(formation.squareCaptures(), state.pendingCaptureCount(), formation.name());
            assertTrue(state.lastFormationMatch().isEmpty(), formation.name());
        }
    }

    @Test
    void detectsHorizontalAndVerticalGunFormations() {
        int[][] horizontal = cellsWithBlackPieces(rowPoints(14, 0));
        List<FormationMatch> horizontalMatches = FormationPatternDetector.findMatches(
                horizontal,
                14,
                PieceColor.BLACK,
                p(13, 1),
                p(13, 0)
        );

        assertFalse(horizontalMatches.isEmpty());
        assertEquals("枪", horizontalMatches.get(0).name());
        assertEquals(2, horizontalMatches.get(0).captureCount());
        assertEquals(14, horizontalMatches.get(0).points().size());

        int[][] vertical = cellsWithBlackPieces(columnPoints(14, 0));
        List<FormationMatch> verticalMatches = FormationPatternDetector.findMatches(
                vertical,
                14,
                PieceColor.BLACK,
                p(1, 13),
                p(0, 13)
        );

        assertFalse(verticalMatches.isEmpty());
        assertEquals("枪", verticalMatches.get(0).name());
        assertEquals(2, verticalMatches.get(0).captureCount());
        assertEquals(14, verticalMatches.get(0).points().size());
    }

    @Test
    void detectsShaWhenCompletedLineAlsoFormsSingleOrDoubleSquare() {
        int[][] singleDoor = cellsWithBlackPieces(withExtra(rowPoints(14, 1), p(5, 0), p(6, 0)));
        List<FormationMatch> singleMatches = FormationPatternDetector.findMatches(
                singleDoor,
                14,
                PieceColor.BLACK,
                p(6, 2),
                p(6, 1)
        );

        assertFalse(singleMatches.isEmpty());
        assertEquals("煞", singleMatches.get(0).name());
        assertEquals(2, singleMatches.get(0).captureCount());

        int[][] doubleDoor = cellsWithBlackPieces(withExtra(rowPoints(14, 1), p(5, 0), p(6, 0), p(7, 0)));
        List<FormationMatch> doubleMatches = FormationPatternDetector.findMatches(
                doubleDoor,
                14,
                PieceColor.BLACK,
                p(6, 2),
                p(6, 1)
        );

        assertFalse(doubleMatches.isEmpty());
        assertEquals("煞", doubleMatches.get(0).name());
        assertEquals(2, doubleMatches.get(0).captureCount());
    }

    @Test
    void rejectsIncompleteMixedOrUnfinishedSpecialLine() {
        int[][] incomplete = cellsWithBlackPieces(rowPoints(14, 0).stream()
                .filter(point -> !point.equals(p(7, 0)))
                .toList());
        List<FormationMatch> incompleteMatches = FormationPatternDetector.findMatches(
                incomplete,
                14,
                PieceColor.BLACK,
                p(13, 1),
                p(13, 0)
        );
        assertTrue(incompleteMatches.isEmpty());

        int[][] mixed = cellsWithBlackPieces(rowPoints(14, 0));
        mixed[7][0] = 1;
        List<FormationMatch> mixedMatches = FormationPatternDetector.findMatches(
                mixed,
                14,
                PieceColor.BLACK,
                p(13, 1),
                p(13, 0)
        );
        assertTrue(mixedMatches.isEmpty());

        int[][] occupiedFrom = cellsWithBlackPieces(withExtra(rowPoints(14, 0), p(13, 1)));
        List<FormationMatch> occupiedFromMatches = FormationPatternDetector.findMatches(
                occupiedFrom,
                14,
                PieceColor.BLACK,
                p(13, 1),
                p(13, 0)
        );
        assertTrue(occupiedFromMatches.isEmpty());
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
    void detectsTemplateAnchoredAtBoardEdge() {
        int[][] cells = cellsWithBlackPieces(List.of(
                p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3), p(1, 3)
        ));

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(1, 1),
                p(1, 2)
        );

        assertFalse(matches.isEmpty());
        assertEquals("平门褡裢", matches.get(0).name());
    }

    @Test
    void rejectsIncompleteFormationTemplate() {
        int[][] cells = cellsWithBlackPieces(List.of(
                p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3)
        ));

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(1, 1),
                p(1, 2)
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void rejectsTemplateWithMixedColorPiece() {
        int[][] cells = cellsWithBlackPieces(List.of(
                p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3), p(1, 3)
        ));
        cells[0][3] = 1;

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(1, 1),
                p(1, 2)
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void rejectsMismatchedMoveEndpoints() {
        int[][] cells = cellsWithBlackPieces(List.of(
                p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 2), p(0, 3), p(1, 3)
        ));

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(1, 1),
                p(0, 2)
        );

        assertTrue(matches.isEmpty());
    }

    @Test
    void rejectsFormationWhenMovedFromPointIsStillOccupied() {
        int[][] cells = cellsWithBlackPieces(List.of(
                p(0, 0), p(1, 0), p(0, 1), p(0, 2), p(1, 1), p(1, 2), p(0, 3), p(1, 3)
        ));

        List<FormationMatch> matches = FormationPatternDetector.findMatches(
                cells,
                14,
                PieceColor.BLACK,
                p(1, 1),
                p(1, 2)
        );

        assertTrue(matches.isEmpty());
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

    @Test
    void traditionalModeTriggersGunAsSpecialLineCapture() {
        BoardState state = gunMoveState(RuleMode.TRADITIONAL_BASIC);

        state.handlePrimaryClick(p(13, 1));
        state.handlePrimaryClick(p(13, 0));

        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertEquals(2, state.pendingCaptureCount());
        assertEquals("枪", state.lastFormationMatch().orElseThrow().name());
    }

    @Test
    void traditionalModeTriggersShaWithOrdinarySquareCaptureCount() {
        BoardState singleDoor = shaMoveState(RuleMode.TRADITIONAL_BASIC, List.of(p(5, 0), p(6, 0)));

        singleDoor.handlePrimaryClick(p(6, 2));
        singleDoor.handlePrimaryClick(p(6, 1));

        assertEquals(BoardPhase.SQUARE_CAPTURE, singleDoor.phase());
        assertEquals(3, singleDoor.pendingCaptureCount());
        assertEquals("煞", singleDoor.lastFormationMatch().orElseThrow().name());

        BoardState doubleDoor = shaMoveState(RuleMode.TRADITIONAL_BASIC, List.of(p(5, 0), p(6, 0), p(7, 0)));

        doubleDoor.handlePrimaryClick(p(6, 2));
        doubleDoor.handlePrimaryClick(p(6, 1));

        assertEquals(BoardPhase.SQUARE_CAPTURE, doubleDoor.phase());
        assertEquals(4, doubleDoor.pendingCaptureCount());
        assertEquals("煞", doubleDoor.lastFormationMatch().orElseThrow().name());
    }

    @Test
    void competitiveModeDoesNotAddSpecialLineFormationCaptures() {
        BoardState gun = lineRowMoveState(RuleMode.COMPETITIVE, 0, p(7, 1), p(7, 0), List.of());

        gun.handlePrimaryClick(p(7, 1));
        gun.handlePrimaryClick(p(7, 0));

        assertEquals(BoardPhase.MOVE, gun.phase());
        assertTrue(gun.lastFormationMatch().isEmpty());

        BoardState sha = lineRowMoveState(RuleMode.COMPETITIVE, 1, p(4, 2), p(4, 1), List.of(p(3, 0), p(4, 0)));

        sha.handlePrimaryClick(p(4, 2));
        sha.handlePrimaryClick(p(4, 1));

        assertEquals(BoardPhase.SQUARE_CAPTURE, sha.phase());
        assertEquals(1, sha.pendingCaptureCount());
        assertTrue(sha.lastFormationMatch().isEmpty());
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

    private static int[][] cellsWithBlackPieces(List<BoardPoint> points) {
        int[][] cells = new int[14][14];
        for (BoardPoint point : points) {
            cells[point.fileIndex()][point.rankIndex()] = 2;
        }
        return cells;
    }

    private static BoardState flatDalianMoveState(RuleMode mode) {
        return formationMoveState(mode, basicFormationCases().get(0));
    }

    private static BoardState gunMoveState(RuleMode mode) {
        return lineRowMoveState(mode, 0, p(13, 1), p(13, 0), List.of());
    }

    private static BoardState shaMoveState(RuleMode mode, List<BoardPoint> extraOwnPieces) {
        return lineRowMoveState(mode, 1, p(6, 2), p(6, 1), extraOwnPieces);
    }

    private static BoardState lineRowMoveState(
            RuleMode mode,
            int rank,
            BoardPoint from,
            BoardPoint to,
            List<BoardPoint> extraOwnPieces
    ) {
        BoardState state = new BoardState(mode);
        state.enterMovePhaseForTesting(1);
        int size = state.ruleConfig().boardSize();
        for (int file = 0; file < size; file++) {
            BoardPoint point = p(file, rank);
            if (!point.equals(to)) {
                state.putForTesting(point, 2);
            }
        }
        state.putForTesting(from, 2);
        for (BoardPoint point : extraOwnPieces) {
            state.putForTesting(point, 2);
        }
        for (int file = 0; file < Math.min(6, size); file++) {
            BoardPoint point = p(file, size - 2);
            if (state.get(point) == 0) {
                state.putForTesting(point, 1);
            }
        }
        return state;
    }

    private static BoardState formationMoveState(RuleMode mode, FormationCase formation) {
        BoardState state = new BoardState(mode);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : formation.initialBlackPieces()) {
            state.putForTesting(point, 2);
        }
        state.putForTesting(formation.from(), 2);
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6), p(4, 6), p(5, 6))) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static List<FormationCase> basicFormationCases() {
        return List.of(
                new FormationCase("平门褡裢", 1, 1,
                        List.of(p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(4, 3), p(3, 4), p(4, 4)),
                        p(4, 2), p(4, 3)),
                new FormationCase("斜门褡裢", 1, 1,
                        List.of(p(3, 1), p(4, 1), p(3, 2), p(4, 2), p(6, 2), p(5, 3), p(6, 3)),
                        p(5, 2), p(4, 2)),
                new FormationCase("平口双门褡裢", 2, 2,
                        List.of(p(3, 1), p(4, 1), p(5, 1), p(3, 2), p(3, 3), p(4, 3),
                                p(5, 3), p(3, 4), p(4, 4), p(5, 4)),
                        p(4, 2), p(4, 3)),
                new FormationCase("斜口双门褡裢", 1, 2,
                        List.of(p(3, 1), p(4, 1), p(3, 2), p(4, 2), p(6, 2), p(7, 2),
                                p(5, 3), p(6, 3), p(7, 3), p(6, 4)),
                        p(5, 2), p(4, 2)),
                new FormationCase("双门拉萨", 1, 2,
                        List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                                p(3, 3), p(4, 3), p(6, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4)),
                        p(4, 2), p(4, 3)),
                new FormationCase("三门拉萨", 2, 3,
                        List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                                p(3, 3), p(4, 3), p(5, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4)),
                        p(4, 2), p(4, 3))
        );
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private static List<BoardPoint> rowPoints(int size, int rank) {
        List<BoardPoint> points = new java.util.ArrayList<>(size);
        for (int file = 0; file < size; file++) {
            points.add(p(file, rank));
        }
        return points;
    }

    private static List<BoardPoint> columnPoints(int size, int file) {
        List<BoardPoint> points = new java.util.ArrayList<>(size);
        for (int rank = 0; rank < size; rank++) {
            points.add(p(file, rank));
        }
        return points;
    }

    private static List<BoardPoint> withExtra(List<BoardPoint> points, BoardPoint... extraPoints) {
        List<BoardPoint> result = new java.util.ArrayList<>(points);
        result.addAll(List.of(extraPoints));
        return result;
    }

    private record FormationCase(
            String name,
            int squareCaptures,
            int formationCaptures,
            List<BoardPoint> points,
            BoardPoint from,
            BoardPoint to
    ) {
        private int totalCaptures() {
            return squareCaptures + formationCaptures;
        }

        private List<BoardPoint> initialBlackPieces() {
            return points.stream()
                    .filter(point -> !point.equals(to))
                    .toList();
        }
    }
}
