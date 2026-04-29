package com.zangjiuqi;

import com.zangjiuqi.app.AiBackend;
import com.zangjiuqi.app.BoardDirection;
import com.zangjiuqi.app.GameMode;
import com.zangjiuqi.app.GameSave;
import com.zangjiuqi.app.GameSaveLoadResult;
import com.zangjiuqi.app.GameSaveService;
import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSaveServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void jqjSaveRoundTripsFullGameSettingsAndBoardState() throws Exception {
        BoardState state = moveState();
        state.handlePrimaryClick(p(1, 1));
        state.handlePrimaryClick(p(2, 1));
        GameSave save = new GameSave(
                true,
                GameMode.AI_VS_AI,
                AiBackend.JAVA,
                6,
                7,
                8,
                true,
                BoardDirection.ROTATED,
                state.saveState()
        );
        Path file = tempDir.resolve("game.jqj");

        GameSaveService.save(file, save);
        GameSaveLoadResult loaded = GameSaveService.load(file, RuleMode.COMPETITIVE);

        assertFalse(loaded.legacyText());
        assertEquals(GameMode.AI_VS_AI, loaded.save().gameMode());
        assertEquals(AiBackend.JAVA, loaded.save().aiBackend());
        assertEquals(6, loaded.save().searchDepth());
        assertEquals(BoardDirection.ROTATED, loaded.save().boardDirection());

        BoardState restored = new BoardState(RuleMode.TRADITIONAL_BASIC);
        restored.restoreState(loaded.save().boardState());
        assertArrayEquals(state.snapshot(), restored.snapshot());
        assertEquals(state.phase(), restored.phase());
        assertEquals(state.currentTurnColor(), restored.currentTurnColor());
        assertEquals(state.lastMovePath(), restored.lastMovePath());
    }

    @Test
    void legacyTextBoardStillLoadsByCurrentRuleMode() throws Exception {
        Path file = tempDir.resolve("board.txt");
        Files.writeString(file, String.join(System.lineSeparator(),
                "1 0 0 0 0 0 0 0",
                "0 2 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0"
        ), StandardCharsets.UTF_8);

        GameSaveLoadResult loaded = GameSaveService.load(file, RuleMode.COMPETITIVE);
        BoardState restored = new BoardState(RuleMode.COMPETITIVE);
        restored.restoreState(loaded.save().boardState());

        assertTrue(loaded.legacyText());
        assertFalse(loaded.save().gameStarted());
        assertEquals(BoardPhase.EMBATTLE, restored.phase());
        assertEquals(1, restored.get(p(0, 0)));
        assertEquals(2, restored.get(p(1, 1)));
        assertEquals(TraditionalWinMode.OFF, restored.traditionalWinMode());
    }

    @Test
    void jqjSaveRoundTripsEveryReservedTraditionalWinMode() throws Exception {
        for (TraditionalWinMode mode : List.of(
                TraditionalWinMode.FIXED_PATTERN_REQUIRED,
                TraditionalWinMode.FIRST_AUSPICIOUS_PATTERN,
                TraditionalWinMode.HANDICAP_TARGET_PATTERN
        )) {
            BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
            state.setTraditionalWinMode(mode);
            GameSave save = new GameSave(
                    false,
                    GameMode.HUMAN_VS_HUMAN,
                    AiBackend.JAVA,
                    4,
                    5,
                    5,
                    false,
                    BoardDirection.NORMAL,
                    state.saveState()
            );
            Path file = tempDir.resolve(mode.name() + ".jqj");

            GameSaveService.save(file, save);
            GameSaveLoadResult loaded = GameSaveService.load(file, RuleMode.TRADITIONAL_BASIC);

            BoardState restored = new BoardState(RuleMode.TRADITIONAL_BASIC);
            restored.restoreState(loaded.save().boardState());
            assertEquals(mode, restored.traditionalWinMode());
        }
    }

    @Test
    void jqjSaveKeepsCompetitiveTraditionalWinModeOff() throws Exception {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.setTraditionalWinMode(TraditionalWinMode.FIXED_PATTERN_REQUIRED);
        GameSave save = new GameSave(
                false,
                GameMode.HUMAN_VS_HUMAN,
                AiBackend.JAVA,
                4,
                5,
                5,
                false,
                BoardDirection.NORMAL,
                state.saveState()
        );
        Path file = tempDir.resolve("competitive-traditional-win.jqj");

        GameSaveService.save(file, save);
        GameSaveLoadResult loaded = GameSaveService.load(file, RuleMode.COMPETITIVE);

        BoardState restored = new BoardState(RuleMode.COMPETITIVE);
        restored.restoreState(loaded.save().boardState());
        assertEquals(RuleMode.COMPETITIVE, restored.ruleConfig().mode());
        assertEquals(TraditionalWinMode.OFF, restored.traditionalWinMode());
    }

    @Test
    void jqjSaveRoundTripsFormationCaptureState() throws Exception {
        BoardState state = formationCaptureState();
        GameSave save = new GameSave(
                true,
                GameMode.HUMAN_VS_HUMAN,
                AiBackend.JAVA,
                4,
                5,
                5,
                false,
                BoardDirection.NORMAL,
                state.saveState()
        );
        Path file = tempDir.resolve("formation-capture.jqj");

        GameSaveService.save(file, save);
        GameSaveLoadResult loaded = GameSaveService.load(file, RuleMode.COMPETITIVE);

        BoardState restored = new BoardState(RuleMode.COMPETITIVE);
        restored.restoreState(loaded.save().boardState());
        assertEquals(RuleMode.TRADITIONAL_BASIC, restored.ruleConfig().mode());
        assertEquals(BoardPhase.SQUARE_CAPTURE, restored.phase());
        assertEquals(PieceColor.BLACK, restored.currentTurnColor());
        assertEquals(2, restored.pendingCaptureCount());
        assertEquals("平门褡裢", restored.lastFormationMatch().orElseThrow().name());
        assertEquals(List.of(p(4, 2), p(4, 3)), restored.tempPath());
    }

    @Test
    void invalidJsonSaveIsRejectedBeforeCallerMutatesCurrentBoard() throws Exception {
        Path file = tempDir.resolve("bad.jqj");
        Files.writeString(file, "{\"format\":\"JQJavaSave\",\"version\":1,\"boardState\":{}}", StandardCharsets.UTF_8);
        BoardState current = moveState();
        int[][] before = current.snapshot();

        assertThrows(RuntimeException.class, () -> GameSaveService.load(file, RuleMode.COMPETITIVE));
        assertArrayEquals(before, current.snapshot());
    }

    @Test
    void corruptedJsonSaveIsRejected() throws Exception {
        Path file = tempDir.resolve("corrupted.jqj");
        Files.writeString(file, "{\"format\":\"JQJavaSave\"", StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> GameSaveService.load(file, RuleMode.COMPETITIVE));
    }

    @Test
    void missingRequiredJsonFieldIsRejected() throws Exception {
        Path file = tempDir.resolve("missing-field.jqj");
        Files.writeString(file, """
                {
                  "format": "JQJavaSave",
                  "version": 1,
                  "gameStarted": false
                }
                """, StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> GameSaveService.load(file, RuleMode.COMPETITIVE));
    }

    @Test
    void legacyTextBoardRejectsRuleModeSizeMismatch() throws Exception {
        Path file = tempDir.resolve("small-board.txt");
        Files.writeString(file, String.join(System.lineSeparator(),
                "1 0 0 0 0 0 0 0",
                "0 2 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0",
                "0 0 0 0 0 0 0 0"
        ), StandardCharsets.UTF_8);

        assertThrows(IllegalArgumentException.class, () -> GameSaveService.load(file, RuleMode.TRADITIONAL_BASIC));
    }

    private static BoardState moveState() {
        BoardState state = new BoardState(RuleMode.COMPETITIVE);
        state.enterMovePhaseForTesting(1);
        state.putForTesting(p(1, 1), 2);
        state.putForTesting(p(6, 0), 4);
        state.putForTesting(p(6, 2), 6);
        state.putForTesting(p(6, 4), 8);
        state.putForTesting(p(6, 6), 10);
        state.putForTesting(p(7, 1), 12);
        state.putForTesting(p(7, 3), 14);
        state.putForTesting(p(7, 5), 16);
        state.putForTesting(p(7, 7), 18);

        state.putForTesting(p(0, 6), 1);
        state.putForTesting(p(1, 6), 3);
        state.putForTesting(p(2, 6), 5);
        state.putForTesting(p(3, 6), 7);
        state.putForTesting(p(4, 6), 9);
        state.putForTesting(p(5, 6), 11);
        state.putForTesting(p(0, 7), 13);
        state.putForTesting(p(2, 7), 15);
        state.putForTesting(p(4, 7), 17);
        return state;
    }

    private static BoardState formationCaptureState() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : List.of(
                p(3, 1), p(4, 1), p(3, 2), p(3, 3), p(3, 4), p(4, 4), p(4, 2)
        )) {
            state.putForTesting(point, 2);
        }
        for (BoardPoint point : List.of(p(0, 6), p(1, 6), p(2, 6), p(3, 6))) {
            state.putForTesting(point, 1);
        }
        state.handlePrimaryClick(p(4, 2));
        state.handlePrimaryClick(p(4, 3));
        return state;
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }
}
