package com.zangjiuqi.core;

public final class BoardRuleConfig {
    private final RuleMode mode;
    private final int boardSize;
    private final int boardPointCount;
    private final int flyPieceThreshold;
    private final int centerPointA;
    private final int centerPointB;
    private final boolean formationCapturesEnabled;
    private final TraditionalWinningPattern traditionalWinningPattern;

    private BoardRuleConfig(RuleMode mode) {
        this(mode, TraditionalWinningPattern.OFF);
    }

    private BoardRuleConfig(RuleMode mode, TraditionalWinningPattern traditionalWinningPattern) {
        this.mode = mode;
        this.boardSize = mode.boardSize();
        this.boardPointCount = boardSize * boardSize;
        this.flyPieceThreshold = boardSize;
        this.centerPointA = boardSize / 2 - 1;
        this.centerPointB = boardSize / 2;
        this.formationCapturesEnabled = mode == RuleMode.TRADITIONAL_BASIC;
        this.traditionalWinningPattern = mode == RuleMode.TRADITIONAL_BASIC
                ? traditionalWinningPattern == null ? TraditionalWinningPattern.OFF : traditionalWinningPattern
                : TraditionalWinningPattern.OFF;
    }

    public static BoardRuleConfig fromMode(RuleMode mode) {
        return new BoardRuleConfig(mode);
    }

    public static BoardRuleConfig fromMode(RuleMode mode, TraditionalWinningPattern traditionalWinningPattern) {
        return new BoardRuleConfig(mode, traditionalWinningPattern);
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

    public boolean traditionalWinningPatternEnabled() {
        return traditionalWinningPattern != TraditionalWinningPattern.OFF;
    }

    public TraditionalWinningPattern traditionalWinningPattern() {
        return traditionalWinningPattern;
    }
}
