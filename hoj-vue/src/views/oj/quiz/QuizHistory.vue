<template>
  <QuizShell
    title="作答记录"
    active="history"
    subtitle="保留每一次尝试，回顾答案与解析。"
    ><section class="quiz-panel" v-loading="loading">
      <div class="quiz-toolbar">
        <el-select
          v-model="kind"
          clearable
          placeholder="全部练习"
          @change="
            page = 1;
            load();
          "
          ><el-option label="单题" value="quiz" /><el-option
            label="套卷"
            value="paper" /></el-select
        ><el-button @click="load">刷新记录</el-button>
      </div>
      <div v-for="a in records" :key="a.id" class="quiz-history-row">
        <div>
          <span class="quiz-pill">{{
            a.kind === "paper" ? "套卷" : "单题"
          }}</span>
          <h3>
            <router-link :to="'/quiz/history/' + a.id">{{
              a.title
            }}</router-link>
          </h3>
          <span class="quiz-muted"
            >{{ a.gmtCreate }} ·
            {{ a.kind === "paper" ? "客观题答对" : "答对" }}
            {{ a.correctCount }} / {{ a.questionCount }}</span
          >
        </div>
        <div>
          <strong>{{ a.score }} / {{ a.maxScore }}</strong>
          <p>
            <router-link :to="'/quiz/history/' + a.id">查看解析 →</router-link>
          </p>
        </div>
      </div>
      <div v-if="!loading && !records.length" class="quiz-empty">
        {{ failed ? "加载失败，请重试" : "还没有作答记录，开始一次练习吧" }}
      </div>
      <el-pagination
        layout="prev, pager, next"
        :current-page.sync="page"
        :page-size="20"
        :total="total"
        @current-change="load"
      /></section
  ></QuizShell>
</template>
<script>
import api from "@/common/api";
import QuizShell from "@/components/oj/quiz/QuizShell.vue";
export default {
  components: { QuizShell },
  data: () => ({
    records: [],
    total: 0,
    page: 1,
    kind: "",
    loading: false,
    failed: false,
    request: 0,
  }),
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      const token = ++this.request;
      this.loading = true;
      this.failed = false;
      try {
        const res = await api.getQuizHistory({
          currentPage: this.page,
          limit: 20,
          kind: this.kind,
        });
        if (token !== this.request) return;
        this.records = res.data.data.records || [];
        this.total = res.data.data.total || 0;
      } catch (e) {
        this.failed = true;
        this.records = [];
      } finally {
        if (token === this.request) this.loading = false;
      }
    },
  },
};
</script>
