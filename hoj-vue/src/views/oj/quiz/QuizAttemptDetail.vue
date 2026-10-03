<template>
  <QuizShell
    :title="
      result.paperTitle ||
      (result.question && result.question.title) ||
      '作答复盘'
    "
    active="history"
    subtitle="查看本次提交时的题目、答案与解析。"
    ><section class="quiz-panel" v-loading="loading">
      <template v-if="loaded"
        ><div class="quiz-score-summary">
          <strong
            >{{ result.score }}
            <small>/ {{ result.maxScore }} 分</small></strong
          >
          <p>{{ result.submittedAt }}</p>
        </div>
        <div class="quiz-actions">
          <router-link
            :to="
              result.kind === 'paper'
                ? '/quiz/paper/' + result.resourceId
                : '/quiz/' + result.resourceId
            "
            >重新练习 →</router-link
          ><el-switch v-model="wrongOnly" active-text="只看未得满分" /></div
      ></template>
      <div v-else-if="!loading" class="quiz-empty">
        记录不存在或无权访问<el-button type="text" @click="fetch"
          >重试</el-button
        >
      </div>
    </section>
    <section
      v-for="(row, i) in rows"
      :key="row.questionId || row.pid || i"
      class="quiz-panel quiz-question-block"
    >
      <div class="quiz-meta">
        <span class="quiz-pill">第 {{ row.no || i + 1 }} 题</span
        ><span>{{
          row.itemType === "problem"
            ? "编程题"
            : row.questionType === 1
            ? "多选题"
            : "单选题"
        }}</span
        ><span>{{ row.score }} / {{ row.maxScore }} 分</span>
      </div>
      <h2>{{ row.title || (row.question && row.question.title) }}</h2>
      <template v-if="row.itemType === 'problem'"
        ><p>{{ row.judgeStatusName }} · {{ row.language || "未提交" }}</p>
        <router-link v-if="row.problemId" :to="'/problem/' + row.problemId"
          >查看编程题 →</router-link
        ></template
      ><template v-else
        ><Markdown
          :content="(row.question && row.question.description) || ''"
          :isAvoidXss="true" /><QuizOptions
          v-if="row.question"
          :options="row.question.options"
          :value="
            row.questionType === 1
              ? (row.userAnswer || '').split('')
              : row.userAnswer
          "
          :multiple="row.questionType === 1"
          reviewed
          :correct-answer="row.correctAnswer" />
        <p>
          你的选择：{{ row.userAnswer || "未作答" }} · 正确答案：{{
            row.correctAnswer
          }}
        </p>
        <div class="quiz-explanation">
          <h3>答案解析</h3>
          <Markdown
            :content="row.explanation || '暂无解析'"
            :isAvoidXss="true"
          /></div
      ></template>
    </section>
    <div v-if="loaded && !rows.length" class="quiz-panel quiz-empty">
      本次所有题目均已得满分。
    </div></QuizShell
  >
</template>
<script>
import api from "@/common/api";
import Markdown from "@/components/oj/common/Markdown";
import QuizShell from "@/components/oj/quiz/QuizShell.vue";
import QuizOptions from "@/components/oj/quiz/QuizOptions.vue";
export default {
  components: { Markdown, QuizShell, QuizOptions },
  data: () => ({
    result: {},
    loading: false,
    loaded: false,
    wrongOnly: false,
    request: 0,
  }),
  computed: {
    rows() {
      const r = this.result;
      const all =
        r.kind === "quiz"
          ? [
              {
                ...r,
                itemType: "quiz",
                questionType: r.question && r.question.questionType,
                title: r.question && r.question.title,
              },
            ]
          : r.itemResults || [];
      return this.wrongOnly ? all.filter((x) => x.score < x.maxScore) : all;
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    $route() {
      this.fetch();
    },
  },
  methods: {
    async fetch() {
      const token = ++this.request;
      this.loading = true;
      this.loaded = false;
      this.result = {};
      this.wrongOnly = false;
      try {
        const res = await api.getQuizAttempt(this.$route.params.attemptId);
        if (token !== this.request) return;
        this.result = res.data.data;
        this.loaded = true;
      } catch (e) {
      } finally {
        if (token === this.request) this.loading = false;
      }
    },
  },
};
</script>
