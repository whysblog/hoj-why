<template>
  <el-row :gutter="20">
    <el-col :md="20" :sm="24">
      <el-card v-loading="loading" shadow>
        <div slot="header">
          <span class="panel-title">{{ detail.title || '...' }}</span>
          <el-tag v-if="detail.author" size="small" style="margin-left: 10px">{{ detail.author }}</el-tag>
        </div>

        <Markdown v-if="detail.description" :content="detail.description" :isAvoidXss="false"></Markdown>
        <div v-else class="muted">暂无套卷说明</div>
        <el-divider></el-divider>

        <div v-for="(item, idx) in paperItems" :key="item.itemType + '-' + item.questionId" class="q-block">
          <template v-if="item.itemType === 'problem'">
            <h4>
              第 {{ idx + 1 }} 题
              <el-tag size="mini" type="success">编程题</el-tag>
              <span class="q-title">{{ item.title }}</span>
              <el-tag v-if="item.problemId" size="mini" style="margin-left: 8px">{{ item.problemId }}</el-tag>
            </h4>
            <QuizProblemEmbed
              v-if="item.problemId"
              :problem-id="item.problemId"
              :pid="item.questionId"
              :max-score="100"
              @status-change="onProblemStatus"
            />
            <div v-else class="muted">题目信息不可用</div>
          </template>

          <template v-else>
            <h4>
              第 {{ idx + 1 }} 题
              <el-tag v-if="(item.quizQuestion.questionType || 0) === 1" size="mini" type="warning">多选</el-tag>
              <el-tag v-else size="mini" type="info">单选</el-tag>
              <span class="q-title">{{ item.quizQuestion.title }}</span>
            </h4>
            <Markdown
              v-if="item.quizQuestion.description"
              :content="item.quizQuestion.description"
              :isAvoidXss="false"
            ></Markdown>
            <div v-if="(item.quizQuestion.questionType || 0) === 1" class="quiz-options">
              <el-checkbox-group v-model="selections[item.quizQuestion.id]">
                <el-checkbox
                  v-for="opt in item.quizQuestion.options"
                  :key="item.quizQuestion.id + '-' + opt.key"
                  :label="opt.key"
                  border
                  class="quiz-check"
                >
                  {{ opt.key }}. {{ opt.text }}
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <el-radio-group v-else v-model="selections[item.quizQuestion.id]" class="quiz-options">
              <el-radio
                v-for="opt in item.quizQuestion.options"
                :key="item.quizQuestion.id + '-' + opt.key"
                :label="opt.key"
                border
                class="quiz-radio"
              >
                {{ opt.key }}. {{ opt.text }}
              </el-radio>
            </el-radio-group>
          </template>
        </div>

        <div style="margin-top: 24px;">
          <el-button type="primary" :loading="submitting" @click="submit">提交答卷</el-button>
          <el-button @click="$router.push({ name: 'QuizPaperList' })">返回套卷列表</el-button>
        </div>
        <p class="muted submit-hint">编程题请在本页提交评测；提交答卷后将汇总客观题判分与各编程题得分。</p>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import Markdown from '@/components/oj/common/Markdown';
import QuizProblemEmbed from '@/components/oj/quiz/QuizProblemEmbed.vue';
import api from '@/common/api';
import { mapGetters } from 'vuex';

const resultStorageKey = (paperId, token) => `hoj_quiz_paper_result_${paperId}_${token}`;
const createResultToken = () => Math.random().toString(36).slice(2, 10);

export default {
  name: 'QuizPaperDetail',
  components: { Markdown, QuizProblemEmbed },
  data() {
    return {
      loading: false,
      submitting: false,
      detail: { questions: [], items: [] },
      selections: {},
      problemStatusMap: {},
    };
  },
  computed: {
    ...mapGetters(['isAuthenticated']),
    paperId() {
      return this.$route.params.paperId;
    },
    paperItems() {
      if (this.detail.items && this.detail.items.length) {
        return this.detail.items
          .map((item) => {
            if (item.itemType === 'problem') return item;
            return {
              ...item,
              itemType: 'quiz',
              quizQuestion: item.quizQuestion || this.findQuestion(item.questionId),
            };
          })
          .filter((item) => item.itemType === 'problem' || item.quizQuestion);
      }
      return (this.detail.questions || []).map((q) => ({
        itemType: 'quiz',
        questionId: q.id,
        quizQuestion: q,
      }));
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    paperId() {
      this.selections = {};
      this.problemStatusMap = {};
      this.fetch();
    },
  },
  methods: {
    findQuestion(id) {
      return (this.detail.questions || []).find((q) => q.id === id);
    },
    onProblemStatus(payload) {
      if (!payload || !payload.pid) return;
      this.$set(this.problemStatusMap, String(payload.pid), payload);
    },
    initSelections(items) {
      const s = {};
      (items || []).forEach((item) => {
        if (item.itemType === 'problem') return;
        const q = item.quizQuestion;
        if (!q) return;
        if ((q.questionType || 0) === 1) {
          s[q.id] = [];
        } else {
          s[q.id] = '';
        }
      });
      this.selections = s;
    },
    fetch() {
      this.loading = true;
      api
        .getQuizPaperDetail(this.paperId)
        .then((res) => {
          this.detail = res.data.data || { questions: [], items: [] };
          this.initSelections(this.paperItems);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    buildAnswers() {
      const answers = {};
      this.paperItems.forEach((item) => {
        if (item.itemType === 'problem' || !item.quizQuestion) return;
        const q = item.quizQuestion;
        const raw = this.selections[q.id];
        if ((q.questionType || 0) === 1) {
          if (Array.isArray(raw) && raw.length >= 2) {
            answers[String(q.id)] = [...raw].sort().join('');
          }
        } else if (raw) {
          answers[String(q.id)] = raw;
        }
      });
      return answers;
    },
    buildProblemSnapshots() {
      const snapshots = {};
      this.paperItems.forEach((item) => {
        if (item.itemType !== 'problem') return;
        const pid = String(item.questionId);
        const st = this.problemStatusMap[pid];
        if (!st) return;
        snapshots[pid] = {
          status: st.status,
          score: st.score,
          language: st.language,
          maxScore: st.maxScore,
        };
      });
      return snapshots;
    },
    submit() {
      if (!this.isAuthenticated) {
        this.$store.commit('changeModalStatus', { mode: 'Login', visible: true });
        this.$message.warning(this.$i18n.t('m.Please_login_first'));
        return;
      }
      this.submitting = true;
      api
        .submitQuizPaper(this.paperId, {
          answers: this.buildAnswers(),
          problemSnapshots: this.buildProblemSnapshots(),
        })
        .then((res) => {
          const data = res.data.data || {};
          const token = createResultToken();
          if (!data.itemResults && data.questionResults) {
            data.itemResults = data.questionResults.map((r) => ({
              ...r,
              itemType: 'quiz',
            }));
          }
          try {
            sessionStorage.setItem(resultStorageKey(this.paperId, token), JSON.stringify(data));
          } catch (e) {
            /* ignore */
          }
          this.$router.push({
            name: 'QuizPaperResult',
            params: { paperId: String(this.paperId), resultToken: token },
          });
        })
        .finally(() => {
          this.submitting = false;
        });
    },
  },
};
</script>

<style scoped>
.panel-title {
  font-size: 1.25rem;
  font-weight: 600;
}
.q-block {
  margin-bottom: 28px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.q-title {
  margin-left: 8px;
  font-weight: normal;
  color: #606266;
}
.quiz-options {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 8px;
}
.quiz-radio {
  margin: 8px 0 !important;
  white-space: normal;
  height: auto;
  padding: 10px 16px;
}
.quiz-check {
  margin: 8px 0 !important;
  display: block;
}
.muted {
  color: #909399;
}
.submit-hint {
  margin-top: 12px;
  font-size: 13px;
}
</style>
