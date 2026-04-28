package com.zangjiuqi.app;

public enum AiBackend {
    JAVA("\u7eaf Java AI"),
    NATIVE("\u539f\u751f AI"),
    NATIVE_VALIDATED("\u539f\u751f AI + Java \u6821\u9a8c");

    private final String displayName;

    AiBackend(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
