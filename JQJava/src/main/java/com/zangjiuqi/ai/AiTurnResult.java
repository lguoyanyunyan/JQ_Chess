package com.zangjiuqi.ai;

public final class AiTurnResult {
    private final String rawMove;
    private final boolean success;
    private final String message;

    private AiTurnResult(String rawMove, boolean success, String message) {
        this.rawMove = rawMove == null ? "" : rawMove;
        this.success = success;
        this.message = message == null ? "" : message;
    }

    public static AiTurnResult success(String rawMove) {
        return new AiTurnResult(rawMove, true, "");
    }

    public static AiTurnResult failure(String rawMove, String message) {
        return new AiTurnResult(rawMove, false, message);
    }

    public String rawMove() {
        return rawMove;
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }
}
