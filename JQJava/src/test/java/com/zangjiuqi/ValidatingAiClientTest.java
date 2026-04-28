package com.zangjiuqi;

import com.zangjiuqi.ai.AiTurnExecutor;
import com.zangjiuqi.ai.AiTurnResult;
import com.zangjiuqi.ai.JavaAiClient;
import com.zangjiuqi.ai.ValidatingAiClient;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatingAiClientTest {
    @Test
    void fallsBackWhenNativeStyleMoveViolatesCompetitiveSingleCaptureRestriction() {
        BoardState state = restrictedSingleCaptureState();
        ValidatingAiClient client = new ValidatingAiClient(
                (ignored, depth, timeout) -> "A8,A6 TC-A7",
                new JavaAiClient()
        );
        AiTurnExecutor executor = new AiTurnExecutor(client);

        AiTurnResult result = executor.execute(state, 1, 1);

        assertTrue(result.success(), result.message());
        assertEquals(0, state.turn());
    }

    @Test
    void keepsPrimaryMoveWhenItPassesBoardStateValidation() {
        BoardState state = restrictedSingleCaptureState();
        ValidatingAiClient client = new ValidatingAiClient(
                (ignored, depth, timeout) -> "A1,A2",
                new JavaAiClient()
        );
        AiTurnExecutor executor = new AiTurnExecutor(client);

        AiTurnResult result = executor.execute(state, 1, 1);

        assertTrue(result.success(), result.message());
        assertEquals("A1,A2", result.rawMove());
    }

    private static BoardState restrictedSingleCaptureState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);

        state.putForTesting(p(0, 0), 2);
        state.putForTesting(p(7, 0), 4);
        state.putForTesting(p(0, 2), 6);
        state.putForTesting(p(1, 2), 8);
        state.putForTesting(p(2, 2), 10);
        state.putForTesting(p(3, 2), 12);
        state.putForTesting(p(4, 2), 14);
        state.putForTesting(p(5, 2), 16);
        state.putForTesting(p(6, 2), 18);

        state.putForTesting(p(6, 0), 1);
        state.putForTesting(p(0, 5), 3);
        state.putForTesting(p(1, 5), 5);
        state.putForTesting(p(2, 5), 7);
        state.putForTesting(p(3, 5), 9);
        state.putForTesting(p(4, 5), 11);
        state.putForTesting(p(5, 5), 13);
        state.putForTesting(p(6, 5), 15);
        state.putForTesting(p(7, 5), 17);
        return state;
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
