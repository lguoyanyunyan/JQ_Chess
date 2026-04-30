package com.zangjiuqi.core;

public enum TraditionalWinningPattern {
    OFF("关闭"),
    LHASA("拉萨");

    private final String displayName;

    TraditionalWinningPattern(String displayName) {
        this.displayName = displayName;
    }

    public boolean matches(FormationMatch match) {
        if (this == OFF || match == null) {
            return false;
        }
        return this == LHASA && match.name().contains("拉萨");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
