package com.zangjiuqi.core;

import java.util.Locale;
import java.util.Objects;

public final class BoardPoint {
    private final int fileIndex;
    private final int rankIndex;

    public BoardPoint(int fileIndex, int rankIndex) {
        if (fileIndex < 0 || rankIndex < 0) {
            throw new IllegalArgumentException("Board coordinates must be non-negative.");
        }
        this.fileIndex = fileIndex;
        this.rankIndex = rankIndex;
    }

    public static BoardPoint parse(String text, int boardSize) {
        Objects.requireNonNull(text, "text");
        String trimmed = text.trim().toUpperCase(Locale.ROOT);
        if (trimmed.length() < 2) {
            throw new IllegalArgumentException("Invalid board point: " + text);
        }

        char rankLetter = trimmed.charAt(0);
        if (rankLetter < 'A' || rankLetter >= 'A' + boardSize) {
            throw new IllegalArgumentException("Rank out of range: " + text);
        }

        int fileNumber;
        try {
            fileNumber = Integer.parseInt(trimmed.substring(1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("File is not a number: " + text, ex);
        }

        if (fileNumber < 1 || fileNumber > boardSize) {
            throw new IllegalArgumentException("File out of range: " + text);
        }

        return new BoardPoint(fileNumber - 1, rankLetter - 'A');
    }

    public int fileIndex() {
        return fileIndex;
    }

    public int rankIndex() {
        return rankIndex;
    }

    public String toNotation() {
        return String.valueOf((char) ('A' + rankIndex)) + (fileIndex + 1);
    }

    public boolean isInside(int boardSize) {
        return fileIndex >= 0 && fileIndex < boardSize && rankIndex >= 0 && rankIndex < boardSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BoardPoint)) {
            return false;
        }
        BoardPoint that = (BoardPoint) o;
        return fileIndex == that.fileIndex && rankIndex == that.rankIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileIndex, rankIndex);
    }

    @Override
    public String toString() {
        return toNotation();
    }
}
