package com.zangjiuqi.core;

import java.util.Optional;

public final class GameResult {
    private static final GameResult ONGOING = new GameResult(false, null, "");

    private final boolean finished;
    private final PieceColor winner;
    private final String reason;

    private GameResult(boolean finished, PieceColor winner, String reason) {
        this.finished = finished;
        this.winner = winner;
        this.reason = reason == null ? "" : reason;
    }

    public static GameResult ongoing() {
        return ONGOING;
    }

    public static GameResult finished(PieceColor winner, String reason) {
        return new GameResult(true, winner, reason);
    }

    public static GameResult draw(String reason) {
        return new GameResult(true, null, reason);
    }

    public boolean finished() {
        return finished;
    }

    public Optional<PieceColor> winner() {
        return Optional.ofNullable(winner);
    }

    public String reason() {
        return reason;
    }
}
