package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TraditionalWinModeTest {
    @Test
    void enabledModesDoNotTriggerAdditionalWinAfterOrdinaryMoveYet() {
        for (TraditionalWinMode mode : enabledModes()) {
            BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
            state.setTraditionalWinMode(mode);
            state.enterMovePhaseForTesting(1);
            state.putForTesting(p(1, 1), 2);
            state.putForTesting(p(3, 3), 1);
            state.putForTesting(p(10, 10), 3);

            state.handlePrimaryClick(p(1, 1));
            state.handlePrimaryClick(p(2, 1));

            assertEquals(BoardPhase.MOVE, state.phase(), mode.toString());
            assertFalse(state.gameResult().finished(), mode.toString());
            assertEquals(mode, state.traditionalWinMode());
        }
    }

    @Test
    void enabledModesDoNotTriggerAdditionalWinAfterSquareCaptureYet() {
        for (TraditionalWinMode mode : enabledModes()) {
            BoardState state = squareCaptureState(mode);

            state.handlePrimaryClick(p(3, 1));
            state.handlePrimaryClick(p(2, 1));

            assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase(), mode.toString());
            assertEquals(1, state.pendingCaptureCount(), mode.toString());
            assertFalse(state.gameResult().finished(), mode.toString());
            assertEquals(mode, state.traditionalWinMode());
        }
    }

    @Test
    void enabledModesDoNotTriggerAdditionalWinAfterBasicFormationYet() {
        for (TraditionalWinMode mode : enabledModes()) {
            BoardState state = formationCaptureState(mode);

            state.handlePrimaryClick(p(4, 2));
            state.handlePrimaryClick(p(4, 3));

            assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase(), mode.toString());
            assertEquals(2, state.pendingCaptureCount(), mode.toString());
            assertFalse(state.gameResult().finished(), mode.toString());
            assertEquals("平门褡裢", state.lastFormationMatch().orElseThrow().name());
            assertEquals(mode, state.traditionalWinMode());
        }
    }

    @Test
    void legacySingleModeSkeletonDoesNotTriggerAdditionalWinYet() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.setTraditionalWinMode(TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(3, 3), 1);
        state.putForTesting(p(10, 10), 3);

        state.handlePrimaryClick(p(1, 1));
        state.handlePrimaryClick(p(2, 1));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
        assertEquals(TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN, state.traditionalWinMode());
    }

    @Test
    void competitiveBoardStateNormalizesTraditionalWinModeToOff() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        state.setTraditionalWinMode(TraditionalWinMode.FIXED_PATTERN_REQUIRED);

        assertEquals(TraditionalWinMode.OFF, state.traditionalWinMode());
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private static TraditionalWinMode[] enabledModes() {
        return new TraditionalWinMode[] {
                TraditionalWinMode.FIXED_PATTERN_REQUIRED,
                TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN,
                TraditionalWinMode.HANDICAP_TARGET_PATTERN
        };
    }

    private static BoardState squareCaptureState(TraditionalWinMode mode) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.setTraditionalWinMode(mode);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(1, 2), 4);
        state.putForTesting(p(2, 2), 6);
        state.putForTesting(p(3, 1), 8);
        state.putForTesting(p(0, 6), 1);
        state.putForTesting(p(1, 6), 3);
        state.putForTesting(p(2, 6), 5);
        return state;
    }

    private static BoardState formationCaptureState(TraditionalWinMode mode) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.setTraditionalWinMode(mode);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : java.util.List.of(
                p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(3, 4), p(4, 4), p(4, 2)
        )) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : java.util.List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6))) {
            state.putForTesting(point, 1);
        }
        return state;
    }
}
