<template>
  <el-row :gutter="20">
    <el-col :md="18" :sm="24">
      <el-card v-loading="loading" shadow>
        <div slot="header">
          <span class="panel-title">{{ detail.title || '...' }}</span>
          <el-tag v-if="detail.difficulty !== undefined" size="small" style="margin-left: 10px">
            {{ levelLabel(detail.difficulty) }}
          </el-tag>
          <el-tag v-if="(detail.questionType || 0) === 1" size="small" type="warning" style="margin-left: 6px">
            多选
          </el-tag>
          <el-tag v-else size="small" type="info" style="margin-left: 6px">单选</el-tag>
        </div>
        <Markdown v-if="detail.description" :content="detail.description" :isAvoidXss="false"></Markdown>
        <div v-else class="muted">暂无题干说明</div>
        <el-divider></el-divider>
        <h4>请选择答案</h4>
        <div v-if="(detail.questionType || 0) === 1" class="quiz-options">
          <el-checkbox-group v-model="pickedMulti">
            <el-checkbox
              v-for="opt in detail.options"
              :key="opt.key"
              :label="opt.key"
              border
              class="quiz-check"
            >{{ opt.key }}. {{ opt.text }}</el-checkbox>
          </el-checkbox-group>
        </div>
        <el-radio-group v-else v-model="picked" class="quiz-options">
          <el-radio
            v-for="opt in detail.options"
            :key="opt.key"
            :label="opt.key"
            border
            class="quiz-radio"
          >{{ opt.key }}. {{ opt.text }}</el-radio>
        </el-radio-group>
        <div style="margin-top: 20px;">
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!canSubmitSingle"
            @click="submit"
          >
            提交答案
          </el-button>
          <el-button @click="$router.push({ name: 'QuizList' })">返回列表</el-button>
        </div>
        <el-alert
          v-if="resultMsg"
          :title="resultMsg"
          :type="resultOk ? 'success' : 'error'"
          show-icon
          style="margin-top: 16px;"
        >
          <template v-if="correctAnswer">
            <p>正确选项为：<strong>{{ correctAnswer }}</strong></p>
          </template>
        </el-alert>
        <div v-if="explanation" class="explanation-box">
          <div class="explanation-label">解析</div>
          <Markdown :content="explanation" :isAvoidXss="false" />
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import Markdown from '@/components/oj/common/Markdown';
import api from '@/common/api';
import { mapGetters } from 'vuex';

const LEVEL = { 0: '简单', 1: '中等', 2: '困难' };

export default {
  name: 'QuizDetail',
  components: { Markdown },
  data() {
    return {
      loading: false,
      submitting: false,
      detail: { options: [], questionType: 0 },
      picked: '',
      pickedMulti: [],
      resultMsg: '',
      resultOk: false,
      correctAnswer: '',
      explanation: '',
    };
  },
  computed: {
    ...mapGetters(['isAuthenticated']),
    quizId() {
      return this.$route.params.quizId;
    },
    canSubmitSingle() {
      if ((this.detail.questionType || 0) === 1) {
        return Array.isArray(this.pickedMulti) && this.pickedMulti.length >= 2;
      }
      return !!this.picked;
    },
  },
  mounted() {
    this.fetch();
  },
  watch: {
    quizId() {
      this.picked = '';
      this.pickedMulti = [];
      this.resultMsg = '';
      this.explanation = '';
      this.fetch();
    },
  },
  methods: {
    levelLabel(d) {
      return LEVEL[d] != null ? LEVEL[d] : d;
    },
    fetch() {
      this.loading = true;
      api
        .getQuizDetail(this.quizId)
        .then((res) => {
          this.detail = res.data.data || { options: [], questionType: 0 };
          this.picked = '';
          this.pickedMulti = [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    submit() {
      if (!this.isAuthenticated) {
        this.$store.commit('changeModalStatus', { mode: 'Login', visible: true });
        this.$message.warning(this.$i18n.t('m.Please_login_first'));
        return;
      }
      this.submitting = true;
      this.resultMsg = '';
      this.explanation = '';
      const answer =
        (this.detail.questionType || 0) === 1
          ? [...this.pickedMulti].sort().join('')
          : this.picked;
      api
        .submitQuizAnswer(this.quizId, answer)
        .then((res) => {
          const data = res.data.data;
          this.resultOk = data.correct;
          this.resultMsg = data.message || (data.correct ? '回答正确' : '回答错误');
          this.correctAnswer = data.correctAnswer || '';
          this.explanation = data.explanation || '';
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
.quiz-options {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
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
.explanation-box {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.explanation-label {
  font-weight: 600;
  margin-bottom: 8px;
}
</style>
