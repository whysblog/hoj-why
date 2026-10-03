<template>
  <QuizShell
    :title="detail.title || '单题练习'"
    active="quiz"
    subtitle="认真思考，再做出你的选择。"
  >
    <div class="quiz-layout">
      <section class="quiz-panel" v-loading="loading">
        <template v-if="detail.id"
          ><div class="quiz-meta">
            <span class="quiz-pill">{{
              detail.questionType === 1 ? "多选题" : "单选题"
            }}</span
            ><span>{{ ["简单", "中等", "困难"][detail.difficulty] }}</span
            ><span>{{ detail.author }}</span>
          </div>
          <Markdown
            :content="detail.description || '暂无题干说明'"
            :isAvoidXss="true"
          />
          <p class="quiz-muted">
            {{
              detail.questionType === 1
                ? "请选择所有正确选项，完全选对得分。"
                : "请选择一个正确选项。"
            }}
          </p>
          <QuizOptions
            v-model="picked"
            :options="detail.options"
            :multiple="detail.questionType === 1"
            :disabled="submitting"
            :reviewed="!!result"
            :correct-answer="result ? result.correctAnswer : ''"
          />
          <div class="quiz-actions">
            <el-button
              v-if="!result"
              type="primary"
              :disabled="!answer || loading"
              :loading="submitting"
              @click="submit"
              >提交答案</el-button
            ><el-button v-else type="primary" @click="retry">再做一次</el-button
            ><router-link
              v-if="result && result.attemptId"
              :to="'/quiz/history/' + result.attemptId"
              >查看本次记录 →</router-link
            >
          </div>
          <div v-if="result" class="quiz-explanation">
            <h3>
              {{ result.correct ? "回答正确" : "继续加油" }} ·
              {{ result.score }} / {{ result.maxScore }} 分
            </h3>
            <p>
              你的选择：{{ result.userAnswer }} · 正确答案：{{
                result.correctAnswer
              }}
            </p>
            <Markdown
              :content="result.explanation || '暂无解析'"
              :isAvoidXss="true"
            />
          </div>
        </template>
        <div v-else-if="!loading" class="quiz-empty">
          题目加载失败<el-button type="text" @click="fetch">重试</el-button>
        </div>
      </section>
      <aside class="quiz-panel quiz-sidebar">
        <h3>练习提示</h3>
        <p class="quiz-muted">
          选择会自动保存在当前浏览器，刷新后可继续作答。提交后可在作答记录中复盘。
        </p>
        <router-link to="/quiz">← 返回题库</router-link>
      </aside>
    </div></QuizShell
  >
</template>
<script>
import Markdown from "@/components/oj/common/Markdown";
import QuizShell from "@/components/oj/quiz/QuizShell.vue";
import QuizOptions from "@/components/oj/quiz/QuizOptions.vue";
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
  components: { Markdown, QuizShell, QuizOptions },
  data: () => ({
    detail: {},
    picked: "",
    result: null,
    loading: false,
    submitting: false,
    request: 0,
  }),
  computed: {
    ...mapGetters(["isAuthenticated", "userInfo"]),
    quizId() {
      return this.$route.params.quizId;
    },
    identity() {
      return this.isAuthenticated && this.userInfo
        ? this.userInfo.uid || this.userInfo.username
        : "guest";
    },
    key() {
      return draftKey("quiz", this.quizId, this.identity);
    },
    signature() {
      return questionSignature([this.detail]);
    },
    answer() {
      return answerText(this.picked);
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    quizId() {
      this.fetch();
    },
    identity() {
      this.fetch();
    },
    picked: {
      deep: true,
      handler() {
        if (this.detail.id && !this.loading && !this.result)
          saveDraft(this.key, this.signature, { picked: this.picked });
      },
    },
  },
  methods: {
    async fetch() {
      const token = ++this.request;
      this.loading = true;
      this.result = null;
      this.detail = {};
      this.picked = "";
      try {
        const res = await api.getQuizDetail(this.quizId);
        if (token !== this.request) return;
        this.detail = res.data.data || {};
        const saved = readDraft(this.key, this.signature).picked;
        this.picked =
          this.detail.questionType === 1
            ? Array.isArray(saved)
              ? saved
              : []
            : typeof saved === "string"
            ? saved
            : "";
      } catch (e) {
      } finally {
        if (token === this.request) this.loading = false;
      }
    },
    retry() {
      this.result = null;
      this.picked = this.detail.questionType === 1 ? [] : "";
      clearDraft(this.key);
    },
    async submit() {
      if (this.submitting || !this.answer) return;
      if (!this.isAuthenticated) {
        this.$store.commit("changeModalStatus", {
          mode: "Login",
          visible: true,
        });
        return;
      }
      const token = this.request,
        key = this.key;
      this.submitting = true;
      try {
        const res = await api.submitQuizAnswer(this.quizId, this.answer);
        if (token !== this.request) return;
        this.result = res.data.data;
        clearDraft(key);
      } catch (e) {
      } finally {
        this.submitting = false;
      }
    },
  },
};
</script>
