<template>
  <div>
    <div class="filter-section">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-input
            v-model="query.keyword"
            :placeholder="$t('m.Enter_keyword')"
            @keyup.enter.native="getMainTrainingList"
            clearable
          >
            <el-button
              slot="append"
              icon="el-icon-search"
              @click="getMainTrainingList"
            ></el-button>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="query.auth"
            :placeholder="$t('m.Training_Auth')"
            @change="getMainTrainingList"
            clearable
          >
            <el-option
              :label="$t('m.All')"
              value=""
            ></el-option>
            <el-option
              :label="$t('m.Public_Training')"
              value="Public"
            ></el-option>
            <el-option
              :label="$t('m.Private_Training')"
              value="Private"
            ></el-option>
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="query.categoryId"
            :placeholder="$t('m.Category')"
            @change="getMainTrainingList"
            clearable
          >
            <el-option
              :label="$t('m.All')"
              value=""
            ></el-option>
            <el-option
              v-for="category in categoryList"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            ></el-option>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button
            type="primary"
            @click="getMainTrainingList"
            :loading="loading"
          >
            {{ $t('m.Search') }}
          </el-button>
        </el-col>
      </el-row>
    </div>

    <vxe-table
      border="inner"
      stripe
      auto-resize
      :data="mainTrainingList"
      :loading="loading"
      align="center"
      ref="mainTrainingTable"
      @checkbox-change="onSelectionChange"
      @checkbox-all="onSelectionChange"
    >
      <vxe-table-column
        type="checkbox"
        width="60"
        align="center"
      ></vxe-table-column>
      
      <vxe-table-column
        field="rank"
        :title="$t('m.Number')"
        min-width="60"
        show-overflow
      ></vxe-table-column>
      
      <vxe-table-column
        field="title"
        :title="$t('m.Title')"
        min-width="200"
        align="center"
      >
        <template v-slot="{ row }">
          <el-link type="primary" @click="viewTrainingDetail(row.id)">
            {{ row.title }}
          </el-link>
        </template>
      </vxe-table-column>

      <vxe-table-column
        field="auth"
        :title="$t('m.Auth')"
        min-width="100"
        align="center"
      >
        <template v-slot="{ row }">
          <el-tag :type="TRAINING_TYPE[row.auth]['color']" effect="dark">
            {{ $t('m.Training_' + row.auth) }}
          </el-tag>
        </template>
      </vxe-table-column>
      
      <vxe-table-column
        field="categoryName"
        :title="$t('m.Category')"
        min-width="130"
        align="center"
      >
        <template v-slot="{ row }">
          <el-tag
            size="medium"
            :style="
              'background-color: #fff;color: ' +
                row.categoryColor +
                ';border-color: ' +
                row.categoryColor +
                ';'
            "
          >{{ row.categoryName }}</el-tag>
        </template>
      </vxe-table-column>

      <vxe-table-column
        field="problemCount"
        :title="$t('m.Problem_Number')"
        min-width="100"
        align="center"
      ></vxe-table-column>
      
      <vxe-table-column
        field="author"
        :title="$t('m.Author')"
        min-width="130"
        align="center"
        show-overflow
      >
        <template v-slot="{ row }">
          <el-link type="info" @click="goUserHome(row.author)">
            {{ row.author }}
          </el-link>
        </template>
      </vxe-table-column>
      
      <vxe-table-column
        field="gmtModified"
        :title="$t('m.Recent_Update')"
        min-width="120"
        align="center"
        show-overflow
      >
        <template v-slot="{ row }">
          <el-tooltip
            :content="row.gmtModified | localtime"
            placement="top"
          >
            <span>{{ row.gmtModified | fromNow }}</span>
          </el-tooltip>
        </template>
      </vxe-table-column>
    </vxe-table>

    <div class="pagination-container">
      <Pagination
        :total="total"
        :page-size="limit"
        @on-change="currentChange"
        :current.sync="currentPage"
        :layout="'prev, pager, next, sizes'"
        @on-page-size-change="onPageSizeChange"
      ></Pagination>
    </div>

    <div class="action-section" v-if="selectedTrainings.length > 0">
      <el-alert
        :title="$t('m.Selected_Trainings') + ': ' + selectedTrainings.length"
        type="info"
        :closable="false"
        show-icon
      ></el-alert>
      <div class="action-buttons">
        <el-button
          type="primary"
          @click="copySelectedTrainings"
          :loading="copyLoading"
        >
          {{ $t('m.Copy_Selected_Trainings') }}
        </el-button>
        <el-button @click="clearSelection">
          {{ $t('m.Clear_Selection') }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/common/api';
import { TRAINING_TYPE } from '@/common/constants';
import mMessage from '@/common/message';
import Pagination from '@/components/oj/common/Pagination';

export default {
  name: 'CopyTrainingFromMain',
  components: {
    Pagination,
  },
  props: {
    groupId: {
      type: [String, Number],
      required: true,
    },
  },
  data() {
    return {
      query: {
        keyword: '',
        auth: '',
        categoryId: '',
      },
      mainTrainingList: [],
      selectedTrainings: [],
      categoryList: [],
      TRAINING_TYPE: {},
      loading: false,
      copyLoading: false,
      currentPage: 1,
      limit: 10,
      total: 0,
    };
  },
  mounted() {
    this.TRAINING_TYPE = Object.assign({}, TRAINING_TYPE);
    this.getTrainingCategoryList();
    this.getMainTrainingList();
  },
  methods: {
    getMainTrainingList() {
      this.loading = true;
      const params = {
        currentPage: this.currentPage,
        limit: this.limit,
        ...this.query,
      };
      
      // 过滤空值
      Object.keys(params).forEach(key => {
        if (params[key] === '' || params[key] === null || params[key] === undefined) {
          delete params[key];
        }
      });

      api.getTrainingList(this.currentPage, this.limit, params).then(
        (res) => {
          this.mainTrainingList = res.data.data.records;
          this.total = res.data.data.total;
          this.loading = false;
        },
        (err) => {
          this.loading = false;
        }
      );
    },

    getTrainingCategoryList() {
      api.getTrainingCategoryList().then((res) => {
        this.categoryList = res.data.data;
      });
    },

    currentChange(page) {
      this.currentPage = page;
      this.getMainTrainingList();
    },

    onPageSizeChange(pageSize) {
      this.limit = pageSize;
      this.currentPage = 1;
      this.getMainTrainingList();
    },

    onSelectionChange({ records }) {
      this.selectedTrainings = records;
    },

    async copySelectedTrainings() {
      if (this.selectedTrainings.length === 0) {
        mMessage.warning(this.$t('m.Please_Select_Trainings'));
        return;
      }

      this.copyLoading = true;
      
      try {
        // 逐个复制选中的训练课程，添加延迟确保每个课程都能被正确识别
        for (let i = 0; i < this.selectedTrainings.length; i++) {
          const training = this.selectedTrainings[i];
          console.log(`正在复制第 ${i + 1}/${this.selectedTrainings.length} 个课程: ${training.title}`);
          
          await this.copySingleTraining(training);
          
          // 如果不是最后一个课程，添加延迟确保服务器处理完成
          if (i < this.selectedTrainings.length - 1) {
            console.log('等待2秒后复制下一个课程...');
            await new Promise(resolve => setTimeout(resolve, 2000));
          }
        }
        
        mMessage.success(this.$t('m.Copy_Trainings_Successfully'));
        this.clearSelection();
        this.$emit('copySuccess');
      } catch (error) {
        console.error('复制训练课程失败:', error);
        mMessage.error(this.$t('m.Copy_Trainings_Failed'));
      } finally {
        this.copyLoading = false;
      }
    },

    async copySingleTraining(sourceTraining) {
      try {
        // 1. 获取主训练的详细信息
        console.log(`正在获取训练课程 ${sourceTraining.id} 的详细信息...`);
        const trainingDetailRes = await api.getTraining(sourceTraining.id);
        console.log('训练详情响应:', trainingDetailRes);
        const trainingDetail = trainingDetailRes.data.data;
        console.log('训练详情数据:', trainingDetail);
        
        // 2. 获取主训练的题目列表
        console.log(`正在获取训练课程 ${sourceTraining.id} 的题目列表...`);
        const problemListRes = await api.getTrainingProblemList(sourceTraining.id);
        console.log('题目列表响应:', problemListRes);
        const problemList = problemListRes.data.data;
        console.log('题目列表数据:', problemList);
        
        // 3. 创建新的团队训练课程
        console.log('团队ID:', this.groupId);
        console.log('用户信息:', this.$store.getters.userInfo);
        console.log('训练详情:', trainingDetail);
        
        // 检查必要字段
        if (!this.groupId) {
          throw new Error('团队ID不能为空');
        }
        if (!this.$store.getters.userInfo?.username) {
          throw new Error('用户信息不能为空');
        }
        
        // 确保分类ID正确
        let categoryId = trainingDetail.trainingCategory?.id || sourceTraining.categoryId;
        console.log('原始分类ID:', categoryId);
        console.log('训练详情中的分类:', trainingDetail.trainingCategory);
        console.log('源训练中的分类ID:', sourceTraining.categoryId);
        console.log('训练详情中的分类名称:', trainingDetail.categoryName);
        
        // 如果分类ID仍然为空，尝试从训练详情中获取
        if (!categoryId && trainingDetail.trainingCategoryId) {
          categoryId = trainingDetail.trainingCategoryId;
          console.log('从trainingCategoryId获取分类ID:', categoryId);
        }
        
        // 如果还是没有分类ID，但有分类名称，尝试从分类列表中查找
        if (!categoryId && trainingDetail.categoryName) {
          console.log('尝试根据分类名称查找分类ID:', trainingDetail.categoryName);
          try {
            const categoryListRes = await api.getTrainingCategoryList();
            const categoryList = categoryListRes.data.data;
            console.log('分类列表:', categoryList);
            
            const matchedCategory = categoryList.find(cat => cat.name === trainingDetail.categoryName);
            if (matchedCategory) {
              categoryId = matchedCategory.id;
              console.log('找到匹配的分类ID:', categoryId);
            }
          } catch (error) {
            console.error('获取分类列表失败:', error);
          }
        }
        
        // 如果还是没有分类ID，尝试使用第一个可用的分类作为默认值
        if (!categoryId) {
          console.warn('无法获取分类ID，尝试使用默认分类');
          try {
            const categoryListRes = await api.getTrainingCategoryList();
            const categoryList = categoryListRes.data.data;
            if (categoryList && categoryList.length > 0) {
              categoryId = categoryList[0].id;
              console.log('使用默认分类ID:', categoryId, '分类名称:', categoryList[0].name);
            } else {
              console.error('没有可用的分类，无法创建训练课程');
              throw new Error('系统中没有可用的训练分类，请联系管理员创建分类');
            }
          } catch (error) {
            console.error('获取默认分类失败:', error);
            throw new Error('无法获取训练分类信息，请稍后重试');
          }
        }
        
        // 优先使用训练详情中的权限信息，如果没有则使用源训练列表中的信息
        const sourceAuth = trainingDetail.auth || sourceTraining.auth || 'Public';
        
        // 尝试多种可能的密码字段名
        let sourcePrivatePwd = trainingDetail.privatePwd || 
                              trainingDetail.password || 
                              trainingDetail.privatePassword ||
                              sourceTraining.privatePwd || 
                              sourceTraining.password || 
                              sourceTraining.privatePassword || 
                              '';
        
        // 如果是私有课程但没有获取到密码，让用户输入密码
        if (sourceAuth === 'Private' && !sourcePrivatePwd) {
          console.warn('私有课程密码无法获取，需要用户输入密码');
          
          try {
            // 使用Element UI的输入框让用户输入密码
            const password = await this.$prompt('该课程是私有课程，请输入密码：', '输入密码', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              inputType: 'password',
              inputPlaceholder: '请输入课程密码',
              inputValidator: (value) => {
                if (!value || value.trim() === '') {
                  return '密码不能为空';
                }
                return true;
              }
            });
            
            // 如果用户输入了密码，使用用户输入的密码
            if (password && password.value) {
              sourcePrivatePwd = password.value.trim();
              console.log('用户输入了密码');
            } else {
              throw new Error('用户取消了密码输入');
            }
          } catch (error) {
            if (error === 'cancel') {
              console.log('用户取消了复制操作');
              throw new Error('用户取消了复制操作');
            } else {
              console.error('密码输入失败:', error);
              throw new Error('密码输入失败');
            }
          }
        }
        
        const newTrainingData = {
          training: {
            rank: sourceTraining.rank || 1000,
            title: sourceTraining.title + ' (副本)',
            description: trainingDetail.description || '',
            privatePwd: sourceAuth === 'Private' ? sourcePrivatePwd : '', // 保持私有密码
            auth: sourceAuth, // 保持原课程的权限类型
            gid: this.groupId, // 团队ID应该放在training对象内部
            author: this.$store.getters.userInfo.username, // 添加作者字段
          },
        };
        
        // 只有在有分类ID时才添加分类信息
        if (categoryId) {
          newTrainingData.trainingCategory = {
            id: categoryId,
          };
        }
        
        console.log('源课程完整信息:', sourceTraining);
        console.log('训练详情完整信息:', trainingDetail);
        console.log('源课程权限信息:', {
          auth: sourceTraining.auth,
          privatePwd: sourceTraining.privatePwd ? '***已设置***' : '未设置',
          password: sourceTraining.password ? '***已设置***' : '未设置',
          description: sourceTraining.description
        });
        console.log('训练详情权限信息:', {
          auth: trainingDetail.auth,
          privatePwd: trainingDetail.privatePwd ? '***已设置***' : '未设置',
          password: trainingDetail.password ? '***已设置***' : '未设置',
          privatePassword: trainingDetail.privatePassword ? '***已设置***' : '未设置'
        });
        console.log('创建训练课程数据:', newTrainingData);
        
        const createRes = await api.addGroupTraining(newTrainingData);
        console.log('创建训练课程响应:', createRes);
        
        // 检查响应数据结构
        if (!createRes || !createRes.data) {
          throw new Error('创建训练课程失败：响应数据为空');
        }
        
        // 尝试获取新创建的训练课程ID
        let newTrainingId = null;
        console.log('完整响应数据结构:', JSON.stringify(createRes.data, null, 2));
        
        // 尝试多种可能的ID获取路径
        if (createRes.data.data && createRes.data.data.id) {
          newTrainingId = createRes.data.data.id;
          console.log('从createRes.data.data.id获取ID:', newTrainingId);
        } else if (createRes.data.data && typeof createRes.data.data === 'number') {
          newTrainingId = createRes.data.data;
          console.log('从createRes.data.data获取ID:', newTrainingId);
        } else if (createRes.data.id) {
          newTrainingId = createRes.data.id;
          console.log('从createRes.data.id获取ID:', newTrainingId);
        } else if (createRes.data.training && createRes.data.training.id) {
          newTrainingId = createRes.data.training.id;
          console.log('从createRes.data.training.id获取ID:', newTrainingId);
        } else {
          // 如果没有返回ID，尝试通过查询最新创建的课程来获取ID
          console.warn('创建训练课程成功，但未返回课程ID，尝试通过查询获取');
          try {
            // 等待一小段时间让服务器完成创建
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            // 查询团队训练列表，获取新创建的课程（多页查询，避免只查首页漏掉）
            const normalizeTitle = (title) => {
              if (!title) {
                return '';
              }
              // 去除半角/全角空格及其他空白字符，避免标题空格差异导致匹配失败
              return title.replace(/[\s\u3000]+/g, '');
            };

            const originalTitle = sourceTraining.title.replace(' (副本)', '');
            const expectedCopyTitle = sourceTraining.title + ' (副本)';
            const normalizedOriginalTitle = normalizeTitle(originalTitle);
            const normalizedExpectedCopyTitle = normalizeTitle(expectedCopyTitle);

            const queryPageSize = 50;
            const maxQueryPages = 5;
            let trainingRecords = [];

            for (let page = 1; page <= maxQueryPages; page++) {
              const trainingListRes = await api.getGroupTrainingList(page, queryPageSize, this.groupId);
              console.log(`查询团队训练列表响应（第${page}页）:`, trainingListRes);
              const currentRecords = trainingListRes?.data?.data?.records || [];
              if (currentRecords.length === 0) {
                break;
              }
              trainingRecords = trainingRecords.concat(currentRecords);

              if (currentRecords.length < queryPageSize) {
                break;
              }
            }

            if (trainingRecords.length > 0) {
              console.log('团队训练列表（多页汇总）:', trainingRecords);

              const matchedCandidates = trainingRecords.filter((training) => {
                const normalizedTitle = normalizeTitle(training.title);
                return normalizedTitle === normalizedExpectedCopyTitle ||
                  (normalizedTitle.includes('(副本)') && normalizedTitle.includes(normalizedOriginalTitle));
              });

              if (matchedCandidates.length > 0) {
                // 同名副本可能存在多个，优先使用ID最大的（通常最新创建）
                const matchedTraining = matchedCandidates.reduce((latest, current) => {
                  return current.id > latest.id ? current : latest;
                });
                newTrainingId = matchedTraining.id;
                console.log('通过查询获取到新创建的课程ID:', newTrainingId);
                console.log('匹配的课程详情:', matchedTraining);
              } else {
                console.warn('无法找到匹配的课程，显示所有副本课程供调试');
                const allCopyTrainings = trainingRecords.filter(t => normalizeTitle(t.title).includes('(副本)'));
                console.log('所有副本课程:', allCopyTrainings);
                console.log('期望的副本标题:', expectedCopyTitle);
                console.log('原始标题:', originalTitle);

                // 如果找不到匹配的课程，跳过题目复制
                console.warn('无法找到匹配的课程，跳过题目复制');
                mMessage.warning(`训练课程"${sourceTraining.title}"创建成功，但无法复制题目（无法获取课程ID）`);
                return;
              }
            } else {
              console.warn('查询团队训练列表失败，跳过题目复制');
              mMessage.warning('训练课程创建成功，但无法复制题目（无法获取课程ID）');
              return;
            }
          } catch (error) {
            console.error('查询新创建课程失败:', error);
            mMessage.warning('训练课程创建成功，但无法复制题目（查询失败）');
            return;
          }
        }
        
        console.log('新创建的训练课程ID:', newTrainingId);
        
        // 4. 复制题目到新创建的团队训练课程
        console.log('题目列表:', problemList);
        if (problemList && problemList.length > 0) {
          let successCount = 0;
          let failCount = 0;
          
          for (const problem of problemList) {
            try {
              console.log(`正在复制题目 ${problem.displayId || problem.problemId} (PID: ${problem.pid}) 到训练课程 ${newTrainingId}`);
              console.log('题目数据结构:', problem);
              
              // 使用现有的添加公共题目到训练课程的接口
              const addResult = await api.addGroupTrainingProblemFromPublic({
                pid: problem.pid,
                tid: newTrainingId,
                displayId: problem.displayId || problem.problemId, // 添加displayId字段
              });
              
              console.log(`题目 ${problem.displayId} 复制成功:`, addResult);
              successCount++;
            } catch (error) {
              console.error(`复制题目 ${problem.displayId || problem.problemId} (PID: ${problem.pid}) 失败:`, error);
              if (error.response) {
                console.error('错误详情:', error.response.data);
                console.error('错误状态码:', error.response.status);
                console.error('错误响应头:', error.response.headers);
              }
              failCount++;
              // 继续复制其他题目，不中断整个过程
            }
          }
          
          console.log(`题目复制完成: 成功 ${successCount} 个，失败 ${failCount} 个`);
          if (failCount > 0) {
            mMessage.warning(`题目复制完成：成功 ${successCount} 个，失败 ${failCount} 个`);
          }
        } else {
          console.log('没有题目需要复制');
        }
        
      } catch (error) {
        console.error(`复制训练课程 ${sourceTraining.title} 失败:`, error);
        if (error.response) {
          console.error('错误响应:', error.response.data);
          console.error('错误状态码:', error.response.status);
        }
        throw error;
      }
    },

    clearSelection() {
      this.selectedTrainings = [];
      this.$refs.mainTrainingTable.clearCheckboxRow();
    },

    viewTrainingDetail(trainingId) {
      // 在新窗口打开训练详情
      const routeData = this.$router.resolve({
        name: 'TrainingDetails',
        params: { trainingID: trainingId },
      });
      window.open(routeData.href, '_blank');
    },

    goUserHome(username) {
      this.$router.push({
        path: '/user-home',
        query: { username },
      });
    },
  },
};
</script>

<style scoped>
.filter-section {
  margin-bottom: 20px;
}

.pagination-container {
  margin: 20px 0;
  text-align: center;
}

.action-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.action-buttons {
  margin-top: 10px;
  text-align: center;
}

.action-buttons .el-button {
  margin: 0 5px;
}
</style>
