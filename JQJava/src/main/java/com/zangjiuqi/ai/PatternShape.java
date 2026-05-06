package com.zangjiuqi.ai;

import com.zangjiuqi.core.PatternTransformMode;

import java.util.List;

final class PatternShape {
    private final String name;
    private final List<Offset> points;
    private final PatternTransformMode transformMode;
    private final int clearOwnMultiplier;
    private final int nearCompleteEmptyThreshold;
    private final int nearCompleteBonusPerStep;
    private final int completeBonus;
    private final int blockedOwnMinimum;
    private final int blockedOwnMultiplier;
    private final int blockedPenalty;
    private final int fallbackOwnMultiplier;
    private final int fallbackBlockedPenalty;

    PatternShape(
            String name,
            List<Offset> points,
            int clearOwnMultiplier,
            int nearCompleteEmptyThreshold,
            int nearCompleteBonusPerStep,
            int completeBonus,
            int blockedOwnMinimum,
            int blockedOwnMultiplier,
            int blockedPenalty,
            int fallbackOwnMultiplier,
            int fallbackBlockedPenalty
    ) {
        this(
                name,
                points,
                PatternTransformMode.ROTATE_AND_MIRROR,
                clearOwnMultiplier,
                nearCompleteEmptyThreshold,
                nearCompleteBonusPerStep,
                completeBonus,
                blockedOwnMinimum,
                blockedOwnMultiplier,
                blockedPenalty,
                fallbackOwnMultiplier,
                fallbackBlockedPenalty
        );
    }

    PatternShape(
            String name,
            List<Offset> points,
            PatternTransformMode transformMode,
            int clearOwnMultiplier,
            int nearCompleteEmptyThreshold,
            int nearCompleteBonusPerStep,
            int completeBonus,
            int blockedOwnMinimum,
            int blockedOwnMultiplier,
            int blockedPenalty,
            int fallbackOwnMultiplier,
            int fallbackBlockedPenalty
    ) {
        this.name = name;
        this.points = List.copyOf(points);
        this.transformMode = transformMode;
        this.clearOwnMultiplier = clearOwnMultiplier;
        this.nearCompleteEmptyThreshold = nearCompleteEmptyThreshold;
        this.nearCompleteBonusPerStep = nearCompleteBonusPerStep;
        this.completeBonus = completeBonus;
        this.blockedOwnMinimum = blockedOwnMinimum;
        this.blockedOwnMultiplier = blockedOwnMultiplier;
        this.blockedPenalty = blockedPenalty;
        this.fallbackOwnMultiplier = fallbackOwnMultiplier;
        this.fallbackBlockedPenalty = fallbackBlockedPenalty;
    }

    String name() {
        return name;
    }

    List<Offset> points() {
        return points;
    }

    PatternTransformMode transformMode() {
        return transformMode;
    }

    int pointCount() {
        return points.size();
    }

    int score(int ownCount, int emptyCount, int blockedCount) {
        if (blockedCount == 0) {
            int score = ownCount * ownCount * clearOwnMultiplier;
            if (emptyCount <= nearCompleteEmptyThreshold) {
                score += (nearCompleteEmptyThreshold + 1 - emptyCount) * nearCompleteBonusPerStep;
            }
            if (emptyCount == 0) {
                score += completeBonus;
            }
            return score;
        }
        if (blockedCount <= 2 && ownCount >= blockedOwnMinimum) {
            return ownCount * ownCount * blockedOwnMultiplier - blockedCount * blockedPenalty;
        }
        return ownCount * fallbackOwnMultiplier - blockedCount * fallbackBlockedPenalty;
    }

    static Offset p(int file, int rank) {
        return new Offset(file, rank);
    }

    record Offset(int file, int rank) {
    }
}
