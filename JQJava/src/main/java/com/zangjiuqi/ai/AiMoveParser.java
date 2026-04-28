package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiMoveParser {
    private AiMoveParser() {
    }

    public static Move parse(String text, int boardSize) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("AI move text is empty.");
        }

        List<BoardPoint> path = new ArrayList<>();
        List<BoardPoint> jumpCaptures = new ArrayList<>();
        List<BoardPoint> squareCaptures = new ArrayList<>();

        String[] tokens = text.trim().split("\\s+");
        for (String token : tokens) {
            if (token.startsWith("TC-")) {
                jumpCaptures.addAll(parsePointList(token.substring(3), boardSize));
            } else if (token.startsWith("FC-")) {
                squareCaptures.addAll(parsePointList(token.substring(3), boardSize));
            } else if (!token.isEmpty()) {
                path.addAll(parsePointList(token, boardSize));
            }
        }

        if (path.isEmpty()) {
            throw new IllegalArgumentException("AI move has no path: " + text);
        }

        return new Move(path, jumpCaptures, squareCaptures);
    }

    private static List<BoardPoint> parsePointList(String text, int boardSize) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<BoardPoint> result = new ArrayList<>();
        String[] parts = text.split(",");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                result.add(BoardPoint.parse(part, boardSize));
            }
        }
        return result;
    }
}
