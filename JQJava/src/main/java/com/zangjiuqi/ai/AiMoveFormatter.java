package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.Move;

final class AiMoveFormatter {
    private AiMoveFormatter() {
    }

    static String toAiNotation(Move move) {
        StringBuilder result = new StringBuilder();
        appendPoints(result, move.path());
        if (!move.jumpCaptures().isEmpty()) {
            result.append(" TC-");
            appendPoints(result, move.jumpCaptures());
        }
        if (!move.squareCaptures().isEmpty()) {
            result.append(" FC-");
            appendPoints(result, move.squareCaptures());
        }
        return result.toString();
    }

    private static void appendPoints(StringBuilder result, java.util.List<BoardPoint> points) {
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(points.get(i).toNotation());
        }
    }
}
