package com.zangjiuqi;

import com.zangjiuqi.ai.AiMoveParser;
import com.zangjiuqi.ai.JavaAiClient;
import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaAiClientTest {
    @Test
    void returnsLegalOpeningMove() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        JavaAiClient client = new JavaAiClient();

        String rawMove = client.requestMove(state, 1, 1);
        Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
        state.applyMove(move);

        assertFalse(rawMove.isEmpty());
        assertEquals(0, state.turn());
    }

    @Test
    void returnsLegalMovePhaseMove() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(6, 6), 1);
        state.putForTesting(p(7, 6), 3);
        state.putForTesting(p(6, 7), 5);
        state.putForTesting(p(7, 7), 7);

        JavaAiClient client = new JavaAiClient();
        String rawMove = client.requestMove(state, 1, 1);
        Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
        state.applyMove(move);

        assertFalse(rawMove.isEmpty());
        assertFalse(state.lastMovePath().isEmpty());
    }

    @Test
    void searchDoesNotMutateSourceBoard() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(6, 6), 1);
        state.putForTesting(p(7, 6), 3);
        state.putForTesting(p(6, 7), 5);
        state.putForTesting(p(7, 7), 7);
        int[][] before = state.snapshot();
        int turn = state.turn();

        JavaAiClient client = new JavaAiClient();
        client.requestMove(state, 3, 1);

        assertArrayEquals(before, state.snapshot());
        assertEquals(turn, state.turn());
    }

    @Test
    void prefersDirectCaptureAtShallowDepth() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhase(PieceColor.BLACK);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(2, 1), 1);
        state.putForTesting(p(4, 4), 4);
        state.putForTesting(p(5, 4), 6);
        state.putForTesting(p(6, 4), 8);
        state.putForTesting(p(7, 4), 10);
        state.putForTesting(p(4, 5), 12);
        state.putForTesting(p(5, 5), 14);
        state.putForTesting(p(6, 5), 16);
        state.putForTesting(p(7, 5), 18);
        state.putForTesting(p(4, 6), 3);
        state.putForTesting(p(5, 6), 5);
        state.putForTesting(p(6, 6), 7);
        state.putForTesting(p(7, 6), 9);
        state.putForTesting(p(4, 7), 11);
        state.putForTesting(p(5, 7), 13);
        state.putForTesting(p(6, 7), 15);
        state.putForTesting(p(7, 7), 17);
        state.putForTesting(p(0, 7), 19);

        JavaAiClient client = new JavaAiClient();
        String rawMove = client.requestMove(state, 1, 1);

        assertEquals("B2,B4 TC-B3", rawMove);
    }

    @Test
    void prefersTraditionalFormationCaptureAtShallowDepth() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : List.of(
                p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(3, 4), p(4, 4), p(4, 2)
        )) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6))) {
            state.putForTesting(point, 1);
        }

        JavaAiClient client = new JavaAiClient();
        String rawMove = client.requestMove(state, 1, 1);
        Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
        state.applyMove(move);

        assertTrue(rawMove.startsWith("C5,D5"), rawMove);
        assertEquals(2, move.squareCaptures().size());
    }

    @Test
    void prefersTraditionalDoubleDoorFormationCaptureAtShallowDepth() {
        BoardState state = traditionalFormationState(
                List.of(p(3, 1), p(4, 1), p(5, 1), p(3, 2), p(3, 3), p(4, 3),
                        p(5, 3), p(3, 4), p(4, 4), p(5, 4)),
                p(4, 2),
                p(4, 3)
        );

        assertAiSelectsFormationMove(state, "C5,D5", 4);
    }

    @Test
    void prefersTraditionalLhasaFormationCaptureAtShallowDepth() {
        BoardState state = traditionalFormationState(
                List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                        p(3, 3), p(4, 3), p(5, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4)),
                p(4, 2),
                p(4, 3)
        );

        assertAiSelectsFormationMove(state, "C5,D5", 5);
    }

    @Test
    void canPlaySeveralLegalOpeningPlies() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        JavaAiClient client = new JavaAiClient();

        for (int i = 0; i < 10; i++) {
            String rawMove = client.requestMove(state, 2, 1);
            Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
            state.applyMove(move);
        }

        assertEquals(10, occupiedCount(state));
    }

    @Test
    void aiTurnExecutorAcceptsInterfaceImplementations() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        com.zangjiuqi.ai.AiTurnExecutor executor = new com.zangjiuqi.ai.AiTurnExecutor((ignored, depth, timeout) -> "D4");

        assertEquals(List.of(), state.lastMovePath());
        assertEquals(true, executor.execute(state, 1, 1).success());
    }

    private static void assertAiSelectsFormationMove(BoardState state, String expectedPrefix, int expectedCaptureCount) {
        int[][] before = state.snapshot();
        JavaAiClient client = new JavaAiClient();

        String rawMove = client.requestMove(state, 1, 1);
        assertArrayEquals(before, state.snapshot());

        Move move = AiMoveParser.parse(rawMove, state.ruleConfig().boardSize());
        assertTrue(rawMove.startsWith(expectedPrefix), rawMove);
        assertEquals(expectedCaptureCount, move.squareCaptures().size(), rawMove);

        state.applyMove(move);
        assertEquals(BoardPhase.MOVE, state.phase());
        assertFalse(state.lastMovePath().isEmpty());
    }

    private static BoardState traditionalFormationState(List<BoardPoint> finalFormationPoints, BoardPoint from, BoardPoint to) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : finalFormationPoints) {
            if (!point.equals(to)) {
                state.putForTesting(point, 2);
            }
        }
        state.putForTesting(from, 2);
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6), p(4, 6), p(5, 6))) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private static int occupiedCount(BoardState state) {
        int count = 0;
        int[][] cells = state.snapshot();
        for (int file = 0; file < cells.length; file++) {
            for (int rank = 0; rank < cells[file].length; rank++) {
                if (cells[file][rank] > 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
