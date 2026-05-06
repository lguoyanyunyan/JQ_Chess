package com.zangjiuqi.ai;

import com.zangjiuqi.core.BoardState;
import com.zangjiuqi.core.RuleMode;

final class AiStrategyProfiles {
    private final AiStrategyProfile competitive;
    private final AiStrategyProfile traditional;

    AiStrategyProfiles(AiMoveGenerator generator) {
        AiEvaluator evaluator = new AiEvaluator(generator);
        this.competitive = new CompetitiveAiProfile(evaluator);
        this.traditional = TraditionalAiProfile.withDefaultHeuristics(evaluator);
    }

    AiStrategyProfile forState(BoardState state) {
        return state.ruleConfig().mode() == RuleMode.TRADITIONAL_BASIC ? traditional : competitive;
    }
}
