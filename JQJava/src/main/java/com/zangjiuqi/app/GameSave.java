package com.zangjiuqi.app;

import com.zangjiuqi.core.BoardState;

public record GameSave(
        boolean gameStarted,
        GameMode gameMode,
        AiBackend aiBackend,
        int searchDepth,
        int whiteTimeoutSeconds,
        int blackTimeoutSeconds,
        boolean showNumbers,
        BoardDirection boardDirection,
        BoardState.SaveState boardState
) {
}
