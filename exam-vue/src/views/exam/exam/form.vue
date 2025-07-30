<template>
  <div class="app-container">
    <!-- 页面标题区域 -->
    <div class="page-header">
      <el-row>
        <el-col :span="18">
          <h2 class="page-title">
            <i class="el-icon-document"></i>
            智能组卷系统
          </h2>
          <p class="page-subtitle">创建个性化考试试卷，支持AI智能选题</p>
        </el-col>
        <el-col :span="6" class="text-right">
          <div class="total-score-display">
            <i class="el-icon-star-on"></i>
            <span class="score-label">试卷总分</span>
            <span class="score-value">{{ postForm.totalScore }}</span>
            <span class="score-unit">分</span>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 组卷信息卡片 -->
    <el-card class="main-card" shadow="hover">
      <div slot="header" class="card-header">
        <span class="card-title">
          <i class="el-icon-collection"></i>
          题库配置
        </span>
      </div>

      <div>

        <el-button 
          class="add-repo-btn" 
          size="medium" 
          type="primary" 
          icon="el-icon-plus" 
          @click="handleAdd"
          round>
          <span>添加题库</span>
        </el-button>

        <div class="table-container">
        <el-table
          :data="repoList"
          :border="true"
          empty-text="🎯 请点击上面的'添加题库'按钮开始配置"
          style="width: 100%; margin-top: 20px; min-width: 1200px;"
          :header-cell-style="{background: '#f5f7fa', color: '#606266', fontWeight: 'bold'}"
          stripe
        >
          <el-table-column
            label="题库选择"
            width="200"
            align="center"
          >
            <template v-slot="scope">
              <repo-select
                v-model="scope.row.repoId"
                :multi="false"
                :excludes="excludes"
                @change="repoChange($event, scope.row)" />
            </template>

          </el-table-column>
          <el-table-column
            align="center"
            width="120"
          >
            <template slot="header">
              <div class="question-header">
                <i class="el-icon-circle-check" style="color: #67C23A;"></i>
                <span class="header-text-inline">单选题数量</span>
              </div>
            </template>

            <template v-slot="scope">
              <div class="count-input-wrapper">
                <el-input-number v-model="scope.row.radioCount" :min="0" :max="scope.row.totalRadio" :controls="false" size="mini" />
                <span class="total-text">/ {{ scope.row.totalRadio }}</span>
              </div>
            </template>

          </el-table-column>

          <el-table-column
            align="center"
            width="100"
          >
            <template slot="header">
              <div class="score-header">
                <i class="el-icon-trophy" style="color: #F56C6C;"></i>
                <span class="header-text-inline">单选分数</span>
              </div>
            </template>
            <template v-slot="scope">
              <el-input-number v-model="scope.row.radioScore" :min="0" :controls="false" style="width: 70px" size="mini" />
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="120"
          >
            <template slot="header">
              <div class="question-header">
                <i class="el-icon-circle-check" style="color: #409EFF;"></i>
                <span class="header-text-inline">多选题数量</span>
              </div>
            </template>

            <template v-slot="scope">
              <div class="count-input-wrapper">
                <el-input-number v-model="scope.row.multiCount" :min="0" :max="scope.row.totalMulti" :controls="false" size="mini" />
                <span class="total-text">/ {{ scope.row.totalMulti }}</span>
              </div>
            </template>

          </el-table-column>

          <el-table-column
            align="center"
            width="100"
          >
            <template slot="header">
              <div class="score-header">
                <i class="el-icon-trophy" style="color: #F56C6C;"></i>
                <span class="header-text-inline">多选分数</span>
              </div>
            </template>
            <template v-slot="scope">
              <el-input-number v-model="scope.row.multiScore" :min="0" :controls="false" style="width: 70px" size="mini" />
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="120"
          >
            <template slot="header">
              <div class="question-header">
                <i class="el-icon-check" style="color: #E6A23C;"></i>
                <span class="header-text-inline">判断题数量</span>
              </div>
            </template>

            <template v-slot="scope">
              <div class="count-input-wrapper">
                <el-input-number v-model="scope.row.judgeCount" :min="0" :max="scope.row.totalJudge" :controls="false" size="mini" />
                <span class="total-text">/ {{ scope.row.totalJudge }}</span>
              </div>
            </template>

          </el-table-column>

          <el-table-column
            align="center"
            width="100"
          >
            <template slot="header">
              <div class="score-header">
                <i class="el-icon-trophy" style="color: #F56C6C;"></i>
                <span class="header-text-inline">判断分数</span>
              </div>
            </template>
            <template v-slot="scope">
              <el-input-number v-model="scope.row.judgeScore" :min="0" :controls="false" style="width: 70px" size="mini" />
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="140"
          >
            <template slot="header">
              <div class="question-header">
                <i class="el-icon-edit-outline" style="color: #909399;"></i>
                <span class="header-text-inline">简答数量</span>
              </div>
            </template>
            <template v-slot="scope">
              <div class="count-input-wrapper">
                <el-input-number v-model="scope.row.saqCount" :min="0" :max="scope.row.totalSaq" :controls="false" size="mini" />
                <span class="total-text">/ {{ scope.row.totalSaq }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="110"
          >
            <template slot="header">
              <div class="score-header">
                <i class="el-icon-trophy" style="color: #F56C6C;"></i>
                <span class="header-text-inline">简答分数</span>
              </div>
            </template>
            <template v-slot="scope">
              <el-input-number v-model="scope.row.saqScore" :min="0" :controls="false" style="width: 70px" size="mini" />
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="140"
          >
            <template slot="header">
              <div class="question-header">
                <i class="el-icon-menu" style="color: #67C23A;"></i>
                <span class="header-text-inline">填空数量</span>
              </div>
            </template>
            <template v-slot="scope">
              <div class="count-input-wrapper">
                <el-input-number v-model="scope.row.gapFillingCount" :min="0" :max="scope.row.totalGapFilling" :controls="false" size="mini" />
                <span class="total-text">/ {{ scope.row.totalGapFilling }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column
            align="center"
            width="110"
          >
            <template slot="header">
              <div class="score-header">
                <i class="el-icon-trophy" style="color: #F56C6C;"></i>
                <span class="header-text-inline">填空分数</span>
              </div>
            </template>
            <template v-slot="scope">
              <el-input-number v-model="scope.row.gapFillingScore" :min="0" :controls="false" style="width: 70px" size="mini" />
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            align="center"
            width="80"
            fixed="right"
          >
            <template v-slot="scope">
              <el-button 
                type="danger" 
                icon="el-icon-delete" 
                size="mini"
                circle 
                @click="removeItem(scope.$index)"
                title="删除此题库" />
            </template>
          </el-table-column>

        </el-table>
        </div>

      </div>

    </el-card>

    <!-- 考试配置卡片 -->
    <el-card class="main-card" shadow="hover" style="margin-top: 25px">
      <div slot="header" class="card-header">
        <span class="card-title">
          <i class="el-icon-setting"></i>
          考试配置
        </span>
      </div>

      <el-form ref="postForm" :model="postForm" :rules="rules" label-position="left" label-width="120px">

        <el-form-item label="📝 考试名称" prop="title">
          <el-input 
            v-model="postForm.title" 
            placeholder="请输入考试名称"
            prefix-icon="el-icon-edit" />
        </el-form-item>

        <el-form-item label="📜 考试描述" prop="content">
          <el-input 
            v-model="postForm.content" 
            type="textarea" 
            :rows="3"
            placeholder="请输入考试描述和说明" />
        </el-form-item>

        <el-form-item label="🎯 总分数" prop="totalScore">
          <el-input-number 
            :value="postForm.totalScore" 
            disabled 
            class="score-input" />
        </el-form-item>

        <el-form-item label="✅ 及格分" prop="qualifyScore">
          <el-input-number 
            v-model="postForm.qualifyScore" 
            :max="postForm.totalScore" 
            :min="0"
            placeholder="请设置及格分数" />
        </el-form-item>

        <el-form-item label="⏰ 考试时长(分钟)" prop="totalTime">
          <el-input-number 
            v-model="postForm.totalTime" 
            :min="1"
            placeholder="请设置考试时长" />
        </el-form-item>

        <el-form-item label="⏱️ 是否限时">
          <el-switch 
            v-model="postForm.timeLimit"
            active-text="开启限时"
            inactive-text="不限时" />
        </el-form-item>

        <el-form-item label="🤖 AI智能组卷" class="ai-toggle-item">
          <div class="ai-toggle-container">
            <el-switch 
              v-model="postForm.useAI"
              active-text="开启"
              inactive-text="关闭"
              active-color="#13ce66"
              inactive-color="#ff4949" />
            <div class="ai-description">
              <el-alert
                v-if="postForm.useAI"
                title="✨ AI智能模式已开启"
                description="系统将使用Qwen3-32B模型智能分析题目质量、难度分布和知识点覆盖，为您选择最优试题组合"
                type="success"
                :closable="false"
                show-icon
                class="ai-alert" />
              <el-alert
                v-else
                title="🎲 传统随机模式"
                description="系统将从题库中随机选择题目组卷"
                type="info"
                :closable="false"
                show-icon
                class="ai-alert" />
            </div>
          </div>
        </el-form-item>


        <el-form-item v-if="postForm.timeLimit" label="考试时间" prop="totalTime">

          <el-date-picker
            v-model="dateValues"
            format="yyyy-MM-dd"
            value-format="yyyy-MM-dd"
            type="daterange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />

        </el-form-item>

      </el-form>

    </el-card>

    <!-- 权限配置卡片 -->
    <el-card class="main-card" shadow="hover" style="margin-top: 25px">
      <div slot="header" class="card-header">
        <span class="card-title">
          <i class="el-icon-key"></i>
          权限配置
        </span>
      </div>

      <div class="permission-selector">
        <el-radio-group v-model="postForm.openType" style="margin-bottom: 20px">
          <el-radio :label="1" border class="permission-radio">
            <i class="el-icon-unlock"></i>
            完全公开
          </el-radio>
          <el-radio :label="2" border class="permission-radio">
            <i class="el-icon-lock"></i>
            部门开放
          </el-radio>
        </el-radio-group>
      </div>

      <el-alert
        v-if="postForm.openType===1"
        title="开放的，任何人都可以进行考试！"
        type="warning"
      />

      <div v-if="postForm.openType===2">
        <el-input
          v-model="filterText"
          placeholder="输入关键字进行过滤"
        />

        <el-tree

          v-loading="treeLoading"
          ref="tree"
          :data="treeData"
          :default-checked-keys="postForm.departIds"
          :props="defaultProps"
          :filter-node-method="filterNode"
          empty-text=" "
          default-expand-all
          show-checkbox
          node-key="id"
          @check-change="handleCheckChange"
        />

      </div>

    </el-card>

    <!-- 操作按钮区域 -->
    <div class="action-buttons">
      <el-button 
        type="primary" 
        size="large"
        icon="el-icon-check"
        @click="handleSave"
        class="save-btn">
        <span>保存试卷</span>
      </el-button>
      <el-button 
        size="large"
        icon="el-icon-refresh"
        @click="resetForm"
        class="reset-btn">
        <span>重置</span>
      </el-button>
    </div>

  </div>
</template>

<script>
import { fetchDetail, saveData } from '@/api/exam/exam'
import { fetchTree } from '@/api/sys/depart/depart'
import RepoSelect from '@/components/RepoSelect'

export default {
  name: 'ExamDetail',
  components: { RepoSelect },
  data() {
    return {

      treeData: [],
      defaultProps: {
        label: 'deptName'
      },
      filterText: '',
      treeLoading: false,
      dateValues: [],
      // 题库
      repoList: [],
      // 已选择的题库
      excludes: [],
      postForm: {
        // 总分数
        totalScore: 0,
        // 题库列表
        repoList: [],
        // 开放类型
        openType: 1,
        // 考试班级列表
        departIds: []
      },
      rules: {
        title: [
          { required: true, message: '考试名称不能为空！' }
        ],

        content: [
          { required: true, message: '考试名称不能为空！' }
        ],

        open: [
          { required: true, message: '考试权限不能为空！' }
        ],

        totalScore: [
          { required: true, message: '考试分数不能为空！' }
        ],

        qualifyScore: [
          { required: true, message: '及格分不能为空！' }
        ],

        totalTime: [
          { required: true, message: '考试时间不能为空！' }
        ],

        ruleId: [
          { required: true, message: '考试规则不能为空' }
        ],
        password: [
          { required: true, message: '考试口令不能为空！' }
        ]
      }
    }
  },

  watch: {

    filterText(val) {
      this.$refs.tree.filter(val)
    },

    dateValues: {

      handler() {
        this.postForm.startTime = this.dateValues[0]
        this.postForm.endTime = this.dateValues[1]
      }
    },

    // 题库变换
    repoList: {

      handler(val) {
        let totalScore = 0
        this.excludes = []
        for (let i = 0; i<val.length; i++) {
          const item = val[i]
          if (item.radioCount > 0 && item.radioScore>0) {
            totalScore += item.radioCount * item.radioScore
          }

          if (item.multiCount>0 && item.multiScore>0) {
            totalScore += item.multiCount * item.multiScore
          }

          if (item.judgeCount>0 && item.judgeScore>0) {
            totalScore += item.judgeCount * item.judgeScore
          }

          if (item.saqCount>0 && item.saqScore>0) {
            totalScore += item.saqCount * item.saqScore
          }

          if (item.gapFillingCount>0 && item.gapFillingScore>0) {
            totalScore += item.gapFillingCount * item.gapFillingScore
          }
          this.excludes.push(item.id)
        }

        // 赋值
        this.postForm.totalScore = totalScore
        this.postForm.repoList = val
        this.$forceUpdate()
      },
      deep: true
    }

  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== undefined) {
      this.fetchData(id)
    }

    fetchTree({}).then(response => {
      this.treeData = response.data
    })
  },
  methods: {

    handleSave() {
      this.$refs.postForm.validate((valid) => {
        if (!valid) {
          return
        }

        if (this.postForm.totalScore === 0) {
          this.$notify({
            title: '提示信息',
            message: '考试规则设置不正确，请确认！',
            type: 'warning',
            duration: 2000
          })

          return
        }

        for (let i = 0; i < this.postForm.repoList.length; i++) {
          const repo = this.postForm.repoList[i]
          if (!repo.repoId) {
            this.$notify({
              title: '提示信息',
              message: '考试题库选择不正确！',
              type: 'warning',
              duration: 2000
            })
            return
          }

          if ((repo.radioCount > 0 && repo.radioScore === 0) || (repo.radioCount === 0 && repo.radioScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的单选题配置！',
              type: 'warning',
              duration: 2000
            })

            return
          }

          if ((repo.multiCount > 0 && repo.multiScore === 0) || (repo.multiCount === 0 && repo.multiScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的多选题配置！',
              type: 'warning',
              duration: 2000
            })

            return
          }

          if ((repo.judgeCount > 0 && repo.judgeScore === 0) || (repo.judgeCount === 0 && repo.judgeScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的判断题配置！',
              type: 'warning',
              duration: 2000
            })
            return
          }

          if ((repo.saqCount > 0 && repo.saqScore === 0) || (repo.saqCount === 0 && repo.saqScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的简答题配置！',
              type: 'warning',
              duration: 2000
            })
            return
          }

          if ((repo.gapFillingCount > 0 && repo.gapFillingScore === 0) || (repo.gapFillingCount === 0 && repo.gapFillingScore > 0)) {
            this.$notify({
              title: '提示信息',
              message: '题库第：[' + (i + 1) + ']项存在无效的填空题配置！',
              type: 'warning',
              duration: 2000
            })
            return
          }
        }

        this.$confirm('确实要提交保存吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.submitForm()
        })
      })
    },

    handleCheckChange() {
      const that = this
      // 置空
      this.postForm.departIds = []
      const nodes = this.$refs.tree.getCheckedNodes()
      nodes.forEach(function(item) {
        that.postForm.departIds.push(item.id)
      })
    },

    // 添加子项
    handleAdd() {
      this.repoList.push({ 
        id: '', 
        rowId: new Date().getTime(), 
        radioCount: 0, 
        radioScore: 0, 
        multiCount: 0, 
        multiScore: 0, 
        judgeCount: 0, 
        judgeScore: 0, 
        saqCount: 0, 
        saqScore: 0,
        gapFillingCount: 0,
        gapFillingScore: 0
      })
    },

    removeItem(index) {
      this.repoList.splice(index, 1)
    },


    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data

        if (this.postForm.startTime && this.postForm.endTime) {
          this.dateValues[0] = this.postForm.startTime
          this.dateValues[1] = this.postForm.endTime
        }
        this.repoList = this.postForm.repoList
      })
    },

    submitForm() {
      // 校验和处理数据
      this.postForm.repoList = this.repoList

      saveData(this.postForm).then(() => {
        this.$notify({
          title: '成功',
          message: '考试保存成功！',
          type: 'success',
          duration: 2000
        })

        this.$router.push({ name: 'ListExam' })
      })
    },

    filterNode(value, data) {
      if (!value) return true
      return data.deptName.indexOf(value) !== -1
    },

    repoChange(e, row) {
      // 赋值ID
      row.id = e.id

      if (e != null) {
        row.totalRadio = e.radioCount
        row.totalMulti = e.multiCount
        row.totalJudge = e.judgeCount
        row.totalSaq = e.saqCount || 0
        row.totalGapFilling = e.gapFillingCount || 0
      } else {
        row.totalRadio = 0
        row.totalMulti = 0
        row.totalJudge = 0
        row.totalSaq = 0
        row.totalGapFilling = 0
      }
    },

    // 重置表单方法
    resetForm() {
      this.$confirm('确定要重置表单吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.repoList = []
        this.postForm = {
          totalScore: 0,
          repoList: [],
          openType: 1,
          departIds: [],
          useAI: false
        }
        this.$message.success('表单已重置')
      })
    }

  }
}
</script>

<style scoped>

/* Table container for horizontal scrolling */
.table-container {
  overflow-x: auto;
  width: 100%;
}
/* 页面整体样式 */
.app-container {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
}

/* 页面标题区域 */
.page-header {
  margin-bottom: 30px;
  padding: 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.page-title {
  color: #2c3e50;
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title i {
  color: #409eff;
  font-size: 32px;
}

.page-subtitle {
  color: #7f8c8d;
  font-size: 14px;
  margin: 8px 0 0 42px;
}

.total-score-display {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 15px 20px;
  border-radius: 10px;
  text-align: center;
  box-shadow: 0 4px 15px 0 rgba(102, 126, 234, 0.4);
}

.total-score-display i {
  font-size: 20px;
  margin-right: 8px;
}

.score-label {
  font-size: 14px;
  opacity: 0.9;
  margin-right: 8px;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  margin-right: 4px;
}

.score-unit {
  font-size: 14px;
  opacity: 0.9;
}

/* 卡片样式 */
.main-card {
  margin-bottom: 25px;
  border-radius: 10px;
  overflow: hidden;
}

.main-card .el-card__header {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  padding: 18px 20px;
  border: none;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  color: white;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title i {
  font-size: 20px;
}

/* 按钮样式 */
.add-repo-btn {
  background: linear-gradient(135deg, #00b894 0%, #00a085 100%);
  border: none;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 15px 0 rgba(0, 184, 148, 0.3);
  transition: all 0.3s ease;
}

.add-repo-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(0, 184, 148, 0.4);
}

/* AI切换区域 */
.ai-toggle-item {
  background: #f8f9ff;
  padding: 20px;
  border-radius: 8px;
  border: 2px dashed #e1e8ed;
}

.ai-toggle-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.ai-description {
  margin-top: 10px;
}

.ai-alert {
  margin-top: 0;
}

/* 权限选择器 */
.permission-selector {
  padding: 10px;
}

.permission-radio {
  margin-right: 20px;
  padding: 15px 20px;
  font-size: 16px;
}

.permission-radio i {
  margin-right: 8px;
  font-size: 18px;
}

/* 操作按钮区域 */
.action-buttons {
  margin-top: 40px;
  padding: 30px;
  text-align: center;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.save-btn {
  background: linear-gradient(135deg, #fd79a8 0%, #e84393 100%);
  border: none;
  padding: 15px 40px;
  font-size: 16px;
  font-weight: 600;
  margin-right: 20px;
  box-shadow: 0 4px 15px 0 rgba(232, 67, 147, 0.4);
  transition: all 0.3s ease;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(232, 67, 147, 0.5);
}

.reset-btn {
  background: linear-gradient(135deg, #74b9ff 0%, #0984e3 100%);
  border: none;
  color: white;
  padding: 15px 40px;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 15px 0 rgba(116, 185, 255, 0.4);
  transition: all 0.3s ease;
}

.reset-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(116, 185, 255, 0.5);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: 15px;
  }
  
  .page-title {
    font-size: 24px;
  }
  
  .total-score-display {
    margin-top: 15px;
  }
  
  .action-buttons {
    padding: 20px;
  }
  
  .save-btn, .reset-btn {
    width: 100%;
    margin: 10px 0;
  }
}

/* 表格美化 */
.el-table {
  border-radius: 8px;
  overflow: hidden;
}

.el-table .el-table__cell {
  padding: 8px 0;
}

.el-input-number {
  width: 100%;
}

.el-input-number--mini {
  line-height: 24px;
}

.el-input-number--mini .el-input__inner {
  height: 24px;
  line-height: 24px;
  font-size: 12px;
  text-align: center;
}

/* 数量输入框容器 */
.count-input-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  gap: 5px;
}

.count-input-wrapper .el-input-number {
  width: 50px;
  flex-shrink: 0;
}

.total-text {
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
  margin-left: 2px;
}

/* 表格头部样式 */
.question-header, .score-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 4px 0;
  white-space: nowrap;
}

.question-header i, .score-header i {
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.header-text-inline {
  font-size: 12px;
  font-weight: 600;
  color: #2c3e50;
  white-space: nowrap;
  line-height: 1.2;
}

/* 表单美化 */
.el-form-item__label {
  font-weight: 600;
  color: #2c3e50;
}

.el-input, .el-textarea {
  border-radius: 6px;
}

.el-input:focus, .el-textarea:focus {
  border-color: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.2);
}

</style>

