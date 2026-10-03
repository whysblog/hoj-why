<template>
  <el-card shadow class="quiz-page quiz-admin">
    <div slot="header" class="clearfix">
      <span class="panel-title">套卷组合管理</span>
      <el-button
        style="float: right"
        type="primary"
        size="small"
        icon="el-icon-plus"
        @click="openCreate"
      >
        新建套卷
      </el-button>
    </div>

    <el-row :gutter="10" style="margin-bottom: 12px">
      <el-col :span="8">
        <el-input
          v-model="keyword"
          placeholder="标题关键字"
          clearable
          size="small"
          @keyup.enter.native="search"
        />
      </el-col>
      <el-col :span="6">
        <el-select
          v-model="statusFilter"
          placeholder="状态"
          clearable
          size="small"
          style="width: 100%"
        >
          <el-option label="全部" :value="null" />
          <el-option label="公开" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>
      </el-col>
      <el-col :span="6">
        <el-select
          v-model="langCategoryFilter"
          placeholder="分类"
          clearable
          size="small"
          style="width: 100%"
        >
          <el-option label="C++" value="cpp" />
          <el-option label="Python" value="python" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" size="small" @click="search">查询</el-button>
      </el-col>
    </el-row>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column
        prop="title"
        label="标题"
        min-width="200"
        show-overflow-tooltip
      />
      <el-table-column
        prop="author"
        label="作者"
        width="120"
        show-overflow-tooltip
      />
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="{ row }"
          ><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{
            row.status === 1 ? "公开" : "隐藏"
          }}</el-tag></template
        >
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="openEdit(row)"
            >编辑</el-button
          >
          <el-button
            type="text"
            size="small"
            style="color: #f56c6c"
            @click="remove(row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 12px; text-align: right"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="limit"
      :current-page.sync="page"
      @current-change="load"
    />

    <el-dialog
      :title="dialogTitle"
      :visible.sync="visible"
      width="860px"
      :before-close="closeDialog"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form
        :disabled="saving"
        ref="formRef"
        :model="paperForm"
        label-width="100px"
        size="small"
      >
        <el-form-item label="标题" required>
          <el-input v-model="paperForm.title" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="paperForm.description"
            type="textarea"
            :rows="3"
            placeholder="可选，支持 Markdown"
          />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="paperForm.author" maxlength="255" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="paperForm.langCategory"
            clearable
            style="width: 160px"
          >
            <el-option label="C++" value="cpp" />
            <el-option label="Python" value="python" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="paperForm.status" style="width: 160px">
            <el-option :value="1" label="公开" />
            <el-option :value="0" label="隐藏" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-divider content-position="left"
        >题目顺序 · {{ orderedItems.length }} 题 /
        {{ totalScore }} 分</el-divider
      >
      <p class="quiz-muted">
        保存时同时保存全部、顺序与分值。公开套卷中的全部题目必须已公开。
      </p>
      <el-row :gutter="8" style="margin-bottom: 10px">
        <el-col :span="5">
          <el-select
            v-model="pickItemType"
            size="small"
            style="width: 100%"
            @change="onPickTypeChange"
          >
            <el-option label="客观题" value="quiz" />
            <el-option label="编程题" value="problem" />
          </el-select>
        </el-col>
        <el-col :span="13">
          <el-select
            v-model="pickQuestionId"
            filterable
            remote
            clearable
            reserve-keyword
            placeholder="搜索题目标题并添加"
            :remote-method="remoteSearchQuestions"
            :loading="searchLoading"
            style="width: 100%"
            @visible-change="onPickVisible"
          >
            <el-option
              v-for="item in searchOptions"
              :key="optionValue(item)"
              :label="optionLabel(item)"
              :value="optionValue(item)"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-button
            type="primary"
            size="small"
            :disabled="!pickQuestionId"
            @click="addPickedQuestion"
          >
            添加到卷尾
          </el-button>
        </el-col>
      </el-row>

      <el-table :data="orderedRows" border size="small" max-height="280">
        <el-table-column prop="sort" label="#" width="50" />
        <el-table-column prop="typeLabel" label="类型" width="90" />
        <el-table-column prop="id" label="题目ID" width="100" />
        <el-table-column
          prop="title"
          label="标题"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column label="分值" width="120">
          <template slot-scope="{ $index }">
            <el-input-number
              v-model="orderedItems[$index].score"
              :min="0"
              :max="1000"
              size="mini"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ $index }">
            <el-button
              type="text"
              size="small"
              :disabled="$index === 0"
              @click="moveUp($index)"
              >上移</el-button
            >
            <el-button
              type="text"
              size="small"
              :disabled="$index === orderedRows.length - 1"
              @click="moveDown($index)"
              >下移</el-button
            >
            <el-button
              type="text"
              size="small"
              style="color: #f56c6c"
              @click="removeAt($index)"
              >移除</el-button
            >
          </template>
        </el-table-column>
      </el-table>

      <span slot="footer">
        <el-button :disabled="saving" @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePaper"
          >保存全部</el-button
        >
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import api from "@/common/api";
import "@/styles/quiz.css";

const emptyPaper = () => ({
  id: null,
  title: "",
  description: "",
  author: "",
  status: 0,
  langCategory: "",
});

export default {
  name: "AdminQuizPaper",
  data() {
    return {
      loading: false,
      records: [],
      total: 0,
      page: 1,
      limit: 15,
      keyword: "",
      statusFilter: null,
      langCategoryFilter: null,
      visible: false,
      isEdit: false,
      saving: false,
      savingItems: false,
      paperForm: emptyPaper(),
      orderedItems: [],
      titleByKey: {},
      pickItemType: "quiz",
      pickQuestionId: null,
      searchOptions: [],
      searchLoading: false,
    };
  },
  computed: {
    totalScore() {
      return this.orderedItems.reduce(
        (s, i) => s + (i.score == null ? 100 : i.score),
        0
      );
    },
    dialogTitle() {
      return this.isEdit ? "编辑套卷" : "新建套卷";
    },
    orderedRows() {
      return this.orderedItems.map((item, i) => ({
        sort: i + 1,
        typeLabel: item.itemType === "problem" ? "编程题" : "客观题",
        id: item.questionId,
        score: item.score == null ? 100 : item.score,
        title:
          this.titleByKey[this.itemKey(item)] ||
          "（请搜索添加或重新打开后刷新标题）",
      }));
    },
  },
  mounted() {
    this.load();
  },
  methods: {
    search() {
      this.page = 1;
      this.load();
    },
    closeDialog(done) {
      if (!this.saving) done();
    },
    load() {
      this.loading = true;
      const params = { currentPage: this.page, limit: this.limit };
      if (this.keyword) params.keyword = this.keyword;
      if (this.statusFilter === 0 || this.statusFilter === 1)
        params.status = this.statusFilter;
      if (this.langCategoryFilter)
        params.langCategory = this.langCategoryFilter;
      api
        .admin_getQuizPaperList(params)
        .then((res) => {
          const data = res.data.data;
          this.records = data.records || [];
          this.total = data.total || 0;
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false;
        });
    },
    openCreate() {
      this.isEdit = false;
      this.paperForm = emptyPaper();
      this.orderedItems = [];
      this.titleByKey = {};
      this.pickQuestionId = null;
      this.searchOptions = [];
      this.visible = true;
    },
    openEdit(row) {
      this.isEdit = true;
      this.loading = true;
      api
        .admin_getQuizPaperDetail(row.id)
        .then((res) => {
          const body = res.data.data;
          const p = body.paper || {};
          this.paperForm = {
            id: p.id,
            title: p.title || "",
            description: p.description || "",
            author: p.author || "",
            status: p.status != null ? p.status : 1,
            langCategory: p.langCategory || "",
          };
          this.orderedItems =
            body.items && body.items.length
              ? body.items.map((item) => ({
                  itemType: item.itemType || "quiz",
                  questionId: item.questionId,
                  score: item.score == null ? 100 : item.score,
                }))
              : (body.questionIds || []).map((id) => ({
                  itemType: "quiz",
                  questionId: id,
                  score: 100,
                }));
          this.titleByKey = {};
          (body.items || []).forEach((item) => {
            if (item.title) {
              this.$set(this.titleByKey, this.itemKey(item), item.title);
            }
          });
          this.pickQuestionId = null;
          this.searchOptions = [];
          this.visible = true;
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false;
        });
    },
    onDialogClosed() {
      this.paperForm = emptyPaper();
      this.orderedItems = [];
      this.titleByKey = {};
    },
    itemKey(item) {
      return `${item.itemType || "quiz"}:${item.questionId}`;
    },
    optionValue(item) {
      return this.pickItemType === "problem" ? item.id || item.pid : item.id;
    },
    optionLabel(item) {
      const id = this.optionValue(item);
      const prefix =
        this.pickItemType === "problem" && item.problemId ? item.problemId : id;
      return `${prefix} - ${item.title}`;
    },
    onPickTypeChange() {
      this.pickQuestionId = null;
      this.searchOptions = [];
      this.remoteSearchQuestions("");
    },
    remoteSearchQuestions(q) {
      const token = (this.searchToken = (this.searchToken || 0) + 1);
      const itemType = this.pickItemType;
      this.searchLoading = true;
      const params = { currentPage: 1, limit: 40 };
      const kw = (q || "").trim();
      if (kw) params.keyword = kw;
      if (itemType === "problem") {
        params.auth = 1;
        params.source = "public";
      }
      const req =
        this.pickItemType === "problem"
          ? api.admin_getProblemList(params)
          : api.admin_getQuizList(params);
      req
        .then((res) => {
          if (token === this.searchToken && itemType === this.pickItemType)
            this.searchOptions = res.data.data.records || [];
        })
        .catch(() => {})
        .finally(() => {
          if (token === this.searchToken) this.searchLoading = false;
        });
    },
    onPickVisible(open) {
      if (open) {
        this.remoteSearchQuestions("");
      }
    },
    addPickedQuestion() {
      if (this.saving) return;
      const id = this.pickQuestionId;
      if (!id) return;
      const item = { itemType: this.pickItemType, questionId: id, score: 100 };
      if (
        this.orderedItems.some(
          (row) => this.itemKey(row) === this.itemKey(item)
        )
      ) {
        this.$message.warning("该题已在列表中");
        return;
      }
      const row = this.searchOptions.find((r) => this.optionValue(r) === id);
      this.orderedItems.push(item);
      if (row && row.title) {
        this.$set(this.titleByKey, this.itemKey(item), row.title);
      }
      this.pickQuestionId = null;
    },
    removeAt(index) {
      if (this.saving) return;
      this.orderedItems.splice(index, 1);
    },
    moveUp(index) {
      if (this.saving) return;
      if (index <= 0) return;
      const next = this.orderedItems.slice();
      const t = next[index - 1];
      next[index - 1] = next[index];
      next[index] = t;
      this.orderedItems = next;
    },
    moveDown(index) {
      if (this.saving) return;
      if (index >= this.orderedItems.length - 1) return;
      const next = this.orderedItems.slice();
      const t = next[index + 1];
      next[index + 1] = next[index];
      next[index] = t;
      this.orderedItems = next;
    },
    async savePaper() {
      if (this.saving) return;
      if (!this.paperForm.title.trim()) {
        this.$message.warning("请填写标题");
        return;
      }
      if (this.paperForm.status === 1 && !this.orderedItems.length) {
        this.$message.warning("公开套卷至少需要一道题");
        return;
      }
      this.saving = true;
      try {
        await api.admin_saveQuizPaper({
          paper: { ...this.paperForm },
          items: this.orderedItems.map((i) => ({ ...i })),
        });
        this.$message.success("套卷信息、题目顺序与分值已全部保存");
        this.visible = false;
        this.load();
      } catch (e) {
      } finally {
        this.saving = false;
      }
    },
    remove(row) {
      this.$confirm("确定删除该套卷？", "提示", { type: "warning" })
        .then(() => api.admin_deleteQuizPaper(row.id))
        .then(() => {
          this.$message.success("已删除");
          this.load();
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.panel-title {
  font-size: 1.2rem;
  font-weight: 600;
}
</style>
