namespace JQ
{
    public enum RuleMode
    {
        TraditionalBasic = 0,
        Competitive = 1,
    }

    internal static class GameRuleConstants
    {
        public const int CompetitiveLosePieceCount = 4;
    }

    internal sealed class BoardRuleConfig
    {
        private BoardRuleConfig(RuleMode mode, int boardSize)
        {
            Mode = mode;
            BoardSize = boardSize;
            BoardPointCount = boardSize * boardSize;
            FlyPieceThreshold = boardSize;
            CenterPointA = boardSize / 2 - 1;
            CenterPointB = boardSize / 2;
        }

        public RuleMode Mode { get; private set; }
        public int BoardSize { get; private set; }
        public int BoardPointCount { get; private set; }
        public int FlyPieceThreshold { get; private set; }
        public int CenterPointA { get; private set; }
        public int CenterPointB { get; private set; }

        public static BoardRuleConfig FromMode(RuleMode mode)
        {
            return mode == RuleMode.Competitive
                ? new BoardRuleConfig(mode, 8)
                : new BoardRuleConfig(mode, 14);
        }
    }

    internal static class AIState
    {
        public const byte Embattle = 1;
        public const byte MoveTraditional = 2;
        public const byte MoveCompetitive = 3;
        public const byte FlyTraditional = 4;
        public const byte FlyCompetitive = 5;
    }
}
