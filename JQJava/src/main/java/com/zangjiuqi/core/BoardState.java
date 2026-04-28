package com.zangjiuqi.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BoardState {
    private static final int COMPETITIVE_LOSE_PIECE_COUNT = 4;
    private static final int REPETITION_DRAW_THRESHOLD = 3;
    private static final String REPETITION_DRAW_REASON = "重复局面和棋";
    private static final int[][] ORTHOGONAL_DIRECTIONS = {
            {1, 0},
            {0, -1},
            {-1, 0},
            {0, 1}
    };

    private BoardRuleConfig ruleConfig;
    private int[][] cells;
    private int turn;
    private int sequence;
    private BoardPhase phase;
    private GameResult gameResult;

    private final List<PlacementRecord> placementHistory = new ArrayList<>();
    private final List<MoveRecord> moveHistory = new ArrayList<>();
    private final List<BoardPoint> tempPath = new ArrayList<>();
    private final List<PieceRecord> tempCaptures = new ArrayList<>();
    private final List<MoveCandidate> candidates = new ArrayList<>();
    private final Map<String, Integer> repetitionCounts = new HashMap<>();

    private int tempPiece;
    private int needSquareCaptureCount;
    private FormationMatch lastFormationMatch;
    private PieceRecord clearedCenterA;
    private PieceRecord clearedCenterB;

    public BoardState(RuleMode mode) {
        reset(mode);
    }

    public void reset(RuleMode mode) {
        this.ruleConfig = BoardRuleConfig.fromMode(mode);
        this.cells = new int[ruleConfig.boardSize()][ruleConfig.boardSize()];
        this.turn = 1;
        this.sequence = 0;
        this.phase = BoardPhase.EMBATTLE;
        this.gameResult = GameResult.ongoing();
        this.placementHistory.clear();
        this.moveHistory.clear();
        clearTempMove();
        this.clearedCenterA = null;
        this.clearedCenterB = null;
        resetRepetitionTracking();
    }

    public BoardRuleConfig ruleConfig() {
        return ruleConfig;
    }

    public TraditionalWinMode traditionalWinMode() {
        return ruleConfig.traditionalWinMode();
    }

    public void setTraditionalWinMode(TraditionalWinMode traditionalWinMode) {
        this.ruleConfig = BoardRuleConfig.fromMode(ruleConfig.mode(), traditionalWinMode);
        resetRepetitionTracking();
    }

    public int turn() {
        return turn;
    }

    public BoardPhase phase() {
        return phase;
    }

    public GameResult gameResult() {
        return gameResult;
    }

    public Optional<BoardPoint> selectedPoint() {
        if (tempPath.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tempPath.get(tempPath.size() - 1));
    }

    public List<MoveCandidate> candidates() {
        return Collections.unmodifiableList(candidates);
    }

    public List<BoardPoint> tempPath() {
        return Collections.unmodifiableList(tempPath);
    }

    public List<BoardPoint> tempCaptures() {
        return capturePoints(tempCaptures);
    }

    public int pendingCaptureCount() {
        return needSquareCaptureCount;
    }

    public Optional<FormationMatch> lastFormationMatch() {
        return Optional.ofNullable(lastFormationMatch);
    }

    public Optional<BoardPoint> lastPlacement() {
        if (phase != BoardPhase.EMBATTLE || placementHistory.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(placementHistory.get(placementHistory.size() - 1).point);
    }

    public List<BoardPoint> lastMovePath() {
        if (moveHistory.isEmpty()) {
            return List.of();
        }
        return List.copyOf(moveHistory.get(moveHistory.size() - 1).path);
    }

    public List<BoardPoint> lastMoveCaptures() {
        if (moveHistory.isEmpty()) {
            return List.of();
        }
        return capturePoints(moveHistory.get(moveHistory.size() - 1).captures);
    }

    public boolean canUndo() {
        return !placementHistory.isEmpty() || !moveHistory.isEmpty() || !tempPath.isEmpty();
    }

    public PieceColor currentTurnColor() {
        if (phase == BoardPhase.EMBATTLE) {
            return turn == 1 ? PieceColor.WHITE : PieceColor.BLACK;
        }
        return movePhaseColorForTurn(turn);
    }

    public String statusText() {
        if (phase == BoardPhase.FINISHED) {
            return gameResult.winner()
                    .map(color -> color.displayName() + "获胜：" + gameResult.reason())
                    .orElse(gameResult.reason());
        }
        if (phase == BoardPhase.EMBATTLE) {
            return "布子阶段，" + currentTurnColor().displayName() + "落子";
        }
        if (phase == BoardPhase.SQUARE_CAPTURE && lastFormationMatch != null) {
            return "补吃阶段，阵型：" + lastFormationMatch.name() + "，还需移除 " + needSquareCaptureCount + " 枚对方棋子";
        }
        if (phase == BoardPhase.SQUARE_CAPTURE) {
            return "成方补吃阶段，还需移除 " + needSquareCaptureCount + " 枚对方棋子";
        }
        if (currentSideCanFly(turn)) {
            return "飞子阶段，" + currentTurnColor().displayName() + "行棋";
        }
        return "走子阶段，" + currentTurnColor().displayName() + "行棋";
    }

    public int get(BoardPoint point) {
        requireInside(point);
        return cells[point.fileIndex()][point.rankIndex()];
    }

    public void handlePrimaryClick(BoardPoint point) {
        requirePlayable();
        requireInside(point);
        if (phase == BoardPhase.EMBATTLE) {
            place(point);
            return;
        }
        if (phase == BoardPhase.MOVE) {
            handleMovePhaseClick(point);
            return;
        }
        if (phase == BoardPhase.SQUARE_CAPTURE) {
            captureSquarePiece(point);
        }
    }

    public void handleSecondaryClick() {
        requirePlayable();
        if (phase != BoardPhase.MOVE) {
            return;
        }
        if (tempPath.size() > 1) {
            finishMoveIfAllowed();
        } else {
            clearTempMove();
        }
    }

    public void applyMove(Move move) {
        StateSnapshot snapshot = StateSnapshot.capture(this);
        try {
            if (phase == BoardPhase.EMBATTLE) {
                applyPlacementMove(move);
            } else if (phase == BoardPhase.MOVE) {
                applyPathMove(move);
            } else if (phase == BoardPhase.SQUARE_CAPTURE) {
                applySquareCaptures(move);
            } else {
                throw new IllegalStateException("Current phase does not accept moves.");
            }
        } catch (RuntimeException ex) {
            snapshot.restore(this);
            throw ex;
        }
    }

    public void undo() {
        if (tempPath.size() > 1 || phase == BoardPhase.SQUARE_CAPTURE) {
            restoreTempMove();
            return;
        }
        if (!tempPath.isEmpty()) {
            clearTempMove();
            return;
        }
        if (!moveHistory.isEmpty()) {
            undoLastMove();
            return;
        }
        if (!placementHistory.isEmpty()) {
            undoLastPlacement();
        }
    }

    public void place(BoardPoint point) {
        requirePlayable();
        requireInside(point);
        if (phase != BoardPhase.EMBATTLE) {
            throw new IllegalStateException("Current phase does not accept placement.");
        }
        if (cells[point.fileIndex()][point.rankIndex()] != 0) {
            throw new IllegalArgumentException("Point is already occupied: " + point);
        }
        if (sequence < 2 && !isCenterPoint(point)) {
            throw new IllegalArgumentException("The first two placements must use the center points.");
        }

        sequence++;
        cells[point.fileIndex()][point.rankIndex()] = sequence;
        placementHistory.add(new PlacementRecord(point, sequence));
        turn = 1 - turn;

        if (sequence == ruleConfig.boardPointCount()) {
            finishEmbattlePhase();
        }
    }

    public void continuePlacementPhase() {
        this.phase = BoardPhase.EMBATTLE;
        this.gameResult = GameResult.ongoing();
        this.moveHistory.clear();
        this.placementHistory.clear();
        this.clearedCenterA = null;
        this.clearedCenterB = null;
        clearTempMove();
        resetPlacementSequenceFromBoard();
        resetRepetitionTracking();
    }

    public void enterMovePhase(PieceColor firstColor) {
        if (firstColor == null) {
            throw new IllegalArgumentException("First move color is required.");
        }
        this.turn = firstColor == PieceColor.BLACK ? 1 : 0;
        this.phase = BoardPhase.MOVE;
        this.gameResult = GameResult.ongoing();
        clearTempMove();
        resetRepetitionTracking();
    }

    public void loadTextBoard(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Board text is required.");
        }

        int size = ruleConfig.boardSize();
        String[] rawLines = text.split("\\R");
        List<String> lines = new ArrayList<>();
        for (String rawLine : rawLines) {
            String line = rawLine.trim();
            if (!line.isEmpty()) {
                lines.add(line);
            }
        }
        if (lines.size() != size) {
            throw boardSizeMismatch();
        }

        int[][] loaded = new int[size][size];
        for (int rank = 0; rank < size; rank++) {
            String[] tokens = lines.get(rank).split("\\s+");
            if (tokens.length != size) {
                throw boardSizeMismatch();
            }
            for (int file = 0; file < size; file++) {
                String token = tokens[file];
                if (!"0".equals(token) && !"1".equals(token) && !"2".equals(token)) {
                    throw new IllegalArgumentException("棋盘文件只能包含 0、1、2：" + token);
                }
                loaded[file][rank] = Integer.parseInt(token);
            }
        }
        loadBoardCodes(loaded);
    }

    public void loadBoardCodes(int[][] encodedCells) {
        int size = ruleConfig.boardSize();
        if (encodedCells == null || encodedCells.length != size) {
            throw boardSizeMismatch();
        }

        int[][] loaded = new int[size][size];
        for (int file = 0; file < size; file++) {
            if (encodedCells[file] == null || encodedCells[file].length != size) {
                throw boardSizeMismatch();
            }
            for (int rank = 0; rank < size; rank++) {
                int value = encodedCells[file][rank];
                if (value < 0 || value > 2) {
                    throw new IllegalArgumentException("棋盘编码只能是 0、1、2：" + value);
                }
                loaded[file][rank] = value;
            }
        }

        this.cells = loaded;
        this.phase = BoardPhase.EMBATTLE;
        this.gameResult = GameResult.ongoing();
        this.placementHistory.clear();
        this.moveHistory.clear();
        this.clearedCenterA = null;
        this.clearedCenterB = null;
        clearTempMove();
        resetPlacementSequenceFromBoard();
        resetRepetitionTracking();
    }

    public void setPhase(BoardPhase phase) {
        this.phase = phase;
    }

    public byte[] toAiBoard() {
        int size = ruleConfig.boardSize();
        byte[] result = new byte[ruleConfig.boardPointCount()];
        for (int rank = 0; rank < size; rank++) {
            for (int file = 0; file < size; file++) {
                int value = cells[file][rank];
                result[rank * size + file] = value > 0 ? (byte) (2 - value % 2) : 0;
            }
        }
        return result;
    }

    public int[][] snapshot() {
        return copyCells(cells);
    }

    public BoardState copyForSimulation() {
        BoardState copy = new BoardState(ruleConfig.mode());
        StateSnapshot.capture(this).restore(copy);
        return copy;
    }

    public SaveState saveState() {
        return new SaveState(
                ruleConfig.mode(),
                ruleConfig.traditionalWinMode(),
                cells,
                turn,
                sequence,
                phase,
                gameResult.finished(),
                gameResult.winner().orElse(null),
                gameResult.reason(),
                placementHistory.stream()
                        .map(record -> new PlacementSnapshot(record.point, record.pieceValue))
                        .toList(),
                moveHistory.stream()
                        .map(record -> new MoveSnapshot(
                                record.pieceValue,
                                record.path,
                                pieceSnapshots(record.captures)
                        ))
                        .toList(),
                tempPath,
                pieceSnapshots(tempCaptures),
                candidates.stream()
                        .map(candidate -> new MoveCandidateSnapshot(
                                candidate.target(),
                                candidate.capturedPoint().orElse(null)
                        ))
                        .toList(),
                tempPiece,
                needSquareCaptureCount,
                lastFormationMatch == null ? null : new FormationSnapshot(
                        lastFormationMatch.name(),
                        lastFormationMatch.triggerPoint(),
                        lastFormationMatch.color(),
                        lastFormationMatch.captureCount(),
                        lastFormationMatch.points()
                ),
                clearedCenterA == null ? null : new PieceSnapshot(clearedCenterA.point, clearedCenterA.pieceValue),
                clearedCenterB == null ? null : new PieceSnapshot(clearedCenterB.point, clearedCenterB.pieceValue),
                repetitionCounts
        );
    }

    public void restoreState(SaveState saveState) {
        if (saveState == null) {
            throw new IllegalArgumentException("Save state is required.");
        }
        BoardRuleConfig restoredConfig = BoardRuleConfig.fromMode(saveState.mode(), saveState.traditionalWinMode());
        requireBoardShape(saveState.cells(), restoredConfig.boardSize());

        this.ruleConfig = restoredConfig;
        this.cells = copyCells(saveState.cells());
        this.turn = saveState.turn();
        this.sequence = saveState.sequence();
        this.phase = saveState.phase();
        this.gameResult = saveState.gameFinished()
                ? saveState.winner() == null
                ? GameResult.draw(saveState.resultReason())
                : GameResult.finished(saveState.winner(), saveState.resultReason())
                : GameResult.ongoing();
        this.placementHistory.clear();
        for (PlacementSnapshot placement : saveState.placementHistory()) {
            this.placementHistory.add(new PlacementRecord(placement.point(), placement.pieceValue()));
        }
        this.moveHistory.clear();
        for (MoveSnapshot move : saveState.moveHistory()) {
            this.moveHistory.add(new MoveRecord(
                    move.pieceValue(),
                    new ArrayList<>(move.path()),
                    pieceRecords(move.captures())
            ));
        }
        this.tempPath.clear();
        this.tempPath.addAll(saveState.tempPath());
        this.tempCaptures.clear();
        this.tempCaptures.addAll(pieceRecords(saveState.tempCaptures()));
        this.candidates.clear();
        for (MoveCandidateSnapshot candidate : saveState.candidates()) {
            this.candidates.add(new MoveCandidate(candidate.target(), candidate.capturedPoint()));
        }
        this.tempPiece = saveState.tempPiece();
        this.needSquareCaptureCount = saveState.pendingCaptureCount();
        this.lastFormationMatch = saveState.lastFormationMatch() == null ? null : new FormationMatch(
                saveState.lastFormationMatch().name(),
                saveState.lastFormationMatch().triggerPoint(),
                saveState.lastFormationMatch().color(),
                saveState.lastFormationMatch().captureCount(),
                saveState.lastFormationMatch().points()
        );
        this.clearedCenterA = saveState.clearedCenterA() == null ? null
                : new PieceRecord(saveState.clearedCenterA().point(), saveState.clearedCenterA().pieceValue());
        this.clearedCenterB = saveState.clearedCenterB() == null ? null
                : new PieceRecord(saveState.clearedCenterB().point(), saveState.clearedCenterB().pieceValue());
        this.repetitionCounts.clear();
        this.repetitionCounts.putAll(saveState.repetitionCounts());
    }

    public AIState currentAiState() {
        if (phase == BoardPhase.EMBATTLE) {
            return AIState.EMBATTLE;
        }
        if (currentSideCanFly(turn)) {
            return ruleConfig.mode() == RuleMode.COMPETITIVE ? AIState.FLY_COMPETITIVE : AIState.FLY_TRADITIONAL;
        }
        return ruleConfig.mode() == RuleMode.COMPETITIVE ? AIState.MOVE_COMPETITIVE : AIState.MOVE_TRADITIONAL;
    }

    public void putForTesting(BoardPoint point, int pieceValue) {
        requireInside(point);
        cells[point.fileIndex()][point.rankIndex()] = pieceValue;
        sequence = Math.max(sequence, pieceValue);
        resetRepetitionTracking();
    }

    public void enterMovePhaseForTesting(int turn) {
        this.turn = turn;
        this.phase = BoardPhase.MOVE;
        this.gameResult = GameResult.ongoing();
        clearTempMove();
        resetRepetitionTracking();
    }

    private void applyPlacementMove(Move move) {
        if (!move.isPlacement() || !move.jumpCaptures().isEmpty() || !move.squareCaptures().isEmpty()) {
            throw new IllegalArgumentException("Embattle AI move must be a single placement point.");
        }
        place(move.path().get(0));
    }

    private void applyPathMove(Move move) {
        if (move.path().size() < 2) {
            throw new IllegalArgumentException("Move-phase AI move must contain at least a start and end point.");
        }
        for (BoardPoint point : move.path()) {
            handlePrimaryClick(point);
        }
        if (phase == BoardPhase.MOVE && tempPath.size() > 1) {
            handleSecondaryClick();
        }
        if (!move.squareCaptures().isEmpty()) {
            applySquareCaptures(move);
        }
        if (phase == BoardPhase.SQUARE_CAPTURE) {
            throw new IllegalArgumentException("AI move formed a square but did not provide enough FC captures.");
        }
    }

    private void applySquareCaptures(Move move) {
        if (move.squareCaptures().isEmpty()) {
            throw new IllegalArgumentException("Square-capture phase requires FC captures.");
        }
        if (phase != BoardPhase.SQUARE_CAPTURE) {
            applyAuthoritativeAiSquareCaptures(move.squareCaptures());
            return;
        }
        for (BoardPoint point : move.squareCaptures()) {
            handlePrimaryClick(point);
        }
    }

    private void applyAuthoritativeAiSquareCaptures(List<BoardPoint> squareCaptures) {
        if (phase != BoardPhase.MOVE || moveHistory.isEmpty()) {
            throw new IllegalArgumentException("AI move provided FC captures outside a completed move.");
        }

        MoveRecord lastMove = moveHistory.get(moveHistory.size() - 1);
        PieceColor movingColor = PieceColor.fromPieceValue(lastMove.pieceValue);
        for (BoardPoint point : squareCaptures) {
            requireInside(point);
            int value = get(point);
            if (value <= 0) {
                throw new IllegalArgumentException("AI FC target is empty: " + point);
            }
            if (PieceColor.fromPieceValue(value) == movingColor) {
                throw new IllegalArgumentException("AI FC target is not an opponent piece: " + point);
            }
            lastMove.captures.add(new PieceRecord(point, value));
            cells[point.fileIndex()][point.rankIndex()] = 0;
        }
        resetRepetitionTracking();
        finishFromRulesIfNeeded();
    }

    private void handleMovePhaseClick(BoardPoint point) {
        int value = get(point);
        if (tempPath.size() < 2 && value > 0 && PieceColor.fromPieceValue(value) == currentTurnColor()) {
            startTempMove(point, value);
            return;
        }
        if (tempPath.isEmpty() || value != 0) {
            throw new IllegalArgumentException("Point is not a legal move target: " + point);
        }

        MoveCandidate candidate = findCandidate(point);
        if (candidate == null) {
            throw new IllegalArgumentException("Point is not a legal move target: " + point);
        }
        applyCandidate(candidate);
    }

    private void startTempMove(BoardPoint point, int value) {
        clearTempMove();
        lastFormationMatch = null;
        tempPiece = value;
        tempPath.add(point);
        candidates.addAll(selectableTargets(point, value, null));
        if (candidates.isEmpty()) {
            clearTempMove();
            throw new IllegalArgumentException("Selected piece has no legal moves: " + point);
        }
    }

    private void applyCandidate(MoveCandidate candidate) {
        BoardPoint from = tempPath.get(tempPath.size() - 1);
        BoardPoint to = candidate.target();
        boolean canFly = currentSideCanFly(turn);

        cells[from.fileIndex()][from.rankIndex()] = 0;
        cells[to.fileIndex()][to.rankIndex()] = tempPiece;
        candidates.clear();

        if (!canFly && candidate.jump()) {
            BoardPoint captured = candidate.capturedPoint().orElseThrow();
            int capturedValue = get(captured);
            tempCaptures.add(new PieceRecord(captured, capturedValue));
            cells[captured.fileIndex()][captured.rankIndex()] = 0;
            candidates.addAll(followUpJumpTargets(to, from, tempPiece));
        }

        tempPath.add(to);
        if (canFly || candidates.isEmpty()) {
            finishMoveIfAllowed();
        }
    }

    private void finishMoveIfAllowed() {
        if (phase != BoardPhase.MOVE || tempPath.size() <= 1) {
            return;
        }
        if (shouldRestrictSingleJump(turn) && tempCaptures.size() == 1) {
            throw new IllegalStateException("竞技化规则下，对手进入飞子后，强势方跳吃不能单吃。");
        }
        judgeSquareOrConfirmMove();
    }

    private void judgeSquareOrConfirmMove() {
        BoardPoint from = tempPath.get(0);
        BoardPoint to = tempPath.get(tempPath.size() - 1);
        int squareCaptureCount = squareCountAt(to);
        int formationCaptureCount = 0;
        lastFormationMatch = null;
        if (ruleConfig.formationCapturesEnabled()) {
            Optional<FormationMatch> match = FormationPatternDetector.bestMatch(
                    cells,
                    ruleConfig.boardSize(),
                    PieceColor.fromPieceValue(tempPiece),
                    from,
                    to
            );
            if (match.isPresent()) {
                lastFormationMatch = match.get();
                formationCaptureCount = lastFormationMatch.captureCount();
            }
        }
        needSquareCaptureCount = squareCaptureCount + formationCaptureCount;
        candidates.clear();
        if (needSquareCaptureCount > 0) {
            phase = BoardPhase.SQUARE_CAPTURE;
        } else {
            confirmTempMove();
        }
    }

    private void captureSquarePiece(BoardPoint point) {
        int value = get(point);
        if (value <= 0 || PieceColor.fromPieceValue(value) != currentTurnColor().opponent()) {
            throw new IllegalArgumentException("Square capture must remove an opponent piece.");
        }
        tempCaptures.add(new PieceRecord(point, value));
        cells[point.fileIndex()][point.rankIndex()] = 0;
        needSquareCaptureCount--;
        if (needSquareCaptureCount == 0) {
            confirmTempMove();
        }
    }

    private void confirmTempMove() {
        moveHistory.add(new MoveRecord(tempPiece, new ArrayList<>(tempPath), new ArrayList<>(tempCaptures)));
        clearTempMove();
        turn = 1 - turn;
        phase = BoardPhase.MOVE;
        if (!finishFromRulesIfNeeded()) {
            recordCurrentPosition();
        }
    }

    private void finishEmbattlePhase() {
        BoardPoint centerA = new BoardPoint(ruleConfig.centerPointA(), ruleConfig.centerPointA());
        BoardPoint centerB = new BoardPoint(ruleConfig.centerPointB(), ruleConfig.centerPointB());
        clearedCenterA = new PieceRecord(centerA, get(centerA));
        clearedCenterB = new PieceRecord(centerB, get(centerB));
        cells[centerA.fileIndex()][centerA.rankIndex()] = 0;
        cells[centerB.fileIndex()][centerB.rankIndex()] = 0;
        phase = BoardPhase.MOVE;
        clearTempMove();
        resetRepetitionTracking();
        finishFromRulesIfNeeded();
    }

    private List<MoveCandidate> selectableTargets(BoardPoint from, int pieceValue, BoardPoint previous) {
        if (currentSideCanFly(turn)) {
            List<MoveCandidate> result = new ArrayList<>();
            for (int file = 0; file < ruleConfig.boardSize(); file++) {
                for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
                    if (cells[file][rank] == 0) {
                        result.add(new MoveCandidate(new BoardPoint(file, rank), null));
                    }
                }
            }
            return result;
        }

        boolean restrictSingleJump = shouldRestrictSingleJump(turn);
        List<MoveCandidate> result = new ArrayList<>();
        for (int[] direction : ORTHOGONAL_DIRECTIONS) {
            int stepFile = from.fileIndex() + direction[0];
            int stepRank = from.rankIndex() + direction[1];
            if (!isInside(stepFile, stepRank)) {
                continue;
            }
            BoardPoint step = new BoardPoint(stepFile, stepRank);
            if (get(step) == 0) {
                result.add(new MoveCandidate(step, null));
            }

            int landingFile = from.fileIndex() + direction[0] * 2;
            int landingRank = from.rankIndex() + direction[1] * 2;
            if (!isInside(landingFile, landingRank)) {
                continue;
            }
            BoardPoint landing = new BoardPoint(landingFile, landingRank);
            if (previous != null && landing.equals(previous)) {
                continue;
            }
            if (get(step) > 0
                    && PieceColor.fromPieceValue(get(step)) != PieceColor.fromPieceValue(pieceValue)
                    && get(landing) == 0) {
                if (!restrictSingleJump || hasFollowUpJumpAfter(from, landing, pieceValue)) {
                    result.add(new MoveCandidate(landing, step));
                }
            }
        }
        return result;
    }

    private List<MoveCandidate> followUpJumpTargets(BoardPoint from, BoardPoint previous, int pieceValue) {
        List<MoveCandidate> result = new ArrayList<>();
        for (int[] direction : ORTHOGONAL_DIRECTIONS) {
            int middleFile = from.fileIndex() + direction[0];
            int middleRank = from.rankIndex() + direction[1];
            int landingFile = from.fileIndex() + direction[0] * 2;
            int landingRank = from.rankIndex() + direction[1] * 2;
            if (!isInside(middleFile, middleRank) || !isInside(landingFile, landingRank)) {
                continue;
            }
            BoardPoint middle = new BoardPoint(middleFile, middleRank);
            BoardPoint landing = new BoardPoint(landingFile, landingRank);
            if (previous != null && landing.equals(previous)) {
                continue;
            }
            if (get(middle) > 0
                    && PieceColor.fromPieceValue(get(middle)) != PieceColor.fromPieceValue(pieceValue)
                    && get(landing) == 0) {
                result.add(new MoveCandidate(landing, middle));
            }
        }
        return result;
    }

    private boolean hasFollowUpJumpAfter(BoardPoint from, BoardPoint landing, int pieceValue) {
        BoardPoint middle = new BoardPoint(
                (from.fileIndex() + landing.fileIndex()) / 2,
                (from.rankIndex() + landing.rankIndex()) / 2
        );
        int sourcePiece = get(from);
        int middlePiece = get(middle);
        int targetPiece = get(landing);

        cells[from.fileIndex()][from.rankIndex()] = 0;
        cells[middle.fileIndex()][middle.rankIndex()] = 0;
        cells[landing.fileIndex()][landing.rankIndex()] = pieceValue;
        boolean result = !followUpJumpTargets(landing, from, pieceValue).isEmpty();
        cells[from.fileIndex()][from.rankIndex()] = sourcePiece;
        cells[middle.fileIndex()][middle.rankIndex()] = middlePiece;
        cells[landing.fileIndex()][landing.rankIndex()] = targetPiece;
        return result;
    }

    private int squareCountAt(BoardPoint point) {
        int pieceValue = get(point);
        if (pieceValue <= 0) {
            return 0;
        }
        PieceColor color = PieceColor.fromPieceValue(pieceValue);
        int count = 0;
        int[] xInc = {1, -1, -1, 1};
        int[] yInc = {-1, -1, 1, 1};
        for (int i = 0; i < xInc.length; i++) {
            int diagonalFile = point.fileIndex() + xInc[i];
            int diagonalRank = point.rankIndex() + yInc[i];
            int horizontalFile = point.fileIndex() + xInc[i];
            int horizontalRank = point.rankIndex();
            int verticalFile = point.fileIndex();
            int verticalRank = point.rankIndex() + yInc[i];
            if (isInside(diagonalFile, diagonalRank)
                    && isInside(horizontalFile, horizontalRank)
                    && isInside(verticalFile, verticalRank)
                    && sameColor(new BoardPoint(diagonalFile, diagonalRank), color)
                    && sameColor(new BoardPoint(horizontalFile, horizontalRank), color)
                    && sameColor(new BoardPoint(verticalFile, verticalRank), color)) {
                count++;
            }
        }
        return count;
    }

    private boolean sameColor(BoardPoint point, PieceColor color) {
        int value = get(point);
        return value > 0 && PieceColor.fromPieceValue(value) == color;
    }

    private boolean finishFromRulesIfNeeded() {
        if (phase != BoardPhase.MOVE || gameResult.finished()) {
            return false;
        }
        int whiteCount = pieceCount(PieceColor.WHITE);
        int blackCount = pieceCount(PieceColor.BLACK);
        if (ruleConfig.mode() == RuleMode.COMPETITIVE) {
            if (whiteCount < COMPETITIVE_LOSE_PIECE_COUNT) {
                finish(PieceColor.BLACK, "白方棋子少于4枚");
                return true;
            }
            if (blackCount < COMPETITIVE_LOSE_PIECE_COUNT) {
                finish(PieceColor.WHITE, "黑方棋子少于4枚");
                return true;
            }
        }
        if (!hasAnyLegalMove(turn)) {
            PieceColor current = currentTurnColor();
            finish(current.opponent(), current.displayName() + "无合法着法");
            return true;
        }
        if (finishFromTraditionalWinIfNeeded()) {
            return true;
        }
        return false;
    }

    private boolean finishFromTraditionalWinIfNeeded() {
        if (!ruleConfig.traditionalPatternWinEnabled()) {
            return false;
        }
        // Traditional fixed/auspicious pattern wins are configured here but intentionally
        // left inactive until the corresponding pattern recognizers are implemented.
        return false;
    }

    private void finish(PieceColor winner, String reason) {
        phase = BoardPhase.FINISHED;
        gameResult = GameResult.finished(winner, reason);
        clearTempMove();
    }

    private void finishDraw(String reason) {
        phase = BoardPhase.FINISHED;
        gameResult = GameResult.draw(reason);
        clearTempMove();
    }

    private boolean hasAnyLegalMove(int currentTurn) {
        PieceColor currentColor = movePhaseColorForTurn(currentTurn);
        boolean currentCanFly = canSideFlyByColor(currentColor);
        boolean hasEmpty = hasEmptyPoint();
        if (currentCanFly && hasEmpty) {
            return true;
        }

        for (int file = 0; file < ruleConfig.boardSize(); file++) {
            for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
                BoardPoint point = new BoardPoint(file, rank);
                if (sameColor(point, currentColor) && !selectableTargets(point, get(point), null).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasEmptyPoint() {
        for (int file = 0; file < ruleConfig.boardSize(); file++) {
            for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
                if (cells[file][rank] == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private int pieceCount(PieceColor color) {
        int count = 0;
        for (int file = 0; file < ruleConfig.boardSize(); file++) {
            for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
                int value = cells[file][rank];
                if (value > 0 && PieceColor.fromPieceValue(value) == color) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean currentSideCanFly(int currentTurn) {
        return canSideFlyByColor(movePhaseColorForTurn(currentTurn));
    }

    private boolean canSideFlyByColor(PieceColor color) {
        return (phase == BoardPhase.MOVE || phase == BoardPhase.SQUARE_CAPTURE)
                && pieceCount(color) <= ruleConfig.flyPieceThreshold();
    }

    private boolean shouldRestrictSingleJump(int currentTurn) {
        PieceColor current = movePhaseColorForTurn(currentTurn);
        return ruleConfig.mode() == RuleMode.COMPETITIVE
                && !canSideFlyByColor(current)
                && canSideFlyByColor(current.opponent());
    }

    private PieceColor movePhaseColorForTurn(int currentTurn) {
        return currentTurn == 1 ? PieceColor.BLACK : PieceColor.WHITE;
    }

    private MoveCandidate findCandidate(BoardPoint target) {
        for (MoveCandidate candidate : candidates) {
            if (candidate.target().equals(target)) {
                return candidate;
            }
        }
        return null;
    }

    private void undoLastMove() {
        MoveRecord move = moveHistory.remove(moveHistory.size() - 1);
        BoardPoint start = move.path.get(0);
        BoardPoint end = move.path.get(move.path.size() - 1);
        cells[end.fileIndex()][end.rankIndex()] = 0;
        cells[start.fileIndex()][start.rankIndex()] = move.pieceValue;
        for (PieceRecord capture : move.captures) {
            cells[capture.point.fileIndex()][capture.point.rankIndex()] = capture.pieceValue;
        }
        turn = 1 - turn;
        phase = BoardPhase.MOVE;
        gameResult = GameResult.ongoing();
        clearTempMove();
        resetRepetitionTracking();
    }

    private void undoLastPlacement() {
        if (phase == BoardPhase.MOVE && moveHistory.isEmpty() && sequence == ruleConfig.boardPointCount()) {
            restoreClearedCenters();
            phase = BoardPhase.EMBATTLE;
        }
        PlacementRecord placement = placementHistory.remove(placementHistory.size() - 1);
        cells[placement.point.fileIndex()][placement.point.rankIndex()] = 0;
        sequence--;
        turn = 1 - turn;
        gameResult = GameResult.ongoing();
        clearTempMove();
        resetRepetitionTracking();
    }

    private void restoreTempMove() {
        if (tempPath.isEmpty()) {
            return;
        }
        for (PieceRecord capture : tempCaptures) {
            cells[capture.point.fileIndex()][capture.point.rankIndex()] = capture.pieceValue;
        }
        if (tempPath.size() > 1) {
            BoardPoint start = tempPath.get(0);
            BoardPoint end = tempPath.get(tempPath.size() - 1);
            cells[start.fileIndex()][start.rankIndex()] = tempPiece;
            cells[end.fileIndex()][end.rankIndex()] = 0;
        }
        phase = BoardPhase.MOVE;
        clearTempMove();
    }

    private void restoreClearedCenters() {
        if (clearedCenterA != null) {
            cells[clearedCenterA.point.fileIndex()][clearedCenterA.point.rankIndex()] = clearedCenterA.pieceValue;
        }
        if (clearedCenterB != null) {
            cells[clearedCenterB.point.fileIndex()][clearedCenterB.point.rankIndex()] = clearedCenterB.pieceValue;
        }
        clearedCenterA = null;
        clearedCenterB = null;
    }

    private void clearTempMove() {
        tempPath.clear();
        tempCaptures.clear();
        candidates.clear();
        tempPiece = 0;
        needSquareCaptureCount = 0;
        lastFormationMatch = null;
    }

    private void requirePlayable() {
        if (phase == BoardPhase.FINISHED) {
            throw new IllegalStateException("Game is already finished.");
        }
    }

    private void requireInside(BoardPoint point) {
        if (!point.isInside(ruleConfig.boardSize())) {
            throw new IllegalArgumentException("Point is outside board: " + point);
        }
    }

    private boolean isInside(int file, int rank) {
        return file >= 0 && file < ruleConfig.boardSize() && rank >= 0 && rank < ruleConfig.boardSize();
    }

    private boolean isCenterPoint(BoardPoint point) {
        return (point.fileIndex() == ruleConfig.centerPointA() && point.rankIndex() == ruleConfig.centerPointA())
                || (point.fileIndex() == ruleConfig.centerPointB() && point.rankIndex() == ruleConfig.centerPointB());
    }

    private void resetPlacementSequenceFromBoard() {
        int occupied = 0;
        int whiteCount = 0;
        int blackCount = 0;
        for (int file = 0; file < ruleConfig.boardSize(); file++) {
            for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
                int value = cells[file][rank];
                if (value <= 0) {
                    continue;
                }
                occupied++;
                if (PieceColor.fromPieceValue(value) == PieceColor.WHITE) {
                    whiteCount++;
                } else {
                    blackCount++;
                }
            }
        }
        this.sequence = occupied;
        this.turn = whiteCount <= blackCount ? 1 : 0;
    }

    private void resetRepetitionTracking() {
        repetitionCounts.clear();
        recordCurrentPosition();
    }

    private void recordCurrentPosition() {
        if (phase != BoardPhase.MOVE || gameResult.finished()) {
            return;
        }
        String positionKey = buildPositionKey();
        int seen = repetitionCounts.getOrDefault(positionKey, 0) + 1;
        repetitionCounts.put(positionKey, seen);
        if (seen >= REPETITION_DRAW_THRESHOLD) {
            finishDraw(REPETITION_DRAW_REASON);
        }
    }

    private String buildPositionKey() {
        StringBuilder key = new StringBuilder(ruleConfig.boardPointCount() + 24);
        key.append(ruleConfig.mode().name()).append('|');
        key.append(ruleConfig.traditionalWinMode().name()).append('|');
        key.append(phase.name()).append('|');
        key.append(turn).append('|');
        for (int rank = 0; rank < ruleConfig.boardSize(); rank++) {
            for (int file = 0; file < ruleConfig.boardSize(); file++) {
                key.append((char) ('0' + cells[file][rank]));
            }
        }
        return key.toString();
    }

    private IllegalArgumentException boardSizeMismatch() {
        int size = ruleConfig.boardSize();
        return new IllegalArgumentException("棋盘文件尺寸不匹配。当前规则需要 " + size + "x" + size + " 棋盘。");
    }

    private static int[][] copyCells(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }

    private static List<BoardPoint> capturePoints(List<PieceRecord> captures) {
        if (captures.isEmpty()) {
            return List.of();
        }
        List<BoardPoint> result = new ArrayList<>(captures.size());
        for (PieceRecord capture : captures) {
            result.add(capture.point);
        }
        return Collections.unmodifiableList(result);
    }

    private static List<MoveRecord> copyMoveHistory(List<MoveRecord> source) {
        List<MoveRecord> result = new ArrayList<>(source.size());
        for (MoveRecord move : source) {
            result.add(new MoveRecord(
                    move.pieceValue,
                    new ArrayList<>(move.path),
                    new ArrayList<>(move.captures)
            ));
        }
        return result;
    }

    private static List<PieceSnapshot> pieceSnapshots(List<PieceRecord> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<PieceSnapshot> result = new ArrayList<>(source.size());
        for (PieceRecord piece : source) {
            result.add(new PieceSnapshot(piece.point, piece.pieceValue));
        }
        return result;
    }

    private static List<PieceRecord> pieceRecords(List<PieceSnapshot> source) {
        if (source.isEmpty()) {
            return List.of();
        }
        List<PieceRecord> result = new ArrayList<>(source.size());
        for (PieceSnapshot piece : source) {
            result.add(new PieceRecord(piece.point(), piece.pieceValue()));
        }
        return result;
    }

    private static void requireBoardShape(int[][] board, int size) {
        if (board == null || board.length != size) {
            throw new IllegalArgumentException("Saved board size does not match rule mode.");
        }
        for (int file = 0; file < size; file++) {
            if (board[file] == null || board[file].length != size) {
                throw new IllegalArgumentException("Saved board size does not match rule mode.");
            }
            for (int rank = 0; rank < size; rank++) {
                if (board[file][rank] < 0) {
                    throw new IllegalArgumentException("Saved board contains a negative piece value.");
                }
            }
        }
    }

    public record SaveState(
            RuleMode mode,
            TraditionalWinMode traditionalWinMode,
            int[][] cells,
            int turn,
            int sequence,
            BoardPhase phase,
            boolean gameFinished,
            PieceColor winner,
            String resultReason,
            List<PlacementSnapshot> placementHistory,
            List<MoveSnapshot> moveHistory,
            List<BoardPoint> tempPath,
            List<PieceSnapshot> tempCaptures,
            List<MoveCandidateSnapshot> candidates,
            int tempPiece,
            int pendingCaptureCount,
            FormationSnapshot lastFormationMatch,
            PieceSnapshot clearedCenterA,
            PieceSnapshot clearedCenterB,
            Map<String, Integer> repetitionCounts
    ) {
        public SaveState {
            traditionalWinMode = mode == RuleMode.TRADITIONAL_BASIC && traditionalWinMode != null
                    ? traditionalWinMode
                    : TraditionalWinMode.OFF;
            cells = copyCells(cells);
            resultReason = resultReason == null ? "" : resultReason;
            placementHistory = List.copyOf(placementHistory == null ? List.of() : placementHistory);
            moveHistory = List.copyOf(moveHistory == null ? List.of() : moveHistory);
            tempPath = List.copyOf(tempPath == null ? List.of() : tempPath);
            tempCaptures = List.copyOf(tempCaptures == null ? List.of() : tempCaptures);
            candidates = List.copyOf(candidates == null ? List.of() : candidates);
            repetitionCounts = Map.copyOf(repetitionCounts == null ? Map.of() : repetitionCounts);
        }
    }

    public record PlacementSnapshot(BoardPoint point, int pieceValue) {
    }

    public record PieceSnapshot(BoardPoint point, int pieceValue) {
    }

    public record MoveSnapshot(int pieceValue, List<BoardPoint> path, List<PieceSnapshot> captures) {
        public MoveSnapshot {
            path = List.copyOf(path == null ? List.of() : path);
            captures = List.copyOf(captures == null ? List.of() : captures);
        }
    }

    public record MoveCandidateSnapshot(BoardPoint target, BoardPoint capturedPoint) {
    }

    public record FormationSnapshot(
            String name,
            BoardPoint triggerPoint,
            PieceColor color,
            int captureCount,
            List<BoardPoint> points
    ) {
        public FormationSnapshot {
            points = List.copyOf(points == null ? List.of() : points);
        }
    }

    private static final class PlacementRecord {
        private final BoardPoint point;
        private final int pieceValue;

        private PlacementRecord(BoardPoint point, int pieceValue) {
            this.point = point;
            this.pieceValue = pieceValue;
        }
    }

    private static final class PieceRecord {
        private final BoardPoint point;
        private final int pieceValue;

        private PieceRecord(BoardPoint point, int pieceValue) {
            this.point = point;
            this.pieceValue = pieceValue;
        }
    }

    private static final class MoveRecord {
        private final int pieceValue;
        private final List<BoardPoint> path;
        private final List<PieceRecord> captures;

        private MoveRecord(int pieceValue, List<BoardPoint> path, List<PieceRecord> captures) {
            this.pieceValue = pieceValue;
            this.path = path;
            this.captures = captures;
        }
    }

    private static final class StateSnapshot {
        private final BoardRuleConfig ruleConfig;
        private final int[][] cells;
        private final int turn;
        private final int sequence;
        private final BoardPhase phase;
        private final GameResult gameResult;
        private final List<PlacementRecord> placementHistory;
        private final List<MoveRecord> moveHistory;
        private final List<BoardPoint> tempPath;
        private final List<PieceRecord> tempCaptures;
        private final List<MoveCandidate> candidates;
        private final int tempPiece;
        private final int needSquareCaptureCount;
        private final FormationMatch lastFormationMatch;
        private final PieceRecord clearedCenterA;
        private final PieceRecord clearedCenterB;
        private final Map<String, Integer> repetitionCounts;

        private StateSnapshot(BoardState state) {
            this.ruleConfig = state.ruleConfig;
            this.cells = copyCells(state.cells);
            this.turn = state.turn;
            this.sequence = state.sequence;
            this.phase = state.phase;
            this.gameResult = state.gameResult;
            this.placementHistory = new ArrayList<>(state.placementHistory);
            this.moveHistory = copyMoveHistory(state.moveHistory);
            this.tempPath = new ArrayList<>(state.tempPath);
            this.tempCaptures = new ArrayList<>(state.tempCaptures);
            this.candidates = new ArrayList<>(state.candidates);
            this.tempPiece = state.tempPiece;
            this.needSquareCaptureCount = state.needSquareCaptureCount;
            this.lastFormationMatch = state.lastFormationMatch;
            this.clearedCenterA = state.clearedCenterA;
            this.clearedCenterB = state.clearedCenterB;
            this.repetitionCounts = new HashMap<>(state.repetitionCounts);
        }

        private static StateSnapshot capture(BoardState state) {
            return new StateSnapshot(state);
        }

        private void restore(BoardState state) {
            state.ruleConfig = ruleConfig;
            state.cells = copyCells(cells);
            state.turn = turn;
            state.sequence = sequence;
            state.phase = phase;
            state.gameResult = gameResult;
            state.placementHistory.clear();
            state.placementHistory.addAll(placementHistory);
            state.moveHistory.clear();
            state.moveHistory.addAll(copyMoveHistory(moveHistory));
            state.tempPath.clear();
            state.tempPath.addAll(tempPath);
            state.tempCaptures.clear();
            state.tempCaptures.addAll(tempCaptures);
            state.candidates.clear();
            state.candidates.addAll(candidates);
            state.tempPiece = tempPiece;
            state.needSquareCaptureCount = needSquareCaptureCount;
            state.lastFormationMatch = lastFormationMatch;
            state.clearedCenterA = clearedCenterA;
            state.clearedCenterB = clearedCenterB;
            state.repetitionCounts.clear();
            state.repetitionCounts.putAll(repetitionCounts);
        }
    }
}
