<template>
  <el-row :gutter="18">
    <el-col :sm="24" :md="18" :lg="18">
      <el-card shadow>
        <div slot="header">
          <el-row :gutter="20" style="margin-bottom: 0.5em;">
            <el-col :xs="24" :sm="8">
              <span class="panel-title">{{ $t('m.NavBar_Objective_Quiz') }}</span>
            </el-col>
            <el-col :xs="24" :sm="16" class="filter-mt" style="text-align: right;">
              <el-button-group>
                <el-button
                  size="small"
                  :type="quizSingleSection ? 'primary' : 'default'"
                  @click="$router.push({ name: 'QuizList' })"
                >{{ $t('m.Quiz_Mode_Single') }}</el-button>
                <el-button
                  size="small"
                  :type="quizPaperSection ? 'primary' : 'default'"
                  @click="$router.push({ name: 'QuizPaperList' })"
                >{{ $t('m.Quiz_Mode_Paper') }}</el-button>
              </el-button-group>
            </el-col>
          </el-row>
          <el-row :gutter="20" style="margin-bottom: 0.5em;">
            <el-col :xs="24" :sm="10">
              <span class="sub-title">{{ $t('m.Quiz_Mode_Paper') }}</span>
            </el-col>
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
            <el-col :xs="24" :sm="6" class="filter-mt" style="text-align: right;">
              <el-button type="primary" size="small" icon="el-icon-search" round @click="loadList">
                搜索
              </el-button>
            </el-col>
          </el-row>
        </div>
        <vxe-table border="inner" stripe auto-resize :data="records" :loading="loading" align="center">
          <vxe-table-column field="id" width="80" title="#"></vxe-table-column>
          <vxe-table-column field="title" min-width="280" :title="$t('m.Title')" align="left">
            <template v-slot="{ row }">
              <el-link type="primary" @click="goDetail(row.id)">{{ row.title }}</el-link>
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

export default {
  name: 'QuizPaperList',
  components: { Pagination },
  data() {
    return {
      records: [],
      total: 0,
      page: 1,
      limit: 20,
      keyword: '',
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
    },
    changeRoute(page) {
      this.$router.push({
        path: '/quiz/paper',
        query: {
          page,
          keyword: this.keyword || undefined,
        },
      });
    },
    loadList() {
      this.loading = true;
      api
        .getQuizPaperList({
          currentPage: this.page,
          limit: this.limit,
          keyword: this.keyword || undefined,
        })
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
      this.$router.push({ name: 'QuizPaperDetail', params: { paperId: String(id) } });
    },
  },
};
</script>

<style scoped>
.panel-title {
  font-size: 1.3rem;
  font-weight: 600;
}
.sub-title {
  font-size: 1rem;
  color: #606266;
  font-weight: 500;
}
.filter-mt {
  margin-top: 6px;
}
</style>
