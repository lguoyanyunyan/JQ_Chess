using System;
using System.Collections;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace JQ
{
    public partial class Form_Main : Form
    {
        public Form_Main()
        {
            System.Windows.Forms.Control.CheckForIllegalCrossThreadCalls = false;
            InitializeComponent();
            comboBox_Gametype.SelectedIndex = 0;
            comboBox_RuleMode.SelectedIndex = (int)RuleMode.Competitive;
            comboBox_Timeout.SelectedIndex = 0;
            comboBox_Timeout2.SelectedIndex = 0;
        }

        private void button_Start_Click(object sender, EventArgs e)
        {
            if (panel_cb.getBStart())
            {
                DialogResult dr = MessageBox.Show(this, "棋局正在进行，确定要重新开始吗？", "重新开始", MessageBoxButtons.YesNo);
                if (!dr.Equals(DialogResult.Yes))
                {
                    return;
                }
            }
            button_Start.Enabled = false;
            comboBox_Gametype.Enabled = false;
            button_undo.Enabled = false;
            button_finish.Enabled = true;
            button_ContinuePuts.Enabled = false;
            button_MovesFromBlack.Enabled = false;
            button_MovesFromWhite.Enabled = false;
            trackBar_aisearchdepth.Enabled = false;
            button_aiNext.Enabled = false;
            comboBox_RuleMode.Enabled = false;
            panel_cb.initGame(this, comboBox_Gametype.SelectedIndex);
            panel_cb.setStart(true, 1, 1, int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
        }

        public void EnableUndo(bool b)
        {
            button_undo.Enabled = b;
        }

        public void FinishGame(bool bUndo)
        {
            panel_cb.KillAIThread();
            comboBox_Gametype.Enabled = true;
            button_Start.Enabled = true;
            button_undo.Enabled = bUndo;
            button_finish.Enabled = false;
            button_ContinuePuts.Enabled = true;
            button_MovesFromBlack.Enabled = true;
            button_MovesFromWhite.Enabled = true;
            trackBar_aisearchdepth.Enabled = true;
            comboBox_RuleMode.Enabled = true;
            if (comboBox_Gametype.SelectedIndex == 3)
            {
                button_aiNext.Enabled = true;
            }
            panel_cb.setStart(false, -1, -1, int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
        }

        private void checkBox_ShowNo_CheckedChanged(object sender, EventArgs e)
        {
            panel_cb.SetShowNo(checkBox_ShowNo.Checked);
            panel_cb.Refresh();
        }

        private void button_undo_Click(object sender, EventArgs e)
        {
            panel_cb.undo();
        }

        private void button_aiNext_Click(object sender, EventArgs e)
        {
            panel_cb.aiNext(int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
            button_finish.Enabled = true;
        }

        private void button_finish_Click(object sender, EventArgs e)
        {
            //DialogResult dr = MessageBox.Show(this, "棋局正在进行，确定要结束吗？", "结束", MessageBoxButtons.YesNo);
            //if (dr.Equals(DialogResult.Yes))
            {
                FinishGame(panel_cb.CanUndo());
            }
        }

        private void button_ContinuePuts_Click(object sender, EventArgs e)
        {
            comboBox_Gametype.Enabled = false;
            button_undo.Enabled = panel_cb.BHumanTurn;
            button_finish.Enabled = true;
            button_ContinuePuts.Enabled = false;
            button_MovesFromBlack.Enabled = false;
            button_MovesFromWhite.Enabled = false;
            trackBar_aisearchdepth.Enabled = false;
            comboBox_RuleMode.Enabled = false;
            panel_cb.Gametype = comboBox_Gametype.SelectedIndex;
            panel_cb.setStart(true, 1, -1, int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
        }

        private void button_MovesFromBlack_Click(object sender, EventArgs e)
        {
            comboBox_Gametype.Enabled = false;
            button_undo.Enabled = panel_cb.BHumanTurn;
            button_finish.Enabled = true;
            button_ContinuePuts.Enabled = false;
            button_MovesFromBlack.Enabled = false;
            button_MovesFromWhite.Enabled = false;
            trackBar_aisearchdepth.Enabled = false;
            comboBox_RuleMode.Enabled = false;
            panel_cb.Gametype = comboBox_Gametype.SelectedIndex;
            panel_cb.setStart(true, 2, 1, int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
        }

        private void button_MovesFromWhite_Click(object sender, EventArgs e)
        {
            comboBox_Gametype.Enabled = false;
            button_undo.Enabled = panel_cb.BHumanTurn;
            button_finish.Enabled = true;
            button_ContinuePuts.Enabled = false;
            button_MovesFromBlack.Enabled = false;
            button_MovesFromWhite.Enabled = false;
            trackBar_aisearchdepth.Enabled = false;
            comboBox_RuleMode.Enabled = false;
            panel_cb.Gametype = comboBox_Gametype.SelectedIndex;
            panel_cb.setStart(true, 2, 0, int.Parse(comboBox_Timeout.SelectedItem.ToString()), int.Parse(comboBox_Timeout2.SelectedItem.ToString()));
        }

        public int GetAISearchDepth()
        {
            return trackBar_aisearchdepth.Value;
        }

        public RuleMode GetRuleMode()
        {
            if (comboBox_RuleMode.SelectedIndex < 0)
            {
                return RuleMode.Competitive;
            }

            return (RuleMode)comboBox_RuleMode.SelectedIndex;
        }

        public void UpdateRuleStatus(string text)
        {
            label_ruleStatus.Text = text;
        }

        private void panel_ColNo_Paint(object sender, PaintEventArgs e)
        {
            Brush blackBrush = new SolidBrush(Color.Black);
            int boardSize = panel_cb.BoardSize;
            float step = 650f / (boardSize - 1);
            for (int i = 0; i < boardSize; i++)
            {
                int w = i < 9 ? 13 : 3;
                e.Graphics.DrawString((i + 1).ToString(), new Font("宋体", 22), blackBrush, 25 + i * step - 25 + w, 0);
            }
        }

        private void panel_RowNo_Paint(object sender, PaintEventArgs e)
        {
            Brush blackBrush = new SolidBrush(Color.Black);
            int boardSize = panel_cb.BoardSize;
            float step = 650f / (boardSize - 1);
            for (int i = 0; i < boardSize; i++)
            {
                e.Graphics.DrawString(((char)(i + 'A')).ToString(), new Font("宋体", 22), blackBrush, 0, 25 + i * step - 15);
            }
        }

        private void Form_Main_FormClosing(object sender, FormClosingEventArgs e)
        {
            panel_cb.KillAIThread();
        }

        private void button_load_Click(object sender, EventArgs e)
        {
            OpenFileDialog ofd = new OpenFileDialog();
            ofd.Multiselect = false;
            DialogResult dr = ofd.ShowDialog(this);
            if (dr.Equals(DialogResult.OK))
            {
                string filename = ofd.FileName;
                try
                {
                    string str = File.ReadAllText(filename);
                    string[] strs = str.Split(new string[] { "\r\n" }, StringSplitOptions.RemoveEmptyEntries);
                    int expectedBoardSize = BoardRuleConfig.FromMode(GetRuleMode()).BoardSize;
                    if (strs.Length == expectedBoardSize)
                    {
                        int[,] t = new int[expectedBoardSize, expectedBoardSize];
                        for (int i = 0; i < expectedBoardSize; i++)
                        {
                            string[] strss = strs[i].Split(new string[] { " " }, StringSplitOptions.RemoveEmptyEntries);
                            if (strss.Length == expectedBoardSize)
                            {
                                for (int j = 0; j < expectedBoardSize; j++)
                                {
                                    if (strss[j].Equals("0"))
                                    {
                                        t[i, j] = 0;
                                    }
                                    else if (strss[j].Equals("1"))
                                    {
                                        t[i, j] = 1;
                                    }
                                    else if (strss[j].Equals("2"))
                                    {
                                        t[i, j] = 2;
                                    }
                                    else
                                    {
                                        return;
                                    }
                                }
                            }
                            else
                            {
                                MessageBox.Show(string.Format("棋盘文件尺寸不匹配。当前规则需要 {0}x{0} 棋盘。", expectedBoardSize));
                                return;
                            }
                        }
                        panel_cb.initGame(this, comboBox_Gametype.SelectedIndex);
                        for (int i = 0; i < expectedBoardSize; i++)
                        {
                            for (int j = 0; j < expectedBoardSize; j++)
                            {
                                panel_cb.Cb[i, j] = t[i, j];
                            }
                        }
                    }
                    else
                    {
                        MessageBox.Show(string.Format("棋盘文件尺寸不匹配。当前规则需要 {0}x{0} 棋盘。", expectedBoardSize));
                        return;
                    }
                }
                catch
                {
                    MessageBox.Show("文件读取失败！");
                    return;
                }
                panel_cb.Refresh();
                FinishGame(false);
            }
        }

        private void Form_Main_Load(object sender, EventArgs e)
        {
            comboBox_Timeout.SelectedIndex = 2;
            comboBox_Timeout2.SelectedIndex = 2;
            UpdateRuleStatus("规则: 竞技化\r\n阶段: 未开始\r\n当前: 无");
        }

        private void comboBox_RuleMode_SelectedIndexChanged(object sender, EventArgs e)
        {
            if (!panel_cb.getBStart())
            {
                string ruleText = GetRuleMode() == RuleMode.Competitive ? "规则: 竞技化" : "规则: 传统基础";
                UpdateRuleStatus(ruleText + "\r\n阶段: 未开始\r\n当前: 无");
                panel_cb.initGame(this, comboBox_Gametype.SelectedIndex);
                panel_ColNo.Refresh();
                panel_RowNo.Refresh();
            }

            panel_cb.RefreshRuleStatus();
        }
    }
}
