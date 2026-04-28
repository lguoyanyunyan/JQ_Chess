package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraditionalWinModeTest {
    @Test
    void enabledSkeletonDoesNotTriggerAdditionalWinYet() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.setTraditionalWinMode(TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(3, 3), 1);
        state.putForTesting(p(10, 10), 3);

        state.handlePrimaryClick(p(1, 1));
        state.handlePrimaryClick(p(2, 1));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(false, state.gameResult().finished());
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
}
