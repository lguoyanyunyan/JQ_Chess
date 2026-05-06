package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinningPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraditionalWinningPatternTest {
    private static final String LHASA_WIN_REASON = "\u4f20\u7edf\u83b7\u80dc\u9635\u578b\uff1a\u62c9\u8428";
    private static final String GOLDFISH_WIN_REASON = "\u4f20\u7edf\u83b7\u80dc\u9635\u578b\uff1a\u91d1\u9c7c";
    private static final String WEAK_GATE_REASON = "\u4f20\u7edf\u98de\u5b50\u4e34\u754c\uff1a\u5f31\u52bf\u65b9\u4fdd\u6709\u68cb\u95e8";

    @Test
    void offDoesNotFinishAfterLhasaCaptureCompletes() {
        BoardState state = lhasaCaptureState(TraditionalWinningPattern.OFF, lhasaDoubleDoorPoints(), p(4, 2), p(4, 3));

        triggerFormation(state, p(4, 2), p(4, 3));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
        assertEquals(TraditionalWinningPattern.OFF, state.traditionalWinningPattern());
    }

    @Test
    void lhasaWinningPatternFinishesAfterDoubleDoorLhasaCaptureCompletesBeforeWeakSideCanFly() {
        BoardState state = lhasaCaptureState(TraditionalWinningPattern.LHASA, lhasaDoubleDoorPoints(), p(4, 2), p(4, 3));

        triggerFormation(state, p(4, 2), p(4, 3));
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertFalse(state.gameResult().finished());

        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertTrue(state.gameResult().finished());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
        assertEquals(LHASA_WIN_REASON, state.gameResult().reason());
        assertEquals("\u9ed1\u65b9\u83b7\u80dc\uff1a" + LHASA_WIN_REASON, state.statusText());
    }

    @Test
    void lhasaWinningPatternFinishesAfterThreeDoorLhasaCaptureCompletesBeforeWeakSideCanFly() {
        BoardState state = lhasaCaptureState(TraditionalWinningPattern.LHASA, lhasaThreeDoorPoints(), p(4, 2), p(4, 3));

        triggerFormation(state, p(4, 2), p(4, 3));
        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
        assertEquals(LHASA_WIN_REASON, state.gameResult().reason());
    }

    @Test
    void lhasaWinningPatternCanFinishWhenCaptureDropsWeakSideToFlyThreshold() {
        BoardState state = lhasaCaptureState(
                TraditionalWinningPattern.LHASA,
                lhasaDoubleDoorPoints(),
                p(4, 2),
                p(4, 3),
                weakNoGatePieces(17),
                winnerExtraPieces()
        );

        triggerFormation(state, p(4, 2), p(4, 3));
        assertEquals(3, state.pendingCaptureCount());

        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
        assertEquals(LHASA_WIN_REASON, state.gameResult().reason());
    }

    @Test
    void flyThresholdAdjudicationFinishesForWeakSideWithGateWhenStrongSideMissesWinningPattern() {
        BoardState state = squareCaptureState(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinningPattern.LHASA,
                weakPiecesWithGateForCount(15)
        );

        triggerFormation(state, p(5, 2), p(4, 2));
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertEquals(1, state.pendingCaptureCount());
        assertFalse(state.gameResult().finished());

        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertEquals(PieceColor.WHITE, state.gameResult().winner().orElseThrow());
        assertEquals(WEAK_GATE_REASON, state.gameResult().reason());
        assertEquals("\u767d\u65b9\u83b7\u80dc\uff1a" + WEAK_GATE_REASON, state.statusText());
    }

    @Test
    void staticLhasaFinishesWhenStrongSideHasKeptLhasaAndWeakSideHasNoGate() {
        BoardState state = staticLhasaSquareCaptureState(TraditionalWinningPattern.LHASA, weakNoGatePieces(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertFalse(state.lastFormationMatch().isPresent());

        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
        assertEquals(LHASA_WIN_REASON, state.gameResult().reason());
    }

    @Test
    void staticLhasaWithWeakGateDoesNotFinishOrTriggerWeakThresholdWin() {
        BoardState state = staticLhasaSquareCaptureState(TraditionalWinningPattern.LHASA, weakPiecesWithGateForCount(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void goldfishWinningPatternFinishesFromStaticGoldfishWhenWeakSideHasNoGate() {
        BoardState state = staticGoldfishSquareCaptureState(TraditionalWinningPattern.GOLDFISH, weakNoGatePieces(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertFalse(state.lastFormationMatch().isPresent());

        completeCaptures(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
        assertEquals(GOLDFISH_WIN_REASON, state.gameResult().reason());
    }

    @Test
    void goldfishSelectionDoesNotLetStaticLhasaWin() {
        BoardState state = staticLhasaSquareCaptureState(TraditionalWinningPattern.GOLDFISH, weakNoGatePieces(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaSelectionDoesNotLetStaticGoldfishWin() {
        BoardState state = staticGoldfishSquareCaptureState(TraditionalWinningPattern.LHASA, weakNoGatePieces(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void staticGoldfishWithWeakGateDoesNotFinishOrTriggerWeakThresholdWin() {
        BoardState state = staticGoldfishSquareCaptureState(TraditionalWinningPattern.GOLDFISH, weakPiecesWithGateForCount(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void staticLhasaDoesNotFinishWhenWeakSideStartedInsideFlyThreshold() {
        BoardState state = staticLhasaSquareCaptureState(TraditionalWinningPattern.LHASA, weakNoGatePieces(14));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void staticLhasaIsDisabledWhenTraditionalWinningPatternIsOff() {
        BoardState state = staticLhasaSquareCaptureState(TraditionalWinningPattern.OFF, weakNoGatePieces(15));

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void staticGoldfishIsDisabledInCompetitiveMode() {
        BoardState state = competitiveStaticGoldfishSquareCaptureState();
        state.setTraditionalWinningPattern(TraditionalWinningPattern.GOLDFISH);

        triggerFormation(state, p(2, 1), p(1, 1));
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        state.handlePrimaryClick(p(0, 6));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
        assertEquals(TraditionalWinningPattern.OFF, state.traditionalWinningPattern());
    }

    @Test
    void flyThresholdAdjudicationDoesNotFinishWhenWeakSideHasNoGate() {
        BoardState state = squareCaptureState(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinningPattern.LHASA,
                weakNoGatePieces(15)
        );

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaWinningPatternWithWeakGateDoesNotAlsoTriggerWeakThresholdWin() {
        BoardState state = lhasaCaptureState(
                TraditionalWinningPattern.LHASA,
                lhasaDoubleDoorPoints(),
                p(4, 2),
                p(4, 3),
                weakPiecesWithGateForCount(17),
                winnerExtraPieces()
        );

        triggerFormation(state, p(4, 2), p(4, 3));
        assertEquals(3, state.pendingCaptureCount());

        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void flyThresholdAdjudicationDoesNotRepeatWhenWeakSideStartedInsideThreshold() {
        BoardState state = squareCaptureState(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinningPattern.LHASA,
                weakPiecesWithGateForCount(14)
        );

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void flyThresholdAdjudicationIsDisabledWhenTraditionalWinningPatternIsOff() {
        BoardState state = squareCaptureState(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinningPattern.OFF,
                weakPiecesWithGateForCount(15)
        );

        triggerFormation(state, p(5, 2), p(4, 2));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaWinningPatternDoesNotFinishWhenWeakSideAlreadyCanFly() {
        BoardState state = lhasaCaptureState(
                TraditionalWinningPattern.LHASA,
                lhasaDoubleDoorPoints(),
                p(4, 2),
                p(4, 3),
                weakFlyingPieces(),
                winnerExtraPieces()
        );

        triggerFormation(state, p(4, 2), p(4, 3));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaWinningPatternDoesNotFinishWhenWeakSideStillHasSquareGate() {
        BoardState state = lhasaCaptureState(
                TraditionalWinningPattern.LHASA,
                lhasaDoubleDoorPoints(),
                p(4, 2),
                p(4, 3),
                weakPiecesWithGate(),
                winnerExtraPieces()
        );

        triggerFormation(state, p(4, 2), p(4, 3));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaWinningPatternDoesNotFinishWhenFormationSideIsNotStronger() {
        BoardState state = lhasaCaptureState(
                TraditionalWinningPattern.LHASA,
                lhasaDoubleDoorPoints(),
                p(4, 2),
                p(4, 3),
                weakNoGatePieces(),
                List.of()
        );

        triggerFormation(state, p(4, 2), p(4, 3));
        completeCaptures(state);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
    }

    @Test
    void lhasaWinningPatternDoesNotFinishForOtherKnownFormations() {
        for (FormationCase formation : List.of(
                flatDalianCase(),
                doubleDoorDalianCase(),
                gunCase(),
                shaCase()
        )) {
            BoardState state = formation.state();
            state.setTraditionalWinningPattern(TraditionalWinningPattern.LHASA);

            triggerFormation(state, formation.from(), formation.to());
            completeCaptures(state);

            assertEquals(BoardPhase.MOVE, state.phase(), formation.name());
            assertFalse(state.gameResult().finished(), formation.name());
            assertEquals(TraditionalWinningPattern.LHASA, state.traditionalWinningPattern(), formation.name());
        }
    }

    @Test
    void competitiveBoardStateNormalizesTraditionalWinningPatternToOff() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        state.setTraditionalWinningPattern(TraditionalWinningPattern.GOLDFISH);

        assertEquals(TraditionalWinningPattern.OFF, state.traditionalWinningPattern());
    }

    private static void triggerFormation(BoardState state, BoardPoint from, BoardPoint to) {
        state.handlePrimaryClick(from);
        state.handlePrimaryClick(to);
    }

    private static void completeCaptures(BoardState state) {
        int needed = state.pendingCaptureCount();
        for (BoardPoint point : captureTargetsForCompletion().subList(0, needed)) {
            state.handlePrimaryClick(point);
        }
    }

    private static BoardState lhasaCaptureState(
            TraditionalWinningPattern winningPattern,
            List<BoardPoint> finalFormationPoints,
            BoardPoint from,
            BoardPoint to
    ) {
        return lhasaCaptureState(
                winningPattern,
                finalFormationPoints,
                from,
                to,
                weakNoGatePieces(),
                winnerExtraPieces()
        );
    }

    private static BoardState lhasaCaptureState(
            TraditionalWinningPattern winningPattern,
            List<BoardPoint> finalFormationPoints,
            BoardPoint from,
            BoardPoint to,
            List<BoardPoint> weakPieces,
            List<BoardPoint> extraWinnerPieces
    ) {
        BoardState state = formationMoveState(finalFormationPoints, from, to, weakPieces, extraWinnerPieces);
        state.setTraditionalWinningPattern(winningPattern);
        return state;
    }

    private static BoardState formationMoveState(
            List<BoardPoint> finalFormationPoints,
            BoardPoint from,
            BoardPoint to
    ) {
        return formationMoveState(finalFormationPoints, from, to, weakNoGatePieces(), winnerExtraPieces());
    }

    private static BoardState formationMoveState(
            List<BoardPoint> finalFormationPoints,
            BoardPoint from,
            BoardPoint to,
            List<BoardPoint> weakPieces,
            List<BoardPoint> extraWinnerPieces
    ) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : finalFormationPoints) {
            if (!point.equals(to)) {
                state.putForTesting(point, 2);
            }
        }
        state.putForTesting(from, 2);
        for (BoardPoint point : extraWinnerPieces) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : weakPieces) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static BoardState squareCaptureState(
            RuleMode mode,
            TraditionalWinningPattern winningPattern,
            List<BoardPoint> weakPieces
    ) {
        BoardState state = new BoardState(mode);
        state.enterMovePhaseForTesting(1);
        state.setTraditionalWinningPattern(winningPattern);
        state.putForTesting(p(3, 1), 2);
        state.putForTesting(p(4, 1), 2);
        state.putForTesting(p(3, 2), 2);
        state.putForTesting(p(5, 2), 2);
        for (BoardPoint point : winnerExtraPieces()) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : weakPieces) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static BoardState staticLhasaSquareCaptureState(
            TraditionalWinningPattern winningPattern,
            List<BoardPoint> weakPieces
    ) {
        BoardState state = squareCaptureState(RuleMode.TRADITIONAL_BASIC, winningPattern, weakPieces);
        for (BoardPoint point : shiftedLhasaDoubleDoorPoints()) {
            state.putForTesting(point, 2);
        }
        return state;
    }

    private static BoardState staticGoldfishSquareCaptureState(
            TraditionalWinningPattern winningPattern,
            List<BoardPoint> weakPieces
    ) {
        BoardState state = squareCaptureState(RuleMode.TRADITIONAL_BASIC, winningPattern, weakPieces);
        for (BoardPoint point : shiftedGoldfishPoints()) {
            state.putForTesting(point, 2);
        }
        return state;
    }

    private static BoardState competitiveStaticGoldfishSquareCaptureState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : List.of(p(0, 0), p(1, 0), p(0, 1), p(2, 1))) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : List.of(
                p(2, 4), p(3, 4), p(4, 4), p(5, 4),
                p(2, 5), p(3, 5), p(5, 5),
                p(1, 6), p(3, 6), p(4, 6),
                p(1, 7), p(2, 7), p(3, 7), p(4, 7)
        )) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6), p(0, 7), p(1, 7))) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static FormationCase flatDalianCase() {
        BoardState state = formationMoveState(
                List.of(p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(4, 3), p(3, 4), p(4, 4)),
                p(4, 2),
                p(4, 3)
        );
        return new FormationCase("flat dalian", state, p(4, 2), p(4, 3));
    }

    private static FormationCase doubleDoorDalianCase() {
        BoardState state = formationMoveState(
                List.of(p(3, 1), p(4, 1), p(5, 1), p(3, 2), p(3, 3), p(4, 3),
                        p(5, 3), p(3, 4), p(4, 4), p(5, 4)),
                p(4, 2),
                p(4, 3)
        );
        return new FormationCase("double-door dalian", state, p(4, 2), p(4, 3));
    }

    private static FormationCase gunCase() {
        BoardState state = lineRowMoveState(0, p(13, 1), p(13, 0), List.of());
        return new FormationCase("gun", state, p(13, 1), p(13, 0));
    }

    private static FormationCase shaCase() {
        BoardState state = lineRowMoveState(1, p(6, 2), p(6, 1), List.of(p(5, 0), p(6, 0)));
        return new FormationCase("sha", state, p(6, 2), p(6, 1));
    }

    private static BoardState lineRowMoveState(int rank, BoardPoint from, BoardPoint to, List<BoardPoint> extraOwnPieces) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
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
        for (BoardPoint point : weakNoGatePieces()) {
            if (state.get(point) == 0) {
                state.putForTesting(point, 1);
            }
        }
        return state;
    }

    private static List<BoardPoint> lhasaDoubleDoorPoints() {
        return List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                p(3, 3), p(4, 3), p(6, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4));
    }

    private static List<BoardPoint> lhasaThreeDoorPoints() {
        return List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                p(3, 3), p(4, 3), p(5, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4));
    }

    private static List<BoardPoint> goldfishPoints() {
        return List.of(
                p(1, 0), p(2, 0), p(3, 0), p(4, 0),
                p(1, 1), p(2, 1), p(4, 1),
                p(0, 2), p(2, 2), p(3, 2),
                p(0, 3), p(1, 3), p(2, 3), p(3, 3)
        );
    }

    private static List<BoardPoint> shiftedLhasaDoubleDoorPoints() {
        return shift(lhasaDoubleDoorPoints(), 3, 3);
    }

    private static List<BoardPoint> shiftedGoldfishPoints() {
        return shift(goldfishPoints(), 6, 4);
    }

    private static List<BoardPoint> shift(List<BoardPoint> points, int fileOffset, int rankOffset) {
        return points.stream()
                .map(point -> p(point.fileIndex() + fileOffset, point.rankIndex() + rankOffset))
                .toList();
    }

    private static List<BoardPoint> captureTargetsForCompletion() {
        return List.of(p(0, 12), p(1, 12), p(2, 12), p(3, 12), p(4, 12), p(5, 12), p(6, 12), p(7, 12));
    }

    private static List<BoardPoint> weakNoGatePieces() {
        return List.of(
                p(0, 12), p(1, 12), p(2, 12), p(3, 12), p(4, 12), p(5, 12), p(6, 12),
                p(7, 12), p(8, 12), p(9, 12), p(10, 12), p(11, 12), p(12, 12), p(13, 12),
                p(0, 10), p(1, 10), p(2, 10), p(3, 10), p(4, 10), p(5, 10), p(6, 10), p(7, 10)
        );
    }

    private static List<BoardPoint> weakNoGatePieces(int count) {
        return weakNoGatePieces().subList(0, count);
    }

    private static List<BoardPoint> weakFlyingPieces() {
        return List.of(
                p(0, 12), p(1, 12), p(2, 12), p(3, 12), p(4, 12), p(5, 12), p(6, 12),
                p(7, 12), p(8, 12), p(9, 12), p(10, 12), p(11, 12), p(12, 12), p(13, 12)
        );
    }

    private static List<BoardPoint> weakPiecesWithGate() {
        return List.of(
                p(0, 12), p(1, 12), p(2, 12), p(3, 12), p(4, 12), p(5, 12), p(6, 12),
                p(7, 12), p(8, 12), p(9, 12), p(10, 12), p(11, 12), p(12, 12), p(13, 12),
                p(0, 10), p(0, 9), p(1, 9), p(2, 10), p(3, 10), p(4, 10), p(5, 10), p(6, 10)
        );
    }

    private static List<BoardPoint> weakPiecesWithGateForCount(int count) {
        return List.of(
                p(0, 12), p(1, 12), p(2, 12), p(3, 12), p(4, 12), p(5, 12),
                p(6, 12), p(7, 12), p(8, 12), p(9, 12), p(10, 12), p(11, 12),
                p(0, 10), p(0, 9), p(1, 9),
                p(12, 12), p(13, 12), p(2, 10), p(3, 10), p(4, 10), p(5, 10), p(6, 10)
        ).subList(0, count);
    }

    private static List<BoardPoint> winnerExtraPieces() {
        return List.of(p(10, 0), p(11, 0), p(12, 0), p(13, 0), p(10, 1), p(11, 1), p(12, 1), p(13, 1), p(10, 2));
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private record FormationCase(String name, BoardState state, BoardPoint from, BoardPoint to) {
    }
}
