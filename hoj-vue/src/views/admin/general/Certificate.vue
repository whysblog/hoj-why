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
      <el-table-column label="操作" width="220">
        <template slot-scope="scope">
          <el-button
            type="primary"
            size="mini"
            @click="openEditDialog(scope.row)"
            icon="el-icon-edit"
          >修改</el-button>
          <el-button
            type="danger"
            size="mini"
            @click="deleteCertificate(scope.row.id)"
            icon="el-icon-delete"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="uploadMode === 'batch' ? '批量上传证书' : '上传证书'" :visible.sync="uploadDialogVisible" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上传方式">
          <el-radio-group v-model="uploadMode" @change="clearUploadFiles">
            <el-radio-button label="single">单个上传</el-radio-button>
            <el-radio-button label="batch">批量上传</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard"></el-input>
        </el-form-item>
        <el-form-item label="证书名称">
          <el-input v-model="form.certificateName" :placeholder="uploadMode === 'batch' ? '批量上传可留空，默认使用文件名' : ''"></el-input>
        </el-form-item>
        <el-form-item label="证书文件">
          <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :multiple="uploadMode === 'batch'"
            :limit="uploadMode === 'batch' ? 50 : 1"
            :file-list="fileList"
            :on-change="handleUploadFileChange"
            :on-remove="handleUploadFileRemove"
            :on-exceed="handleUploadExceed"
            :before-upload="beforeUpload"
          >
            <el-button size="small" type="primary">选择文件</el-button>
            <div slot="tip" class="el-upload__tip">{{ uploadTip }}</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUpload">开始上传</el-button>
      </div>
    </el-dialog>

    <el-dialog title="修改证书" :visible.sync="editDialogVisible" width="460px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="姓名">
          <el-input v-model="editForm.name"></el-input>
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="editForm.idCard"></el-input>
        </el-form-item>
        <el-form-item label="证书名称">
          <el-input v-model="editForm.certificateName"></el-input>
        </el-form-item>
        <el-form-item label="替换文件">
          <el-upload
            class="upload-demo"
            action=""
            :auto-upload="false"
            :limit="1"
            :file-list="editFileList"
            :on-change="handleEditFileChange"
            :on-remove="handleEditFileRemove"
            :on-exceed="handleEditFileExceed"
            :before-upload="beforeUpload"
          >
            <el-button size="small" type="primary">选择新文件</el-button>
            <div slot="tip" class="el-upload__tip">不选择文件则只修改证书信息；文件大小不超过50MB</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存修改</el-button>
      </div>
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
      submitting: false,
      uploadDialogVisible: false,
      editDialogVisible: false,
      uploadMode: 'single',
      form: {
        name: '',
        idCard: '',
        certificateName: ''
      },
      editForm: {
        id: null,
        name: '',
        idCard: '',
        certificateName: ''
      },
      fileList: [],
      editFileList: []
    }
  },
  computed: {
    uploadTip() {
      return this.uploadMode === 'batch'
        ? '最多可批量选择50个证书文件，单个文件不超过50MB；证书名称留空时使用各文件名'
        : '只能选择1个证书文件，且不超过50MB'
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
      this.uploadMode = 'single'
      this.fileList = []
      this.uploadDialogVisible = true
    },
    openEditDialog(row) {
      this.editForm = {
        id: row.id,
        name: row.name,
        idCard: row.idCard,
        certificateName: row.certificateName
      }
      this.editFileList = []
      this.editDialogVisible = true
    },
    beforeUpload(file) {
      const isLt50M = file.size / 1024 / 1024 < 50
      if (!isLt50M) {
        this.$message.error('上传文件大小不能超过 50MB!')
      }
      return isLt50M
    },
    handleUploadFileChange(file, fileList) {
      if (!this.beforeUpload(file.raw || file)) {
        this.fileList = fileList.filter(item => item.uid !== file.uid)
        return
      }
      this.fileList = this.uploadMode === 'batch' ? fileList : fileList.slice(-1)
    },
    handleUploadFileRemove(file, fileList) {
      this.fileList = fileList
    },
    handleUploadExceed() {
      this.$message.warning(this.uploadMode === 'batch' ? '一次最多上传50个证书文件' : '单个上传模式只能选择1个证书文件')
    },
    handleEditFileChange(file, fileList) {
      if (!this.beforeUpload(file.raw || file)) {
        this.editFileList = fileList.filter(item => item.uid !== file.uid)
        return
      }
      this.editFileList = fileList.slice(-1)
    },
    handleEditFileRemove(file, fileList) {
      this.editFileList = fileList
    },
    handleEditFileExceed() {
      this.$message.warning('修改证书时只能选择1个替换文件')
    },
    clearUploadFiles() {
      this.fileList = []
    },
    validateUploadForm() {
      if (!this.form.name || !this.form.idCard) {
        this.$message.error('请先填写姓名和身份证号')
        return false
      }
      if (this.uploadMode === 'single' && !this.form.certificateName) {
        this.$message.error('请先填写证书名称')
        return false
      }
      if (this.fileList.length === 0) {
        this.$message.error('请先选择证书文件')
        return false
      }
      return true
    },
    submitUpload() {
      if (!this.validateUploadForm()) {
        return
      }
      const data = new FormData()
      data.append('name', this.form.name)
      data.append('idCard', this.form.idCard)
      data.append('certificateName', this.form.certificateName || '')
      this.fileList.forEach(file => {
        data.append(this.uploadMode === 'batch' ? 'files' : 'file', file.raw)
      })

      this.submitting = true
      const request = this.uploadMode === 'batch'
        ? api.admin_batchUploadCertificate(data)
        : this.$http.post('/api/admin/certificate/upload', data)
      request.then(res => {
        if (res.data.status === 200) {
          this.$message.success(res.data.msg || res.data.data || '上传成功')
          this.uploadDialogVisible = false
          this.getCertificateList()
        } else {
          this.$message.error(res.data.msg || '上传失败')
        }
      }).catch(() => {
        this.$message.error('上传失败')
      }).finally(() => {
        this.submitting = false
      })
    },
    validateEditForm() {
      if (!this.editForm.name || !this.editForm.idCard || !this.editForm.certificateName) {
        this.$message.error('请填写姓名、身份证号和证书名称')
        return false
      }
      return true
    },
    submitEdit() {
      if (!this.validateEditForm()) {
        return
      }
      const data = new FormData()
      data.append('id', this.editForm.id)
      data.append('name', this.editForm.name)
      data.append('idCard', this.editForm.idCard)
      data.append('certificateName', this.editForm.certificateName)
      if (this.editFileList.length > 0) {
        data.append('file', this.editFileList[0].raw)
      }

      this.submitting = true
      api.admin_updateCertificate(data).then(res => {
        if (res.data.status === 200) {
          this.$message.success(res.data.msg || res.data.data || '修改成功')
          this.editDialogVisible = false
          this.getCertificateList()
        } else {
          this.$message.error(res.data.msg || '修改失败')
        }
      }).catch(() => {
        this.$message.error('修改失败')
      }).finally(() => {
        this.submitting = false
      })
    },
    deleteCertificate(id) {
      this.$confirm('确定删除该证书吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        api.admin_deleteCertificate(id).then(res => {
          if (res.data.status === 200) {
            this.$message.success('删除成功')
            this.getCertificateList()
          } else {
            this.$message.error(res.data.msg || '删除失败')
          }
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
