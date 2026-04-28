package com.zangjiuqi.app;

public enum BoardDirection {
    NORMAL("正常"),
    ROTATED("黑方视角");

    private final String displayName;

    BoardDirection(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
