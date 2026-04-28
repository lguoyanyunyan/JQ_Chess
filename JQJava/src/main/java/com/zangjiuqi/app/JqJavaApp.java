package com.zangjiuqi.app;

import com.zangjiuqi.ai.AiMoveParser;
import com.zangjiuqi.ai.AiClient;
import com.zangjiuqi.ai.JavaAiClient;
import com.zangjiuqi.ai.NativeAiClient;
import com.zangjiuqi.ai.ValidatingAiClient;
import com.zangjiuqi.core.BoardPhase;
import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.AIState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.MoveCandidate;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinMode;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

public final class JqJavaApp extends Application {
    private static final double CANVAS_SIZE = 720.0;
    private static final double MARGIN = 42.0;
    private static final double BOARD_PIXELS = 620.0;
    private static final String PANEL_BACKGROUND = "#f3f3f3";
    private static final String PANEL_TEXT = "#202124";

    private final Canvas boardCanvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
    private final Label statusLabel = new Label();
    private final Label stateSummaryLabel = new Label();
    private final ComboBox<RuleMode> modeBox = new ComboBox<>();
    private final ComboBox<GameMode> gameModeBox = new ComboBox<>();
    private final ComboBox<AiBackend> aiBackendBox = new ComboBox<>();
    private final ComboBox<BoardDirection> boardDirectionBox = new ComboBox<>();
    private final ComboBox<TraditionalWinMode> traditionalWinModeBox = new ComboBox<>();
    private final Spinner<Integer> depthSpinner = new Spinner<>();
    private final Spinner<Integer> timeoutWhiteSpinner = new Spinner<>();
    private final Spinner<Integer> timeoutBlackSpinner = new Spinner<>();
    private final Button startButton = new Button("重新开始");
    private final Button finishButton = new Button("结束");
    private final Button continuePlacementButton = new Button("继续下子布局");
    private final Button movesFromBlackButton = new Button("从黑方开始行棋");
    private final Button movesFromWhiteButton = new Button("从白方开始行棋");
    private final Button aiButton = new Button("AI 下一步");
    private final Button undoButton = new Button("悔棋");
    private final Button loadButton = new Button("读取文件");
    private final Button saveButton = new Button("保存对局");
    private final CheckBox showNumberCheckBox = new CheckBox("显示落子编号");

    private BoardState boardState = new BoardState(RuleMode.COMPETITIVE);
    private AiClient whiteAiClient;
    private AiClient blackAiClient;
    private PauseTransition pendingAiPause;
    private boolean aiRunning;
    private boolean aiPausedAfterError;
    private boolean gameStarted;
    private String currentSaveName = "";

    @Override
    public void start(Stage stage) {
        modeBox.getItems().setAll(RuleMode.COMPETITIVE, RuleMode.TRADITIONAL_BASIC);
        modeBox.setValue(RuleMode.COMPETITIVE);
        modeBox.valueProperty().addListener((ignored, oldValue, newValue) -> resetBoardForRule(newValue));

        gameModeBox.getItems().setAll(GameMode.HUMAN_VS_HUMAN, GameMode.HUMAN_VS_AI, GameMode.AI_VS_HUMAN, GameMode.AI_VS_AI);
        gameModeBox.setValue(GameMode.HUMAN_VS_HUMAN);
        gameModeBox.valueProperty().addListener((ignored, oldValue, newValue) -> {
            refreshView();
            triggerAiIfNeeded();
        });
        aiBackendBox.getItems().setAll(AiBackend.JAVA, AiBackend.NATIVE, AiBackend.NATIVE_VALIDATED);
        aiBackendBox.setValue(AiBackend.JAVA);
        aiBackendBox.valueProperty().addListener((ignored, oldValue, newValue) -> {
            destroyAiClients();
            refreshView();
        });
        boardDirectionBox.getItems().setAll(BoardDirection.NORMAL, BoardDirection.ROTATED);
        boardDirectionBox.setValue(BoardDirection.NORMAL);
        boardDirectionBox.valueProperty().addListener((ignored, oldValue, newValue) -> refreshView());
        traditionalWinModeBox.getItems().setAll(TraditionalWinMode.values());
        traditionalWinModeBox.setValue(TraditionalWinMode.OFF);
        traditionalWinModeBox.valueProperty().addListener((ignored, oldValue, newValue) -> {
            if (newValue != null) {
                boardState.setTraditionalWinMode(newValue);
                refreshView();
            }
        });

        depthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 4));
        timeoutWhiteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 5));
        timeoutBlackSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300, 5));

        startButton.setOnAction(event -> startNewGame());
        finishButton.setOnAction(event -> finishGame());
        continuePlacementButton.setOnAction(event -> continuePlacement());
        movesFromBlackButton.setOnAction(event -> enterMovePhase(PieceColor.BLACK));
        movesFromWhiteButton.setOnAction(event -> enterMovePhase(PieceColor.WHITE));
        aiButton.setOnAction(event -> requestAiMove(true));
        undoButton.setOnAction(event -> undo());
        loadButton.setOnAction(event -> loadBoardFromFile(stage));
        saveButton.setOnAction(event -> saveGameToFile(stage));
        showNumberCheckBox.setOnAction(event -> refreshView());

        boardCanvas.setOnMouseClicked(event -> {
            if (!gameStarted || aiRunning || isAutomaticAiTurn()) {
                return;
            }
            if (event.getButton() == MouseButton.SECONDARY) {
                handleSecondaryClick();
            } else if (event.getButton() == MouseButton.PRIMARY) {
                handlePrimaryClick(event.getX(), event.getY());
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(boardCanvas);
        root.setRight(createControlPanel());
        root.setBottom(statusLabel);
        BorderPane.setMargin(boardCanvas, new Insets(10, 0, 10, 10));
        BorderPane.setMargin(statusLabel, new Insets(0, 10, 10, 10));

        statusLabel.setWrapText(true);
        stateSummaryLabel.setWrapText(true);
        refreshView("未开始。");

        stage.setTitle("藏久棋 Java 版");
        stage.setScene(new Scene(root));
        stage.setMinWidth(980);
        stage.setMinHeight(820);
        stage.show();
    }

    @Override
    public void stop() {
        cancelPendingAi();
        destroyAiClients();
    }

    private BorderPane createControlPanel() {
        VBox stateBox = new VBox(6, sectionTitle("当前状态"), stateSummaryLabel);
        stateBox.setPadding(new Insets(10, 10, 6, 10));
        stateBox.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");

        GridPane gameSettings = twoColumnGrid();
        addSettingRow(gameSettings, 0, "规则", modeBox);
        addSettingRow(gameSettings, 1, "模式", gameModeBox);
        addSettingRow(gameSettings, 2, "AI深度", depthSpinner);

        GridPane aiSettings = twoColumnGrid();
        addSettingRow(aiSettings, 0, "白方超时", timeoutWhiteSpinner);
        addSettingRow(aiSettings, 1, "黑方超时", timeoutBlackSpinner);
        addSettingRow(aiSettings, 2, "AI后端", aiBackendBox);
        addSettingRow(aiSettings, 3, "棋盘方向", boardDirectionBox);
        addSettingRow(aiSettings, 4, "传统胜负", traditionalWinModeBox);

        GridPane fileActions = buttonGrid();
        addButtonPair(fileActions, 0, loadButton, saveButton);

        GridPane gameActions = buttonGrid();
        addButtonPair(gameActions, 0, startButton, undoButton);
        addButtonPair(gameActions, 1, continuePlacementButton, aiButton);
        addButtonPair(gameActions, 2, movesFromBlackButton, movesFromWhiteButton);
        gameActions.add(finishButton, 0, 3, 2, 1);

        VBox scrollContent = new VBox(8,
                new Separator(),
                sectionTitle("对局设置"),
                gameSettings,
                sectionTitle("AI 与规则选项"),
                aiSettings,
                showNumberCheckBox,
                new Separator(),
                sectionTitle("文件"),
                fileActions,
                sectionTitle("操作"),
                gameActions
        );
        scrollContent.setPadding(new Insets(0, 10, 10, 10));
        scrollContent.setFillWidth(true);
        scrollContent.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");
        showNumberCheckBox.setTextFill(Color.web(PANEL_TEXT));

        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        BorderPane controls = new BorderPane();
        controls.setTop(stateBox);
        controls.setCenter(scrollPane);
        controls.setPrefWidth(280);
        controls.setMinWidth(250);
        controls.setStyle("-fx-background-color: " + PANEL_BACKGROUND + ";");
        stateSummaryLabel.setMaxWidth(Double.MAX_VALUE);
        stateSummaryLabel.setStyle("-fx-padding: 8; -fx-text-fill: " + PANEL_TEXT + "; -fx-background-color: #fafafa; -fx-border-color: #c8c8c8; -fx-border-radius: 4; -fx-background-radius: 4;");
        configureWideControls();
        return controls;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: " + PANEL_TEXT + ";");
        return label;
    }

    private GridPane twoColumnGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        ColumnConstraints left = new ColumnConstraints();
        left.setMinWidth(72);
        ColumnConstraints right = new ColumnConstraints();
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(left, right);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private GridPane buttonGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(left, right);
        grid.setMaxWidth(Double.MAX_VALUE);
        return grid;
    }

    private void addSettingRow(GridPane grid, int row, String labelText, Control control) {
        Label label = new Label(labelText);
        label.setMinWidth(72);
        label.setTextFill(Color.web(PANEL_TEXT));
        grid.add(label, 0, row);
        grid.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private void addButtonPair(GridPane grid, int row, Button left, Button right) {
        grid.add(left, 0, row);
        grid.add(right, 1, row);
        GridPane.setHgrow(left, Priority.ALWAYS);
        GridPane.setHgrow(right, Priority.ALWAYS);
    }

    private void configureWideControls() {
        for (Control control : List.of(
                modeBox,
                gameModeBox,
                depthSpinner,
                aiBackendBox,
                boardDirectionBox,
                traditionalWinModeBox,
                timeoutWhiteSpinner,
                timeoutBlackSpinner
        )) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
        for (Button button : List.of(
                loadButton,
                saveButton,
                startButton,
                undoButton,
                continuePlacementButton,
                movesFromBlackButton,
                movesFromWhiteButton,
                aiButton,
                finishButton
        )) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void resetBoardForRule(RuleMode mode) {
        cancelPendingAi();
        destroyAiClients();
        boardState = new BoardState(mode);
        if (mode != RuleMode.TRADITIONAL_BASIC) {
            traditionalWinModeBox.setValue(TraditionalWinMode.OFF);
        }
        aiRunning = false;
        aiPausedAfterError = false;
        gameStarted = false;
        currentSaveName = "";
        refreshView("规则已切换，棋局未开始。");
    }

    private void startNewGame() {
        cancelPendingAi();
        destroyAiClients();
        boardState = new BoardState(modeBox.getValue());
        boardState.setTraditionalWinMode(traditionalWinModeBox.getValue());
        aiRunning = false;
        aiPausedAfterError = false;
        gameStarted = true;
        currentSaveName = "";
        refreshView("新棋局开始。");
        triggerAiIfNeeded();
    }

    private void finishGame() {
        cancelPendingAi();
        destroyAiClients();
        gameStarted = false;
        aiRunning = false;
        aiPausedAfterError = false;
        refreshView("对局已结束。");
    }

    private void continuePlacement() {
        try {
            cancelPendingAi();
            aiPausedAfterError = false;
            boardState.continuePlacementPhase();
            gameStarted = true;
            refreshView("继续下子布局。");
            triggerAiIfNeeded();
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private void enterMovePhase(PieceColor firstColor) {
        try {
            cancelPendingAi();
            aiPausedAfterError = false;
            boardState.enterMovePhase(firstColor);
            gameStarted = true;
            refreshView("从" + firstColor.displayName() + "开始行棋。");
            triggerAiIfNeeded();
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private void loadBoardFromFile(Stage stage) {
        if (aiRunning) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("读取棋盘文件");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JQJava 对局", "*.jqj"),
                new FileChooser.ExtensionFilter("文本文件", "*.txt"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            GameSaveLoadResult result = GameSaveService.load(file.toPath(), modeBox.getValue());
            applyLoadedSave(result.save());
            aiPausedAfterError = false;
            currentSaveName = file.getName();
            refreshView((result.legacyText() ? "已读取旧棋盘文件：" : "已读取对局文件：") + file.getName());
            triggerAiIfNeeded();
        } catch (IOException ex) {
            refreshView("文件读取失败：" + ex.getMessage());
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private void saveGameToFile(Stage stage) {
        if (aiRunning) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("保存对局");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JQJava 对局", "*.jqj"),
                new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        chooser.setInitialFileName(currentSaveName == null || currentSaveName.isBlank() ? "zangjiuqi.jqj" : currentSaveName);
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        if (!file.getName().contains(".")) {
            file = new File(file.getParentFile(), file.getName() + ".jqj");
        }

        try {
            GameSaveService.save(file.toPath(), currentGameSave());
            currentSaveName = file.getName();
            refreshView("已保存对局：" + file.getName());
        } catch (IOException | RuntimeException ex) {
            refreshView("保存失败：" + ex.getMessage());
        }
    }

    private GameSave currentGameSave() {
        return new GameSave(
                gameStarted,
                gameModeBox.getValue(),
                aiBackendBox.getValue(),
                depthSpinner.getValue(),
                timeoutWhiteSpinner.getValue(),
                timeoutBlackSpinner.getValue(),
                showNumberCheckBox.isSelected(),
                boardDirectionBox.getValue(),
                boardState.saveState()
        );
    }

    private void applyLoadedSave(GameSave save) {
        cancelPendingAi();
        destroyAiClients();
        gameStarted = false;
        aiRunning = false;
        aiPausedAfterError = false;

        modeBox.setValue(save.boardState().mode());
        traditionalWinModeBox.setValue(save.boardState().traditionalWinMode());
        gameModeBox.setValue(save.gameMode());
        aiBackendBox.setValue(save.aiBackend());
        depthSpinner.getValueFactory().setValue(save.searchDepth());
        timeoutWhiteSpinner.getValueFactory().setValue(save.whiteTimeoutSeconds());
        timeoutBlackSpinner.getValueFactory().setValue(save.blackTimeoutSeconds());
        showNumberCheckBox.setSelected(save.showNumbers());
        boardDirectionBox.setValue(save.boardDirection());

        BoardState restored = new BoardState(save.boardState().mode());
        restored.restoreState(save.boardState());
        boardState = restored;
        gameStarted = save.gameStarted();
    }

    private void handlePrimaryClick(double mouseX, double mouseY) {
        Optional<BoardPoint> point = pointFromMouse(mouseX, mouseY);
        if (point.isEmpty()) {
            return;
        }
        try {
            boardState.handlePrimaryClick(point.get());
            aiPausedAfterError = false;
            refreshView();
            triggerAiIfNeeded();
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private void handleSecondaryClick() {
        try {
            boardState.handleSecondaryClick();
            aiPausedAfterError = false;
            refreshView();
            triggerAiIfNeeded();
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private void undo() {
        try {
            boardState.undo();
            aiPausedAfterError = false;
            refreshView();
        } catch (RuntimeException ex) {
            refreshView(ex.getMessage());
        }
    }

    private Optional<BoardPoint> pointFromMouse(double mouseX, double mouseY) {
        int size = boardState.ruleConfig().boardSize();
        double step = BOARD_PIXELS / (size - 1);
        int file = (int) Math.round((mouseX - MARGIN) / step);
        int rank = (int) Math.round((mouseY - MARGIN) / step);
        if (file < 0 || file >= size || rank < 0 || rank >= size) {
            return Optional.empty();
        }

        double pointX = MARGIN + file * step;
        double pointY = MARGIN + rank * step;
        if (Math.hypot(mouseX - pointX, mouseY - pointY) > step * 0.35) {
            return Optional.empty();
        }
        return Optional.of(new BoardPoint(boardCoordinate(file), boardCoordinate(rank)));
    }

    private void triggerAiIfNeeded() {
        cancelPendingAi();
        if (isAutomaticAiTurn()) {
            requestAiMove(false);
        }
    }

    private void scheduleNextAiIfNeeded() {
        cancelPendingAi();
        if (!isAutomaticAiTurn()) {
            return;
        }
        pendingAiPause = new PauseTransition(Duration.millis(gameModeBox.getValue() == GameMode.AI_VS_AI ? 650 : 250));
        pendingAiPause.setOnFinished(event -> {
            pendingAiPause = null;
            if (isAutomaticAiTurn()) {
                requestAiMove(false);
            }
        });
        pendingAiPause.play();
    }

    private void cancelPendingAi() {
        if (pendingAiPause != null) {
            pendingAiPause.stop();
            pendingAiPause = null;
        }
    }

    private void requestAiMove(boolean manual) {
        cancelPendingAi();
        if (aiRunning) {
            return;
        }
        if (manual) {
            aiPausedAfterError = false;
        }
        if (!gameStarted) {
            refreshView("对局未开始。");
            return;
        }
        if (boardState.phase() == BoardPhase.FINISHED) {
            refreshView("对局已经结束。");
            return;
        }

        aiRunning = true;
        refreshControls();
        refreshStateSummary();
        statusLabel.setText("AI 计算中...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return getAiClient(boardState.currentTurnColor()).requestMove(boardState, depthSpinner.getValue(), currentAiTimeout());
            }
        };

        task.setOnSucceeded(event -> {
            aiRunning = false;
            String rawMove = task.getValue();
            try {
                Move move = AiMoveParser.parse(rawMove, boardState.ruleConfig().boardSize());
                boardState.applyMove(move);
                refreshView("AI 执行：" + rawMove);
                scheduleNextAiIfNeeded();
            } catch (RuntimeException ex) {
                aiPausedAfterError = true;
                refreshView("AI 返回非法着法：" + rawMove + "；原因：" + ex.getMessage());
            }
        });

        task.setOnFailed(event -> {
            aiRunning = false;
            aiPausedAfterError = true;
            Throwable ex = task.getException();
            refreshView("AI 调用失败：" + (ex == null ? "未知错误" : ex.getMessage()));
        });

        Thread worker = new Thread(task, "jq-ai-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean isAutomaticAiTurn() {
        return gameStarted
                && !aiPausedAfterError
                && boardState.phase() != BoardPhase.FINISHED
                && gameModeBox.getValue().isAiControlled(boardState.currentTurnColor());
    }

    private int currentAiTimeout() {
        return boardState.currentTurnColor() == PieceColor.WHITE
                ? timeoutWhiteSpinner.getValue()
                : timeoutBlackSpinner.getValue();
    }

    private AiClient getAiClient(PieceColor color) {
        if (color == PieceColor.WHITE) {
            if (whiteAiClient == null) {
                whiteAiClient = createAiClient("jqai-white");
            }
            return whiteAiClient;
        }
        if (blackAiClient == null) {
            blackAiClient = createAiClient("jqai-black");
        }
        return blackAiClient;
    }

    private AiClient createAiClient(String instanceName) {
        AiBackend backend = aiBackendBox.getValue();
        if (backend == AiBackend.NATIVE) {
            return NativeAiClient.bundledIsolated(instanceName);
        }
        if (backend == AiBackend.NATIVE_VALIDATED) {
            return new ValidatingAiClient(NativeAiClient.bundledIsolated(instanceName), new JavaAiClient());
        }
        return new JavaAiClient();
    }

    private void destroyAiClients() {
        if (whiteAiClient != null) {
            whiteAiClient.destroyHashtable();
            whiteAiClient = null;
        }
        if (blackAiClient != null) {
            blackAiClient.destroyHashtable();
            blackAiClient = null;
        }
    }

    private void refreshView() {
        refreshView(null);
    }

    private void refreshView(String message) {
        drawBoard();
        refreshControls();
        refreshStateSummary();
        String suffix = isAutomaticAiTurn() && !aiRunning ? " | 等待 AI 行棋" : "";
        String stateText = gameStarted ? boardState.statusText() : "阶段：未开始";
        statusLabel.setText(message == null || message.isBlank()
                ? stateText + suffix
                : message + " | " + stateText + suffix);
    }

    private void refreshStateSummary() {
        stateSummaryLabel.setText(buildStateSummary());
    }

    private String buildStateSummary() {
        int[] pieceCounts = countPieces();
        StringBuilder summary = new StringBuilder();
        summary.append("对局：").append(gameStarted ? "进行中" : "未开始");
        if (aiRunning) {
            summary.append(" / AI计算中");
        } else if (aiPausedAfterError) {
            summary.append(" / AI已暂停");
        } else if (isAutomaticAiTurn()) {
            summary.append(" / 等待AI");
        }
        summary.append('\n');
        summary.append("规则：").append(ruleModeText(boardState.ruleConfig().mode()))
                .append("  ").append(boardState.ruleConfig().boardSize()).append("x")
                .append(boardState.ruleConfig().boardSize()).append('\n');
        summary.append("模式：").append(gameModeText(gameModeBox.getValue())).append('\n');
        summary.append("阶段：").append(phaseText()).append('\n');
        if (boardState.phase() == BoardPhase.FINISHED) {
            summary.append("结果：").append(resultText()).append('\n');
        } else if (gameStarted) {
            summary.append("轮到：").append(colorText(boardState.currentTurnColor())).append('\n');
        }
        summary.append("棋子：黑 ").append(pieceCounts[PieceColor.BLACK.code()])
                .append(" / 白 ").append(pieceCounts[PieceColor.WHITE.code()]);
        if (boardState.phase() == BoardPhase.SQUARE_CAPTURE) {
            summary.append('\n').append("待补吃：").append(boardState.pendingCaptureCount());
        }
        boardState.lastFormationMatch()
                .ifPresent(match -> summary.append('\n').append("阵型：").append(match.name())
                        .append("，可吃 ").append(match.captureCount()));
        boardState.selectedPoint()
                .ifPresent(point -> summary.append('\n').append("选中：").append(point));
        List<BoardPoint> lastPath = boardState.lastMovePath();
        if (!lastPath.isEmpty()) {
            summary.append('\n').append("上一手：").append(formatPath(lastPath));
        }
        if (currentSaveName != null && !currentSaveName.isBlank()) {
            summary.append('\n').append("文件：").append(currentSaveName);
        }
        return summary.toString();
    }

    private int[] countPieces() {
        int[] counts = new int[3];
        int[][] cells = boardState.snapshot();
        for (int file = 0; file < cells.length; file++) {
            for (int rank = 0; rank < cells[file].length; rank++) {
                int value = cells[file][rank];
                if (value > 0) {
                    counts[PieceColor.fromPieceValue(value).code()]++;
                }
            }
        }
        return counts;
    }

    private String resultText() {
        return boardState.gameResult().winner()
                .map(winner -> colorText(winner) + "获胜")
                .orElse("和棋");
    }

    private String formatPath(List<BoardPoint> path) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                text.append(" -> ");
            }
            text.append(path.get(i));
        }
        return text.toString();
    }

    private String ruleModeText(RuleMode mode) {
        return mode == RuleMode.COMPETITIVE ? "竞技化" : "传统基础";
    }

    private String gameModeText(GameMode mode) {
        if (mode == GameMode.HUMAN_VS_AI) {
            return "人机对战";
        }
        if (mode == GameMode.AI_VS_HUMAN) {
            return "机人对战";
        }
        if (mode == GameMode.AI_VS_AI) {
            return "机机对战";
        }
        return "人人对战";
    }

    private String phaseText() {
        if (boardState.phase() == BoardPhase.EMBATTLE) {
            return "布子";
        }
        if (boardState.phase() == BoardPhase.MOVE
                && (boardState.currentAiState() == AIState.FLY_COMPETITIVE
                || boardState.currentAiState() == AIState.FLY_TRADITIONAL)) {
            return "飞子";
        }
        if (boardState.phase() == BoardPhase.MOVE) {
            return "走子";
        }
        if (boardState.phase() == BoardPhase.SQUARE_CAPTURE) {
            return "补吃";
        }
        return "结束";
    }

    private String colorText(PieceColor color) {
        return color == PieceColor.WHITE ? "白方" : "黑方";
    }

    private void refreshControls() {
        boolean lock = aiRunning;
        boolean active = gameStarted && boardState.phase() != BoardPhase.FINISHED;
        modeBox.setDisable(lock || gameStarted);
        gameModeBox.setDisable(lock || gameStarted);
        aiBackendBox.setDisable(lock || gameStarted);
        boardDirectionBox.setDisable(lock);
        traditionalWinModeBox.setDisable(lock || gameStarted || modeBox.getValue() != RuleMode.TRADITIONAL_BASIC);
        depthSpinner.setDisable(lock || gameStarted);
        timeoutWhiteSpinner.setDisable(lock || gameStarted);
        timeoutBlackSpinner.setDisable(lock || gameStarted);
        loadButton.setDisable(lock);
        saveButton.setDisable(lock);
        startButton.setDisable(lock);
        finishButton.setDisable(lock || !active);
        continuePlacementButton.setDisable(lock || gameStarted);
        movesFromBlackButton.setDisable(lock || gameStarted);
        movesFromWhiteButton.setDisable(lock || gameStarted);
        aiButton.setDisable(lock || !active);
        undoButton.setDisable(lock || !active || !boardState.canUndo());
        showNumberCheckBox.setDisable(lock);
    }

    private void drawBoard() {
        GraphicsContext g = boardCanvas.getGraphicsContext2D();
        int size = boardState.ruleConfig().boardSize();
        double step = BOARD_PIXELS / (size - 1);

        g.setFill(Color.rgb(246, 231, 189));
        g.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        drawGrid(g, size, step);
        drawCenterDiagonal(g, step);

        drawLabels(g, size, step);

        int[][] cells = boardState.snapshot();
        for (int file = 0; file < size; file++) {
            for (int rank = 0; rank < size; rank++) {
                if (cells[file][rank] > 0) {
                    drawPiece(g, file, rank, cells[file][rank], step);
                }
            }
        }

        drawLastMove(g, step);
        boardState.lastPlacement().ifPresent(point -> drawFilledMarker(g, point, Color.BLUE, 6, step));
        drawTempPath(g, step);
        drawCandidates(g, step);
        drawCaptureMarks(g, boardState.lastMoveCaptures(), step);
        drawCaptureMarks(g, boardState.tempCaptures(), step);
        boardState.selectedPoint().ifPresent(point -> drawSelection(g, point, step));
    }

    private void drawGrid(GraphicsContext g, int size, double step) {
        g.setStroke(Color.rgb(28, 28, 28));
        for (int i = 0; i < size; i++) {
            double pos = MARGIN + i * step;
            g.setLineWidth(i == 0 || i == size - 1 ? 4 : 2);
            g.strokeLine(MARGIN, pos, MARGIN + BOARD_PIXELS, pos);
            g.strokeLine(pos, MARGIN, pos, MARGIN + BOARD_PIXELS);
        }
    }

    private void drawCenterDiagonal(GraphicsContext g, double step) {
        BoardPoint centerA = new BoardPoint(
                boardState.ruleConfig().centerPointA(),
                boardState.ruleConfig().centerPointA()
        );
        BoardPoint centerB = new BoardPoint(
                boardState.ruleConfig().centerPointB(),
                boardState.ruleConfig().centerPointB()
        );
        g.setStroke(Color.rgb(28, 28, 28));
        g.setLineWidth(2);
        g.strokeLine(pointX(centerA, step), pointY(centerA, step), pointX(centerB, step), pointY(centerB, step));
    }

    private void drawLabels(GraphicsContext g, int size, double step) {
        g.setFont(Font.font(13));
        g.setFill(Color.rgb(30, 30, 30));
        for (int i = 0; i < size; i++) {
            double pos = MARGIN + i * step;
            int boardIndex = boardCoordinate(i);
            g.fillText(String.valueOf(boardIndex + 1), pos - 4, 24);
            g.fillText(String.valueOf((char) ('A' + boardIndex)), 18, pos + 4);
        }
    }

    private void drawTempPath(GraphicsContext g, double step) {
        if (boardState.tempPath().size() < 2) {
            return;
        }
        g.setStroke(Color.rgb(40, 105, 190));
        g.setLineWidth(3);
        for (int i = 1; i < boardState.tempPath().size(); i++) {
            BoardPoint previous = boardState.tempPath().get(i - 1);
            BoardPoint current = boardState.tempPath().get(i);
            g.strokeLine(pointX(previous, step), pointY(previous, step), pointX(current, step), pointY(current, step));
        }
        drawFilledMarker(g, boardState.tempPath().get(boardState.tempPath().size() - 1), Color.BLUE, 6, step);
    }

    private void drawLastMove(GraphicsContext g, double step) {
        List<BoardPoint> path = boardState.lastMovePath();
        if (path.size() < 2) {
            return;
        }
        g.setStroke(Color.GREEN);
        g.setLineWidth(3);
        for (int i = 1; i < path.size(); i++) {
            BoardPoint previous = path.get(i - 1);
            BoardPoint current = path.get(i);
            g.strokeLine(pointX(previous, step), pointY(previous, step), pointX(current, step), pointY(current, step));
        }
        drawFilledMarker(g, path.get(path.size() - 1), Color.BLUE, 6, step);
    }

    private void drawCandidates(GraphicsContext g, double step) {
        for (MoveCandidate candidate : boardState.candidates()) {
            BoardPoint target = candidate.target();
            double radius = candidate.jump() ? 9 : 7;
            g.setFill(Color.rgb(38, 140, 78, 0.85));
            g.fillOval(pointX(target, step) - radius, pointY(target, step) - radius, radius * 2, radius * 2);
        }
    }

    private void drawCaptureMarks(GraphicsContext g, List<BoardPoint> captures, double step) {
        if (captures.isEmpty()) {
            return;
        }
        double radius = Math.min(18, step * 0.30);
        g.setStroke(Color.RED);
        g.setLineWidth(4);
        for (BoardPoint point : captures) {
            double x = pointX(point, step);
            double y = pointY(point, step);
            g.strokeLine(x - radius, y - radius, x + radius, y + radius);
            g.strokeLine(x - radius, y + radius, x + radius, y - radius);
        }
    }

    private void drawFilledMarker(GraphicsContext g, BoardPoint point, Color color, double radius, double step) {
        g.setFill(color);
        g.fillOval(pointX(point, step) - radius, pointY(point, step) - radius, radius * 2, radius * 2);
    }

    private void drawSelection(GraphicsContext g, BoardPoint point, double step) {
        double radius = Math.min(24, step * 0.42);
        g.setStroke(Color.rgb(40, 105, 190));
        g.setLineWidth(3);
        g.strokeOval(pointX(point, step) - radius, pointY(point, step) - radius, radius * 2, radius * 2);
    }

    private void drawPiece(GraphicsContext g, int file, int rank, int value, double step) {
        BoardPoint point = new BoardPoint(file, rank);
        double x = pointX(point, step);
        double y = pointY(point, step);
        double radius = Math.min(18, step * 0.32);

        boolean white = value % 2 == 1;
        g.setFill(white ? Color.WHITE : Color.rgb(35, 35, 35));
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        g.setStroke(Color.rgb(40, 40, 40));
        g.setLineWidth(1.5);
        g.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

        if (showNumberCheckBox.isSelected()) {
            g.setFill(Color.RED);
            g.setFont(Font.font(12));
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(String.valueOf(value), x, y + 4);
            g.setTextAlign(TextAlignment.LEFT);
        }
    }

    private double pointX(BoardPoint point, double step) {
        return MARGIN + displayCoordinate(point.fileIndex()) * step;
    }

    private double pointY(BoardPoint point, double step) {
        return MARGIN + displayCoordinate(point.rankIndex()) * step;
    }

    private int displayCoordinate(int boardIndex) {
        return boardDirectionBox.getValue() == BoardDirection.ROTATED
                ? boardState.ruleConfig().boardSize() - 1 - boardIndex
                : boardIndex;
    }

    private int boardCoordinate(int displayIndex) {
        return boardDirectionBox.getValue() == BoardDirection.ROTATED
                ? boardState.ruleConfig().boardSize() - 1 - displayIndex
                : displayIndex;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
