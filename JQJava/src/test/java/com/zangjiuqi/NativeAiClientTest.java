package com.zangjiuqi;

import com.zangjiuqi.ai.NativeAiClient;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class NativeAiClientTest {
    @Test
    void bundledDllReturnsCompetitiveOpeningMove() {
        NativeAiClient client = NativeAiClient.bundled();
        BoardState state = new BoardState(RuleMode.COMPETITIVE);

        String move = client.requestMove(state, 1, 1);

        assertFalse(move.isEmpty());
        BoardPoint.parse(move, state.ruleConfig().boardSize());
        client.destroyHashtable();
    }

    @Test
    void bundledDllReturnsTraditionalOpeningMove() {
        NativeAiClient client = NativeAiClient.bundled();
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);

        String move = client.requestMove(state, 1, 1);

        assertFalse(move.isEmpty());
        BoardPoint.parse(move, state.ruleConfig().boardSize());
        client.destroyHashtable();
    }
}
