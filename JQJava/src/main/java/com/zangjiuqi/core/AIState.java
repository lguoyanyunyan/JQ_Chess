package com.zangjiuqi.core;

public enum AIState {
    EMBATTLE(1),
    MOVE_TRADITIONAL(2),
    MOVE_COMPETITIVE(3),
    FLY_TRADITIONAL(4),
    FLY_COMPETITIVE(5);

    private final byte code;

    AIState(int code) {
        this.code = (byte) code;
    }

    public byte code() {
        return code;
    }
}
