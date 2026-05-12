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
        <div v-for="(q, idx) in detail.questions" :key="q.id" class="q-block">
          <h4>
            第 {{ idx + 1 }} 题
            <el-tag v-if="(q.questionType || 0) === 1" size="mini" type="warning">多选</el-tag>
            <el-tag v-else size="mini" type="info">单选</el-tag>
            <span class="q-title">{{ q.title }}</span>
          </h4>
          <Markdown v-if="q.description" :content="q.description" :isAvoidXss="false"></Markdown>
          <div v-if="(q.questionType || 0) === 1" class="quiz-options">
            <el-checkbox-group v-model="selections[q.id]">
              <el-checkbox
                v-for="opt in q.options"
                :key="q.id + '-' + opt.key"
                :label="opt.key"
                border
                class="quiz-check"
              >
                {{ opt.key }}. {{ opt.text }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <el-radio-group v-else v-model="selections[q.id]" class="quiz-options">
            <el-radio
              v-for="opt in q.options"
              :key="q.id + '-' + opt.key"
              :label="opt.key"
              border
              class="quiz-radio"
            >
              {{ opt.key }}. {{ opt.text }}
            </el-radio>
          </el-radio-group>
        </div>
        <div style="margin-top: 24px;">
          <el-button type="primary" :loading="submitting" @click="submit">提交整卷</el-button>
          <el-button @click="$router.push({ name: 'QuizPaperList' })">返回套卷列表</el-button>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import Markdown from '@/components/oj/common/Markdown';
import api from '@/common/api';
import { mapGetters } from 'vuex';

const resultStorageKey = (paperId) => `hoj_quiz_paper_result_${paperId}`;

export default {
  name: 'QuizPaperDetail',
  components: { Markdown },
  data() {
    return {
      loading: false,
      submitting: false,
      detail: { questions: [] },
      selections: {},
    };
  },
  computed: {
    ...mapGetters(['isAuthenticated']),
    paperId() {
      return this.$route.params.paperId;
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    paperId() {
      this.selections = {};
      this.fetch();
    },
  },
  methods: {
    initSelections(questions) {
      const s = {};
      (questions || []).forEach((q) => {
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
          this.detail = res.data.data || { questions: [] };
          this.initSelections(this.detail.questions);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    buildAnswers() {
      const answers = {};
      (this.detail.questions || []).forEach((q) => {
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
    submit() {
      if (!this.isAuthenticated) {
        this.$store.commit('changeModalStatus', { mode: 'Login', visible: true });
        this.$message.warning(this.$i18n.t('m.Please_login_first'));
        return;
      }
      this.submitting = true;
      const answers = this.buildAnswers();
      api
        .submitQuizPaper(this.paperId, answers)
        .then((res) => {
          const data = res.data.data;
          try {
            sessionStorage.setItem(resultStorageKey(this.paperId), JSON.stringify(data));
          } catch (e) {
            /* ignore quota */
          }
          this.$router.push({
            name: 'QuizPaperResult',
            params: { paperId: String(this.paperId) },
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
</style>
