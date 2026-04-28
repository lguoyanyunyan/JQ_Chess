package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;

public interface AiClient {
    String requestMove(BoardState state, int searchDepth, int timeoutSeconds);

    default void destroyHashtable() {
    }
}
