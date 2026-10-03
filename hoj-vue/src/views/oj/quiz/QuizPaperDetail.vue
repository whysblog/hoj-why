<template>
  <QuizShell
    :title="detail.title || '套卷练习'"
    active="paper"
    subtitle="按自己的节奏完成作答，提交后逐题复盘。"
    ><div class="quiz-layout">
      <div v-loading="loading">
        <section class="quiz-panel">
          <Markdown
            :content="
              detail.description || '选择题完全选对得分；编程题请先完成评测。'
            "
            :isAvoidXss="true"
          />
          <div v-if="!loading && !detail.id" class="quiz-empty">
            套卷加载失败<el-button type="text" @click="fetch"
              >重新加载</el-button
            >
          </div>
        </section>
        <section
          v-for="(item, i) in items"
          :id="'quiz-item-' + i"
          :key="item.itemType + '-' + item.questionId"
          class="quiz-panel quiz-question-block"
        >
          <div class="quiz-meta">
            <span class="quiz-pill">第 {{ i + 1 }} 题</span
            ><span>{{
              item.itemType === "problem"
                ? "编程题"
                : item.quizQuestion.questionType === 1
                ? "多选题"
                : "单选题"
            }}</span
            ><span>{{ item.score == null ? 100 : item.score }} 分</span>
          </div>
          <h2>
            {{ item.title || (item.quizQuestion && item.quizQuestion.title) }}
          </h2>
          <QuizProblemEmbed
            :key="identity + '-' + item.questionId"
            v-if="item.itemType === 'problem'"
            :problem-id="item.problemId"
            :pid="item.questionId"
            :max-score="item.score == null ? 100 : item.score"
            @status-change="onProblemStatus"
          />
          <template v-else
            ><Markdown
              :content="item.quizQuestion.description || ''"
              :isAvoidXss="true" /><QuizOptions
              :value="selections[item.questionId]"
              @input="$set(selections, item.questionId, $event)"
              :options="item.quizQuestion.options"
              :multiple="item.quizQuestion.questionType === 1"
              :disabled="submitting"
          /></template>
        </section>
      </div>
      <aside class="quiz-panel quiz-sidebar">
        <h3>答题卡</h3>
        <p class="quiz-muted">
          已作答 {{ answered }} / {{ items.length }} · 总分 {{ maxScore }}
        </p>
        <el-progress
          :percentage="
            items.length ? Math.round((answered / items.length) * 100) : 0
          "
          color="#167d75"
        />
        <div class="quiz-grid">
          <button
            v-for="(item, i) in items"
            :key="i"
            :class="{ answered: hasAnswer(item) }"
            @click="jump(i)"
            :aria-label="'跳到第' + (i + 1) + '题'"
          >
            {{ i + 1 }}
          </button>
        </div>
        <p class="quiz-muted">
          客观题选项自动保存。编程题请等待评测完成后再交卷。
        </p>
        <div class="quiz-actions">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="loading || !items.length || pending"
            @click="submit"
            >{{ pending ? "等待评测完成" : "提交答卷" }}</el-button
          ><el-button :disabled="submitting" @click="reset">清空选择</el-button>
        </div>
      </aside>
    </div></QuizShell
  >
</template>
<script>
import Markdown from "@/components/oj/common/Markdown";
import QuizShell from "@/components/oj/quiz/QuizShell.vue";
import QuizOptions from "@/components/oj/quiz/QuizOptions.vue";
import QuizProblemEmbed from "@/components/oj/quiz/QuizProblemEmbed.vue";
import api from "@/common/api";
import { mapGetters } from "vuex";
import {
  answerText,
  draftKey,
  readDraft,
  saveDraft,
  clearDraft,
  questionSignature,
} from "@/common/quiz";
export default {
  components: { Markdown, QuizShell, QuizOptions, QuizProblemEmbed },
  data: () => ({
    detail: {},
    selections: {},
    problemStatusMap: {},
    loading: false,
    submitting: false,
    request: 0,
  }),
  computed: {
    ...mapGetters(["isAuthenticated", "userInfo"]),
    paperId() {
      return this.$route.params.paperId;
    },
    identity() {
      return this.isAuthenticated && this.userInfo
        ? this.userInfo.uid || this.userInfo.username
        : "guest";
    },
    key() {
      return draftKey("paper", this.paperId, this.identity);
    },
    items() {
      return this.detail.items || [];
    },
    signature() {
      return (
        JSON.stringify(
          this.items.map((i) => [i.itemType, i.questionId, i.score])
        ) +
        questionSignature(
          this.items.filter((i) => i.quizQuestion).map((i) => i.quizQuestion)
        )
      );
    },
    answered() {
      return this.items.filter(this.hasAnswer).length;
    },
    pending() {
      return Object.values(this.problemStatusMap).some((s) => s.pending);
    },
    maxScore() {
      return this.items.reduce(
        (s, i) => s + (i.score == null ? 100 : i.score),
        0
      );
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    paperId() {
      this.fetch();
    },
    identity() {
      this.fetch();
    },
    selections: {
      deep: true,
      handler() {
        if (this.detail.id && !this.loading)
          saveDraft(this.key, this.signature, this.selections);
      },
    },
  },
  methods: {
    hasAnswer(item) {
      return item.itemType === "problem"
        ? !!(this.problemStatusMap[item.questionId] || {}).submitId
        : !!answerText(this.selections[item.questionId]);
    },
    jump(i) {
      const el = document.getElementById("quiz-item-" + i);
      if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
    },
    onProblemStatus(s) {
      this.$set(this.problemStatusMap, s.pid, s);
    },
    async fetch() {
      const token = ++this.request;
      this.loading = true;
      this.detail = {};
      this.selections = {};
      this.problemStatusMap = {};
      try {
        const res = await api.getQuizPaperDetail(this.paperId);
        if (token !== this.request) return;
        this.detail = res.data.data || {};
        const saved = readDraft(this.key, this.signature);
        const answers = {};
        this.items
          .filter((i) => i.quizQuestion)
          .forEach((i) => {
            const v = saved[i.questionId];
            answers[i.questionId] =
              i.quizQuestion.questionType === 1
                ? Array.isArray(v)
                  ? v
                  : []
                : typeof v === "string"
                ? v
                : "";
          });
        this.selections = answers;
      } catch (e) {
      } finally {
        if (token === this.request) this.loading = false;
      }
    },
    async reset() {
      try {
        await this.$confirm("清空本卷客观题选择？", "清空草稿", {
          type: "warning",
        });
        this.selections = {};
        clearDraft(this.key);
      } catch (e) {}
    },
    async submit() {
      if (this.submitting || this.pending) return;
      if (!this.isAuthenticated) {
        this.$store.commit("changeModalStatus", {
          mode: "Login",
          visible: true,
        });
        return;
      }
      this.submitting = true;
      const token = this.request,
        key = this.key;
      try {
        if (this.answered < this.items.length)
          await this.$confirm(
            "还有未作答题目，确定提交？未作答记为 0 分。",
            "提交答卷",
            { type: "warning" }
          );
        if (token !== this.request) return;
        const answers = {},
          problemSnapshots = {};
        this.items.forEach((i) => {
          if (i.itemType === "problem") {
            const s = this.problemStatusMap[i.questionId];
            if (s && s.submitId)
              problemSnapshots[i.questionId] = { submitId: s.submitId };
          } else
            answers[i.questionId] = answerText(this.selections[i.questionId]);
        });
        const res = await api.submitQuizPaper(this.paperId, {
          answers,
          problemSnapshots,
        });
        if (token !== this.request) return;
        clearDraft(key);
        this.$router.push("/quiz/history/" + res.data.data.attemptId);
      } catch (e) {
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
