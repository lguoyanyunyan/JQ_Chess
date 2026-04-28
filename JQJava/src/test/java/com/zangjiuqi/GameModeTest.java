package com.zangjiuqi;

import com.zangjiuqi.app.GameMode;
import com.zangjiuqi.core.PieceColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModeTest {
    @Test
    void humanVsHumanHasNoAiSide() {
        assertFalse(GameMode.HUMAN_VS_HUMAN.isAiControlled(PieceColor.WHITE));
        assertFalse(GameMode.HUMAN_VS_HUMAN.isAiControlled(PieceColor.BLACK));
    }

    @Test
    void humanVsAiUsesBlackAi() {
        assertFalse(GameMode.HUMAN_VS_AI.isAiControlled(PieceColor.WHITE));
        assertTrue(GameMode.HUMAN_VS_AI.isAiControlled(PieceColor.BLACK));
    }

    @Test
    void aiVsHumanUsesWhiteAi() {
        assertTrue(GameMode.AI_VS_HUMAN.isAiControlled(PieceColor.WHITE));
        assertFalse(GameMode.AI_VS_HUMAN.isAiControlled(PieceColor.BLACK));
    }

    @Test
    void aiVsAiUsesBothAiSides() {
        assertTrue(GameMode.AI_VS_AI.isAiControlled(PieceColor.WHITE));
        assertTrue(GameMode.AI_VS_AI.isAiControlled(PieceColor.BLACK));
    }
}
