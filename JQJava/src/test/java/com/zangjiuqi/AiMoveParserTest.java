package com.zangjiuqi;

import com.zangjiuqi.ai.AiMoveParser;
import com.zangjiuqi.core.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMoveParserTest {
    @Test
    void parsesPlacement() {
        Move move = AiMoveParser.parse("D4", 8);

        assertTrue(move.isPlacement());
        assertEquals("D4", move.path().get(0).toNotation());
    }

    @Test
    void parsesPathAndCaptures() {
        Move move = AiMoveParser.parse("A1,A3,C3 TC-A2,B3 FC-D4", 8);

        assertEquals(3, move.path().size());
        assertEquals("C3", move.path().get(2).toNotation());
        assertEquals(2, move.jumpCaptures().size());
        assertEquals("A2", move.jumpCaptures().get(0).toNotation());
        assertEquals(1, move.squareCaptures().size());
        assertEquals("D4", move.squareCaptures().get(0).toNotation());
    }

    @Test
    void rejectsEmptyMoveText() {
        assertThrows(IllegalArgumentException.class, () -> AiMoveParser.parse("", 8));
        assertThrows(IllegalArgumentException.class, () -> AiMoveParser.parse("   ", 8));
    }

    @Test
    void rejectsOutOfRangeCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> AiMoveParser.parse("I1", 8));
        assertThrows(IllegalArgumentException.class, () -> AiMoveParser.parse("A9", 8));
        assertThrows(IllegalArgumentException.class, () -> AiMoveParser.parse("A1 FC-Z9", 8));
    }
}
