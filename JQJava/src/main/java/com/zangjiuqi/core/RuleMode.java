package com.zangjiuqi.core;

public enum RuleMode {
    TRADITIONAL_BASIC("传统基础", 14),
    COMPETITIVE("竞技化", 8);

    private final String displayName;
    private final int boardSize;

    RuleMode(String displayName, int boardSize) {
        this.displayName = displayName;
        this.boardSize = boardSize;
    }

    public String displayName() {
        return displayName;
    }

    public int boardSize() {
        return boardSize;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
