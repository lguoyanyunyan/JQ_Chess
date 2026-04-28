package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStateRepetitionTest {
    @Test
    void repeatedFullPositionEndsAsDrawOnThirdOccurrence() {
        BoardState state = repetitionState();

        playCycle(state);
        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());

        playCycle(state);

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertTrue(state.gameResult().finished());
        assertTrue(state.gameResult().winner().isEmpty());
        assertEquals("重复局面和棋", state.gameResult().reason());
    }

    @Test
    void undoClearsPriorRepetitionCount() {
        BoardState state = repetitionState();

        playCycle(state);
        state.undo();
        move(state, point(0, 6), point(0, 7));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.gameResult().finished());
        assertEquals("飞子阶段，黑方行棋", state.statusText());
    }

    private static BoardState repetitionState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.putForTesting(point(0, 1), 2);
        state.putForTesting(point(5, 7), 4);
        state.putForTesting(point(6, 7), 6);
        state.putForTesting(point(7, 7), 8);

        state.putForTesting(point(0, 7), 1);
        state.putForTesting(point(5, 0), 3);
        state.putForTesting(point(6, 0), 5);
        state.putForTesting(point(7, 0), 7);
        state.enterMovePhaseForTesting(1);
        return state;
    }

    private static void playCycle(BoardState state) {
        move(state, point(0, 1), point(0, 0));
        move(state, point(0, 7), point(0, 6));
        move(state, point(0, 0), point(0, 1));
        move(state, point(0, 6), point(0, 7));
    }

    private static void move(BoardState state, BoardPoint from, BoardPoint to) {
        state.handlePrimaryClick(from);
        state.handlePrimaryClick(to);
    }

    private static BoardPoint point(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
