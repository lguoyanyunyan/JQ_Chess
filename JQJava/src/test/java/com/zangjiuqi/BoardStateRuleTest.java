package com.zangjiuqi;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardStateRuleTest {
    @Test
    void firstTwoPlacementsMustUseCenterPoints() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        assertThrows(IllegalArgumentException.class, () -> state.place(point(0, 0)));

        state.place(point(3, 3));
        state.place(point(4, 4));
        state.place(point(0, 0));

        assertEquals(3, state.get(point(0, 0)));
    }

    @Test
    void fullCompetitiveEmbattleClearsCenterAndMovesToMovePhase() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.place(point(3, 3));
        state.place(point(4, 4));

        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                BoardPoint point = point(file, rank);
                if (point.equals(point(3, 3)) || point.equals(point(4, 4))) {
                    continue;
                }
                state.place(point);
            }
        }

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(0, state.get(point(3, 3)));
        assertEquals(0, state.get(point(4, 4)));
    }

    @Test
    void adjacentMoveConfirmsTurn() {
        BoardState state = moveState();

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(2, 1));

        assertEquals(0, state.get(point(1, 1)));
        assertEquals(2, state.get(point(2, 1)));
        assertEquals(List.of(point(1, 1), point(2, 1)), state.lastMovePath());
        assertEquals(List.of(), state.lastMoveCaptures());
        assertEquals(0, state.turn());
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void jumpMoveCapturesMiddlePiece() {
        BoardState state = moveState();
        state.putForTesting(point(2, 1), 1);

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(3, 1));

        assertEquals(0, state.get(point(1, 1)));
        assertEquals(0, state.get(point(2, 1)));
        assertEquals(2, state.get(point(3, 1)));
        assertEquals(List.of(point(1, 1), point(3, 1)), state.lastMovePath());
        assertEquals(List.of(point(2, 1)), state.lastMoveCaptures());
        assertEquals(0, state.turn());
    }

    @Test
    void multiJumpKeepsTemporaryCaptureTrackUntilConfirmed() {
        BoardState state = moveState();
        state.putForTesting(point(2, 1), 1);
        state.putForTesting(point(4, 1), 3);

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(3, 1));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(List.of(point(1, 1), point(3, 1)), state.tempPath());
        assertEquals(List.of(point(2, 1)), state.tempCaptures());
        assertTrue(state.lastMovePath().isEmpty());
    }

    @Test
    void completedMultiJumpExposesFullLastMoveTrack() {
        BoardState state = moveState();
        state.putForTesting(point(2, 1), 1);
        state.putForTesting(point(4, 1), 3);

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(3, 1));
        state.handlePrimaryClick(point(5, 1));

        assertEquals(List.of(point(1, 1), point(3, 1), point(5, 1)), state.lastMovePath());
        assertEquals(List.of(point(2, 1), point(4, 1)), state.lastMoveCaptures());
        assertTrue(state.tempPath().isEmpty());
        assertTrue(state.tempCaptures().isEmpty());
    }

    @Test
    void undoRestoresUnconfirmedMultiJump() {
        BoardState state = moveState();
        state.putForTesting(point(2, 1), 1);
        state.putForTesting(point(4, 1), 3);

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(3, 1));
        state.undo();

        assertEquals(2, state.get(point(1, 1)));
        assertEquals(1, state.get(point(2, 1)));
        assertEquals(0, state.get(point(3, 1)));
        assertTrue(state.tempPath().isEmpty());
        assertTrue(state.tempCaptures().isEmpty());
        assertEquals(1, state.turn());
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void squareMoveRequiresAndAppliesCapture() {
        BoardState state = moveState();
        state.putForTesting(point(1, 2), 4);
        state.putForTesting(point(2, 2), 6);
        state.putForTesting(point(3, 1), 8);

        state.handlePrimaryClick(point(3, 1));
        state.handlePrimaryClick(point(2, 1));

        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        state.handlePrimaryClick(point(0, 6));

        assertEquals(0, state.get(point(0, 6)));
        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(0, state.turn());
    }

    @Test
    void squareCaptureRejectsOwnPieceAndKeepsPendingCapture() {
        BoardState state = squareCaptureState();

        state.handlePrimaryClick(point(3, 1));
        state.handlePrimaryClick(point(2, 1));

        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
        assertEquals(1, state.pendingCaptureCount());
        assertThrows(IllegalArgumentException.class, () -> state.handlePrimaryClick(point(1, 1)));
        assertEquals(2, state.get(point(1, 1)));
        assertEquals(1, state.pendingCaptureCount());
        assertEquals(BoardPhase.SQUARE_CAPTURE, state.phase());
    }

    @Test
    void applyMoveRollsBackWhenRequiredSquareCaptureIsMissing() {
        BoardState state = squareCaptureState();

        assertThrows(IllegalArgumentException.class, () -> state.applyMove(new Move(
                List.of(point(3, 1), point(2, 1)),
                List.of(),
                List.of()
        )));

        assertEquals(BoardPhase.MOVE, state.phase());
        assertEquals(8, state.get(point(3, 1)));
        assertEquals(0, state.get(point(2, 1)));
        assertTrue(state.lastMovePath().isEmpty());
    }

    @Test
    void flyingSideCanMoveToAnyEmptyPoint() {
        BoardState state = flyingMoveState(8);

        state.handlePrimaryClick(point(0, 0));
        state.handlePrimaryClick(point(5, 5));

        assertEquals(0, state.get(point(0, 0)));
        assertEquals(2, state.get(point(5, 5)));
        assertEquals(List.of(point(0, 0), point(5, 5)), state.lastMovePath());
    }

    @Test
    void sideAboveFlyThresholdCannotMoveToNonAdjacentEmptyPoint() {
        BoardState state = flyingMoveState(9);

        state.handlePrimaryClick(point(0, 0));

        assertThrows(IllegalArgumentException.class, () -> state.handlePrimaryClick(point(5, 5)));
        assertEquals(2, state.get(point(0, 0)));
        assertEquals(0, state.get(point(5, 5)));
        assertFalse(state.tempPath().isEmpty());
    }

    @Test
    void movePhaseRejectsOpponentPieceAndOccupiedTarget() {
        BoardState state = moveState();

        assertThrows(IllegalArgumentException.class, () -> state.handlePrimaryClick(point(0, 6)));

        state.handlePrimaryClick(point(1, 1));
        assertThrows(IllegalArgumentException.class, () -> state.handlePrimaryClick(point(0, 6)));
        assertEquals(2, state.get(point(1, 1)));
        assertEquals(1, state.get(point(0, 6)));
    }

    @Test
    void undoRestoresCompletedMove() {
        BoardState state = moveState();

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(2, 1));
        state.undo();

        assertEquals(2, state.get(point(1, 1)));
        assertEquals(0, state.get(point(2, 1)));
        assertTrue(state.lastMovePath().isEmpty());
        assertTrue(state.lastMoveCaptures().isEmpty());
        assertEquals(1, state.turn());
        assertEquals(BoardPhase.MOVE, state.phase());
    }

    @Test
    void lastPlacementIsVisibleOnlyDuringEmbattle() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.place(point(3, 3));

        assertEquals(point(3, 3), state.lastPlacement().orElseThrow());

        state.place(point(4, 4));
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                BoardPoint point = point(file, rank);
                if (point.equals(point(3, 3)) || point.equals(point(4, 4))) {
                    continue;
                }
                state.place(point);
            }
        }

        assertEquals(BoardPhase.MOVE, state.phase());
        assertTrue(state.lastPlacement().isEmpty());
    }

    @Test
    void fewerThanFourPiecesLosesAfterConfirmedMove() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(point(1, 1), 2);
        state.putForTesting(point(6, 6), 1);
        state.putForTesting(point(7, 6), 3);
        state.putForTesting(point(6, 7), 5);

        state.handlePrimaryClick(point(1, 1));
        state.handlePrimaryClick(point(2, 1));

        assertEquals(BoardPhase.FINISHED, state.phase());
        assertTrue(state.gameResult().finished());
        assertEquals(PieceColor.BLACK, state.gameResult().winner().orElseThrow());
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

    private static BoardState squareCaptureState() {
        BoardState state = moveState();
        state.putForTesting(point(1, 2), 4);
        state.putForTesting(point(2, 2), 6);
        state.putForTesting(point(3, 1), 8);
        return state;
    }

    private static BoardState flyingMoveState(int blackCount) {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        List<BoardPoint> blackPieces = List.of(
                point(0, 0), point(7, 0), point(0, 2), point(1, 2), point(2, 2),
                point(3, 2), point(4, 2), point(5, 2), point(6, 2)
        );
        for (int i = 0; i < blackCount; i++) {
            state.putForTesting(blackPieces.get(i), (i + 1) * 2);
        }
        state.putForTesting(point(0, 6), 1);
        state.putForTesting(point(1, 6), 3);
        state.putForTesting(point(2, 6), 5);
        state.putForTesting(point(3, 6), 7);
        return state;
    }

    private static BoardPoint point(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
