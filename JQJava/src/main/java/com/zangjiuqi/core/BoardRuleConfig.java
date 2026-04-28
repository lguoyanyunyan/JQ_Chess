package com.zangjiuqi.core;

public final class BoardRuleConfig {
    private final RuleMode mode;
    private final int boardSize;
    private final int boardPointCount;
    private final int flyPieceThreshold;
    private final int centerPointA;
    private final int centerPointB;
    private final boolean formationCapturesEnabled;
    private final TraditionalWinMode traditionalWinMode;

    private BoardRuleConfig(RuleMode mode) {
        this(mode, TraditionalWinMode.OFF);
    }

    private BoardRuleConfig(RuleMode mode, TraditionalWinMode traditionalWinMode) {
        this.mode = mode;
        this.boardSize = mode.boardSize();
        this.boardPointCount = boardSize * boardSize;
        this.flyPieceThreshold = boardSize;
        this.centerPointA = boardSize / 2 - 1;
        this.centerPointB = boardSize / 2;
        this.formationCapturesEnabled = mode == RuleMode.TRADITIONAL_BASIC;
        this.traditionalWinMode = mode == RuleMode.TRADITIONAL_BASIC
                ? traditionalWinMode == null ? TraditionalWinMode.OFF : traditionalWinMode
                : TraditionalWinMode.OFF;
    }

    public static BoardRuleConfig fromMode(RuleMode mode) {
        return new BoardRuleConfig(mode);
    }

    public static BoardRuleConfig fromMode(RuleMode mode, TraditionalWinMode traditionalWinMode) {
        return new BoardRuleConfig(mode, traditionalWinMode);
    }

    public RuleMode mode() {
        return mode;
    }

    public int boardSize() {
        return boardSize;
    }

    public int boardPointCount() {
        return boardPointCount;
    }

    public int flyPieceThreshold() {
        return flyPieceThreshold;
    }

    public int centerPointA() {
        return centerPointA;
    }

    public int centerPointB() {
        return centerPointB;
    }

    public boolean formationCapturesEnabled() {
        return formationCapturesEnabled;
    }

    public boolean traditionalPatternWinEnabled() {
        return traditionalWinMode != TraditionalWinMode.OFF;
    }

    public TraditionalWinMode traditionalWinMode() {
        return traditionalWinMode;
    }
}
