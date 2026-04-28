namespace JQ
{
    partial class Form_Main
    {
        /// <summary>
        /// 必需的设计器变量。
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// 清理所有正在使用的资源。
        /// </summary>
        /// <param name="disposing">如果应释放托管资源，为 true；否则为 false。</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows 窗体设计器生成的代码

        /// <summary>
        /// 设计器支持所需的方法 - 不要修改
        /// 使用代码编辑器修改此方法的内容。
        /// </summary>
        private void InitializeComponent()
        {
            this.button_undo = new System.Windows.Forms.Button();
            this.comboBox_Gametype = new System.Windows.Forms.ComboBox();
            this.comboBox_Timeout = new System.Windows.Forms.ComboBox();
            this.button_Start = new System.Windows.Forms.Button();
            this.checkBox_ShowNo = new System.Windows.Forms.CheckBox();
            this.button_finish = new System.Windows.Forms.Button();
            this.button_ContinuePuts = new System.Windows.Forms.Button();
            this.button_MovesFromBlack = new System.Windows.Forms.Button();
            this.button_MovesFromWhite = new System.Windows.Forms.Button();
            this.button_aiNext = new System.Windows.Forms.Button();
            this.panel_ColNo = new System.Windows.Forms.Panel();
            this.panel_RowNo = new System.Windows.Forms.Panel();
            this.label_aisearchdepth = new System.Windows.Forms.Label();
            this.label_aisearchtimeout = new System.Windows.Forms.Label();
            this.trackBar_aisearchdepth = new System.Windows.Forms.TrackBar();
            this.button_load = new System.Windows.Forms.Button();
            this.comboBox_Timeout2 = new System.Windows.Forms.ComboBox();
            this.label_ruleMode = new System.Windows.Forms.Label();
            this.comboBox_RuleMode = new System.Windows.Forms.ComboBox();
            this.label_ruleStatus = new System.Windows.Forms.Label();
            this.panel_cb = new JQ.MyPanel();
            ((System.ComponentModel.ISupportInitialize)(this.trackBar_aisearchdepth)).BeginInit();
            this.SuspendLayout();
            // 
            // button_undo
            // 
            this.button_undo.Enabled = false;
            this.button_undo.Location = new System.Drawing.Point(768, 364);
            this.button_undo.Margin = new System.Windows.Forms.Padding(4);
            this.button_undo.Name = "button_undo";
            this.button_undo.Size = new System.Drawing.Size(154, 31);
            this.button_undo.TabIndex = 14;
            this.button_undo.Text = "悔棋";
            this.button_undo.UseVisualStyleBackColor = true;
            this.button_undo.Click += new System.EventHandler(this.button_undo_Click);
            // 
            // comboBox_Gametype
            // 
            this.comboBox_Gametype.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.comboBox_Gametype.FormattingEnabled = true;
            this.comboBox_Gametype.Items.AddRange(new object[] {
            "人人对战",
            "人机对战",
            "机人对战",
            "机机对战"});
            this.comboBox_Gametype.Location = new System.Drawing.Point(785, 37);
            this.comboBox_Gametype.Name = "comboBox_Gametype";
            this.comboBox_Gametype.Size = new System.Drawing.Size(121, 24);
            this.comboBox_Gametype.TabIndex = 3;
            // 
            // comboBox_Timeout
            // 
            this.comboBox_Timeout.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.comboBox_Timeout.FormattingEnabled = true;
            this.comboBox_Timeout.Items.AddRange(new object[] {
            "1",
            "3",
            "5",
            "10",
            "15",
            "20",
            "25",
            "30",
            "60",
            "120",
            "300"});
            this.comboBox_Timeout.Location = new System.Drawing.Point(785, 163);
            this.comboBox_Timeout.Name = "comboBox_Timeout";
            this.comboBox_Timeout.Size = new System.Drawing.Size(48, 24);
            this.comboBox_Timeout.TabIndex = 7;
            // 
            // button_Start
            // 
            this.button_Start.Location = new System.Drawing.Point(768, 326);
            this.button_Start.Margin = new System.Windows.Forms.Padding(4);
            this.button_Start.Name = "button_Start";
            this.button_Start.Size = new System.Drawing.Size(154, 31);
            this.button_Start.TabIndex = 13;
            this.button_Start.Text = "重新开始";
            this.button_Start.UseVisualStyleBackColor = true;
            this.button_Start.Click += new System.EventHandler(this.button_Start_Click);
            // 
            // checkBox_ShowNo
            // 
            this.checkBox_ShowNo.AutoSize = true;
            this.checkBox_ShowNo.Location = new System.Drawing.Point(785, 245);
            this.checkBox_ShowNo.Name = "checkBox_ShowNo";
            this.checkBox_ShowNo.Size = new System.Drawing.Size(123, 20);
            this.checkBox_ShowNo.TabIndex = 11;
            this.checkBox_ShowNo.Text = "显示落子编号";
            this.checkBox_ShowNo.UseVisualStyleBackColor = true;
            this.checkBox_ShowNo.CheckedChanged += new System.EventHandler(this.checkBox_ShowNo_CheckedChanged);
            // 
            // button_finish
            // 
            this.button_finish.Enabled = false;
            this.button_finish.Location = new System.Drawing.Point(768, 666);
            this.button_finish.Margin = new System.Windows.Forms.Padding(4);
            this.button_finish.Name = "button_finish";
            this.button_finish.Size = new System.Drawing.Size(154, 31);
            this.button_finish.TabIndex = 19;
            this.button_finish.Text = "结束";
            this.button_finish.UseVisualStyleBackColor = true;
            this.button_finish.Click += new System.EventHandler(this.button_finish_Click);
            // 
            // button_ContinuePuts
            // 
            this.button_ContinuePuts.Enabled = false;
            this.button_ContinuePuts.Font = new System.Drawing.Font("宋体", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.button_ContinuePuts.Location = new System.Drawing.Point(768, 402);
            this.button_ContinuePuts.Margin = new System.Windows.Forms.Padding(4);
            this.button_ContinuePuts.Name = "button_ContinuePuts";
            this.button_ContinuePuts.Size = new System.Drawing.Size(154, 31);
            this.button_ContinuePuts.TabIndex = 15;
            this.button_ContinuePuts.Text = "继续下子布局";
            this.button_ContinuePuts.UseVisualStyleBackColor = true;
            this.button_ContinuePuts.Click += new System.EventHandler(this.button_ContinuePuts_Click);
            // 
            // button_MovesFromBlack
            // 
            this.button_MovesFromBlack.Enabled = false;
            this.button_MovesFromBlack.Font = new System.Drawing.Font("宋体", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.button_MovesFromBlack.Location = new System.Drawing.Point(768, 440);
            this.button_MovesFromBlack.Margin = new System.Windows.Forms.Padding(4);
            this.button_MovesFromBlack.Name = "button_MovesFromBlack";
            this.button_MovesFromBlack.Size = new System.Drawing.Size(154, 31);
            this.button_MovesFromBlack.TabIndex = 16;
            this.button_MovesFromBlack.Text = "从黑方开始行棋";
            this.button_MovesFromBlack.UseVisualStyleBackColor = true;
            this.button_MovesFromBlack.Click += new System.EventHandler(this.button_MovesFromBlack_Click);
            // 
            // button_MovesFromWhite
            // 
            this.button_MovesFromWhite.Enabled = false;
            this.button_MovesFromWhite.Font = new System.Drawing.Font("宋体", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.button_MovesFromWhite.Location = new System.Drawing.Point(768, 478);
            this.button_MovesFromWhite.Margin = new System.Windows.Forms.Padding(4);
            this.button_MovesFromWhite.Name = "button_MovesFromWhite";
            this.button_MovesFromWhite.Size = new System.Drawing.Size(154, 31);
            this.button_MovesFromWhite.TabIndex = 17;
            this.button_MovesFromWhite.Text = "从白方开始行棋";
            this.button_MovesFromWhite.UseVisualStyleBackColor = true;
            this.button_MovesFromWhite.Click += new System.EventHandler(this.button_MovesFromWhite_Click);
            // 
            // button_aiNext
            // 
            this.button_aiNext.Enabled = false;
            this.button_aiNext.Location = new System.Drawing.Point(768, 516);
            this.button_aiNext.Margin = new System.Windows.Forms.Padding(4);
            this.button_aiNext.Name = "button_aiNext";
            this.button_aiNext.Size = new System.Drawing.Size(154, 31);
            this.button_aiNext.TabIndex = 18;
            this.button_aiNext.Text = "AI下一步";
            this.button_aiNext.UseVisualStyleBackColor = true;
            this.button_aiNext.Click += new System.EventHandler(this.button_aiNext_Click);
            // 
            // panel_ColNo
            // 
            this.panel_ColNo.Location = new System.Drawing.Point(50, 20);
            this.panel_ColNo.Name = "panel_ColNo";
            this.panel_ColNo.Size = new System.Drawing.Size(700, 30);
            this.panel_ColNo.TabIndex = 1;
            this.panel_ColNo.Paint += new System.Windows.Forms.PaintEventHandler(this.panel_ColNo_Paint);
            // 
            // panel_RowNo
            // 
            this.panel_RowNo.Location = new System.Drawing.Point(20, 50);
            this.panel_RowNo.Name = "panel_RowNo";
            this.panel_RowNo.Size = new System.Drawing.Size(30, 700);
            this.panel_RowNo.TabIndex = 2;
            this.panel_RowNo.Paint += new System.Windows.Forms.PaintEventHandler(this.panel_RowNo_Paint);
            // 
            // label_aisearchdepth
            // 
            this.label_aisearchdepth.AutoSize = true;
            this.label_aisearchdepth.Location = new System.Drawing.Point(808, 68);
            this.label_aisearchdepth.Name = "label_aisearchdepth";
            this.label_aisearchdepth.Size = new System.Drawing.Size(88, 16);
            this.label_aisearchdepth.TabIndex = 4;
            this.label_aisearchdepth.Text = "AI搜索深度";
            // 
            // label_aisearchtimeout
            // 
            this.label_aisearchtimeout.AutoSize = true;
            this.label_aisearchtimeout.Location = new System.Drawing.Point(802, 141);
            this.label_aisearchtimeout.Name = "label_aisearchtimeout";
            this.label_aisearchtimeout.Size = new System.Drawing.Size(104, 16);
            this.label_aisearchtimeout.TabIndex = 6;
            this.label_aisearchtimeout.Text = "搜索超时时限";
            // 
            // trackBar_aisearchdepth
            // 
            this.trackBar_aisearchdepth.Location = new System.Drawing.Point(768, 90);
            this.trackBar_aisearchdepth.Maximum = 20;
            this.trackBar_aisearchdepth.Minimum = 1;
            this.trackBar_aisearchdepth.Name = "trackBar_aisearchdepth";
            this.trackBar_aisearchdepth.Size = new System.Drawing.Size(154, 45);
            this.trackBar_aisearchdepth.TabIndex = 5;
            this.trackBar_aisearchdepth.Value = 20;
            // 
            // button_load
            // 
            this.button_load.Location = new System.Drawing.Point(768, 288);
            this.button_load.Margin = new System.Windows.Forms.Padding(4);
            this.button_load.Name = "button_load";
            this.button_load.Size = new System.Drawing.Size(154, 31);
            this.button_load.TabIndex = 12;
            this.button_load.Text = "读取文件";
            this.button_load.UseVisualStyleBackColor = true;
            this.button_load.Click += new System.EventHandler(this.button_load_Click);
            // 
            // comboBox_Timeout2
            // 
            this.comboBox_Timeout2.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.comboBox_Timeout2.FormattingEnabled = true;
            this.comboBox_Timeout2.Items.AddRange(new object[] {
            "1",
            "3",
            "5",
            "10",
            "15",
            "20",
            "25",
            "30",
            "60",
            "120",
            "300"});
            this.comboBox_Timeout2.Location = new System.Drawing.Point(848, 163);
            this.comboBox_Timeout2.Name = "comboBox_Timeout2";
            this.comboBox_Timeout2.Size = new System.Drawing.Size(48, 24);
            this.comboBox_Timeout2.TabIndex = 8;
            // 
            // label_ruleMode
            // 
            this.label_ruleMode.AutoSize = true;
            this.label_ruleMode.Location = new System.Drawing.Point(808, 193);
            this.label_ruleMode.Name = "label_ruleMode";
            this.label_ruleMode.Size = new System.Drawing.Size(72, 16);
            this.label_ruleMode.TabIndex = 9;
            this.label_ruleMode.Text = "规则模式";
            // 
            // comboBox_RuleMode
            // 
            this.comboBox_RuleMode.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.comboBox_RuleMode.FormattingEnabled = true;
            this.comboBox_RuleMode.Items.AddRange(new object[] {
            "传统基础",
            "竞技化"});
            this.comboBox_RuleMode.Location = new System.Drawing.Point(785, 212);
            this.comboBox_RuleMode.Name = "comboBox_RuleMode";
            this.comboBox_RuleMode.Size = new System.Drawing.Size(121, 24);
            this.comboBox_RuleMode.TabIndex = 10;
            this.comboBox_RuleMode.SelectedIndexChanged += new System.EventHandler(this.comboBox_RuleMode_SelectedIndexChanged);
            // 
            // label_ruleStatus
            // 
            this.label_ruleStatus.BorderStyle = System.Windows.Forms.BorderStyle.FixedSingle;
            this.label_ruleStatus.Location = new System.Drawing.Point(768, 566);
            this.label_ruleStatus.Name = "label_ruleStatus";
            this.label_ruleStatus.Padding = new System.Windows.Forms.Padding(4, 3, 4, 3);
            this.label_ruleStatus.Size = new System.Drawing.Size(154, 72);
            this.label_ruleStatus.TabIndex = 20;
            this.label_ruleStatus.TextAlign = System.Drawing.ContentAlignment.TopLeft;
            // 
            // panel_cb
            // 
            this.panel_cb.BackColor = System.Drawing.Color.Tan;
            this.panel_cb.BHumanTurn = true;
            this.panel_cb.Cursor = System.Windows.Forms.Cursors.Arrow;
            this.panel_cb.Gametype = 0;
            this.panel_cb.Location = new System.Drawing.Point(50, 50);
            this.panel_cb.Margin = new System.Windows.Forms.Padding(4);
            this.panel_cb.Name = "panel_cb";
            this.panel_cb.Size = new System.Drawing.Size(700, 700);
            this.panel_cb.TabIndex = 0;
            // 
            // Form_Main
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(8F, 16F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(934, 770);
            this.Controls.Add(this.label_ruleStatus);
            this.Controls.Add(this.comboBox_RuleMode);
            this.Controls.Add(this.label_ruleMode);
            this.Controls.Add(this.comboBox_Timeout2);
            this.Controls.Add(this.button_load);
            this.Controls.Add(this.trackBar_aisearchdepth);
            this.Controls.Add(this.label_aisearchtimeout);
            this.Controls.Add(this.label_aisearchdepth);
            this.Controls.Add(this.panel_RowNo);
            this.Controls.Add(this.panel_ColNo);
            this.Controls.Add(this.button_aiNext);
            this.Controls.Add(this.button_MovesFromWhite);
            this.Controls.Add(this.button_MovesFromBlack);
            this.Controls.Add(this.button_ContinuePuts);
            this.Controls.Add(this.button_finish);
            this.Controls.Add(this.checkBox_ShowNo);
            this.Controls.Add(this.button_Start);
            this.Controls.Add(this.comboBox_Gametype);
            this.Controls.Add(this.comboBox_Timeout);
            this.Controls.Add(this.button_undo);
            this.Controls.Add(this.panel_cb);
            this.Font = new System.Drawing.Font("宋体", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(134)));
            this.Margin = new System.Windows.Forms.Padding(4);
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.Name = "Form_Main";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "久棋";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.Form_Main_FormClosing);
            this.Load += new System.EventHandler(this.Form_Main_Load);
            ((System.ComponentModel.ISupportInitialize)(this.trackBar_aisearchdepth)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private MyPanel panel_cb;
        private System.Windows.Forms.Button button_undo;
        private System.Windows.Forms.ComboBox comboBox_Gametype;
        private System.Windows.Forms.Label label_aisearchtimeout;
        private System.Windows.Forms.ComboBox comboBox_Timeout;
        private System.Windows.Forms.Button button_Start;
        private System.Windows.Forms.CheckBox checkBox_ShowNo;
        private System.Windows.Forms.Button button_finish;
        private System.Windows.Forms.Button button_ContinuePuts;
        private System.Windows.Forms.Button button_MovesFromBlack;
        private System.Windows.Forms.Button button_MovesFromWhite;
        private System.Windows.Forms.Button button_aiNext;
        private System.Windows.Forms.Panel panel_ColNo;
        private System.Windows.Forms.Panel panel_RowNo;
        private System.Windows.Forms.Label label_aisearchdepth;
        private System.Windows.Forms.TrackBar trackBar_aisearchdepth;
        private System.Windows.Forms.Button button_load;
        private System.Windows.Forms.ComboBox comboBox_Timeout2;
        private System.Windows.Forms.Label label_ruleMode;
        private System.Windows.Forms.ComboBox comboBox_RuleMode;
        private System.Windows.Forms.Label label_ruleStatus;
    }
}

