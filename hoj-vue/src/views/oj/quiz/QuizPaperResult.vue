<template>
  <el-row :gutter="20">
    <el-col :md="20" :sm="24">
      <el-card shadow>
        <div slot="header">
          <span class="panel-title">答卷解析</span>
          <el-tag v-if="result.paperTitle" size="small" style="margin-left: 10px">{{ result.paperTitle }}</el-tag>
        </div>
        <el-alert v-if="result.message" :title="result.message" type="info" show-icon :closable="false" />
        <div v-if="!loaded" class="muted" style="margin-top: 16px;">未找到本次作答结果，请重新提交套卷。</div>
        <template v-else>
          <el-divider content-position="left">逐题对照</el-divider>
          <el-table :data="result.questionResults || []" border stripe style="width: 100%;">
            <el-table-column prop="no" label="#" width="56" align="center" />
            <el-table-column prop="title" label="题目" min-width="200" show-overflow-tooltip />
            <el-table-column label="题型" width="72" align="center">
              <template slot-scope="{ row }">
                {{ (row.questionType || 0) === 1 ? '多选' : '单选' }}
              </template>
            </el-table-column>
            <el-table-column label="结果" width="96" align="center">
              <template slot-scope="{ row }">
                <el-tag v-if="row.outcome === 'CORRECT'" type="success" size="small">正确</el-tag>
                <el-tag v-else-if="row.outcome === 'WRONG'" type="danger" size="small">错误</el-tag>
                <el-tag v-else type="info" size="small">未作答</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userAnswer" label="你的答案" width="110" align="center">
              <template slot-scope="{ row }">
                <span>{{ row.userAnswer || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="correctAnswer" label="正确答案" width="110" align="center" />
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" size="small" @click="goQuestion(row.questionId)">看解析</el-button>
              </template>
            </el-table-column>
          </el-table>
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
const storageKey = (paperId) => `hoj_quiz_paper_result_${paperId}`;

export default {
  name: 'QuizPaperResult',
  data() {
    return {
      loaded: false,
      result: {
        paperTitle: '',
        message: '',
        questionResults: [],
      },
    };
  },
  computed: {
    paperId() {
      return this.$route.params.paperId;
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
        this.result = { paperTitle: '', message: '', questionResults: [] };
        return;
      }
      try {
        this.result = JSON.parse(raw) || {};
        this.loaded = true;
      } catch (e) {
        this.loaded = false;
      }
    },
    redo() {
      this.$router.push({ name: 'QuizPaperDetail', params: { paperId: String(this.paperId) } });
    },
    goQuestion(qid) {
      this.$router.push({ name: 'QuizDetail', params: { quizId: String(qid) } });
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
</style>
