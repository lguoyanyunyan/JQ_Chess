package com.zangjiuqi;

import com.zangjiuqi.core.BoardRuleConfig;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;
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
        assertEquals(TraditionalWinMode.OFF, config.traditionalWinMode());
        assertFalse(config.traditionalPatternWinEnabled());
    }

    @Test
    void traditionalModeUsesFourteenByFourteenBoard() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(RuleMode.TRADITIONAL_BASIC);

        assertEquals(14, config.boardSize());
        assertEquals(196, config.boardPointCount());
        assertEquals(6, config.centerPointA());
        assertEquals(7, config.centerPointB());
        assertEquals(14, config.flyPieceThreshold());
        assertEquals(TraditionalWinMode.OFF, config.traditionalWinMode());
        assertFalse(config.traditionalPatternWinEnabled());
    }

    @Test
    void traditionalModeCanEnablePatternWinSkeleton() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(
                RuleMode.TRADITIONAL_BASIC,
                TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN
        );

        assertEquals(TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN, config.traditionalWinMode());
        assertTrue(config.traditionalPatternWinEnabled());
    }

    @Test
    void competitiveModeNormalizesTraditionalWinModeToOff() {
        BoardRuleConfig config = BoardRuleConfig.fromMode(
                RuleMode.COMPETITIVE,
                TraditionalWinMode.FIXED_PATTERN_REQUIRED
        );

        assertEquals(TraditionalWinMode.OFF, config.traditionalWinMode());
        assertFalse(config.traditionalPatternWinEnabled());
    }
}
