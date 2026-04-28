package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStateControlTest {
    @Test
    void enterMovePhaseCanChooseBlackOrWhiteFirst() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        state.enterMovePhase(PieceColor.BLACK);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(PieceColor.BLACK, state.currentTurnColor());

        state.enterMovePhase(PieceColor.WHITE);

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(PieceColor.WHITE, state.currentTurnColor());
    }

    @Test
    void continuePlacementKeepsBoardAndClearsMoveHistory() {
        BoardState state = moveState();
        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(2, 1));

        state.continuePlacementPhase();

        assertEquals(BoardPhase.EMBATTLE, state.phase());
        assertEquals(2, state.get(point(2, 1)));
        assertTrue(state.lastMovePath().isEmpty());
    }

    @Test
    void loadsCompetitiveTextBoardByCurrentRuleSize() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        state.loadTextBoard(String.join(System.lineSeparator(),
                "1 0 0 0 0 0 0 0",
                "0 2 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0"
        ));

        assertEquals(1, state.get(point(0, 0)));
        assertEquals(2, state.get(point(1, 1)));
        assertEquals(BoardPhase.EMBATTLE, state.phase());
        assertEquals(PieceColor.WHITE, state.currentTurnColor());
    }

    @Test
    void loadRejectsWrongBoardSizeWithoutChangingCurrentBoard() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.loadTextBoard(String.join(System.lineSeparator(),
                "1 0 0 0 0 0 0 0",
                "0 2 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0"
        ));

        assertThrows(IllegalArgumentException.class, () -> state.loadTextBoard("1 0\n0 1"));

        assertEquals(1, state.get(point(0, 0)));
        assertEquals(2, state.get(point(1, 1)));
    }

    @Test
    void loadRejectsInvalidCellCodeWithoutChangingCurrentBoard() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.loadBoardCodes(emptyBoard(8));

        assertThrows(IllegalArgumentException.class, () -> state.loadTextBoard(String.join(System.lineSeparator(),
                "1 0 0 0 0 0 0 0",
                "0 3 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0"
        )));

        assertEquals(0, state.get(point(0, 0)));
        assertEquals(0, state.get(point(1, 1)));
    }

    @Test
    void traditionalModeRequiresFourteenRowsAndColumns() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        int[][] board = emptyBoard(14);
        board[13][13] = 2;

        state.loadBoardCodes(board);

        assertEquals(2, state.get(point(13, 13)));
        assertThrows(IllegalArgumentException.class, () -> state.loadBoardCodes(emptyBoard(8)));
    }

    private static BoardState moveState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(point(1, 1), 2);
        state.putForTesting(point(6, 0), 4);
        state.putForTesting(point(6, 2), 6);
        state.putForTesting(point(6, 4), 8);
        state.putForTesting(point(6, 6), 10);
        state.putForTesting(point(7, 1), 12);
        state.putForTesting(point(7, 3), 14);
        state.putForTesting(point(7, 5), 16);
        state.putForTesting(point(7, 7), 18);

        state.putForTesting(point(0, 6), 1);
        state.putForTesting(point(1, 6), 3);
        state.putForTesting(point(2, 6), 5);
        state.putForTesting(point(3, 6), 7);
        state.putForTesting(point(4, 6), 9);
        state.putForTesting(point(5, 6), 11);
        state.putForTesting(point(0, 7), 13);
        state.putForTesting(point(2, 7), 15);
        state.putForTesting(point(4, 7), 17);
        return state;
    }

    private static int[][] emptyBoard(int size) {
        return new int[size][size];
    }

    private static BoardPoint point(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
