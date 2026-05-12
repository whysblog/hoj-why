<template>
  <div class="real-name-auth-page">
    <div class="auth-container">
      <div class="auth-header">
        <h1>{{ $t('m.Real_Name_Authentication') }}</h1>
        <p>{{ $t('m.Real_Name_Required_Description') }}</p>
      </div>
      
      <el-card class="auth-card">
        <el-form
          :model="realNameForm"
          :rules="realNameRules"
          ref="realNameForm"
          label-width="120px"
          label-position="left"
        >
          <el-form-item :label="$t('m.Username')" prop="username">
            <el-input v-model="realNameForm.username" disabled></el-input>
          </el-form-item>
          
          <el-form-item :label="$t('真实姓名')" prop="realname" required>
            <el-input
              v-model="realNameForm.realname"
              :placeholder="$t('m.Please_Enter_Real_Name')"
              maxlength="20"
              show-word-limit
            ></el-input>
          </el-form-item>
          <el-form-item :label="$t('m.Phone')" prop="phone" required>
            <el-input
              v-model="realNameForm.phone"
              :placeholder="$t('m.Please_Enter_Phone_China')"
              maxlength="20"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button 
              type="primary" 
              @click="submitRealName"
              :loading="loading"
              size="large"
              style="width: 100%;"
            >
              {{ $t('m.Submit_Real_Name') }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
import api from '@/common/api'
import myMessage from '@/common/message'

export default {
  name: 'RealNameAuth',
  data() {
    return {
      loading: false,
      realNameForm: {
        username: '',
        realname: '',
        email: '',
        phone: '',
        school: '',
        studentId: ''
      },
      realNameRules: {
        realname: [
          { required: true, message: this.$i18n.t('m.Real_Name_Required'), trigger: 'blur' },
          { min: 2, max: 20, message: this.$i18n.t('m.Real_Name_Length_Error'), trigger: 'blur' },
          { pattern: /^[\u4e00-\u9fa5a-zA-Z\s]+$/, message: this.$i18n.t('m.Real_Name_Format_Error'), trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: this.$i18n.t('m.Email_Format_Error'), trigger: 'blur' }
        ],
        phone: [
          { 
            validator: this.validatePhoneNumber, 
            trigger: 'blur' 
          }
        ]
      },
      // 个人API配置
      personalAPI: {
        enabled: false, // 暂时禁用个人API，使用系统默认API
        baseURL: 'http://localhost:8888', // 你的个人API地址
        endpoint: '/api/real-name-auth' // 实名认证接口路径
      }
    }
  },
  mounted() {
    this.initForm()
  },
  methods: {
    // 验证手机号码格式
    validatePhoneNumber(rule, value, callback) {
      // 强制实名时手机号必填
      if (!value || value.trim() === '') {
        callback(new Error(this.$i18n.t('m.Phone_Required')))
        return
      }
      
      // 移除可能的+86前缀和空格
      const cleanPhone = value.replace(/^\+86\s*/, '').replace(/\s/g, '')
      
      // 验证中国手机号码格式：1开头，第二位是3-9，总共11位数字
      const phoneRegex = /^1[3-9]\d{9}$/
      
      if (!phoneRegex.test(cleanPhone)) {
        callback(new Error(this.$i18n.t('m.Phone_Format_Error_China')))
        return
      }
      
      // 验证通过，更新表单数据为清理后的格式
      this.realNameForm.phone = cleanPhone
      callback()
    },
    
    initForm() {
      const userInfo = this.$store.getters.userInfo
      if (userInfo) {
        this.realNameForm = {
          username: userInfo.username || '',
          realname: userInfo.realname || '',
          email: userInfo.email || '',
          phone: userInfo.phone || '',
          school: userInfo.school || '',
          studentId: userInfo.number || '' // 注意：使用number字段
        }
      }
    },
    
    submitRealName() {
      this.$refs.realNameForm.validate((valid) => {
        if (!valid) return
        
        this.loading = true
        
        // 构建完整的用户信息数据
        const userInfo = this.$store.getters.userInfo
        const updateData = {
          ...userInfo, // 保留现有用户信息
          realname: this.realNameForm.realname,
          email: this.realNameForm.email,
          phone: this.realNameForm.phone,
          school: this.realNameForm.school,
          number: this.realNameForm.studentId, // 注意：API中使用的是number字段
          gender: userInfo.gender || 'secrecy' // 确保有gender字段
        }
        
        // 过滤空值
        const filteredData = this.filterEmptyValue(updateData)
        
        // 使用个人设置的API提交实名认证信息
        this.submitToPersonalAPI(filteredData)
      })
    },
    
    // 过滤空值的方法
    filterEmptyValue(obj) {
      const filtered = {}
      Object.keys(obj).forEach(key => {
        if (obj[key] !== null && obj[key] !== undefined && obj[key] !== '') {
          filtered[key] = obj[key]
        }
      })
      return filtered
    },
    
    // 使用个人API提交实名认证
    submitToPersonalAPI(data) {
      if (!this.personalAPI.enabled) {
        // 如果个人API未启用，使用默认API
        api.changeUserInfo(data).then(
          (res) => {
            this.loading = false
            myMessage.success(this.$i18n.t('m.Real_Name_Submit_Success'))
            this.$store.dispatch('setUserInfo', res.data.data)
            this.$router.push({ name: 'ProblemList' })
          },
          (error) => {
            this.loading = false
            console.error('实名认证提交失败:', error)
            if (error.response && error.response.data) {
              myMessage.error(error.response.data.msg || this.$i18n.t('m.Real_Name_Submit_Failed'))
            } else {
              myMessage.error(this.$i18n.t('m.Real_Name_Submit_Failed'))
            }
          }
        )
        return
      }
      
      // 使用个人API
      const axios = require('axios')
      const personalAxios = axios.create({
        baseURL: this.personalAPI.baseURL,
        timeout: 10000
      })
      
      // 添加请求拦截器，设置认证头
      personalAxios.interceptors.request.use(config => {
        const token = localStorage.getItem('token')
        if (token) {
          config.headers.Authorization = token
        }
        config.headers['Content-Type'] = 'application/json'
        return config
      })
      
      personalAxios.post(this.personalAPI.endpoint, data)
        .then(response => {
          this.loading = false
          myMessage.success(this.$i18n.t('m.Real_Name_Submit_Success'))
          
          // 更新用户信息
          if (response.data && response.data.data) {
            this.$store.dispatch('setUserInfo', response.data.data)
          }
          
          // 跳转到问题列表页面（需要登录的页面）
          this.$router.push({ name: 'ProblemList' })
        })
        .catch(error => {
          this.loading = false
          console.error('个人API实名认证提交失败:', error)
          
          if (error.code === 'ECONNREFUSED' || error.message.includes('Network Error')) {
            // 个人API连接失败，自动切换到系统API
            console.log('个人API连接失败，自动切换到系统API')
            myMessage.warning('个人API连接失败，正在使用系统默认API重试...')
            this.submitToSystemAPI(data)
          } else if (error.response && error.response.data) {
            myMessage.error(error.response.data.msg || this.$i18n.t('m.Real_Name_Submit_Failed'))
          } else {
            myMessage.error(this.$i18n.t('m.Real_Name_Submit_Failed'))
          }
        })
    },
    
    // 使用系统默认API提交实名认证
    submitToSystemAPI(data) {
      api.changeUserInfo(data).then(
        (res) => {
          this.loading = false
          myMessage.success(this.$i18n.t('m.Real_Name_Submit_Success'))
          this.$store.dispatch('setUserInfo', res.data.data)
          this.$router.push({ name: 'ProblemList' })
        },
        (error) => {
          this.loading = false
          console.error('系统API实名认证提交失败:', error)
          if (error.response && error.response.data) {
            myMessage.error(error.response.data.msg || this.$i18n.t('m.Real_Name_Submit_Failed'))
          } else {
            myMessage.error(this.$i18n.t('m.Real_Name_Submit_Failed'))
          }
        }
      )
    }
  }
}
</script>

<style scoped>
.real-name-auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-container {
  width: 100%;
  max-width: 600px;
}

.auth-header {
  text-align: center;
  margin-bottom: 30px;
  color: white;
}

.auth-header h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
  font-weight: 300;
}

.auth-header p {
  font-size: 1.1rem;
  opacity: 0.9;
}

.auth-card {
  border-radius: 15px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: none;
}

.el-form-item {
  margin-bottom: 25px;
}

.el-input {
  border-radius: 8px;
}

.el-button {
  border-radius: 8px;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
}

.phone-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .auth-container {
    max-width: 100%;
  }
  
  .auth-header h1 {
    font-size: 2rem;
  }
  
  .auth-header p {
    font-size: 1rem;
  }
}
</style>
