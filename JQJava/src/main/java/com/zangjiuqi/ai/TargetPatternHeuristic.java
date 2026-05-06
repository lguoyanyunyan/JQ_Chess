package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.PieceColor;

import java.util.List;

final class TargetPatternHeuristic implements TraditionalPatternHeuristic {
    private final List<PatternShape> shapes;
    private final PatternShapeScanner scanner = new PatternShapeScanner();

    TargetPatternHeuristic(List<PatternShape> shapes) {
        this.shapes = List.copyOf(shapes);
    }

    @Override
    public int boardScore(BoardState state, PieceColor color) {
        return scanner.bestScore(state, color, shapes).score();
    }
}
