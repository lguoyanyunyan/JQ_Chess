package com.zangjiuqi;

import com.zangjiuqi.core.BoardPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardPointTest {
    @Test
    void parsesAndFormatsCompetitivePoints() {
        BoardPoint point = BoardPoint.parse("C5", 8);

        assertEquals(4, point.fileIndex());
        assertEquals(2, point.rankIndex());
        assertEquals("C5", point.toNotation());
    }

    @Test
    void parsesAndFormatsTraditionalPoints() {
        BoardPoint point = BoardPoint.parse("A14", 14);

        assertEquals(13, point.fileIndex());
        assertEquals(0, point.rankIndex());
        assertEquals("A14", point.toNotation());
    }

    @Test
    void rejectsOutOfRangePoints() {
        assertThrows(IllegalArgumentException.class, () -> BoardPoint.parse("I1", 8));
        assertThrows(IllegalArgumentException.class, () -> BoardPoint.parse("A0", 8));
    }
}
