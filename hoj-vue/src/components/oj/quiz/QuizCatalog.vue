<template>
  <QuizShell
    :title="paper ? '套卷练习' : '客观题练习'"
    :active="paper ? 'paper' : 'quiz'"
    subtitle="从一次选择开始，检验知识，理解每一道题。"
  >
    <section class="quiz-panel">
      <div class="quiz-toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索题目标题"
          clearable
          @keyup.enter.native="search"
        />
        <el-select
          v-model="langCategory"
          clearable
          placeholder="全部分类"
          @change="search"
          ><el-option label="C++" value="cpp" /><el-option
            label="Python"
            value="python"
        /></el-select>
        <el-select
          v-if="!paper"
          v-model="difficulty"
          clearable
          placeholder="全部难度"
          @change="search"
          ><el-option
            v-for="(l, i) in levels"
            :key="i"
            :label="l"
            :value="String(i)"
        /></el-select>
        <el-select
          v-if="!paper"
          v-model="questionType"
          clearable
          placeholder="全部题型"
          @change="search"
          ><el-option label="单选" value="0" /><el-option
            label="多选"
            value="1"
        /></el-select>
        <el-button type="primary" icon="el-icon-search" @click="search"
          >搜索</el-button
        >
      </div>
      <p class="quiz-muted">共 {{ total }} {{ paper ? "份套卷" : "道题目" }}</p>
      <div v-loading="loading" class="quiz-catalog">
        <router-link
          v-for="q in records"
          :key="q.id"
          class="quiz-tile"
          :to="paper ? '/quiz/paper/' + q.id : '/quiz/' + q.id"
        >
          <div class="quiz-meta">
            <span class="quiz-pill">{{
              paper ? "套卷" : q.questionType === 1 ? "多选题" : "单选题"
            }}</span
            ><span v-if="!paper">{{ levels[q.difficulty] }}</span
            ><span>{{
              q.langCategory === "cpp"
                ? "C++"
                : q.langCategory === "python"
                ? "Python"
                : "综合"
            }}</span>
          </div>
          <h2>{{ q.title }}</h2>
          <div class="quiz-muted">
            #{{ q.id }} · {{ q.author || "AKOJ 题库" }}
          </div>
          <span class="quiz-card-arrow">开始练习 →</span></router-link
        >
      </div>
      <div v-if="!loading && !records.length" class="quiz-empty">
        {{ failed ? "加载失败，请重试" : "暂无匹配内容，试试其他筛选条件"
        }}<el-button v-if="failed" type="text" @click="load"
          >重新加载</el-button
        >
      </div>
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="20"
        :current-page="page"
        @current-change="navigate"
      /></section
  ></QuizShell>
</template>
<script>
import api from "@/common/api";
import QuizShell from "./QuizShell.vue";
export default {
  components: { QuizShell },
  props: { paper: Boolean },
  data: () => ({
    records: [],
    total: 0,
    page: 1,
    keyword: "",
    langCategory: "",
    difficulty: "",
    questionType: "",
    loading: false,
    failed: false,
    request: 0,
    levels: ["简单", "中等", "困难"],
  }),
  mounted() {
    this.parse();
  },
  watch: {
    $route() {
      this.parse();
    },
  },
  methods: {
    parse() {
      const q = this.$route.query;
      this.page = Math.max(1, parseInt(q.page) || 1);
      ["keyword", "langCategory", "difficulty", "questionType"].forEach(
        (k) => (this[k] = q[k] || "")
      );
      this.load();
    },
    search() {
      this.navigate(1);
    },
    navigate(page) {
      const query = { page: String(page) };
      ["keyword", "langCategory", "difficulty", "questionType"].forEach((k) => {
        if (this[k] !== "") query[k] = this[k];
      });
      const route = { path: this.paper ? "/quiz/paper" : "/quiz", query };
      if (this.$router.resolve(route).route.fullPath === this.$route.fullPath)
        this.load();
      else this.$router.push(route);
    },
    async load() {
      const token = ++this.request;
      this.loading = true;
      this.failed = false;
      try {
        const params = {
          currentPage: this.page,
          limit: 20,
          keyword: this.keyword,
          langCategory: this.langCategory,
        };
        if (!this.paper) {
          params.difficulty = this.difficulty;
          params.questionType = this.questionType;
        }
        const res = await (this.paper
          ? api.getQuizPaperList(params)
          : api.getQuizList(params));
        if (token !== this.request) return;
        const data = res.data.data || {};
        this.records = data.records || [];
        this.total = data.total || 0;
      } catch (e) {
        if (token === this.request) {
          this.records = [];
          this.failed = true;
        }
      } finally {
        if (token === this.request) this.loading = false;
      }
    },
  },
};
</script>
