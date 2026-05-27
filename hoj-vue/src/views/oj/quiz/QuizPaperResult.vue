<template>
  <el-row :gutter="20">
    <el-col :md="20" :sm="24">
      <el-card shadow>
        <div slot="header">
          <span class="panel-title">答卷解析</span>
          <el-tag v-if="result.paperTitle" size="small" style="margin-left: 10px">{{ result.paperTitle }}</el-tag>
        </div>
        <el-alert v-if="result.message" :title="result.message" type="info" show-icon :closable="false" />
        <div v-if="loaded && problemLangSummary.length" class="lang-summary">
          <el-tag
            v-for="item in problemLangSummary"
            :key="item.lang"
            size="small"
            type="success"
            effect="plain"
          >
            {{ item.lang }}：{{ item.count }}题，{{ item.score }} / {{ item.maxScore }}
          </el-tag>
        </div>
        <div v-if="!loaded" class="muted" style="margin-top: 16px;">未找到本次作答结果，请重新提交套卷。</div>
        <template v-else>
          <el-divider content-position="left">逐题对照</el-divider>
          <div v-for="row in itemResults" :key="row.itemType + '-' + (row.questionId || row.pid || row.no)" class="result-block">
            <div class="result-head">
              <span class="result-no">第 {{ row.no }} 题</span>
              <el-tag size="mini" :type="row.itemType === 'problem' ? 'success' : 'info'">
                {{ row.itemType === 'problem' ? '编程题' : ((row.questionType || 0) === 1 ? '多选' : '单选') }}
              </el-tag>
              <span class="result-title">{{ row.title }}</span>
              <el-tag v-if="row.itemType === 'problem' && row.problemId" size="mini">{{ row.problemId }}</el-tag>
            </div>

            <template v-if="row.itemType === 'problem'">
              <div class="result-meta">
                <span>评测状态：</span>
                <el-tag size="small" :type="judgeTagType(row.judgeStatus)">{{ row.judgeStatusName || '—' }}</el-tag>
                <span class="score-line">得分 <strong>{{ row.score != null ? row.score : 0 }}</strong> / {{ row.maxScore != null ? row.maxScore : 100 }}</span>
              </div>
            </template>

            <template v-else>
              <div class="result-meta">
                <el-tag v-if="row.outcome === 'CORRECT'" type="success" size="small">正确</el-tag>
                <el-tag v-else-if="row.outcome === 'WRONG'" type="danger" size="small">错误</el-tag>
                <el-tag v-else type="info" size="small">未作答</el-tag>
                <span>你的答案：<strong>{{ row.userAnswer || '—' }}</strong></span>
                <span>正确答案：<strong>{{ row.correctAnswer || '—' }}</strong></span>
              </div>
              <div v-if="row.explanation" class="explanation-box">
                <div class="explanation-label">解析</div>
                <Markdown :content="row.explanation" :isAvoidXss="false" />
              </div>
              <div v-else class="muted explanation-empty">暂无解析</div>
            </template>
          </div>

          <div style="margin-top: 20px;">
            <el-button type="primary" @click="redo">再答一次</el-button>
            <el-button @click="$router.push({ name: 'QuizPaperList' })">套卷列表</el-button>
            <el-button @click="$router.push({ name: 'QuizList' })">单题列表</el-button>
          </div>
        </template>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import Markdown from '@/components/oj/common/Markdown';
import { JUDGE_STATUS } from '@/common/constants';

const storageKey = (paperId) => `hoj_quiz_paper_result_${paperId}`;

export default {
  name: 'QuizPaperResult',
  components: { Markdown },
  data() {
    return {
      loaded: false,
      result: {
        paperTitle: '',
        message: '',
        itemResults: [],
        questionResults: [],
      },
    };
  },
  computed: {
    paperId() {
      return this.$route.params.paperId;
    },
    itemResults() {
      if (this.result.itemResults && this.result.itemResults.length) {
        return this.result.itemResults;
      }
      return (this.result.questionResults || []).map((r) => ({
        ...r,
        itemType: 'quiz',
      }));
    },
    problemLangSummary() {
      const map = {};
      this.itemResults
        .filter((row) => row.itemType === 'problem')
        .forEach((row) => {
          const lang = this.normalizeLanguage(row.language);
          if (!map[lang]) {
            map[lang] = { lang, count: 0, score: 0, maxScore: 0 };
          }
          map[lang].count += 1;
          map[lang].score += Number(row.score || 0);
          map[lang].maxScore += Number(row.maxScore || 0);
        });
      return Object.values(map);
    },
  },
  mounted() {
    this.loadFromStorage();
  },
  watch: {
    paperId() {
      this.loadFromStorage();
    },
  },
  methods: {
    loadFromStorage() {
      const raw = sessionStorage.getItem(storageKey(this.paperId));
      if (!raw) {
        this.loaded = false;
        this.result = { paperTitle: '', message: '', itemResults: [], questionResults: [] };
        return;
      }
      try {
        this.result = JSON.parse(raw) || {};
        this.loaded = true;
      } catch (e) {
        this.loaded = false;
      }
    },
    judgeTagType(status) {
      const key = String(status);
      return (JUDGE_STATUS[key] && JUDGE_STATUS[key].type) || 'info';
    },
    redo() {
      this.$router.push({ name: 'QuizPaperDetail', params: { paperId: String(this.paperId) } });
    },
    normalizeLanguage(language) {
      const text = String(language || '').toLowerCase();
      if (!text) return '未提交';
      if (text.includes('python') || text.includes('pypy')) return 'Python';
      if (text.includes('c++') || text.includes('cpp') || text.includes('g++') || text.includes('clang++')) return 'C++';
      return language;
    },
  },
};
</script>

<style scoped>
.panel-title {
  font-size: 1.25rem;
  font-weight: 600;
}
.muted {
  color: #909399;
}
.lang-summary {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.result-block {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}
.result-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.result-no {
  font-weight: 600;
}
.result-title {
  color: #303133;
}
.result-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 14px;
  color: #606266;
}
.score-line {
  margin-left: 4px;
}
.explanation-box {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}
.explanation-label {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}
.explanation-empty {
  margin-top: 8px;
  font-size: 13px;
}
</style>
