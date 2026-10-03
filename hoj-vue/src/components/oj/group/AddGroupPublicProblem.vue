<template>
  <div style="text-align:center">
    <div style="margin-bottom:10px">
      <span class="tips">{{ $t('m.Group_Add_From_Public_Problem_Tips') }}</span>
    </div>
    <vxe-input
      v-model="keyword"
      :placeholder="$t('m.Enter_keyword')"
      type="search"
      size="medium"
      @search-click="filterByKeyword"
      @keyup.enter.native="filterByKeyword"
      style="margin-bottom:10px"
    ></vxe-input>
    <vxe-table
      :data="problemList"
      :loading="loading"
      auto-resize
      stripe
      align="center"
    >
      <vxe-table-column title="ID" min-width="100" field="problemId">
      </vxe-table-column>
      <vxe-table-column min-width="150" :title="$t('m.Title')" field="title">
      </vxe-table-column>
      <vxe-table-column :title="$t('m.Option')" align="center" min-width="100">
        <template v-slot="{ row }">
          <el-tooltip effect="dark" :content="$t('m.Add')" placement="top">
            <el-button
              icon="el-icon-plus"
              size="mini"
              @click.native="addPublicProblem(row.pid)"
              :disabled="adding"
              type="primary"
            >
            </el-button>
          </el-tooltip>
        </template>
      </vxe-table-column>
    </vxe-table>
    <el-pagination
      class="page"
      layout="prev, pager, next, sizes"
      @current-change="currentChange"
      :page-size="limit"
      :current-page.sync="currentPage"
      :total="total"
      @size-change="onPageSizeChange"
      :page-sizes="[10, 30, 50, 100]"
    >
    </el-pagination>
  </div>
</template>
<script>
import api from '@/common/api';
import mMessage from '@/common/message';
import Pagination from '@/components/oj/common/Pagination';
export default {
  name: 'AddGroupPublicProblem',
  components: {
    Pagination,
  },
  props: {
    groupId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      currentPage: 1,
      limit: 10,
      total: 0,
      loading: false,
      adding: false,
      problemList: [],
      keyword: '',
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
      this.getPublicProblem();
    },
    onPageSizeChange(pageSize) {
      this.limit = pageSize;
      this.currentPage = 1;
      this.init();
    },
    currentChange(page) {
      this.currentPage = page;
      this.init();
    },
    getPublicProblem() {
      this.loading = true;
      let params = {
        keyword: this.keyword,
        currentPage: this.currentPage,
        limit: this.limit,
        oj: 'Mine',
      };
      api.getProblemList(params)
        .then((res) => {
          this.loading = false;
          this.total = res.data.data.total;
          this.problemList = res.data.data.records;
        })
        .catch(() => {
          this.loading = false;
        });
    },
    addPublicProblem(id) {
      if (this.adding) return;
      this.adding = true;
      let data = {
        pid: id,
        gid: this.groupId,
      };
      api.addGroupProblemFromPublic(data).then(
        (res) => {
          this.adding = false;
          mMessage.success(this.$i18n.t('m.Add_Successfully'));
          this.$emit('currentChangeProblem');
          this.currentChange(1);
        },
        () => { this.adding = false; }
      );
    },
    filterByKeyword() {
      this.currentPage = 1;
      this.getPublicProblem();
    },
  },
};
</script>
<style scoped>
.page {
  margin-top: 20px;
  text-align: right;
}
.tips {
  color: red;
  font-weight: bolder;
  font-size: 1rem;
}
</style>
