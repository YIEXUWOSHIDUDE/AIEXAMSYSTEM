<template>
  <div class="app-container">
    <el-card shadow="never">
      <h3>AI知识点识别</h3>
      <p class="description">使用AI分析题目内容，自动识别对应的知识大纲</p>

      <!-- 参数设置 -->
      <el-form :inline="true" :model="identifyForm" class="identify-form">
        <el-form-item label="学科">
          <el-select v-model="identifyForm.subject" placeholder="请选择学科" @change="onSubjectChange">
            <el-option label="数学" value="数学" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="语文" value="语文" />
            <el-option label="英语" value="英语" />
            <el-option label="生物" value="生物" />
            <el-option label="历史" value="历史" />
            <el-option label="地理" value="地理" />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="identifyForm.grade" placeholder="请选择年级">
            <el-option label="七年级" value="七年级" />
            <el-option label="八年级" value="八年级" />
            <el-option label="九年级" value="九年级" />
            <el-option label="高一" value="高一" />
            <el-option label="高二" value="高二" />
            <el-option label="高三" value="高三" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="startBatchIdentify" :loading="identifying">
            开始批量识别
          </el-button>
          <el-button @click="clearResults">清空结果</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 识别进度 -->
    <el-card v-if="showProgress" shadow="never" style="margin-top: 20px;">
      <h4>识别进度</h4>
      <el-progress 
        :percentage="progress" 
        :status="progressStatus"
        :stroke-width="18"
      />
      <p class="progress-info">
        已处理: {{ processedCount }} / {{ totalCount }} 题目
        成功: {{ successCount }} 失败: {{ failCount }}
      </p>
    </el-card>

    <!-- 识别结果 -->
    <el-card v-if="results.length > 0" shadow="never" style="margin-top: 20px;">
      <div slot="header" class="clearfix">
        <span>识别结果</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="saveAllMappings">
          保存所有映射
        </el-button>
      </div>

      <el-table :data="results" border>
        <el-table-column label="题目ID" prop="questionId" width="150" />
        <el-table-column label="题目内容" prop="questionContent" min-width="300">
          <template slot-scope="{row}">
            <div class="question-content">{{ row.questionContent.substring(0, 100) }}...</div>
          </template>
        </el-table-column>
        <el-table-column label="识别状态" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.status === 'success' ? 'success' : 'danger'">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="识别结果" width="200">
          <template slot-scope="{row}">
            <div v-if="row.result">
              <p><strong>知识点:</strong> {{ row.result.knowledgePoint || '未识别' }}</p>
              <p><strong>置信度:</strong> {{ row.result.confidence || 0 }}</p>
            </div>
            <span v-else>识别失败</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="{row}">
            <el-button 
              v-if="row.result && row.result.outlineId" 
              size="mini" 
              type="primary" 
              @click="saveMapping(row)"
              :loading="row.saving"
            >
              保存映射
            </el-button>
            <el-button size="mini" @click="retryIdentify(row)">重试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getQuestionList } from '@/api/qu/qu'
import { identifyBatchOutlines, identifySingleOutline, saveOutlineMapping } from '@/api/outline'

export default {
  name: 'OutlineAIIdentify',
  data() {
    return {
      identifyForm: {
        subject: '',
        grade: ''
      },
      identifying: false,
      showProgress: false,
      progress: 0,
      progressStatus: '',
      processedCount: 0,
      totalCount: 0,
      successCount: 0,
      failCount: 0,
      results: []
    }
  },
  methods: {
    onSubjectChange() {
      this.identifyForm.grade = ''
    },
    async startBatchIdentify() {
      if (!this.identifyForm.subject || !this.identifyForm.grade) {
        this.$message.warning('请先选择学科和年级')
        return
      }

      this.identifying = true
      this.showProgress = true
      this.progress = 0
      this.progressStatus = ''
      this.results = []
      this.processedCount = 0
      this.successCount = 0
      this.failCount = 0

      try {
        // 获取题目列表
        const questionResponse = await getQuestionList({ 
          pageSize: 100,
          pageNumber: 1 
        })
        
        const questions = questionResponse.data.records || []
        this.totalCount = questions.length

        if (this.totalCount === 0) {
          this.$message.info('没有找到题目数据')
          this.identifying = false
          this.showProgress = false
          return
        }

        // 准备识别
        const questionIds = questions.map(q => q.id)
        
        // 批量识别
        const batchResponse = await identifyBatchOutlines({
          questionIds: questionIds,
          subject: this.identifyForm.subject,
          grade: this.identifyForm.grade
        })

        if (batchResponse.success) {
          const batchResults = batchResponse.data.results || {}
          
          // 处理结果
          for (const question of questions) {
            const result = batchResults[question.id]
            this.results.push({
              questionId: question.id,
              questionContent: question.content,
              status: result ? 'success' : 'failed',
              result: result,
              saving: false
            })
            
            if (result) {
              this.successCount++
            } else {
              this.failCount++
            }
            
            this.processedCount++
            this.progress = Math.round((this.processedCount / this.totalCount) * 100)
            
            // 给UI一些时间更新
            await this.$nextTick()
          }
          
          this.progressStatus = this.failCount > 0 ? 'exception' : 'success'
          this.$message.success(`批量识别完成！成功: ${this.successCount}, 失败: ${this.failCount}`)
        }

      } catch (error) {
        console.error('批量识别失败:', error)
        this.$message.error('批量识别失败: ' + (error.message || '未知错误'))
        this.progressStatus = 'exception'
      } finally {
        this.identifying = false
      }
    },
    async retryIdentify(row) {
      try {
        const response = await identifySingleOutline({
          questionId: row.questionId,
          subject: this.identifyForm.subject,
          grade: this.identifyForm.grade
        })

        if (response.success) {
          row.result = response.data
          row.status = 'success'
          this.$message.success('重新识别成功')
        } else {
          this.$message.error('重新识别失败')
        }
      } catch (error) {
        console.error('重新识别失败:', error)
        this.$message.error('重新识别失败')
      }
    },
    async saveMapping(row) {
      if (!row.result || !row.result.outlineId) {
        this.$message.warning('没有有效的识别结果')
        return
      }

      row.saving = true
      try {
        const response = await saveOutlineMapping({
          questionId: row.questionId,
          outlineId: row.result.outlineId
        })

        if (response.success) {
          this.$message.success('保存映射成功')
        } else {
          this.$message.error('保存映射失败')
        }
      } catch (error) {
        console.error('保存映射失败:', error)
        this.$message.error('保存映射失败')
      } finally {
        row.saving = false
      }
    },
    async saveAllMappings() {
      const validResults = this.results.filter(r => r.result && r.result.outlineId)
      
      if (validResults.length === 0) {
        this.$message.warning('没有有效的识别结果可以保存')
        return
      }

      this.$confirm(`确定要保存 ${validResults.length} 个映射关系吗？`, '确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        let successCount = 0
        let failCount = 0

        for (const row of validResults) {
          try {
            const response = await saveOutlineMapping({
              questionId: row.questionId,
              outlineId: row.result.outlineId
            })
            
            if (response.success) {
              successCount++
            } else {
              failCount++
            }
          } catch (error) {
            failCount++
          }
          
          // 避免请求过快
          await new Promise(resolve => setTimeout(resolve, 100))
        }

        this.$message.success(`批量保存完成！成功: ${successCount}, 失败: ${failCount}`)
      })
    },
    clearResults() {
      this.results = []
      this.showProgress = false
      this.progress = 0
      this.processedCount = 0
      this.totalCount = 0
      this.successCount = 0
      this.failCount = 0
    }
  }
}
</script>

<style scoped>
.description {
  color: #666;
  margin-bottom: 20px;
}

.identify-form {
  margin-bottom: 20px;
}

.progress-info {
  margin-top: 10px;
  color: #666;
}

.question-content {
  word-break: break-all;
  line-height: 1.4;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}
</style>