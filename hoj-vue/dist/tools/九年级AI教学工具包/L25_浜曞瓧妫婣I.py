# -*- coding: utf-8 -*-
"""
井字棋AI对弈系统 — 第25课《与机器下井字棋》
基于教材25.3「根据棋谱数据学习下棋」的得分表方法

评分规则（教材25.3）：
  1. 每一步，可下棋的位置均赋为 0；
  2. 胜利者获胜的位置 +100，失败者失败以外的位置 -100；
  3. 获胜前一步位置 +10，失败前一步以外的位置 -10；
  4. 获胜前两步位置 +1，失败前两步以外的位置 -1。

棋盘编号（教材"思路2"）：
  1 | 2 | 3
  ---+---+---
  4 | 5 | 6
  ---+---+---
  7 | 8 | 9
"""

import tkinter as tk
from tkinter import ttk, messagebox
from itertools import permutations
from collections import defaultdict
import random

# ============== 棋盘工具 ==============

# 8 条获胜线（按编号 1-9）
WIN_LINES = [
    (1,2,3),(4,5,6),(7,8,9),  # 横
    (1,4,7),(2,5,8),(3,6,9),  # 竖
    (1,5,9),(3,5,7),          # 斜
]

def check_winner(seq):
    """给定落子序列（如 '15243'），返回胜者：1/2/0 (平局/未结束)"""
    p1 = set(int(c) for i,c in enumerate(seq) if i%2==0)
    p2 = set(int(c) for i,c in enumerate(seq) if i%2==1)
    for line in WIN_LINES:
        if set(line).issubset(p1): return 1
        if set(line).issubset(p2): return 2
    return 0  # 未分出胜负

def is_finished(seq):
    """棋局是否结束：分出胜负或下满9步"""
    w = check_winner(seq)
    return w!=0 or len(seq)==9

# ============== 棋谱生成 + 训练 ==============

def generate_games():
    """穷举所有合法棋谱，去除"重复结束"的（即在更早步骤已分胜负的）"""
    games = []
    # DFS 生成
    def dfs(seq):
        if is_finished(seq):
            games.append(seq)
            return
        used = set(seq)
        for pos in range(1,10):
            if str(pos) not in used:
                dfs(seq + str(pos))
    dfs("")
    return games

def train_score_table(games):
    """
    根据棋谱训练得分表
    返回：dict[state_str] -> dict[pos_int] -> score
    """
    table = defaultdict(lambda: defaultdict(float))

    for game in games:
        winner = check_winner(game)
        # 平局不参与训练（教材未明说，但平局得分均为0）
        if winner == 0:
            continue

        L = len(game)
        # 对每一步累积得分
        for step_idx in range(L):
            state = game[:step_idx]  # 该步前的状态
            # 当前下棋者：奇数步(从1计)是玩家1，对应序列索引偶数
            current_player = 1 if step_idx % 2 == 0 else 2
            placed_pos = int(game[step_idx])

            # 相对于结束的步数
            steps_from_end = L - 1 - step_idx
            # 当前下棋者是赢家还是输家
            is_winner_step = (current_player == winner)

            # 该状态下的所有空位
            used = set(int(c) for c in state)
            empty = [p for p in range(1,10) if p not in used]

            if steps_from_end == 0:
                # 最后一步
                if is_winner_step:
                    # 胜利者获胜的位置 +100
                    table[state][placed_pos] += 100
                else:
                    # 失败者：失败位置以外的位置 -100
                    for pos in empty:
                        if pos != placed_pos:
                            table[state][pos] -= 100
                    # placed_pos 保持 0
                    if placed_pos not in table[state]:
                        table[state][placed_pos] += 0
            elif steps_from_end == 1:
                # 倒数第二步
                if is_winner_step:
                    # 获胜前一步位置 +10
                    table[state][placed_pos] += 10
                else:
                    # 失败前一步以外的位置 -10
                    for pos in empty:
                        if pos != placed_pos:
                            table[state][pos] -= 10
            elif steps_from_end == 2:
                # 倒数第三步
                if is_winner_step:
                    # 获胜前两步位置 +1
                    table[state][placed_pos] += 1
                else:
                    # 失败前两步以外的位置 -1
                    for pos in empty:
                        if pos != placed_pos:
                            table[state][pos] -= 1
            # 其他步骤都是 0，保持初始值

            # 确保所有空位都有条目（即使是 0）
            for pos in empty:
                if pos not in table[state]:
                    table[state][pos] += 0

    # 转为普通 dict
    return {k: dict(v) for k,v in table.items()}

# ============== GUI ==============

class TicTacToeGUI:
    def __init__(self, root):
        self.root = root
        root.title("井字棋 AI — 基于得分表学习（第25课）")
        root.configure(bg="#0f1729")
        root.geometry("950x620")

        # 训练
        print("正在生成棋谱并训练模型……")
        self.games = generate_games()
        self.table = train_score_table(self.games)
        print(f"棋谱总数：{len(self.games)}")
        print(f"训练状态数：{len(self.table)}")

        # 颜色
        self.C_BG = "#0f1729"
        self.C_PANEL = "#1a2440"
        self.C_PANEL2 = "#243054"
        self.C_INK = "#e6ecf5"
        self.C_MUTED = "#8b9bbf"
        self.C_ACCENT = "#fbbf24"
        self.C_BLUE = "#60a5fa"
        self.C_GOOD = "#34d399"
        self.C_WARN = "#f87171"

        # 状态
        self.state = ""        # 当前序列（如 "15"）
        self.human_player = 1  # 1=人先手, 2=AI先手
        self.game_over = False

        self._build_ui()
        self._reset_board()

    def _build_ui(self):
        # 顶部标题
        title_frame = tk.Frame(self.root, bg=self.C_BG)
        title_frame.pack(fill="x", padx=20, pady=(15,5))
        tk.Label(title_frame, text="第25课", bg=self.C_ACCENT, fg=self.C_BG,
                 font=("PingFang SC",10,"bold"), padx=10, pady=2).pack(side="left")
        tk.Label(title_frame, text="  井字棋 AI 对弈系统",
                 bg=self.C_BG, fg=self.C_INK,
                 font=("PingFang SC",16,"bold")).pack(side="left")
        tk.Label(title_frame,
                 text=f"模型：{len(self.table)} 状态 · {len(self.games)} 棋谱",
                 bg=self.C_BG, fg=self.C_MUTED,
                 font=("PingFang SC",11)).pack(side="right")

        # 主体两列
        main = tk.Frame(self.root, bg=self.C_BG)
        main.pack(fill="both", expand=True, padx=20, pady=10)

        # 左列：棋盘 + 控制
        left = tk.Frame(main, bg=self.C_BG)
        left.pack(side="left", fill="y")

        board_frame = tk.Frame(left, bg=self.C_PANEL,
                               highlightbackground="#2d3a5e", highlightthickness=1)
        board_frame.pack(pady=10)
        tk.Label(board_frame, text="棋盘（点击格子落子）",
                 bg=self.C_PANEL, fg=self.C_ACCENT,
                 font=("PingFang SC",11,"bold")).pack(pady=(12,8))

        grid = tk.Frame(board_frame, bg=self.C_PANEL)
        grid.pack(padx=20, pady=(0,16))

        self.btns = {}
        for pos in range(1,10):
            row = (pos-1)//3
            col = (pos-1)%3
            btn = tk.Button(grid, text=str(pos), width=4, height=2,
                            font=("Menlo",24,"bold"),
                            bg=self.C_PANEL2, fg=self.C_MUTED,
                            activebackground="#2c3a64",
                            relief="flat", bd=0,
                            command=lambda p=pos: self.on_click(p))
            btn.grid(row=row, column=col, padx=3, pady=3, ipadx=8, ipady=8)
            self.btns[pos] = btn

        # 状态栏
        self.status = tk.Label(left, text="", bg=self.C_BG, fg=self.C_INK,
                               font=("PingFang SC",12,"bold"), pady=8)
        self.status.pack()

        # 控制按钮
        ctrl = tk.Frame(left, bg=self.C_BG)
        ctrl.pack(pady=10)
        tk.Button(ctrl, text="🔄 新开一局（人先手）",
                  bg=self.C_ACCENT, fg=self.C_BG, font=("PingFang SC",11,"bold"),
                  relief="flat", padx=14, pady=6,
                  command=lambda: self._reset_board(human_first=True)).pack(side="left", padx=4)
        tk.Button(ctrl, text="🤖 新开一局（AI先手）",
                  bg=self.C_PANEL2, fg=self.C_INK, font=("PingFang SC",11),
                  relief="flat", padx=14, pady=6,
                  command=lambda: self._reset_board(human_first=False)).pack(side="left", padx=4)

        # 右列：AI 思考过程
        right = tk.Frame(main, bg=self.C_PANEL,
                         highlightbackground="#2d3a5e", highlightthickness=1)
        right.pack(side="right", fill="both", expand=True, padx=(20,0), pady=10)

        tk.Label(right, text="AI 思考过程", bg=self.C_PANEL, fg=self.C_ACCENT,
                 font=("PingFang SC",11,"bold"), anchor="w").pack(fill="x", padx=15, pady=(12,4))
        tk.Label(right, text="基于教材25.3评分规则训练的得分表",
                 bg=self.C_PANEL, fg=self.C_MUTED,
                 font=("PingFang SC",10), anchor="w").pack(fill="x", padx=15)

        # 当前状态显示
        self.state_lbl = tk.Label(right, text="", bg=self.C_PANEL2, fg=self.C_INK,
                                   font=("Menlo",13,"bold"), pady=10)
        self.state_lbl.pack(fill="x", padx=15, pady=10)

        # 得分表
        self.score_text = tk.Text(right, bg="#0a1224", fg=self.C_INK,
                                  font=("Menlo",11), bd=0, relief="flat",
                                  padx=14, pady=12, height=15, wrap="word")
        self.score_text.pack(fill="both", expand=True, padx=15, pady=(0,10))

        # 配置 tag 颜色
        self.score_text.tag_config("good", foreground=self.C_GOOD)
        self.score_text.tag_config("warn", foreground=self.C_WARN)
        self.score_text.tag_config("muted", foreground=self.C_MUTED)
        self.score_text.tag_config("accent", foreground=self.C_ACCENT, font=("Menlo",11,"bold"))
        self.score_text.tag_config("title", foreground=self.C_BLUE, font=("Menlo",12,"bold"))

        # 底部说明
        tk.Label(right,
                 text="💡 AI 选择得分最高的位置（多个并列时随机选一个）",
                 bg=self.C_PANEL, fg=self.C_MUTED,
                 font=("PingFang SC",10), anchor="w").pack(fill="x", padx=15, pady=(0,12))

    def _reset_board(self, human_first=True):
        self.state = ""
        self.human_player = 1 if human_first else 2
        self.game_over = False
        self._refresh_board()
        self._update_status()
        self._show_score_table()
        # AI 先手则立即落子
        if self.human_player == 2:
            self.root.after(400, self.ai_move)

    def _refresh_board(self):
        used_p1 = set(int(c) for i,c in enumerate(self.state) if i%2==0)
        used_p2 = set(int(c) for i,c in enumerate(self.state) if i%2==1)
        for pos, btn in self.btns.items():
            if pos in used_p1:
                btn.config(text="○", fg=self.C_BLUE, bg=self.C_PANEL2,
                           state="disabled", disabledforeground=self.C_BLUE)
            elif pos in used_p2:
                btn.config(text="×", fg=self.C_WARN, bg=self.C_PANEL2,
                           state="disabled", disabledforeground=self.C_WARN)
            else:
                btn.config(text=str(pos), fg=self.C_MUTED, bg=self.C_PANEL2,
                           state="normal" if not self.game_over else "disabled")

    def _update_status(self):
        winner = check_winner(self.state)
        if winner != 0:
            self.game_over = True
            who = "你" if winner == self.human_player else "AI"
            color = self.C_GOOD if winner == self.human_player else self.C_WARN
            self.status.config(text=f"🏆 {who} 获胜！", fg=color)
        elif len(self.state) == 9:
            self.game_over = True
            self.status.config(text="🤝 平局", fg=self.C_ACCENT)
        else:
            step = len(self.state) + 1
            current = 1 if step%2==1 else 2
            who = "你" if current == self.human_player else "AI"
            mark = "○" if current == 1 else "×"
            self.status.config(text=f"第 {step} 步 — 轮到 {who} ({mark})", fg=self.C_INK)

    def _show_score_table(self):
        self.state_lbl.config(text=f"当前状态：「{self.state or '空'}」  步数：{len(self.state)}")

        self.score_text.delete("1.0", "end")

        if self.game_over:
            self.score_text.insert("end", "本局结束\n", "muted")
            return

        if len(self.state) == 9:
            return

        scores = self.table.get(self.state, {})
        used = set(int(c) for c in self.state)
        empty = [p for p in range(1,10) if p not in used]

        # 当前下棋者
        current = 1 if len(self.state)%2==0 else 2
        is_ai_turn = (current != self.human_player)

        if is_ai_turn:
            self.score_text.insert("end", "🤖 AI 评估当前状态各位置得分：\n\n", "title")
        else:
            self.score_text.insert("end", "📊 当前状态下各位置的训练得分：\n\n", "title")

        if not scores:
            self.score_text.insert("end", "（该状态未在训练数据中出现，AI 将随机落子）\n", "muted")
            return

        # 显示得分
        sorted_scores = sorted(scores.items(), key=lambda x: -x[1])
        max_score = sorted_scores[0][1] if sorted_scores else 0

        for pos, score in sorted_scores:
            if pos not in empty: continue
            mark = "★" if score == max_score else "  "
            tag = "good" if score > 0 else ("warn" if score < 0 else "muted")
            if score == max_score:
                tag = "accent"
            self.score_text.insert("end",
                f"  {mark} 位置 {pos}：{score:+8.1f}\n", tag)

        if is_ai_turn:
            best_positions = [p for p,s in sorted_scores if s == max_score and p in empty]
            self.score_text.insert("end",
                f"\n→ 最高分位置：{best_positions}\n", "accent")
            self.score_text.insert("end",
                f"→ AI 将从中随机选择一个落子\n", "muted")

    def on_click(self, pos):
        if self.game_over: return
        # 当前应该是人下棋
        current = 1 if len(self.state)%2==0 else 2
        if current != self.human_player: return
        if str(pos) in self.state: return

        self.state += str(pos)
        self._refresh_board()
        self._update_status()
        self._show_score_table()

        if not self.game_over:
            self.root.after(500, self.ai_move)

    def ai_move(self):
        if self.game_over: return
        current = 1 if len(self.state)%2==0 else 2
        if current == self.human_player: return  # 不该 AI 下

        used = set(int(c) for c in self.state)
        empty = [p for p in range(1,10) if p not in used]

        scores = self.table.get(self.state, {})
        if scores:
            valid_scores = [(p, scores.get(p, 0)) for p in empty]
            max_s = max(s for _,s in valid_scores)
            best = [p for p,s in valid_scores if s == max_s]
            choice = random.choice(best)
        else:
            # 兜底：未见过的状态随机选
            choice = random.choice(empty)

        self.state += str(choice)
        self._refresh_board()
        self._update_status()
        self._show_score_table()


def main():
    root = tk.Tk()
    app = TicTacToeGUI(root)
    root.mainloop()


if __name__ == "__main__":
    main()
