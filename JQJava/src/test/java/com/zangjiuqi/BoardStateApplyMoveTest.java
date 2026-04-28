package com.zangjiuqi;

import com.zangjiuqi.ai.AiTurnExecutor;
import com.zangjiuqi.ai.AiTurnResult;
import com.zangjiuqi.ai.NativeAiClient;
import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStateApplyMoveTest {
    @Test
    void appliesPlacementMove() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        state.applyMove(new Move(List.of(point(3, 3)), List.of(), List.of()));

        assertEquals(1, state.get(point(3, 3)));
        assertEquals(0, state.turn());
    }

    @Test
    void appliesAdjacentMove() {
        BoardState state = moveState();

        state.applyMove(new Move(List.of(point(1, 1), point(2, 1)), List.of(), List.of()));

        assertEquals(0, state.get(point(1, 1)));
        assertEquals(2, state.get(point(2, 1)));
        assertEquals(0, state.turn());
    }

    @Test
    void appliesJumpMove() {
        BoardState state = moveState();
        state.putForTesting(point(2, 1), 1);

        state.applyMove(new Move(List.of(point(1, 1), point(3, 1)), List.of(point(2, 1)), List.of()));

        assertEquals(0, state.get(point(1, 1)));
        assertEquals(0, state.get(point(2, 1)));
        assertEquals(2, state.get(point(3, 1)));
    }

    @Test
    void incompleteRestrictedMultiJumpRollsBackBoard() {
        BoardState state = restrictedMultiJumpState();

        assertThrows(IllegalStateException.class,
                () -> state.applyMove(new Move(List.of(point(1, 1), point(3, 1)), List.of(point(2, 1)), List.of())));

        assertEquals(2, state.get(point(1, 1)));
        assertEquals(1, state.get(point(2, 1)));
        assertEquals(0, state.get(point(3, 1)));
        assertEquals(3, state.get(point(4, 1)));
        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(1, state.turn());
    }

    @Test
    void appliesSquareCaptureMove() {
        BoardState state = moveState();
        state.putForTesting(point(1, 2), 4);
        state.putForTesting(point(2, 2), 6);
        state.putForTesting(point(3, 1), 8);

        state.applyMove(new Move(List.of(point(3, 1), point(2, 1)), List.of(), List.of(point(0, 6))));

        assertEquals(0, state.get(point(0, 6)));
        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(0, state.turn());
    }

    @Test
    void appliesAiNotationSquareCaptureWithOriginalCoordinateProtocol() {
        BoardState state = moveState();
        state.putForTesting(point(4, 3), 1);
        state.putForTesting(point(5, 3), 6);
        state.putForTesting(point(4, 2), 8);

        state.applyMove(new Move(
                List.of(BoardPoint.parse("C5", 8), BoardPoint.parse("C6", 8)),
                List.of(),
                List.of(BoardPoint.parse("D5", 8))
        ));

        assertEquals(0, state.get(point(4, 2)));
        assertEquals(0, state.get(point(4, 3)));
        assertEquals(6, state.get(point(5, 3)));
        assertEquals(8, state.get(point(5, 2)));
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void aiBoardUsesOriginalRowMajorProtocol() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.putForTesting(BoardPoint.parse("C5", 8), 1);

        byte[] aiBoard = state.toAiBoard();

        assertEquals(1, aiBoard[2 * 8 + 4]);
        assertEquals(0, aiBoard[4 * 8 + 2]);
    }

    @Test
    void screenshotAiMoveWithFcUsesOriginalCoordinateProtocol() {
        BoardState state = moveState();
        state.putForTesting(BoardPoint.parse("D6", 8), 4);
        state.putForTesting(BoardPoint.parse("D5", 8), 6);
        state.putForTesting(BoardPoint.parse("C5", 8), 8);
        state.putForTesting(BoardPoint.parse("C4", 8), 1);

        state.applyMove(new Move(
                List.of(BoardPoint.parse("C5", 8), BoardPoint.parse("C6", 8)),
                List.of(),
                List.of(BoardPoint.parse("C4", 8))
        ));

        assertEquals(0, state.get(BoardPoint.parse("C4", 8)));
        assertEquals(0, state.get(BoardPoint.parse("C5", 8)));
        assertEquals(8, state.get(BoardPoint.parse("C6", 8)));
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void invalidMoveRollsBackBoard() {
        BoardState state = moveState();

        assertThrows(IllegalArgumentException.class,
                () -> state.applyMove(new Move(List.of(point(1, 1), point(4, 4)), List.of(), List.of())));

        assertEquals(2, state.get(point(1, 1)));
        assertEquals(0, state.get(point(4, 4)));
        assertEquals(1, state.turn());
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void nativeAiOpeningMoveCanBeExecuted() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        AiTurnExecutor executor = new AiTurnExecutor(NativeAiClient.bundled());

        AiTurnResult result = executor.execute(state, 1, 1);

        assertTrue(result.success(), result.message());
        assertFalse(result.rawMove().isEmpty());
        assertEquals(0, state.turn());
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

    private static BoardState restrictedMultiJumpState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);

        state.putForTesting(point(1, 1), 2);
        state.putForTesting(point(7, 0), 4);
        state.putForTesting(point(7, 1), 6);
        state.putForTesting(point(7, 2), 8);
        state.putForTesting(point(7, 3), 10);
        state.putForTesting(point(7, 4), 12);
        state.putForTesting(point(7, 5), 14);
        state.putForTesting(point(7, 6), 16);
        state.putForTesting(point(7, 7), 18);

        state.putForTesting(point(2, 1), 1);
        state.putForTesting(point(4, 1), 3);
        state.putForTesting(point(0, 6), 5);
        state.putForTesting(point(1, 6), 7);
        state.putForTesting(point(2, 6), 9);
        state.putForTesting(point(3, 6), 11);
        state.putForTesting(point(4, 6), 13);
        state.putForTesting(point(5, 6), 15);
        return state;
    }

    private static BoardPoint point(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
