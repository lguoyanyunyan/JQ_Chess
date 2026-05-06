package com.zangjiuqi.core;

import java.util.List;

public final class TraditionalPatternTemplates {
    private TraditionalPatternTemplates() {
    }

    public static List<TraditionalPatternTemplate> lhasaTemplates() {
        return List.of(
                new TraditionalPatternTemplate(
                        "双门拉萨",
                        List.of(
                                p(0, 0), p(1, 0), p(2, 0), p(3, 0),
                                p(0, 1), p(2, 1), p(3, 1),
                                p(0, 2), p(1, 2), p(3, 2),
                                p(0, 3), p(1, 3), p(2, 3), p(3, 3)
                        )
                ),
                new TraditionalPatternTemplate(
                        "三门拉萨",
                        List.of(
                                p(0, 0), p(1, 0), p(2, 0), p(3, 0),
                                p(0, 1), p(2, 1), p(3, 1),
                                p(0, 2), p(1, 2), p(2, 2),
                                p(0, 3), p(1, 3), p(2, 3), p(3, 3)
                        )
                )
        );
    }

    public static TraditionalPatternTemplate goldfishTemplate() {
        return new TraditionalPatternTemplate(
                "金鱼",
                List.of(
                        p(1, 0), p(2, 0), p(3, 0), p(4, 0),
                        p(1, 1), p(2, 1), p(4, 1),
                        p(0, 2), p(2, 2), p(3, 2),
                        p(0, 3), p(1, 3), p(2, 3), p(3, 3)
                ),
                PatternTransformMode.ROTATE_AND_MIRROR
        );
    }

    public static List<TraditionalPatternTemplate> goldfishTemplates() {
        return List.of(goldfishTemplate());
    }

    public static List<TraditionalPatternTemplate> templatesFor(TraditionalWinningPattern pattern) {
        return switch (pattern) {
            case LHASA -> lhasaTemplates();
            case GOLDFISH -> goldfishTemplates();
            case OFF -> List.of();
        };
    }

    private static TraditionalPatternTemplate.Offset p(int file, int rank) {
        return TraditionalPatternTemplate.p(file, rank);
    }
}
