package com.zangjiuqi.ai;

import com.zangjiuqi.core.TraditionalWinningPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraditionalPatternCatalogTest {
    @Test
    void defaultHeuristicsAreSelectedByBoardConfiguration() {
        List<TraditionalPatternHeuristic> heuristics = TraditionalPatternCatalog.defaultHeuristics();

        assertTrue(heuristics.isEmpty());
    }

    @Test
    void heuristicsForLhasaOnlyEnableLhasa() {
        List<TraditionalPatternHeuristic> heuristics = TraditionalPatternCatalog.heuristicsFor(TraditionalWinningPattern.LHASA);

        assertEquals(1, heuristics.size());
        assertInstanceOf(LhasaPatternHeuristic.class, heuristics.get(0));
    }

    @Test
    void heuristicsForGoldfishOnlyEnableGoldfish() {
        List<TraditionalPatternHeuristic> heuristics = TraditionalPatternCatalog.heuristicsFor(TraditionalWinningPattern.GOLDFISH);

        assertEquals(1, heuristics.size());
        assertInstanceOf(TargetPatternHeuristic.class, heuristics.get(0));
    }

    @Test
    void heuristicsForOffAreEmpty() {
        assertTrue(TraditionalPatternCatalog.heuristicsFor(TraditionalWinningPattern.OFF).isEmpty());
    }

    @Test
    void verifiedTargetShapesContainSelectableTemplates() {
        List<PatternShape> shapes = TraditionalPatternCatalog.verifiedTargetShapes();

        assertEquals(3, shapes.size());
        assertEquals(List.of("\u53cc\u95e8\u62c9\u8428", "\u4e09\u95e8\u62c9\u8428", "\u91d1\u9c7c"),
                shapes.stream().map(PatternShape::name).toList());
        assertTrue(shapes.stream().allMatch(shape -> shape.pointCount() == 14));
    }

    @Test
    void goldfishShapesAreAvailableForSelectedGoldfishPattern() {
        List<PatternShape> shapes = TraditionalPatternCatalog.goldfishShapes();

        assertEquals(1, shapes.size());
        assertEquals("\u91d1\u9c7c", shapes.get(0).name());
        assertEquals(14, shapes.get(0).pointCount());
    }
}
