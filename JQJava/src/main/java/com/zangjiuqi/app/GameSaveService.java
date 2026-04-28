package com.zangjiuqi.app;

import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GameSaveService {
    private static final String FORMAT = "JQJavaSave";
    private static final int VERSION = 1;

    private GameSaveService() {
    }

    public static void save(Path path, GameSave save) throws IOException {
        Files.writeString(path, toJson(save), StandardCharsets.UTF_8);
    }

    public static GameSaveLoadResult load(Path path, RuleMode legacyRuleMode) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (looksLikeJson(text)) {
            return new GameSaveLoadResult(fromJson(text), false);
        }

        BoardState state = new BoardState(legacyRuleMode);
        state.loadTextBoard(text);
        GameSave save = new GameSave(
                false,
                GameMode.HUMAN_VS_HUMAN,
                AiBackend.JAVA,
                4,
                5,
                5,
                false,
                BoardDirection.NORMAL,
                state.saveState()
        );
        return new GameSaveLoadResult(save, true);
    }

    private static boolean looksLikeJson(String text) {
        return text != null && text.stripLeading().startsWith("{");
    }

    private static String toJson(GameSave save) {
        StringBuilder json = new StringBuilder(8192);
        json.append("{\n");
        field(json, 1, "format", FORMAT).append(",\n");
        field(json, 1, "version", VERSION).append(",\n");
        field(json, 1, "gameStarted", save.gameStarted()).append(",\n");
        field(json, 1, "gameMode", save.gameMode().name()).append(",\n");
        field(json, 1, "aiBackend", save.aiBackend().name()).append(",\n");
        field(json, 1, "searchDepth", save.searchDepth()).append(",\n");
        field(json, 1, "whiteTimeoutSeconds", save.whiteTimeoutSeconds()).append(",\n");
        field(json, 1, "blackTimeoutSeconds", save.blackTimeoutSeconds()).append(",\n");
        field(json, 1, "showNumbers", save.showNumbers()).append(",\n");
        field(json, 1, "boardDirection", save.boardDirection().name()).append(",\n");
        indent(json, 1).append("\"boardState\": ");
        writeBoardState(json, save.boardState(), 1);
        json.append('\n').append("}\n");
        return json.toString();
    }

    private static void writeBoardState(StringBuilder json, BoardState.SaveState state, int level) {
        json.append("{\n");
        field(json, level + 1, "mode", state.mode().name()).append(",\n");
        field(json, level + 1, "traditionalWinMode", state.traditionalWinMode().name()).append(",\n");
        field(json, level + 1, "turn", state.turn()).append(",\n");
        field(json, level + 1, "sequence", state.sequence()).append(",\n");
        field(json, level + 1, "phase", state.phase().name()).append(",\n");
        field(json, level + 1, "gameFinished", state.gameFinished()).append(",\n");
        field(json, level + 1, "winner", state.winner() == null ? null : state.winner().name()).append(",\n");
        field(json, level + 1, "resultReason", state.resultReason()).append(",\n");
        indent(json, level + 1).append("\"cells\": ");
        writeIntMatrix(json, state.cells(), level + 1);
        json.append(",\n");
        writePlacementHistory(json, state.placementHistory(), level + 1);
        json.append(",\n");
        writeMoveHistory(json, state.moveHistory(), level + 1);
        json.append(",\n");
        writePointList(json, "tempPath", state.tempPath(), level + 1);
        json.append(",\n");
        writePieceList(json, "tempCaptures", state.tempCaptures(), level + 1);
        json.append(",\n");
        writeCandidates(json, state.candidates(), level + 1);
        json.append(",\n");
        field(json, level + 1, "tempPiece", state.tempPiece()).append(",\n");
        field(json, level + 1, "pendingCaptureCount", state.pendingCaptureCount()).append(",\n");
        indent(json, level + 1).append("\"lastFormationMatch\": ");
        writeFormation(json, state.lastFormationMatch(), level + 1);
        json.append(",\n");
        indent(json, level + 1).append("\"clearedCenterA\": ");
        writePiece(json, state.clearedCenterA());
        json.append(",\n");
        indent(json, level + 1).append("\"clearedCenterB\": ");
        writePiece(json, state.clearedCenterB());
        json.append(",\n");
        writeRepetitionCounts(json, state.repetitionCounts(), level + 1);
        json.append('\n');
        indent(json, level).append('}');
    }

    private static void writeIntMatrix(StringBuilder json, int[][] cells, int level) {
        json.append("[\n");
        for (int file = 0; file < cells.length; file++) {
            indent(json, level + 1).append('[');
            for (int rank = 0; rank < cells[file].length; rank++) {
                if (rank > 0) {
                    json.append(',');
                }
                json.append(cells[file][rank]);
            }
            json.append(']');
            if (file + 1 < cells.length) {
                json.append(',');
            }
            json.append('\n');
        }
        indent(json, level).append(']');
    }

    private static void writePlacementHistory(StringBuilder json, List<BoardState.PlacementSnapshot> placements, int level) {
        indent(json, level).append("\"placementHistory\": [");
        for (int i = 0; i < placements.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            BoardState.PlacementSnapshot placement = placements.get(i);
            json.append("{\"point\":");
            string(json, placement.point().toNotation());
            json.append(",\"pieceValue\":").append(placement.pieceValue()).append('}');
        }
        json.append(']');
    }

    private static void writeMoveHistory(StringBuilder json, List<BoardState.MoveSnapshot> moves, int level) {
        indent(json, level).append("\"moveHistory\": [");
        for (int i = 0; i < moves.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            BoardState.MoveSnapshot move = moves.get(i);
            json.append("{\"pieceValue\":").append(move.pieceValue()).append(",\"path\":");
            writePointArrayInline(json, move.path());
            json.append(",\"captures\":");
            writePieceArrayInline(json, move.captures());
            json.append('}');
        }
        json.append(']');
    }

    private static void writePointList(StringBuilder json, String name, List<BoardPoint> points, int level) {
        indent(json, level).append('"').append(name).append("\": ");
        writePointArrayInline(json, points);
    }

    private static void writePieceList(StringBuilder json, String name, List<BoardState.PieceSnapshot> pieces, int level) {
        indent(json, level).append('"').append(name).append("\": ");
        writePieceArrayInline(json, pieces);
    }

    private static void writeCandidates(StringBuilder json, List<BoardState.MoveCandidateSnapshot> candidates, int level) {
        indent(json, level).append("\"candidates\": [");
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            BoardState.MoveCandidateSnapshot candidate = candidates.get(i);
            json.append("{\"target\":");
            string(json, candidate.target().toNotation());
            json.append(",\"capturedPoint\":");
            if (candidate.capturedPoint() == null) {
                json.append("null");
            } else {
                string(json, candidate.capturedPoint().toNotation());
            }
            json.append('}');
        }
        json.append(']');
    }

    private static void writeFormation(StringBuilder json, BoardState.FormationSnapshot formation, int level) {
        if (formation == null) {
            json.append("null");
            return;
        }
        json.append("{\"name\":");
        string(json, formation.name());
        json.append(",\"triggerPoint\":");
        string(json, formation.triggerPoint().toNotation());
        json.append(",\"color\":");
        string(json, formation.color().name());
        json.append(",\"captureCount\":").append(formation.captureCount()).append(",\"points\":");
        writePointArrayInline(json, formation.points());
        json.append('}');
    }

    private static void writePiece(StringBuilder json, BoardState.PieceSnapshot piece) {
        if (piece == null) {
            json.append("null");
            return;
        }
        json.append("{\"point\":");
        string(json, piece.point().toNotation());
        json.append(",\"pieceValue\":").append(piece.pieceValue()).append('}');
    }

    private static void writeRepetitionCounts(StringBuilder json, Map<String, Integer> counts, int level) {
        indent(json, level).append("\"repetitionCounts\": {");
        int index = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            string(json, entry.getKey());
            json.append(':').append(entry.getValue());
        }
        json.append('}');
    }

    private static void writePointArrayInline(StringBuilder json, List<BoardPoint> points) {
        json.append('[');
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            string(json, points.get(i).toNotation());
        }
        json.append(']');
    }

    private static void writePieceArrayInline(StringBuilder json, List<BoardState.PieceSnapshot> pieces) {
        json.append('[');
        for (int i = 0; i < pieces.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            writePiece(json, pieces.get(i));
        }
        json.append(']');
    }

    private static StringBuilder field(StringBuilder json, int level, String name, String value) {
        indent(json, level).append('"').append(name).append("\": ");
        if (value == null) {
            json.append("null");
        } else {
            string(json, value);
        }
        return json;
    }

    private static StringBuilder field(StringBuilder json, int level, String name, int value) {
        return indent(json, level).append('"').append(name).append("\": ").append(value);
    }

    private static StringBuilder field(StringBuilder json, int level, String name, boolean value) {
        return indent(json, level).append('"').append(name).append("\": ").append(value);
    }

    private static StringBuilder indent(StringBuilder json, int level) {
        return json.append("  ".repeat(level));
    }

    private static void string(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                json.append('\\').append(c);
            } else if (c == '\n') {
                json.append("\\n");
            } else if (c == '\r') {
                json.append("\\r");
            } else if (c == '\t') {
                json.append("\\t");
            } else {
                json.append(c);
            }
        }
        json.append('"');
    }

    private static GameSave fromJson(String text) {
        Object parsed = new JsonParser(text).parse();
        Map<String, Object> root = asObject(parsed, "root");
        if (!FORMAT.equals(asString(root.get("format"), "format"))) {
            throw new IllegalArgumentException("Unsupported save format.");
        }
        int version = asInt(root.get("version"), "version");
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported save version: " + version);
        }

        return new GameSave(
                asBoolean(root.get("gameStarted"), "gameStarted"),
                GameMode.valueOf(asString(root.get("gameMode"), "gameMode")),
                AiBackend.valueOf(asString(root.get("aiBackend"), "aiBackend")),
                asInt(root.get("searchDepth"), "searchDepth"),
                asInt(root.get("whiteTimeoutSeconds"), "whiteTimeoutSeconds"),
                asInt(root.get("blackTimeoutSeconds"), "blackTimeoutSeconds"),
                asBoolean(root.get("showNumbers"), "showNumbers"),
                BoardDirection.valueOf(asString(root.get("boardDirection"), "boardDirection")),
                readBoardState(asObject(root.get("boardState"), "boardState"))
        );
    }

    private static BoardState.SaveState readBoardState(Map<String, Object> json) {
        RuleMode mode = RuleMode.valueOf(asString(json.get("mode"), "mode"));
        int boardSize = mode.boardSize();
        return new BoardState.SaveState(
                mode,
                readTraditionalWinMode(json.get("traditionalWinMode")),
                readIntMatrix(json.get("cells"), boardSize),
                asInt(json.get("turn"), "turn"),
                asInt(json.get("sequence"), "sequence"),
                BoardPhase.valueOf(asString(json.get("phase"), "phase")),
                asBoolean(json.get("gameFinished"), "gameFinished"),
                json.get("winner") == null ? null : PieceColor.valueOf(asString(json.get("winner"), "winner")),
                asString(json.get("resultReason"), "resultReason"),
                readPlacements(json.get("placementHistory"), boardSize),
                readMoves(json.get("moveHistory"), boardSize),
                readPoints(json.get("tempPath"), boardSize),
                readPieces(json.get("tempCaptures"), boardSize),
                readCandidates(json.get("candidates"), boardSize),
                asInt(json.get("tempPiece"), "tempPiece"),
                asInt(json.get("pendingCaptureCount"), "pendingCaptureCount"),
                readFormation(json.get("lastFormationMatch"), boardSize),
                readPiece(json.get("clearedCenterA"), boardSize),
                readPiece(json.get("clearedCenterB"), boardSize),
                readStringIntMap(json.get("repetitionCounts"))
        );
    }

    private static int[][] readIntMatrix(Object value, int size) {
        List<Object> rows = asArray(value, "cells");
        if (rows.size() != size) {
            throw new IllegalArgumentException("Saved board size does not match rule mode.");
        }
        int[][] cells = new int[size][size];
        for (int file = 0; file < size; file++) {
            List<Object> row = asArray(rows.get(file), "cells[" + file + "]");
            if (row.size() != size) {
                throw new IllegalArgumentException("Saved board size does not match rule mode.");
            }
            for (int rank = 0; rank < size; rank++) {
                cells[file][rank] = asInt(row.get(rank), "cells");
            }
        }
        return cells;
    }

    private static List<BoardState.PlacementSnapshot> readPlacements(Object value, int boardSize) {
        List<BoardState.PlacementSnapshot> result = new ArrayList<>();
        for (Object item : asArray(value, "placementHistory")) {
            Map<String, Object> object = asObject(item, "placementHistory item");
            result.add(new BoardState.PlacementSnapshot(
                    point(object.get("point"), boardSize, "point"),
                    asInt(object.get("pieceValue"), "pieceValue")
            ));
        }
        return result;
    }

    private static List<BoardState.MoveSnapshot> readMoves(Object value, int boardSize) {
        List<BoardState.MoveSnapshot> result = new ArrayList<>();
        for (Object item : asArray(value, "moveHistory")) {
            Map<String, Object> object = asObject(item, "moveHistory item");
            result.add(new BoardState.MoveSnapshot(
                    asInt(object.get("pieceValue"), "pieceValue"),
                    readPoints(object.get("path"), boardSize),
                    readPieces(object.get("captures"), boardSize)
            ));
        }
        return result;
    }

    private static List<BoardPoint> readPoints(Object value, int boardSize) {
        List<BoardPoint> result = new ArrayList<>();
        for (Object item : asArray(value, "points")) {
            result.add(point(item, boardSize, "point"));
        }
        return result;
    }

    private static List<BoardState.PieceSnapshot> readPieces(Object value, int boardSize) {
        List<BoardState.PieceSnapshot> result = new ArrayList<>();
        for (Object item : asArray(value, "pieces")) {
            BoardState.PieceSnapshot piece = readPiece(item, boardSize);
            if (piece != null) {
                result.add(piece);
            }
        }
        return result;
    }

    private static List<BoardState.MoveCandidateSnapshot> readCandidates(Object value, int boardSize) {
        List<BoardState.MoveCandidateSnapshot> result = new ArrayList<>();
        for (Object item : asArray(value, "candidates")) {
            Map<String, Object> object = asObject(item, "candidate");
            Object captured = object.get("capturedPoint");
            result.add(new BoardState.MoveCandidateSnapshot(
                    point(object.get("target"), boardSize, "target"),
                    captured == null ? null : point(captured, boardSize, "capturedPoint")
            ));
        }
        return result;
    }

    private static BoardState.FormationSnapshot readFormation(Object value, int boardSize) {
        if (value == null) {
            return null;
        }
        Map<String, Object> object = asObject(value, "formation");
        return new BoardState.FormationSnapshot(
                asString(object.get("name"), "name"),
                point(object.get("triggerPoint"), boardSize, "triggerPoint"),
                PieceColor.valueOf(asString(object.get("color"), "color")),
                asInt(object.get("captureCount"), "captureCount"),
                readPoints(object.get("points"), boardSize)
        );
    }

    private static BoardState.PieceSnapshot readPiece(Object value, int boardSize) {
        if (value == null) {
            return null;
        }
        Map<String, Object> object = asObject(value, "piece");
        return new BoardState.PieceSnapshot(
                point(object.get("point"), boardSize, "point"),
                asInt(object.get("pieceValue"), "pieceValue")
        );
    }

    private static Map<String, Integer> readStringIntMap(Object value) {
        Map<String, Object> object = asObject(value, "repetitionCounts");
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            result.put(entry.getKey(), asInt(entry.getValue(), entry.getKey()));
        }
        return result;
    }

    private static BoardPoint point(Object value, int boardSize, String field) {
        return BoardPoint.parse(asString(value, field), boardSize);
    }

    private static TraditionalWinMode readTraditionalWinMode(Object value) {
        if (value == null) {
            return TraditionalWinMode.OFF;
        }
        return TraditionalWinMode.valueOf(asString(value, "traditionalWinMode"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value, String field) {
        if (!(value instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("Expected object for " + field + ".");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asArray(Object value, String field) {
        if (!(value instanceof List<?>)) {
            throw new IllegalArgumentException("Expected array for " + field + ".");
        }
        return (List<Object>) value;
    }

    private static String asString(Object value, String field) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected string for " + field + ".");
        }
        return (String) value;
    }

    private static int asInt(Object value, String field) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("Expected number for " + field + ".");
        }
        return ((Number) value).intValue();
    }

    private static boolean asBoolean(Object value, String field) {
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Expected boolean for " + field + ".");
        }
        return (Boolean) value;
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text;
        }

        private Object parse() {
            Object value = readValue();
            skipWhitespace();
            if (index != text.length()) {
                throw new IllegalArgumentException("Unexpected JSON content at " + index + ".");
            }
            return value;
        }

        private Object readValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON.");
            }
            char c = text.charAt(index);
            if (c == '{') {
                return readObject();
            }
            if (c == '[') {
                return readArray();
            }
            if (c == '"') {
                return readString();
            }
            if (c == 't' || c == 'f') {
                return readBoolean();
            }
            if (c == 'n') {
                readLiteral("null");
                return null;
            }
            return readNumber();
        }

        private Map<String, Object> readObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                result.put(key, readValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> readArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return result;
                }
                expect(',');
            }
        }

        private String readString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return result.toString();
                }
                if (c == '\\') {
                    if (index >= text.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape.");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"', '\\', '/' -> result.append(escaped);
                        case 'b' -> result.append('\b');
                        case 'f' -> result.append('\f');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        case 'u' -> {
                            if (index + 4 > text.length()) {
                                throw new IllegalArgumentException("Invalid unicode escape.");
                            }
                            result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("Invalid JSON escape: " + escaped);
                    }
                } else {
                    result.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string.");
        }

        private Boolean readBoolean() {
            if (text.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (text.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean at " + index + ".");
        }

        private Number readNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            return Integer.parseInt(text.substring(start, index));
        }

        private void readLiteral(String literal) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected " + literal + " at " + index + ".");
            }
            index += literal.length();
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at " + index + ".");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
