package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardPoint;
import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.Move;
import com.zangjiuqi.core.PieceColor;
import com.zangjiuqi.core.RuleMode;
import com.zangjiuqi.core.TraditionalWinningPattern;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProfileTest {
    @Test
    void lhasaHeuristicRanksHigherProgressAboveLowerProgress() {
        LhasaPatternHeuristic heuristic = new LhasaPatternHeuristic();
        BoardState low = traditionalStateWithBlack(lhasaDoubleDoorPoints().subList(0, 7));
        BoardState high = traditionalStateWithBlack(lhasaDoubleDoorPoints().subList(0, 13));
        BoardState complete = traditionalStateWithBlack(lhasaDoubleDoorPoints());

        int lowScore = heuristic.boardScore(low, PieceColor.BLACK);
        int highScore = heuristic.boardScore(high, PieceColor.BLACK);
        int completeScore = heuristic.boardScore(complete, PieceColor.BLACK);

        assertTrue(highScore > lowScore, highScore + " <= " + lowScore);
        assertTrue(completeScore > highScore, completeScore + " <= " + highScore);
    }

    @Test
    void traditionalProfileRewardsGateBreakingNearFlyThreshold() {
        TraditionalAiProfile profile = TraditionalAiProfile.withDefaultHeuristics(new AiEvaluator(new AiMoveGenerator()));
        BoardState state = gateBreakingState();
        Move breakGate = new Move(List.of(p(5, 2), p(4, 2)), List.of(), List.of(p(0, 10)));
        Move leaveGate = new Move(List.of(p(5, 2), p(4, 2)), List.of(), List.of(p(5, 10)));

        int breakScore = profile.moveOrderScore(state, breakGate);
        int leaveScore = profile.moveOrderScore(state, leaveGate);

        assertTrue(breakScore > leaveScore + 20_000, breakScore + " <= " + leaveScore);
    }

    @Test
    void competitiveProfileDoesNotUseTraditionalPatternHeuristic() {
        AiEvaluator evaluator = new AiEvaluator(new AiMoveGenerator());
        CompetitiveAiProfile competitive = new CompetitiveAiProfile(evaluator);
        TraditionalAiProfile traditional = new TraditionalAiProfile(evaluator, List.of(new StubPatternHeuristic()));
        BoardState state = gateBreakingState();
        Move quietMove = new Move(List.of(p(5, 2), p(6, 2)), List.of(), List.of());

        int competitiveScore = competitive.moveOrderScore(state, quietMove);
        int traditionalScore = traditional.moveOrderScore(state, quietMove);

        assertTrue(traditionalScore > competitiveScore + 50_000, traditionalScore + " <= " + competitiveScore);
    }

    @Test
    void traditionalProfileCanCombineFuturePatternHeuristics() {
        AiEvaluator evaluator = new AiEvaluator(new AiMoveGenerator());
        TraditionalAiProfile lowProfile = new TraditionalAiProfile(evaluator, List.of(
                new StubPatternHeuristic(10),
                new StubPatternHeuristic(500)
        ));
        TraditionalAiProfile highProfile = new TraditionalAiProfile(evaluator, List.of(
                new StubPatternHeuristic(10),
                new StubPatternHeuristic(50_000)
        ));
        BoardState state = gateBreakingState();

        int lowScore = lowProfile.evaluate(state, PieceColor.BLACK);
        int highScore = highProfile.evaluate(state, PieceColor.BLACK);

        assertTrue(highScore > lowScore + 80_000, highScore + " <= " + lowScore);
    }

    @Test
    void traditionalPatternOffDoesNotApplyWinningPatternUrgency() {
        TraditionalAiProfile profile = TraditionalAiProfile.withDefaultHeuristics(new AiEvaluator(new AiMoveGenerator()));
        BoardState off = traditionalStateWithBlack(lhasaDoubleDoorPoints());
        addWhiteNearFlyThreshold(off);
        BoardState lhasa = traditionalStateWithBlack(lhasaDoubleDoorPoints());
        addWhiteNearFlyThreshold(lhasa);
        lhasa.setTraditionalWinningPattern(TraditionalWinningPattern.LHASA);

        int offScore = profile.evaluate(off, PieceColor.BLACK);
        int lhasaScore = profile.evaluate(lhasa, PieceColor.BLACK);

        assertTrue(lhasaScore > offScore + 60_000, lhasaScore + " <= " + offScore);
    }

    @Test
    void selectedTraditionalWinningPatternDrivesTargetHeuristic() {
        TraditionalAiProfile profile = TraditionalAiProfile.withDefaultHeuristics(new AiEvaluator(new AiMoveGenerator()));
        BoardState lhasaSelected = traditionalStateWithBlack(lhasaDoubleDoorPoints());
        addWhiteNearFlyThreshold(lhasaSelected);
        lhasaSelected.setTraditionalWinningPattern(TraditionalWinningPattern.LHASA);
        BoardState goldfishSelectedOnLhasaBoard = traditionalStateWithBlack(lhasaDoubleDoorPoints());
        addWhiteNearFlyThreshold(goldfishSelectedOnLhasaBoard);
        goldfishSelectedOnLhasaBoard.setTraditionalWinningPattern(TraditionalWinningPattern.GOLDFISH);

        BoardState goldfishSelected = traditionalStateWithBlack(goldfishPoints());
        addWhiteNearFlyThreshold(goldfishSelected);
        goldfishSelected.setTraditionalWinningPattern(TraditionalWinningPattern.GOLDFISH);
        BoardState lhasaSelectedOnGoldfishBoard = traditionalStateWithBlack(goldfishPoints());
        addWhiteNearFlyThreshold(lhasaSelectedOnGoldfishBoard);
        lhasaSelectedOnGoldfishBoard.setTraditionalWinningPattern(TraditionalWinningPattern.LHASA);

        int lhasaScore = profile.evaluate(lhasaSelected, PieceColor.BLACK);
        int lhasaBoardWithGoldfishTargetScore = profile.evaluate(goldfishSelectedOnLhasaBoard, PieceColor.BLACK);
        int goldfishScore = profile.evaluate(goldfishSelected, PieceColor.BLACK);
        int goldfishBoardWithLhasaTargetScore = profile.evaluate(lhasaSelectedOnGoldfishBoard, PieceColor.BLACK);

        assertTrue(lhasaScore > lhasaBoardWithGoldfishTargetScore + 60_000,
                lhasaScore + " <= " + lhasaBoardWithGoldfishTargetScore);
        assertTrue(goldfishScore > goldfishBoardWithLhasaTargetScore + 60_000,
                goldfishScore + " <= " + goldfishBoardWithLhasaTargetScore);
    }

    private static BoardState traditionalStateWithBlack(List<BoardPoint> points) {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        for (BoardPoint point : points) {
            state.putForTesting(point, 2);
        }
        return state;
    }

    private static BoardState gateBreakingState() {
        BoardState state = new BoardState(RuleMode.TRADITIONAL_BASIC);
        state.enterMovePhaseForTesting(1);
        state.setTraditionalWinningPattern(com.zangjiuqi.core.TraditionalWinningPattern.LHASA);
        state.putForTesting(p(3, 1), 2);
        state.putForTesting(p(4, 1), 2);
        state.putForTesting(p(3, 2), 2);
        state.putForTesting(p(5, 2), 2);
        for (BoardPoint point : List.of(
                p(0, 10), p(0, 9), p(1, 9),
                p(2, 10), p(3, 10), p(4, 10), p(5, 10), p(6, 10), p(7, 10),
                p(8, 10), p(9, 10), p(10, 10), p(11, 10), p(12, 10), p(13, 10)
        )) {
            state.putForTesting(point, 1);
        }
        return state;
    }

    private static List<BoardPoint> lhasaDoubleDoorPoints() {
        return List.of(p(3, 1), p(4, 1), p(5, 1), p(6, 1), p(3, 2), p(5, 2), p(6, 2),
                p(3, 3), p(4, 3), p(6, 3), p(3, 4), p(4, 4), p(5, 4), p(6, 4));
    }

    private static List<BoardPoint> goldfishPoints() {
        return List.of(
                p(4, 1), p(5, 1), p(6, 1), p(7, 1),
                p(4, 2), p(5, 2), p(7, 2),
                p(3, 3), p(5, 3), p(6, 3),
                p(3, 4), p(4, 4), p(5, 4), p(6, 4)
        );
    }

    private static void addWhiteNearFlyThreshold(BoardState state) {
        for (BoardPoint point : List.of(
                p(0, 10), p(1, 10), p(2, 10), p(3, 10), p(4, 10), p(5, 10), p(6, 10),
                p(7, 10), p(8, 10), p(9, 10), p(10, 10), p(11, 10), p(12, 10), p(13, 10)
        )) {
            state.putForTesting(point, 1);
        }
    }

    private static BoardPoint p(int file, int rank) {
        return new BoardPoint(file, rank);
    }

    private static final class StubPatternHeuristic implements TraditionalPatternHeuristic {
        private final int score;

        private StubPatternHeuristic() {
            this(100_000);
        }

        private StubPatternHeuristic(int score) {
            this.score = score;
        }

        @Override
        public int boardScore(BoardState state, PieceColor color) {
            return color == PieceColor.BLACK ? score : score / 10;
        }

        @Override
        public int moveScore(BoardState state, Move move, PieceColor color) {
            return score;
        }
    }
}
