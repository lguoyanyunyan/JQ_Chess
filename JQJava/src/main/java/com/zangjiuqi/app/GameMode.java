package com.zangjiuqi.app;

import com.zangjiuqi.core.PieceColor;

public enum GameMode {
    HUMAN_VS_HUMAN("人人对战", false, false),
    HUMAN_VS_AI("人机对战", false, true),
    AI_VS_HUMAN("机人对战", true, false),
    AI_VS_AI("机机对战", true, true);

    private final String displayName;
    private final boolean whiteAi;
    private final boolean blackAi;

    GameMode(String displayName, boolean whiteAi, boolean blackAi) {
        this.displayName = displayName;
        this.whiteAi = whiteAi;
        this.blackAi = blackAi;
    }

    public boolean isAiControlled(PieceColor color) {
        return color == PieceColor.WHITE ? whiteAi : blackAi;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
