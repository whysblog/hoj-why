<template>
  <el-row :gutter="18">
    <el-col :sm="24" :md="18" :lg="18">
      <el-card shadow>
        <div slot="header">
          <el-row :gutter="20" style="margin-bottom: 0.5em;">
            <el-col :xs="24" :sm="8">
              <span class="panel-title">{{ $t('m.Problem_Bank') }}</span>
            </el-col>
            <el-col :xs="24" :sm="16" class="filter-mt" style="text-align: right;">
              <el-button-group>
                <el-button
                  size="small"
                  type="primary"
                  @click="$router.push({ name: 'QuizList' })"
                >客观题</el-button>
                <el-button
                  size="small"
                  type="default"
                  @click="$router.push({ name: 'QuizPaperList' })"
                >套卷组合</el-button>
              </el-button-group>
            </el-col>
          </el-row>
          <el-row :gutter="20" style="margin-bottom: 0.5em;">
            <el-col :xs="24" :sm="8">
              <el-input
                v-model="keyword"
                :placeholder="$t('m.Enter_keyword')"
                size="medium"
                clearable
                @keyup.enter.native="loadList"
                class="filter-mt"
              />
            </el-col>
            <el-col :xs="24" :sm="8" class="filter-mt" style="text-align: right;">
              <el-button type="primary" size="small" icon="el-icon-search" round @click="loadList">
                搜索
              </el-button>
              <el-button size="small" icon="el-icon-refresh" round @click="onReset">{{ $t('m.Reset') }}</el-button>
            </el-col>
          </el-row>
          <section>
            <b class="problem-filter">{{ $t('m.Level') }}</b>
            <div>
              <el-tag
                size="medium"
                class="filter-item"
                :effect="difficulty === null ? 'dark' : 'plain'"
                @click="setDifficulty(null)"
              >{{ $t('m.All') }}</el-tag>
              <el-tag
                v-for="lv in [0, 1, 2]"
                :key="lv"
                size="medium"
                class="filter-item"
                :effect="difficulty === lv ? 'dark' : 'plain'"
                @click="setDifficulty(lv)"
              >{{ QUIZ_LEVEL[lv] }}</el-tag>
            </div>
          </section>
        </div>
        <vxe-table
          border="inner"
          stripe
          auto-resize
          :data="records"
          :loading="loading"
          align="center"
        >
          <vxe-table-column field="id" width="80" title="#"></vxe-table-column>
          <vxe-table-column field="title" min-width="240" :title="$t('m.Title')" align="left">
            <template v-slot="{ row }">
              <el-link type="primary" @click="goDetail(row.id)">{{ row.title }}</el-link>
            </template>
          </vxe-table-column>
          <vxe-table-column field="questionType" width="90" title="题型">
            <template v-slot="{ row }">
              <el-tag v-if="(row.questionType || 0) === 1" size="small" type="warning">多选</el-tag>
              <el-tag v-else size="small" type="info">单选</el-tag>
            </template>
          </vxe-table-column>
          <vxe-table-column field="difficulty" width="100" :title="$t('m.Level')">
            <template v-slot="{ row }">
              <el-tag size="small" :type="difficultyTagType(row.difficulty)">
                {{ QUIZ_LEVEL[row.difficulty] || row.difficulty }}
              </el-tag>
            </template>
          </vxe-table-column>
          <vxe-table-column field="author" width="140" :title="$t('m.Author')"></vxe-table-column>
        </vxe-table>
        <Pagination
          :total="total"
          :page-size="limit"
          @on-change="changeRoute"
          :current.sync="page"
        ></Pagination>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import Pagination from '@/components/oj/common/Pagination';
import api from '@/common/api';

const QUIZ_LEVEL = {
  0: '简单',
  1: '中等',
  2: '困难',
};

export default {
  name: 'QuizList',
  components: { Pagination },
  data() {
    return {
      QUIZ_LEVEL,
      records: [],
      total: 0,
      page: 1,
      limit: 20,
      keyword: '',
      difficulty: null,
      loading: false,
    };
  },
  computed: {
    quizSingleSection() {
      const p = this.$route.path;
      return p === '/quiz' || (p.startsWith('/quiz/') && !p.startsWith('/quiz/paper'));
    },
    quizPaperSection() {
      return this.$route.path.startsWith('/quiz/paper');
    },
  },
  mounted() {
    this.parseRoute();
    this.loadList();
  },
  watch: {
    $route() {
      this.parseRoute();
      this.loadList();
    },
  },
  methods: {
    parseRoute() {
      this.page = parseInt(this.$route.query.page) || 1;
      this.keyword = this.$route.query.keyword || '';
      if (this.$route.query.difficulty === undefined || this.$route.query.difficulty === '') {
        this.difficulty = null;
      } else {
        const d = parseInt(this.$route.query.difficulty, 10);
        this.difficulty = Number.isNaN(d) ? null : d;
      }
    },
    changeRoute(page) {
      this.$router.push({
        path: '/quiz',
        query: {
          page,
          keyword: this.keyword || undefined,
          difficulty: this.difficulty === null ? undefined : this.difficulty,
        },
      });
    },
    setDifficulty(key) {
      this.difficulty = key;
      this.changeRoute(1);
    },
    onReset() {
      this.keyword = '';
      this.difficulty = null;
      this.changeRoute(1);
    },
    loadList() {
      this.loading = true;
      const params = {
        currentPage: this.page,
        limit: this.limit,
      };
      if (this.keyword) params.keyword = this.keyword;
      if (this.difficulty !== null) params.difficulty = this.difficulty;
      api
        .getQuizList(params)
        .then((res) => {
          const data = res.data.data;
          this.records = data.records || [];
          this.total = data.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    goDetail(id) {
      this.$router.push({ name: 'QuizDetail', params: { quizId: String(id) } });
    },
    difficultyTagType(d) {
      if (d === 0) return 'success';
      if (d === 2) return 'danger';
      return 'warning';
    },
  },
};
</script>

<style scoped>
.panel-title {
  font-size: 1.3rem;
  font-weight: 600;
}
.problem-filter {
  margin-right: 10px;
  font-weight: 600;
}
.filter-item {
  margin: 4px 6px 4px 0;
  cursor: pointer;
}
.filter-mt {
  margin-top: 6px;
}
</style>
