package com.zangjiuqi.core;

public enum PieceColor {
    WHITE(1, "白方"),
    BLACK(2, "黑方");

    private final int code;
    private final String displayName;

    PieceColor(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public int code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static PieceColor fromPieceValue(int pieceValue) {
        if (pieceValue <= 0) {
            throw new IllegalArgumentException("Empty point has no color.");
        }
        return pieceValue % 2 == 1 ? WHITE : BLACK;
    }

    public PieceColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
