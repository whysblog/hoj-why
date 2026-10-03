<template>
  <div v-loading="loading" class="quiz-problem-embed">
    <el-row :gutter="16">
      <el-col :span="24" :md="12">
        <div class="embed-panel">
          <Markdown
            v-if="problemData.problem && problemData.problem.description"
            :content="problemData.problem.description"
            :isAvoidXss="true"
          />
          <div v-else class="muted">暂无题目描述</div>
          <template v-if="problemData.problem"
            ><section
              v-for="field in ['input', 'output', 'hint']"
              :key="field"
              v-show="problemData.problem[field]"
            >
              <h4>
                {{
                  { input: "输入说明", output: "输出说明", hint: "提示" }[field]
                }}
              </h4>
              <Markdown
                :content="problemData.problem[field] || ''"
                :isAvoidXss="true"
              />
            </section>
            <section
              v-for="(ex, i) in problemData.problem.examples || []"
              :key="i"
            >
              <h4>样例 {{ i + 1 }}</h4>
              <pre>{{ ex.input }}</pre>
              <pre>{{ ex.output }}</pre>
            </section></template
          >
        </div>
      </el-col>
      <el-col :span="24" :md="12">
        <div class="embed-panel code-panel">
          <CodeMirror
            v-if="problemData.problem && problemData.problem.id && language"
            :value.sync="code"
            :languages="problemData.languages"
            :language.sync="language"
            :theme.sync="theme"
            :height.sync="height"
            :fontSize.sync="fontSize"
            :tabSize.sync="tabSize"
            @resetCode="onResetToTemplate"
            @changeLang="onChangeLang"
            @changeTheme="onChangeTheme"
            :pid="problemData.problem.id"
            :type="'public'"
            :isAuthenticated="isAuthenticated"
            :isRemoteJudge="problemData.problem.isRemote"
            :submitDisabled="false"
          />
          <div class="embed-actions">
            <div v-if="statusVisible" class="status-line">
              <span>状态：</span>
              <el-tag :type="statusTagType" size="small">{{
                statusText
              }}</el-tag>
              <span v-if="displayScore !== null" class="score-text"
                >得分 {{ displayScore }} / {{ maxScore }}</span
              >
            </div>
            <el-button
              type="primary"
              size="small"
              :loading="submitting"
              :disabled="!isAuthenticated || submitting"
              @click="submitCode"
            >
              提交评测
            </el-button>
            <span v-if="!isAuthenticated" class="muted hint"
              >请先登录后提交</span
            >
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import Markdown from "@/components/oj/common/Markdown";
import CodeMirror from "@/components/oj/common/CodeMirror.vue";
import api from "@/common/api";
import utils from "@/common/utils";
import { JUDGE_STATUS, JUDGE_STATUS_RESERVE } from "@/common/constants";
import { mapGetters } from "vuex";

export default {
  name: "QuizProblemEmbed",
  components: { Markdown, CodeMirror },
  props: {
    problemId: { type: String, required: true },
    pid: { type: [Number, String], default: null },
    maxScore: { type: Number, default: 100 },
  },
  data() {
    return {
      loading: false,
      request: 0,
      submitting: false,
      problemData: { problem: null, languages: [] },
      code: "",
      language: "",
      theme: "solarized",
      height: 360,
      fontSize: 14,
      tabSize: 4,
      submissionId: "",
      result: { status: -10 },
      statusVisible: false,
      refreshStatus: null,
      displayScore: null,
      hasSubmittedInThisSession: false,
    };
  },
  computed: {
    ...mapGetters(["isAuthenticated"]),
    statusText() {
      const key = String(this.result.status);
      return (JUDGE_STATUS[key] && JUDGE_STATUS[key].name) || "Unknown";
    },
    statusTagType() {
      const key = String(this.result.status);
      return (JUDGE_STATUS[key] && JUDGE_STATUS[key].type) || "info";
    },
  },
  watch: {
    problemId: {
      immediate: true,
      handler() {
        this.loadProblem();
      },
    },
  },
  beforeDestroy() {
    this.request++;
    if (this.refreshStatus) {
      clearTimeout(this.refreshStatus);
    }
  },
  methods: {
    loadProblem() {
      if (!this.problemId) return;
      const token = ++this.request;
      if (this.refreshStatus) clearTimeout(this.refreshStatus);
      this.submissionId = "";
      this.submitting = false;
      this.code = "";
      this.problemData = { problem: null, languages: [] };
      this.loading = true;
      this.hasSubmittedInThisSession = false;
      this.statusVisible = false;
      this.displayScore = null;
      api
        .getProblem(this.problemId)
        .then((res) => {
          if (token !== this.request) return;
          const result = res.data.data || {};
          if (result.problem && result.problem.examples) {
            result.problem.examples = utils.stringToExamples(
              result.problem.examples
            );
          }
          this.problemData = result;
          this.initLanguageAndTemplate();
          this.loadStatus();
        })
        .catch(() => {})
        .finally(() => {
          if (token === this.request) this.loading = false;
        });
    },
    loadStatus() {
      if (!this.isAuthenticated || !this.problemData.problem) return;
      const token = this.request;
      const pid = this.problemData.problem.id;
      api
        .getUserProblemStatus([pid], false, null, null, true)
        .then((res) => {
          if (
            token !== this.request ||
            this.submitting ||
            this.hasSubmittedInThisSession
          )
            return;
          const map = res.data.data || {};
          const st = map[pid];
          if (st && st.status != null && st.status !== -10) {
            this.result.status = st.status;
            this.statusVisible = true;
          }
        })
        .catch(() => {});
    },
    initLanguageAndTemplate() {
      const langs = this.problemData.languages || [];
      if (langs.length) {
        if (!this.language || langs.indexOf(this.language) === -1) {
          this.language = langs[0];
        }
      }
      const tpl = this.problemData.codeTemplate;
      if (tpl && tpl[this.language]) {
        this.code = tpl[this.language];
      }
    },
    onChangeLang(newLang) {
      const tpl = this.problemData.codeTemplate || {};
      if (this.code === (tpl[this.language] || "")) {
        this.code = tpl[newLang] || "";
      }
      this.language = newLang;
    },
    onChangeTheme(newTheme) {
      this.theme = newTheme;
    },
    onResetToTemplate() {
      const tpl = this.problemData.codeTemplate;
      if (tpl && tpl[this.language]) {
        this.code = tpl[this.language];
        return;
      }
      api.getProblemCodeTemplate(this.problemData.problem.id).then((res) => {
        const list = res.data.data || [];
        const found = list.find((t) => t.language === this.language);
        if (found) this.code = found.code || "";
      });
    },
    submitCode() {
      if (!this.isAuthenticated) {
        this.$message.warning("请先登录");
        return;
      }
      if (!this.code || !this.code.trim()) {
        this.$message.error("代码不能为空");
        return;
      }
      if (!this.language) {
        this.$message.error("请选择编程语言");
        return;
      }
      const token = this.request;
      this.submitting = true;
      this.statusVisible = true;
      this.result = { status: 5 };
      this.submissionId = "";
      this.displayScore = null;
      this.hasSubmittedInThisSession = false;
      this.emitStatus(5, 0);
      api
        .submitCode({
          pid: this.problemId,
          language: this.language,
          code: this.code,
          cid: 0,
          isRemote: this.problemData.problem.isRemote,
        })
        .then((res) => {
          if (token !== this.request) return;
          this.submissionId = res.data.data && res.data.data.submitId;
          this.emitStatus(5, 0);
          this.checkSubmissionStatus();
        })
        .catch(() => {
          if (token !== this.request) return;
          this.submitting = false;
          this.statusVisible = false;
          this.emitStatus(-10, 0);
        });
    },
    checkSubmissionStatus() {
      if (this.refreshStatus) clearTimeout(this.refreshStatus);
      const token = this.request;
      const check = () => {
        if (token !== this.request) return;
        api
          .getSubmission(this.submissionId)
          .then((res) => {
            if (token !== this.request) return;
            const sub = res.data.data.submission;
            this.result.status = sub.status;
            const pending = [
              9,
              JUDGE_STATUS_RESERVE.Pending,
              JUDGE_STATUS_RESERVE.Compiling,
              JUDGE_STATUS_RESERVE.Judging,
            ];
            if (!pending.includes(sub.status)) {
              this.submitting = false;
              clearTimeout(this.refreshStatus);
              this.hasSubmittedInThisSession = true;
              const score = this.calcScore(sub);
              this.displayScore = score;
              this.emitStatus(sub.status, score);
            } else {
              this.refreshStatus = setTimeout(check, 2000);
            }
          })
          .catch(() => {
            if (token !== this.request) return;
            this.submitting = false;
            this.hasSubmittedInThisSession = false;
            this.statusVisible = false;
            this.emitStatus(-10, 0);
            this.$message.warning("获取评测结果失败，请重新提交评测");
          });
      };
      check();
    },
    calcScore(sub) {
      const max = this.maxScore == null ? 100 : this.maxScore;
      if (sub.status === 0) return max;
      if (
        this.problemData.problem &&
        this.problemData.problem.type === 1 &&
        sub.score != null &&
        sub.score > 0
      )
        return Math.round(
          Math.min(1, sub.score / (this.problemData.problem.ioScore || 100)) *
            max
        );
      return 0;
    },
    emitStatus(status, score) {
      this.$emit("status-change", {
        problemId: this.problemId,
        pid:
          this.pid || (this.problemData.problem && this.problemData.problem.id),
        language: this.language,
        status,
        score: score != null ? score : this.calcScore({ status, score }),
        maxScore: this.maxScore,
        submitId: this.hasSubmittedInThisSession ? this.submissionId : null,
        pending: this.submitting,
        submittedInThisSession: this.hasSubmittedInThisSession,
      });
    },
  },
};
</script>

<style scoped>
.quiz-problem-embed {
  margin-top: 8px;
}
.embed-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
  min-height: 200px;
}
.code-panel {
  margin-top: 12px;
}
@media (min-width: 992px) {
  .code-panel {
    margin-top: 0;
  }
}
.embed-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.score-text {
  color: #606266;
  font-size: 13px;
}
.muted {
  color: #909399;
}
.hint {
  font-size: 12px;
}
</style>
