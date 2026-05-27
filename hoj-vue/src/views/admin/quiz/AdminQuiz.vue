<template>
  <el-card shadow>
    <div slot="header" class="clearfix">
      <span class="panel-title">客观题管理</span>
      <el-button style="float: right" type="primary" size="small" icon="el-icon-plus" @click="openCreate">
        新建题目
      </el-button>
    </div>
    <el-row :gutter="10" style="margin-bottom: 12px;">
      <el-col :span="8">
        <el-input v-model="keyword" placeholder="标题关键词" clearable size="small" @keyup.enter.native="load" />
      </el-col>
      <el-col :span="6">
        <el-select v-model="statusFilter" placeholder="状态" clearable size="small" style="width: 100%;">
          <el-option label="全部" :value="null" />
          <el-option label="公开" :value="1" />
          <el-option label="隐藏" :value="0" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" size="small" @click="load">查询</el-button>
      </el-col>
    </el-row>
    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="answer" label="答案" width="80" />
      <el-table-column prop="questionType" label="题型" width="80">
        <template slot-scope="{ row }">
          {{ (row.questionType || 0) === 1 ? '多选' : '单选' }}
        </template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="80">
        <template slot-scope="{ row }">{{ ['简', '中', '难'][row.difficulty] || row.difficulty }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="{ row }">{{ row.status === 1 ? '公开' : '隐藏' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button type="text" size="small" style="color: #f56c6c" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 12px; text-align: right;"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="limit"
      :current-page.sync="page"
      @current-change="load"
    />

    <el-dialog :title="dialogTitle" :visible.sync="visible" width="720px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" label-width="100px" size="small">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="题干说明">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="支持 Markdown / HTML" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.explanation" type="textarea" :rows="4" placeholder="提交后展示给学生，支持 Markdown" />
        </el-form-item>
        <el-form-item label="选项 A" required>
          <el-input v-model="form.optionA" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选项 B" required>
          <el-input v-model="form.optionB" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选项 C" required>
          <el-input v-model="form.optionC" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="选项 D" required>
          <el-input v-model="form.optionD" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.questionType">
            <el-radio :label="0">单选</el-radio>
            <el-radio :label="1">多选</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.questionType === 0" label="正确答案" required>
          <el-select v-model="form.answer" placeholder="选择" style="width: 120px;">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="正确答案" required>
          <el-select
            v-model="form.answerMulti"
            multiple
            collapse-tags
            placeholder="至少选两项"
            style="width: 220px;"
          >
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" style="width: 160px;">
            <el-option :value="0" label="简单" />
            <el-option :value="1" label="中等" />
            <el-option :value="2" label="困难" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px;">
            <el-option :value="1" label="公开" />
            <el-option :value="0" label="隐藏" />
          </el-select>
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model="form.author" maxlength="255" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </span>
    </el-dialog>
  </el-card>
</template>

<script>
import api from '@/common/api';

const emptyForm = () => ({
  id: null,
  title: '',
  description: '',
  explanation: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  questionType: 0,
  answer: 'A',
  answerMulti: ['A', 'B'],
  difficulty: 1,
  status: 1,
  author: '',
});

export default {
  name: 'AdminQuiz',
  data() {
    return {
      loading: false,
      records: [],
      total: 0,
      page: 1,
      limit: 15,
      keyword: '',
      statusFilter: null,
      visible: false,
      isEdit: false,
      saving: false,
      form: emptyForm(),
    };
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑客观题' : '新建客观题';
    },
  },
  mounted() {
    this.load();
  },
  methods: {
    load() {
      this.loading = true;
      const params = { currentPage: this.page, limit: this.limit };
      if (this.keyword) params.keyword = this.keyword;
      if (this.statusFilter === 0 || this.statusFilter === 1) params.status = this.statusFilter;
      api
        .admin_getQuizList(params)
        .then((res) => {
          const data = res.data.data;
          this.records = data.records || [];
          this.total = data.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    openCreate() {
      this.isEdit = false;
      this.form = emptyForm();
      this.visible = true;
    },
    openEdit(row) {
      this.isEdit = true;
      this.loading = true;
      api
        .admin_getQuizDetail(row.id)
        .then((res) => {
          const q = res.data.data;
          this.form = {
            id: q.id,
            title: q.title || '',
            description: q.description || '',
            explanation: q.explanation || '',
            optionA: q.optionA || '',
            optionB: q.optionB || '',
            optionC: q.optionC || '',
            optionD: q.optionD || '',
            questionType: q.questionType != null ? q.questionType : 0,
            answer: 'A',
            answerMulti: ['A', 'B'],
            difficulty: q.difficulty != null ? q.difficulty : 1,
            status: q.status != null ? q.status : 1,
            author: q.author || '',
          };
          const qt = this.form.questionType || 0;
          const ans = (q.answer || 'A').toUpperCase().replace(/[^ABCD]/g, '');
          if (qt === 0) {
            this.form.answer = ans.slice(0, 1) || 'A';
          } else {
            this.form.answerMulti = ans.split('').filter(Boolean);
            if (this.form.answerMulti.length < 2) {
              this.form.answerMulti = ['A', 'B'];
            }
          }
          this.visible = true;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    resetForm() {
      this.form = emptyForm();
    },
    save() {
      this.saving = true;
      const payload = { ...this.form };
      delete payload.id;
      delete payload.answerMulti;
      if (payload.questionType === 1) {
        const letters = [...new Set(this.form.answerMulti || [])].sort().join('');
        payload.answer = letters;
      }
      if ((payload.answer || '').length < 1) {
        this.$message.warning('请设置正确答案');
        this.saving = false;
        return;
      }
      if (payload.questionType === 1 && (payload.answer || '').length < 2) {
        this.$message.warning('多选题请至少选择两个选项');
        this.saving = false;
        return;
      }
      const req = this.isEdit
        ? api.admin_updateQuiz(this.form.id, { ...payload, id: this.form.id })
        : api.admin_createQuiz(payload);
      req
        .then(() => {
          this.$message.success('保存成功');
          this.visible = false;
          this.load();
        })
        .finally(() => {
          this.saving = false;
        });
    },
    remove(row) {
      this.$confirm('确定删除该题目？', '提示', { type: 'warning' })
        .then(() => api.admin_deleteQuiz(row.id))
        .then(() => {
          this.$message.success('已删除');
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
