package com.zangjiuqi;

import com.zangjiuqi.core.BoardRuleConfig;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinningPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardRuleConfigTest {
    @Test
    void competitiveModeUsesEightByEightBoard() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(RuleMode.COMPETITIVE);

        assertEquals(8, config.boardSize());
        assertEquals(64, config.boardPointCount());
        assertEquals(3, config.centerPointA());
        assertEquals(4, config.centerPointB());
        assertEquals(8, config.flyPieceThreshold());
        assertEquals(TraditionalWinningPattern.OFF, config.traditionalWinningPattern());
        assertFalse(config.traditionalWinningPatternEnabled());
    }

    @Test
    void traditionalModeUsesFourteenByFourteenBoard() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(RuleMode.TRADITIONAL_BASIC);

        assertEquals(14, config.boardSize());
        assertEquals(196, config.boardPointCount());
        assertEquals(6, config.centerPointA());
        assertEquals(7, config.centerPointB());
        assertEquals(14, config.flyPieceThreshold());
        assertEquals(TraditionalWinningPattern.OFF, config.traditionalWinningPattern());
        assertFalse(config.traditionalWinningPatternEnabled());
    }

    @Test
    void traditionalModeCanEnableWinningPattern() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinningPattern.LHASA
        );

        assertEquals(TraditionalWinningPattern.LHASA, config.traditionalWinningPattern());
        assertTrue(config.traditionalWinningPatternEnabled());
    }

    @Test
    void competitiveModeNormalizesTraditionalWinningPatternToOff() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(
                RuleMode.COMPETITIVE,
                TraditionalWinningPattern.LHASA
        );

        assertEquals(TraditionalWinningPattern.OFF, config.traditionalWinningPattern());
        assertFalse(config.traditionalWinningPatternEnabled());
    }
}
