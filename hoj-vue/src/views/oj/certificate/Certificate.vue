<template>
  <el-row :gutter="20">
    <el-col :span="24">
      <el-card>
        <div slot="header">
          <span class="panel-title">证书查询系统</span>
        </div>
        <div class="query-form">
          <el-form :inline="true" :model="query" class="demo-form-inline">
            <el-form-item label="姓名">
              <el-input v-model="query.name" placeholder="请输入姓名"></el-input>
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input v-model="query.idCard" placeholder="请输入身份证号"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onQuery" icon="el-icon-search">查询</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-table :data="certificates" v-loading="loading" style="width: 100%" border>
          <el-table-column prop="certificateName" label="证书名称" min-width="200"></el-table-column>
          <el-table-column prop="name" label="姓名" width="150"></el-table-column>
          <el-table-column prop="gmtCreate" label="上传时间" width="200">
            <template slot-scope="scope">
              {{ scope.row.gmtCreate | localtime }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template slot-scope="scope">
              <el-button type="success" size="small" @click="download(scope.row)" icon="el-icon-download">下载证书</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import api from '@/common/api'
import utils from '@/common/utils'

export default {
  name: 'Certificate',
  data() {
    return {
      query: {
        name: '',
        idCard: ''
      },
      certificates: [],
      loading: false
    }
  },
  methods: {
    onQuery() {
      if (!this.query.name || !this.query.idCard) {
        this.$message.error('姓名和身份证号不能为空')
        return
      }
      this.loading = true
      api.queryCertificates(this.query.name, this.query.idCard).then(res => {
        this.certificates = res.data.data
        this.loading = false
        if (this.certificates.length === 0) {
          this.$message.info('未查询到相关证书')
        }
      }).catch(() => {
        this.loading = false
      })
    },
    download(row) {
      let url = `/api/certificate/download?id=${row.id}`
      utils.downloadFile(url)
    }
  }
}
</script>

<style scoped>
.query-form {
  margin-bottom: 20px;
  text-align: center;
}
.panel-title {
  font-size: 20px;
  font-weight: 500;
}
</style>
