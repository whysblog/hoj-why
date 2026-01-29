<template>
  <el-card>
    <div slot="header">
      <span class="panel-title">证书管理</span>
      <el-button
        type="primary"
        size="small"
        style="float: right;"
        @click="openUploadDialog"
        icon="el-icon-plus"
      >上传证书</el-button>
    </div>

    <el-table :data="certificateList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="姓名" width="120"></el-table-column>
      <el-table-column prop="idCard" label="身份证号" width="180"></el-table-column>
      <el-table-column prop="certificateName" label="证书名称" min-width="200"></el-table-column>
      <el-table-column prop="gmtCreate" label="创建时间" width="180">
        <template slot-scope="scope">
          {{ scope.row.gmtCreate | localtime }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template slot-scope="scope">
          <el-button
            type="danger"
            size="mini"
            @click="deleteCertificate(scope.row.id)"
            icon="el-icon-delete"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="上传证书" :visible.sync="uploadDialogVisible" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard"></el-input>
        </el-form-item>
        <el-form-item label="证书名称">
          <el-input v-model="form.certificateName"></el-input>
        </el-form-item>
        <el-form-item label="证书文件">
          <el-upload
            class="upload-demo"
            action="/api/admin/certificate/upload"
            :headers="uploadHeaders"
            :data="form"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :before-upload="beforeUpload"
            :limit="1"
            :file-list="fileList"
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <div slot="tip" class="el-upload__tip">只能上传证书文件，且不超过50MB</div>
          </el-upload>
        </el-form-item>
      </el-form>
    </el-dialog>
  </el-card>
</template>

<script>
import api from '@/common/api'

export default {
  name: 'AdminCertificate',
  data() {
    return {
      certificateList: [],
      loading: false,
      uploadDialogVisible: false,
      form: {
        name: '',
        idCard: '',
        certificateName: ''
      },
      fileList: [],
      uploadHeaders: {
        Authorization: localStorage.getItem('token')
      }
    }
  },
  mounted() {
    this.getCertificateList()
  },
  methods: {
    getCertificateList() {
      this.loading = true
      api.admin_getCertificateList().then(res => {
        this.certificateList = res.data.data
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    openUploadDialog() {
      this.form = {
        name: '',
        idCard: '',
        certificateName: ''
      }
      this.fileList = []
      this.uploadDialogVisible = true
    },
    beforeUpload(file) {
      if (!this.form.name || !this.form.idCard || !this.form.certificateName) {
        this.$message.error('请先填写姓名、身份证号和证书名称')
        return false
      }
      const isLt50M = file.size / 1024 / 1024 < 50
      if (!isLt50M) {
        this.$message.error('上传文件大小不能超过 50MB!')
      }
      return isLt50M
    },
    handleUploadSuccess(res) {
      if (res.status === 200) {
        this.$message.success('上传成功')
        this.uploadDialogVisible = false
        this.getCertificateList()
      } else {
        this.$message.error(res.msg)
      }
    },
    handleUploadError() {
      this.$message.error('上传失败')
    },
    deleteCertificate(id) {
      this.$confirm('确定删除该证书吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        api.admin_deleteCertificate(id).then(res => {
          this.$message.success('删除成功')
          this.getCertificateList()
        })
      })
    }
  }
}
</script>

<style scoped>
.panel-title {
  font-size: 20px;
  font-weight: 500;
}
</style>
