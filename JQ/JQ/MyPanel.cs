using System;
using System.Collections;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace JQ
{
    public class MyPanel : Panel
    {
        private static StringBuilder bestmove = new StringBuilder(2000);
        private const int BoardMargin = 25;
        private const int BoardLinePixels = 650;
        private Pen cbPen;
        private Pen cbPenx;
        private Pen redPen;
        private Pen greenPen;
        private Brush redBrush;
        private Brush greenBrush;
        private Brush blueBrush;
        private Brush blackBrush;
        private Brush whiteBrush;
        private int[,] cb;
        private ArrayList pts;
        private ArrayList moves;
        private int turn;
        private int timeout;
        private int timeout2;
        private bool bShowNo;
        private int no;
        private bool bStart;
        private bool bHumanTurn;
        private int gametype;
        private int iState;
        private Form_Main parent;
        private Move tempMove;
        private Move lastMove;

        public bool bAIRunning { get; private set; }

        private Point lastPts;
        private ArrayList nextMaybePath;
        private int needKillCount;
        private int boardDirection;
        private int p66;
        private int p77;
        private Thread aiThread;
        private Thread aiThread2;
        private BoardRuleConfig ruleConfig;

        public int BoardSize
        {
            get { return ruleConfig.BoardSize; }
        }

        public int Gametype
        {
            get { return gametype; }
            set { gametype = value; }
        }

        public bool BHumanTurn
        {
            get { return bHumanTurn; }
            set { bHumanTurn = value; }
        }

        public bool bAINext { get; private set; }

        public int[,] Cb
        {
            get { return cb; }
            set { cb = value; }
        }

        public MyPanel()
        {
            cbPenx = new Pen(Color.Black, 4);
            cbPen = new Pen(Color.Black, 2);
            redPen = new Pen(Color.Red, 10);
            greenPen = new Pen(Color.Green, 10);
            redBrush = new SolidBrush(Color.Red);
            greenBrush = new SolidBrush(Color.Green);
            blueBrush = new SolidBrush(Color.Blue);
            blackBrush = new SolidBrush(Color.Black);
            whiteBrush = new SolidBrush(Color.White);
            ruleConfig = BoardRuleConfig.FromMode(RuleMode.Competitive);
            cb = new int[ruleConfig.BoardSize, ruleConfig.BoardSize];
            pts = new ArrayList();
            moves = new ArrayList();
            turn = 1;
            bHumanTurn = true;
            no = 0;
            iState = 0;
            lastPts = new Point(-1, -1);
            tempMove = new Move(0);
            nextMaybePath = new ArrayList();
            needKillCount = 0;
            p66 = 0;
            p77 = 0;
            boardDirection = 0;
        }

        public void KillAIThread()
        {
            if (aiThread != null)
            {
                aiThread.Abort();
            }
            if (aiThread2 != null)
            {
                aiThread2.Abort();
            }
        }

        public void initGame(Form_Main parent, int gametype)
        {
            this.parent = parent;
            this.gametype = gametype;
            ruleConfig = BoardRuleConfig.FromMode(GetRuleMode());
            cb = new int[ruleConfig.BoardSize, ruleConfig.BoardSize];
            no = 0;
            pts.Clear();
            moves.Clear();
            turn = 1;
            iState = 0;
            bStart = false;
            bAINext = false;
            bAIRunning = false;
            tempMove = new Move(0);
            lastMove = null;
            lastPts = new Point(-1, -1);
            nextMaybePath = new ArrayList();
            needKillCount = 0;
            p66 = 0;
            p77 = 0;
            RefreshRuleStatus();
            Refresh();
        }

        [DllImport(@"jqai.dll", EntryPoint = "getAIMove", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void getAIMove(byte[] cb, byte state, byte side, int aisearchdepth, int timeout, [MarshalAs(UnmanagedType.LPStr)] StringBuilder strbm);

        [DllImport(@"jqai2.dll", EntryPoint = "getAIMove", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void getAIMove2(byte[] cb, byte state, byte side, int aisearchdepth, int timeout, [MarshalAs(UnmanagedType.LPStr)] StringBuilder strbm);

        [DllImport(@"jqai.dll", EntryPoint = "getAIMoveEx", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void getAIMoveEx(byte[] cb, byte state, byte side, int boardSize, int aisearchdepth, int timeout, [MarshalAs(UnmanagedType.LPStr)] StringBuilder strbm);

        [DllImport(@"jqai2.dll", EntryPoint = "getAIMoveEx", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void getAIMoveEx2(byte[] cb, byte state, byte side, int boardSize, int aisearchdepth, int timeout, [MarshalAs(UnmanagedType.LPStr)] StringBuilder strbm);

        [DllImport(@"jqai.dll", EntryPoint = "destroy_hashtable", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void destroy_hashtable();

        [DllImport(@"jqai2.dll", EntryPoint = "destroy_hashtable", SetLastError = true, CharSet = CharSet.Ansi, ExactSpelling = false)]
        public static extern void destroy_hashtable2();

        public void RefreshRuleStatus()
        {
            UpdateRuleStatus();
        }

        private RuleMode GetRuleMode()
        {
            if (parent == null)
            {
                return RuleMode.Competitive;
            }

            return parent.GetRuleMode();
        }

        private bool IsCompetitiveMode()
        {
            return ruleConfig.Mode == RuleMode.Competitive;
        }

        private bool IsInsideBoard(int x, int y)
        {
            return x > -1 && x < ruleConfig.BoardSize && y > -1 && y < ruleConfig.BoardSize;
        }

        private bool IsCenterPoint(int x, int y)
        {
            return (x == ruleConfig.CenterPointA && y == ruleConfig.CenterPointA)
                || (x == ruleConfig.CenterPointB && y == ruleConfig.CenterPointB);
        }

        private float CellStep
        {
            get { return BoardLinePixels / (float)(ruleConfig.BoardSize - 1); }
        }

        private float BoardCenterCoord(int index)
        {
            return BoardMargin + index * CellStep;
        }

        private int PieceDiameter
        {
            get { return Math.Min(50, Math.Max(30, (int)(CellStep * 0.72f))); }
        }

        private int HitTestBoardIndex(int pixel)
        {
            int index = (int)Math.Round((pixel - BoardMargin) / CellStep);
            return index < 0 || index >= ruleConfig.BoardSize ? -1 : index;
        }

        private bool TryParseBoardPoint(string text, out Point point)
        {
            point = new Point(-1, -1);
            if (string.IsNullOrWhiteSpace(text) || text.Length < 2)
            {
                return false;
            }

            int y = char.ToUpperInvariant(text[0]) - 'A';
            int x;
            if (!int.TryParse(text.Substring(1), out x))
            {
                return false;
            }

            x--;
            if (!IsInsideBoard(x, y))
            {
                return false;
            }

            point = new Point(x, y);
            return true;
        }

        private void FinishInvalidAIMove(string moveText)
        {
            bAIRunning = false;
            bAINext = false;
            FinishGameWithMessage("AI 返回非法着法，对局结束：" + moveText);
        }

        private void FinishEmbattlePhase()
        {
            p66 = cb[ruleConfig.CenterPointA, ruleConfig.CenterPointA];
            p77 = cb[ruleConfig.CenterPointB, ruleConfig.CenterPointB];
            cb[ruleConfig.CenterPointA, ruleConfig.CenterPointA] = 0;
            cb[ruleConfig.CenterPointB, ruleConfig.CenterPointB] = 0;
            lastPts = new Point(-1, -1);
            iState = 2;
        }

        private bool IsMovePhaseActive()
        {
            return iState == 2 || iState == 3;
        }

        private int GetMovePhaseColorForTurn(int currentTurn)
        {
            return currentTurn == 1 ? 2 : 1;
        }

        private string GetColorName(int color)
        {
            return color == 1 ? "白方" : "黑方";
        }

        private string GetCurrentTurnName()
        {
            if (iState == 1)
            {
                return turn == 1 ? "白方" : "黑方";
            }

            return GetColorName(GetMovePhaseColorForTurn(turn));
        }

        private void GetPieceCounts(out int whiteCount, out int blackCount)
        {
            whiteCount = 0;
            blackCount = 0;
            for (int i = 0; i < ruleConfig.BoardSize; i++)
            {
                for (int j = 0; j < ruleConfig.BoardSize; j++)
                {
                    if (cb[i, j] > 0)
                    {
                        if (cb[i, j] % 2 == 1)
                        {
                            whiteCount++;
                        }
                        else
                        {
                            blackCount++;
                        }
                    }
                }
            }
        }

        private int GetPieceCountByColor(int color)
        {
            int whiteCount;
            int blackCount;
            GetPieceCounts(out whiteCount, out blackCount);
            return color == 1 ? whiteCount : blackCount;
        }

        private bool CanSideFlyByColor(int color)
        {
            return IsMovePhaseActive() && GetPieceCountByColor(color) <= ruleConfig.FlyPieceThreshold;
        }

        private bool CurrentSideCanFly(int currentTurn)
        {
            return CanSideFlyByColor(GetMovePhaseColorForTurn(currentTurn));
        }

        private bool RivalSideCanFly(int currentTurn)
        {
            return CanSideFlyByColor(GetMovePhaseColorForTurn(1 - currentTurn));
        }

        private bool ShouldRestrictSingleJump(int currentTurn)
        {
            return iState == 2 && IsCompetitiveMode() && !CurrentSideCanFly(currentTurn) && RivalSideCanFly(currentTurn);
        }

        private byte GetAIState()
        {
            if (iState == 1)
            {
                return AIState.Embattle;
            }

            if (CurrentSideCanFly(turn))
            {
                return IsCompetitiveMode() ? AIState.FlyCompetitive : AIState.FlyTraditional;
            }

            return IsCompetitiveMode() ? AIState.MoveCompetitive : AIState.MoveTraditional;
        }

        private void UpdateRuleStatus()
        {
            if (parent == null)
            {
                return;
            }

            string ruleText = GetRuleMode() == RuleMode.Competitive ? "规则: 竞技化" : "规则: 传统基础";
            string stageText = "阶段: 未开始";
            string currentText = "当前: 无";

            if (iState == 1)
            {
                stageText = "阶段: 布局";
                currentText = string.Format("当前: {0}布局", GetCurrentTurnName());
            }
            else if (iState == 2)
            {
                bool canFly = CurrentSideCanFly(turn);
                stageText = canFly ? "阶段: 飞子" : "阶段: 走子";
                currentText = string.Format("当前: {0}{1}", GetCurrentTurnName(), canFly ? "飞子" : "走子");
            }
            else if (iState == 3)
            {
                stageText = "阶段: 成方补吃";
                currentText = string.Format("当前: {0}补吃 {1} 子", GetCurrentTurnName(), needKillCount);
            }

            parent.UpdateRuleStatus(ruleText + "\r\n" + stageText + "\r\n" + currentText);
        }

        private void AIMove()
        {
            lastMove = null;
            bAIRunning = true;
            if (iState == 1 && turn == 1 || iState == 2 && turn == 0)
            {
                aiThread = new Thread(new ThreadStart(AIThink));
                aiThread.Start();
            }
            else if (iState == 1 && turn == 0 || iState == 2 && turn == 1)
            {
                aiThread2 = new Thread(new ThreadStart(AIThink));
                aiThread2.Start();
            }
        }

        private void AIThink()
        {
            byte[] tcb = new byte[ruleConfig.BoardPointCount];
            for (int i = 0; i < ruleConfig.BoardSize; i++)
            {
                for (int j = 0; j < ruleConfig.BoardSize; j++)
                {
                    tcb[i * ruleConfig.BoardSize + j] = cb[i, j] > 0 ? (byte)(2 - cb[i, j] % 2) : (byte)0;
                }
            }

            int to = 5;
            if (iState == 1 && turn == 1 || iState == 2 && turn == 0)
            {
                to = timeout;
            }
            else if (iState == 1 && turn == 0 || iState == 2 && turn == 1)
            {
                to = timeout2;
            }

            bestmove.Length = 0;
            byte aiState = GetAIState();
            if (gametype != 3)
            {
                getAIMoveEx(tcb, aiState, (byte)turn, ruleConfig.BoardSize, parent.GetAISearchDepth(), to, bestmove);
            }
            else
            {
                if (iState == 1 && turn == 1 || iState == 2 && turn == 0)
                {
                    getAIMoveEx(tcb, aiState, (byte)turn, ruleConfig.BoardSize, parent.GetAISearchDepth(), to, bestmove);
                }
                else if (iState == 1 && turn == 0 || iState == 2 && turn == 1)
                {
                    getAIMoveEx2(tcb, aiState, (byte)turn, ruleConfig.BoardSize, parent.GetAISearchDepth(), to, bestmove);
                }
            }

            if (bestmove.ToString().Length == 0)
            {
                bAIRunning = false;
                bAINext = false;
                if (iState == 2)
                {
                    FinishGameWithMessage("当前规则下 AI 无合法着法，对局结束。");
                }
                return;
            }

            if (bStart || bAINext)
            {
                if (iState == 1)
                {
                    Point point;
                    if (!TryParseBoardPoint(bestmove.ToString(), out point) || cb[point.Y, point.X] != 0)
                    {
                        FinishInvalidAIMove(bestmove.ToString());
                        return;
                    }

                    int x = point.X;
                    int y = point.Y;
                    cb[y, x] = ++no;
                    lastPts = new Point(x, y);
                    pts.Add(lastPts);
                    if (no == ruleConfig.BoardPointCount)
                    {
                        FinishEmbattlePhase();
                    }
                }
                else if (iState == 2)
                {
                    string bm = bestmove.ToString();
                    string[] bms = bm.Split(' ');
                    string[] mvs = bms[0].Split(',');
                    int sx = 0;
                    int sy = 0;
                    int ex = 0;
                    int ey = 0;
                    tempMove = new Move(0);
                    for (int i = 0; i < mvs.Length; i++)
                    {
                        Point point;
                        if (!TryParseBoardPoint(mvs[i], out point))
                        {
                            FinishInvalidAIMove(bm);
                            return;
                        }

                        int x = point.X;
                        int y = point.Y;
                        tempMove.Path.Add(point);
                        if (i == 0)
                        {
                            sx = x;
                            sy = y;
                        }
                        if (i == mvs.Length - 1)
                        {
                            ex = x;
                            ey = y;
                        }
                    }
                    if (tempMove.Path.Count < 2 || cb[sy, sx] <= 0 || cb[ey, ex] != 0)
                    {
                        FinishInvalidAIMove(bm);
                        return;
                    }

                    tempMove.Piece = cb[sy, sx];
                    cb[ey, ex] = cb[sy, sx];
                    cb[sy, sx] = 0;
                    int movingPieceParity = tempMove.Piece % 2;
                    bool aiMoveCanFly = CurrentSideCanFly(turn);
                    for (int j = 1; j < bms.Length; j++)
                    {
                        if (aiMoveCanFly && bms[j].StartsWith("TC-"))
                        {
                            continue;
                        }
                        if (!bms[j].StartsWith("TC-") && !bms[j].StartsWith("FC-"))
                        {
                            continue;
                        }

                        string[] skilled = bms[j].Substring(3).Split(',');
                        for (int i = 0; i < skilled.Length; i++)
                        {
                            Point point;
                            if (!TryParseBoardPoint(skilled[i], out point))
                            {
                                FinishInvalidAIMove(bm);
                                return;
                            }

                            int x = point.X;
                            int y = point.Y;
                            if (cb[y, x] <= 0 || cb[y, x] % 2 == movingPieceParity)
                            {
                                continue;
                            }

                            tempMove.Dead.Add(new APiece(new Point(x, y), cb[y, x]));
                            cb[y, x] = 0;
                        }
                    }
                    ConfirmTempMoveFirstStage();
                }

                turn = 1 - turn;
                SetHumanTurn();
                Refresh();
                UpdateRuleStatus();
                bAINext = false;
                Thread.Sleep(50);
                if (iState == 2 && TryFinishFromRules())
                {
                    bAIRunning = false;
                    bAINext = false;
                    return;
                }

                if (bStart && !bHumanTurn)
                {
                    if (gametype == 3)
                    {
                        if (iState == 1 && turn == 1 || iState == 2 && turn == 0)
                        {
                            aiThread = new Thread(new ThreadStart(AIThink));
                            aiThread.Start();
                        }
                        else if (iState == 1 && turn == 0 || iState == 2 && turn == 1)
                        {
                            aiThread2 = new Thread(new ThreadStart(AIThink));
                            aiThread2.Start();
                        }
                    }
                    else
                    {
                        if (turn == 1)
                        {
                            aiThread = new Thread(new ThreadStart(AIThink));
                            aiThread.Start();
                        }
                        else
                        {
                            aiThread2 = new Thread(new ThreadStart(AIThink));
                            aiThread2.Start();
                        }
                    }
                }
            }

            bAIRunning = false;
            bAINext = false;
        }

        public void setStart(bool b, int s, int t, int to, int to2)
        {
            if (b == false)
            {
                while (aiThread != null && aiThread.IsAlive)
                {
                    ;
                }
                destroy_hashtable();
                if (gametype == 3)
                {
                    while (aiThread2 != null && aiThread2.IsAlive)
                    {
                        ;
                    }
                    destroy_hashtable2();
                }
            }

            lastMove = null;
            lastPts = new Point(-1, -1);
            bStart = b;
            iState = s == -1 ? iState : s;
            turn = t == -1 ? turn : t;
            timeout = to;
            timeout2 = to2;
            SetHumanTurn();
            Cursor = b ? Cursors.Cross : Cursors.Arrow;
            Refresh();
            UpdateRuleStatus();
            if (bStart && !bHumanTurn)
            {
                AIMove();
            }
        }

        public void aiNext(int to, int to2)
        {
            if (bAIRunning == true)
            {
                return;
            }

            lastMove = null;
            lastPts = new Point(-1, -1);
            bStart = false;
            timeout = to;
            timeout2 = to2;
            SetHumanTurn();
            Refresh();
            UpdateRuleStatus();
            bAINext = true;
            AIMove();
        }

        public bool getBStart()
        {
            return bStart;
        }

        private void FinishTempMove()
        {
            tempMove.Path.Clear();
            tempMove.Dead.Clear();
            nextMaybePath.Clear();
        }

        private void ConfirmTempMove()
        {
            ConfirmTempMoveFirstStage();
            turn = 1 - turn;
            iState = 2;
            SetHumanTurn();
            UpdateRuleStatus();
            if (TryFinishFromRules())
            {
                return;
            }
            if (bStart && !bHumanTurn)
            {
                AIMove();
            }
        }

        private void ConfirmTempMoveFirstStage()
        {
            for (int i = 0; i < tempMove.Dead.Count; i++)
            {
                APiece a = (APiece)tempMove.Dead[i];
                cb[a.Pts.Y, a.Pts.X] = 0;
            }
            moves.Add(tempMove);
            nextMaybePath.Clear();
            lastMove = tempMove;
            tempMove = new Move(0);
            no++;
            parent.EnableUndo(true);
        }

        private int ChengFangCount()
        {
            needKillCount = 0;
            Point p = (Point)tempMove.Path[tempMove.Path.Count - 1];
            int[] xInc = { 1, -1, -1, 1 };
            int[] yInc = { -1, -1, 1, 1 };
            int x = 0;
            int y = 0;
            for (int i = 0; i < 4; i++)
            {
                x = p.X + xInc[i];
                y = p.Y + yInc[i];
                if (IsInsideBoard(x, y)
                    && cb[y, x] > 0 && cb[y, x] % 2 == cb[p.Y, p.X] % 2
                    && cb[p.Y, x] > 0 && cb[p.Y, x] % 2 == cb[p.Y, p.X] % 2
                    && cb[y, p.X] > 0 && cb[y, p.X] % 2 == cb[p.Y, p.X] % 2)
                {
                    needKillCount++;
                }
            }
            return needKillCount;
        }

        public void TurnBoard()
        {
            boardDirection = 1 - boardDirection;
        }

        private void CollectJumpTargets(ArrayList targets, int x, int y, Point previous, int pieceValue)
        {
            int[] xInc = { 1, 0, -1, 0 };
            int[] yInc = { 0, -1, 0, 1 };
            for (int i = 0; i < 4; i++)
            {
                int tx = x + xInc[i];
                int ty = y + yInc[i];
                int ttx = x + 2 * xInc[i];
                int tty = y + 2 * yInc[i];
                if (ttx == previous.X && tty == previous.Y)
                {
                    continue;
                }
                if (IsInsideBoard(ttx, tty) && cb[ty, tx] > 0 && cb[ty, tx] % 2 != pieceValue % 2 && cb[tty, ttx] == 0)
                {
                    targets.Add(new Point(ttx, tty));
                }
            }
        }

        private bool HasFollowUpJumpAfter(int fromX, int fromY, int toX, int toY, int pieceValue)
        {
            int middleX = (fromX + toX) / 2;
            int middleY = (fromY + toY) / 2;
            int sourcePiece = cb[fromY, fromX];
            int middlePiece = cb[middleY, middleX];
            int targetPiece = cb[toY, toX];

            cb[fromY, fromX] = 0;
            cb[middleY, middleX] = 0;
            cb[toY, toX] = pieceValue;

            ArrayList jumpTargets = new ArrayList();
            CollectJumpTargets(jumpTargets, toX, toY, new Point(fromX, fromY), pieceValue);

            cb[fromY, fromX] = sourcePiece;
            cb[middleY, middleX] = middlePiece;
            cb[toY, toX] = targetPiece;

            return jumpTargets.Count > 0;
        }

        private void PopulateSelectableTargets(int x, int y)
        {
            nextMaybePath.Clear();
            if (CurrentSideCanFly(turn))
            {
                for (int i = 0; i < ruleConfig.BoardSize; i++)
                {
                    for (int j = 0; j < ruleConfig.BoardSize; j++)
                    {
                        if (cb[i, j] == 0)
                        {
                            nextMaybePath.Add(new Point(j, i));
                        }
                    }
                }
                return;
            }

            bool restrictSingleJump = ShouldRestrictSingleJump(turn);
            int[] xInc = { 1, 0, -1, 0 };
            int[] yInc = { 0, -1, 0, 1 };
            for (int i = 0; i < 4; i++)
            {
                int tx = x + xInc[i];
                int ty = y + yInc[i];
                int ttx = x + 2 * xInc[i];
                int tty = y + 2 * yInc[i];
                if (IsInsideBoard(tx, ty) && cb[ty, tx] == 0)
                {
                    nextMaybePath.Add(new Point(tx, ty));
                }
                if (IsInsideBoard(ttx, tty) && cb[ty, tx] > 0 && cb[ty, tx] % 2 != cb[y, x] % 2 && cb[tty, ttx] == 0)
                {
                    if (!restrictSingleJump || HasFollowUpJumpAfter(x, y, ttx, tty, cb[y, x]))
                    {
                        nextMaybePath.Add(new Point(ttx, tty));
                    }
                }
            }
        }

        private void PopulateFollowUpJumpTargets(Point from, Point previous, int pieceValue)
        {
            nextMaybePath.Clear();
            CollectJumpTargets(nextMaybePath, from.X, from.Y, previous, pieceValue);
        }

        private bool CanFinishCurrentMove()
        {
            if (ShouldRestrictSingleJump(turn) && tempMove.Dead.Count == 1)
            {
                MessageBox.Show(parent, "竞技化规则下，对手进入飞子后强势方跳吃不能单吃，必须连续多吃。", "非法走法", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return false;
            }

            return true;
        }

        private bool HasAnyLegalMove(int currentTurn)
        {
            if (iState != 2)
            {
                return true;
            }

            int currentColor = GetMovePhaseColorForTurn(currentTurn);
            bool currentCanFly = CanSideFlyByColor(currentColor);
            bool restrictSingleJump = IsCompetitiveMode() && !currentCanFly && CanSideFlyByColor(GetMovePhaseColorForTurn(1 - currentTurn));
            bool hasEmpty = false;
            for (int i = 0; i < ruleConfig.BoardSize; i++)
            {
                for (int j = 0; j < ruleConfig.BoardSize; j++)
                {
                    if (cb[i, j] == 0)
                    {
                        hasEmpty = true;
                    }
                    if (cb[i, j] > 0 && (cb[i, j] % 2 == 1 ? 1 : 2) == currentColor)
                    {
                        if (currentCanFly && hasEmpty)
                        {
                            return true;
                        }

                        int[] xInc = { 1, 0, -1, 0 };
                        int[] yInc = { 0, -1, 0, 1 };
                        for (int d = 0; d < 4; d++)
                        {
                            int tx = j + xInc[d];
                            int ty = i + yInc[d];
                            int ttx = j + 2 * xInc[d];
                            int tty = i + 2 * yInc[d];
                            if (IsInsideBoard(tx, ty) && cb[ty, tx] == 0)
                            {
                                return true;
                            }
                            if (IsInsideBoard(ttx, tty) && cb[ty, tx] > 0 && cb[ty, tx] % 2 != cb[i, j] % 2 && cb[tty, ttx] == 0)
                            {
                                if (!restrictSingleJump || HasFollowUpJumpAfter(j, i, ttx, tty, cb[i, j]))
                                {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            return currentCanFly && hasEmpty;
        }

        private void FinishGameWithMessage(string message)
        {
            UpdateRuleStatus();
            MessageBox.Show(parent, message, "对局结束", MessageBoxButtons.OK, MessageBoxIcon.Information);
            parent.FinishGame(CanUndo());
        }

        private bool TryFinishFromRules()
        {
            if (iState != 2)
            {
                UpdateRuleStatus();
                return false;
            }

            int whiteCount;
            int blackCount;
            GetPieceCounts(out whiteCount, out blackCount);
            if (IsCompetitiveMode())
            {
                if (whiteCount < GameRuleConstants.CompetitiveLosePieceCount)
                {
                    FinishGameWithMessage("白方棋子少于4枚，黑方获胜。");
                    return true;
                }
                if (blackCount < GameRuleConstants.CompetitiveLosePieceCount)
                {
                    FinishGameWithMessage("黑方棋子少于4枚，白方获胜。");
                    return true;
                }
            }

            if (!HasAnyLegalMove(turn))
            {
                FinishGameWithMessage(string.Format("{0}当前无合法着法，对局结束。", GetCurrentTurnName()));
                return true;
            }

            UpdateRuleStatus();
            return false;
        }

        protected override void OnMouseDown(MouseEventArgs e)
        {
            if (bHumanTurn == true)
            {
                if (e.Button == MouseButtons.Left)
                {
                    if (bStart == true)
                    {
                        int x = HitTestBoardIndex(e.X);
                        int y = HitTestBoardIndex(e.Y);
                        if (iState == 1)
                        {
                            if (IsInsideBoard(x, y) && cb[y, x] == 0)
                            {
                                if (no < 2 && !IsCenterPoint(x, y))
                                {
                                }
                                else
                                {
                                    cb[y, x] = ++no;
                                    turn = 1 - turn;
                                    SetHumanTurn();
                                    lastPts = new Point(x, y);
                                    pts.Add(lastPts);
                                    if (no == ruleConfig.BoardPointCount)
                                    {
                                        FinishEmbattlePhase();
                                    }
                                    parent.EnableUndo(true);
                                    UpdateRuleStatus();
                                    if (bStart && !bHumanTurn)
                                    {
                                        AIMove();
                                    }
                                    Refresh();
                                }
                            }
                        }
                        else if (iState == 2)
                        {
                            bool currentTurnCanFly = CurrentSideCanFly(turn);
                            if (tempMove.Path.Count < 2 && IsInsideBoard(x, y) && cb[y, x] > 0 && turn != cb[y, x] % 2)
                            {
                                lastMove = null;
                                FinishTempMove();
                                parent.EnableUndo(false);
                                PopulateSelectableTargets(x, y);
                                if (nextMaybePath.Count > 0)
                                {
                                    tempMove.Piece = cb[y, x];
                                    tempMove.Path.Add(new Point(x, y));
                                }
                            }
                            else if (tempMove.Path.Count > 0 && IsInsideBoard(x, y) && cb[y, x] == 0 && nextMaybePath.Contains(new Point(x, y)))
                            {
                                lastMove = null;
                                Point p = (Point)tempMove.Path[tempMove.Path.Count - 1];
                                cb[p.Y, p.X] = 0;
                                cb[y, x] = tempMove.Piece;
                                nextMaybePath.Clear();
                                if (!currentTurnCanFly)
                                {
                                    int mx = x;
                                    int my = y;
                                    if (Math.Abs(p.X - x) == 2)
                                    {
                                        mx = (p.X + x) / 2;
                                    }
                                    if (Math.Abs(p.Y - y) == 2)
                                    {
                                        my = (p.Y + y) / 2;
                                    }
                                    if (mx != x || my != y)
                                    {
                                        tempMove.Dead.Add(new APiece(new Point(mx, my), cb[my, mx]));
                                        cb[my, mx] = 0;
                                        PopulateFollowUpJumpTargets(new Point(x, y), p, tempMove.Piece);
                                    }
                                }
                                tempMove.Path.Add(new Point(x, y));
                                if (currentTurnCanFly || nextMaybePath.Count == 0)
                                {
                                    if (CanFinishCurrentMove())
                                    {
                                        JudgeCfOrConfirmMove();
                                    }
                                }
                            }
                            Refresh();
                        }
                        else if (iState == 3)
                        {
                            if (IsInsideBoard(x, y) && cb[y, x] > 0 && turn == cb[y, x] % 2)
                            {
                                tempMove.Dead.Add(new APiece(new Point(x, y), cb[y, x]));
                                cb[y, x] = 0;
                                needKillCount--;
                                if (needKillCount == 0)
                                {
                                    Cursor = Cursors.Cross;
                                    iState = 2;
                                    ConfirmTempMove();
                                }
                                UpdateRuleStatus();
                                Refresh();
                            }
                        }
                    }
                }
                else if (e.Button == MouseButtons.Right)
                {
                    if (bStart == true)
                    {
                        if (iState == 2)
                        {
                            if (tempMove.Path.Count > 1)
                            {
                                if (CanFinishCurrentMove())
                                {
                                    JudgeCfOrConfirmMove();
                                }
                            }
                            else
                            {
                                FinishTempMove();
                                parent.EnableUndo(true);
                            }
                            Refresh();
                        }
                    }
                }
            }
            base.OnMouseDown(e);
        }

        private void JudgeCfOrConfirmMove()
        {
            needKillCount = ChengFangCount();
            if (needKillCount > 0)
            {
                nextMaybePath.Clear();
                Cursor = Cursors.Hand;
                iState = 3;
                UpdateRuleStatus();
            }
            else
            {
                ConfirmTempMove();
            }
        }

        public void CancelTempMove()
        {
            if (iState == 2 && tempMove.Path.Count > 0)
            {
                FinishTempMove();
                parent.EnableUndo(true);
                Refresh();
                UpdateRuleStatus();
            }
        }

        private void SetHumanTurn()
        {
            switch (gametype)
            {
                case 0:
                    bHumanTurn = true;
                    break;
                case 1:
                    bHumanTurn = iState == 1 ? turn == 1 : turn == 0;
                    break;
                case 2:
                    bHumanTurn = iState == 1 ? turn == 0 : turn == 1;
                    break;
                case 3:
                    bHumanTurn = false;
                    break;
            }
            if (bHumanTurn == true && (pts.Count > 0 || moves.Count > 0))
            {
                parent.EnableUndo(true);
            }
        }

        private void RestoreTempMove()
        {
            for (int i = 0; i < tempMove.Dead.Count; i++)
            {
                APiece p = (APiece)tempMove.Dead[i];
                cb[p.Pts.Y, p.Pts.X] = p.Piece;
            }

            if (tempMove.Path.Count > 1)
            {
                Point sp = (Point)tempMove.Path[0];
                Point ep = (Point)tempMove.Path[tempMove.Path.Count - 1];
                cb[sp.Y, sp.X] = tempMove.Piece;
                cb[ep.Y, ep.X] = 0;
            }

            tempMove = new Move(0);
            nextMaybePath.Clear();
            needKillCount = 0;
            iState = 2;
            Cursor = Cursors.Cross;
        }

        public void undo()
        {
            if (iState == 3 && tempMove.Path.Count > 0)
            {
                RestoreTempMove();
                Refresh();
            }
            else if (iState == 2 && moves.Count > 0)
            {
                Move m = (Move)moves[moves.Count - 1];
                for (int i = 0; i < m.Dead.Count; i++)
                {
                    APiece p = (APiece)m.Dead[i];
                    cb[p.Pts.Y, p.Pts.X] = p.Piece;
                }
                Point sp = (Point)m.Path[0];
                Point ep = (Point)m.Path[m.Path.Count - 1];
                cb[sp.Y, sp.X] = m.Piece;
                cb[ep.Y, ep.X] = 0;
                moves.RemoveAt(moves.Count - 1);
                Refresh();
                no--;
                turn = 1 - turn;
                SetHumanTurn();
            }
            else if (iState == 2 && moves.Count == 0 && no == ruleConfig.BoardPointCount || iState == 1)
            {
                if (iState == 2 && moves.Count == 0)
                {
                    cb[ruleConfig.CenterPointA, ruleConfig.CenterPointA] = p66;
                    cb[ruleConfig.CenterPointB, ruleConfig.CenterPointB] = p77;
                    Refresh();
                    iState = 1;
                    SetHumanTurn();
                }
                if (pts.Count > 0)
                {
                    Point pt = (Point)pts[pts.Count - 1];
                    pts.RemoveAt(pts.Count - 1);
                    cb[pt.Y, pt.X] = 0;
                    Refresh();
                }
                no--;
                turn = 1 - turn;
                SetHumanTurn();
                if (no == 0)
                {
                    parent.EnableUndo(false);
                }
            }
            bool bEnableUndoButton = true;
            if (moves.Count == 0 && pts.Count == 0)
            {
                bEnableUndoButton = false;
            }
            UpdateRuleStatus();
            parent.FinishGame(bEnableUndoButton);
        }

        public bool CanUndo()
        {
            return !(moves.Count == 0 && pts.Count == 0);
        }

        public void SetShowNo(bool bChecked)
        {
            bShowNo = bChecked;
        }

        protected override void OnPaint(PaintEventArgs e)
        {
            Rectangle rect = e.ClipRectangle;
            BufferedGraphicsContext currentContext = BufferedGraphicsManager.Current;
            BufferedGraphics myBuffer = currentContext.Allocate(e.Graphics, rect);
            Graphics g = myBuffer.Graphics;
            g.SmoothingMode = SmoothingMode.HighQuality;
            g.PixelOffsetMode = PixelOffsetMode.HighSpeed;
            g.Clear(this.BackColor);
            int pieceDiameter = PieceDiameter;
            int pieceRadius = pieceDiameter / 2;
            for (int i = 0; i < ruleConfig.BoardSize; i++)
            {
                float lineCoord = BoardCenterCoord(i);
                if (i == 0 || i == ruleConfig.BoardSize - 1)
                {
                    g.DrawLine(cbPenx, BoardMargin, lineCoord, BoardMargin + BoardLinePixels, lineCoord);
                    g.DrawLine(cbPenx, lineCoord, BoardMargin, lineCoord, BoardMargin + BoardLinePixels);
                }
                else
                {
                    g.DrawLine(cbPen, BoardMargin, lineCoord, BoardMargin + BoardLinePixels, lineCoord);
                    g.DrawLine(cbPen, lineCoord, BoardMargin, lineCoord, BoardMargin + BoardLinePixels);
                }
            }
            g.DrawLine(cbPen, BoardCenterCoord(ruleConfig.CenterPointA), BoardCenterCoord(ruleConfig.CenterPointA), BoardCenterCoord(ruleConfig.CenterPointB), BoardCenterCoord(ruleConfig.CenterPointB));
            for (int i = 0; i < ruleConfig.BoardSize; i++)
            {
                for (int j = 0; j < ruleConfig.BoardSize; j++)
                {
                    float pieceX = BoardCenterCoord(j) - pieceRadius;
                    float pieceY = BoardCenterCoord(i) - pieceRadius;
                    if (cb[i, j] % 2 == 1)
                    {
                        g.FillEllipse(whiteBrush, pieceX, pieceY, pieceDiameter, pieceDiameter);
                    }
                    else if (cb[i, j] > 0 && cb[i, j] % 2 == 0)
                    {
                        g.FillEllipse(blackBrush, pieceX, pieceY, pieceDiameter, pieceDiameter);
                    }
                    if (bShowNo && cb[i, j] > 0)
                    {
                        string str = cb[i, j].ToString();
                        int len = str.Length;
                        g.DrawString(str, new Font("宋体", 22), redBrush, pieceX + (pieceDiameter - len * 12) / 2, pieceY + 10);
                    }
                }
            }
            Point p;
            Point q;
            if (lastPts != null && !(lastPts.X == -1 && lastPts.Y == -1))
            {
                p = lastPts;
                g.FillEllipse(blueBrush, BoardCenterCoord(p.X) - 15, BoardCenterCoord(p.Y) - 15, 30, 30);
                if (bShowNo && cb[p.Y, p.X] > 0)
                {
                    string str = cb[p.Y, p.X].ToString();
                    int len = str.Length;
                    g.DrawString(str, new Font("宋体", 22), redBrush, BoardCenterCoord(p.X) - pieceRadius + (pieceDiameter - len * 12) / 2, BoardCenterCoord(p.Y) - pieceRadius + 10);
                }
            }
            if (tempMove.Path.Count > 0)
            {
                p = (Point)tempMove.Path[tempMove.Path.Count - 1];
                g.FillEllipse(blueBrush, BoardCenterCoord(p.X) - 15, BoardCenterCoord(p.Y) - 15, 30, 30);
                for (int i = 0; i < tempMove.Dead.Count; i++)
                {
                    APiece a = (APiece)tempMove.Dead[i];
                    g.DrawLine(redPen, BoardCenterCoord(a.Pts.X) - 15, BoardCenterCoord(a.Pts.Y) - 15, BoardCenterCoord(a.Pts.X) + 15, BoardCenterCoord(a.Pts.Y) + 15);
                    g.DrawLine(redPen, BoardCenterCoord(a.Pts.X) - 15, BoardCenterCoord(a.Pts.Y) + 15, BoardCenterCoord(a.Pts.X) + 15, BoardCenterCoord(a.Pts.Y) - 15);
                }
            }
            for (int i = 0; i < nextMaybePath.Count; i++)
            {
                p = (Point)nextMaybePath[i];
                g.FillEllipse(greenBrush, BoardCenterCoord(p.X) - 15, BoardCenterCoord(p.Y) - 15, 30, 30);
            }
            if (lastMove != null && lastMove.Path.Count > 0)
            {
                q = (Point)lastMove.Path[0];
                for (int i = 1; i < lastMove.Path.Count; i++)
                {
                    p = (Point)lastMove.Path[i];
                    g.DrawLine(greenPen, BoardCenterCoord(q.X), BoardCenterCoord(q.Y), BoardCenterCoord(p.X), BoardCenterCoord(p.Y));
                    q = p;
                }
                p = (Point)lastMove.Path[lastMove.Path.Count - 1];
                g.FillEllipse(blueBrush, BoardCenterCoord(p.X) - 15, BoardCenterCoord(p.Y) - 15, 30, 30);
                if (bShowNo && cb[p.Y, p.X] > 0)
                {
                    string str = cb[p.Y, p.X].ToString();
                    int len = str.Length;
                    g.DrawString(str, new Font("宋体", 22), redBrush, BoardCenterCoord(p.X) - pieceRadius + (pieceDiameter - len * 12) / 2, BoardCenterCoord(p.Y) - pieceRadius + 10);
                }
                for (int i = 0; i < lastMove.Dead.Count; i++)
                {
                    APiece a = (APiece)lastMove.Dead[i];
                    g.DrawLine(redPen, BoardCenterCoord(a.Pts.X) - 15, BoardCenterCoord(a.Pts.Y) - 15, BoardCenterCoord(a.Pts.X) + 15, BoardCenterCoord(a.Pts.Y) + 15);
                    g.DrawLine(redPen, BoardCenterCoord(a.Pts.X) - 15, BoardCenterCoord(a.Pts.Y) + 15, BoardCenterCoord(a.Pts.X) + 15, BoardCenterCoord(a.Pts.Y) - 15);
                }
            }
            myBuffer.Render(e.Graphics);
            g.Dispose();
            myBuffer.Dispose();
        }
    }
}
