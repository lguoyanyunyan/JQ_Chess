package com.zangjiuqi.core;

public enum TraditionalWinMode {
    OFF("关闭"),
    FIXED_PATTERN_REQUIRED("固定棋形获胜"),
    FIRST_AUSPICIOUS_PATTERN("吉祥阵型获胜"),
    HANDICAP_TARGET_PATTERN("让棋指定阵型");

    private final String displayName;

    TraditionalWinMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
