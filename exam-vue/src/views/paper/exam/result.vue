<template>
  <div class="app-container">

    <h2 class="text-center">{{ paperData.title }}</h2>
    <p class="text-center" style="color: #666">{{ paperData.createTime }}</p>

    <el-row :gutter="24" style="margin-top: 50px">

      <el-col :span="8" class="text-center">
        考生姓名：{{ paperData.userId_dictText }}
      </el-col>

      <el-col :span="8" class="text-center">
        考试用时：{{ paperData.userTime }}分钟
      </el-col>

      <el-col :span="8" class="text-center">
        考试得分：{{ paperData.userScore }}
      </el-col>

    </el-row>

    <el-card style="margin-top: 20px">

      <div v-for="item in paperData.quList" :key="item.id" class="qu-content">

        <p>{{ item.sort + 1 }}.{{ item.content }}（得分：{{ item.actualScore }}）</p>
        <p v-if="item.image!=null && item.image!=''">
          <el-image :src="item.image" style="max-width:100%;" />
        </p>
        <div v-if="item.quType === 1 || item.quType===3">
          <el-radio-group v-model="radioValues[item.id]">
            <el-radio v-for="an in item.answerList" :label="an.id">
              {{ an.abc }}.{{ an.content }}
              <div v-if="an.image!=null && an.image!=''" style="clear: both">
                <el-image :src="an.image" style="max-width:100%;" />
              </div>
            </el-radio>
          </el-radio-group>

          <el-row :gutter="24">

            <el-col :span="12" style="color: #24da70">
              正确答案：{{ radioRights[item.id] }}
            </el-col>

            <el-col v-if="!item.answered" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：未答
            </el-col>

            <el-col v-if="item.answered && !item.isRight" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：{{ myRadio[item.id] }}
            </el-col>

            <el-col v-if="item.answered && item.isRight" :span="12" style="text-align: right; color: #24da70;">
              答题结果：{{ myRadio[item.id] }}
            </el-col>

          </el-row>

        </div>

        <div v-if="item.quType === 4">

          <el-row :gutter="24" style="margin-bottom: 10px;">
            <el-col :span="24">
              <strong>我的回答：</strong>{{ item.answer || '未答' }}
            </el-col>
          </el-row>

          <!-- AI Judge Results Section -->
          <div v-if="aiJudgeResults[item.id]" class="ai-judge-result">
            <el-divider content-position="left">
              <i class="el-icon-cpu"></i> AI智能评判结果
            </el-divider>

            <el-row :gutter="24" style="margin-bottom: 10px;">
              <el-col :span="8">
                <el-tag :type="getScoreTagType(aiJudgeResults[item.id].score, item.score)" size="medium">
                  AI评分：{{ aiJudgeResults[item.id].score || '0' }} / {{ item.score }}分
                </el-tag>
              </el-col>
              <el-col :span="16" style="text-align: right;">
                <el-button 
                  size="mini" 
                  type="primary" 
                  icon="el-icon-refresh" 
                  @click="requestAIJudge(item)"
                  :loading="judgingItems[item.id]">
                  {{ judgingItems[item.id] ? '评判中...' : '重新评判' }}
                </el-button>
              </el-col>
            </el-row>

            <el-row :gutter="24" style="margin-bottom: 10px;">
              <el-col :span="24">
                <div class="feedback-section">
                  <h4><i class="el-icon-chat-dot-round"></i> 详细反馈：</h4>
                  <p class="feedback-text">{{ aiJudgeResults[item.id].feedback || '暂无反馈' }}</p>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="24" v-if="aiJudgeResults[item.id].keyPointsCovered && aiJudgeResults[item.id].keyPointsCovered.length > 0">
              <el-col :span="24">
                <div class="key-points-section">
                  <h4><i class="el-icon-check"></i> 已掌握要点：</h4>
                  <el-tag 
                    v-for="point in aiJudgeResults[item.id].keyPointsCovered" 
                    :key="point" 
                    type="success" 
                    size="small" 
                    style="margin: 2px;">
                    {{ point }}
                  </el-tag>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="24" v-if="aiJudgeResults[item.id].keyPointsMissed && aiJudgeResults[item.id].keyPointsMissed.length > 0">
              <el-col :span="24">
                <div class="key-points-section">
                  <h4><i class="el-icon-close"></i> 遗漏要点：</h4>
                  <el-tag 
                    v-for="point in aiJudgeResults[item.id].keyPointsMissed" 
                    :key="point" 
                    type="danger" 
                    size="small" 
                    style="margin: 2px;">
                    {{ point }}
                  </el-tag>
                </div>
              </el-col>
            </el-row>

            <el-row :gutter="24" v-if="aiJudgeResults[item.id].suggestions && aiJudgeResults[item.id].suggestions.length > 0">
              <el-col :span="24">
                <div class="suggestions-section">
                  <h4><i class="el-icon-lightbulb"></i> 改进建议：</h4>
                  <ul class="suggestions-list">
                    <li v-for="suggestion in aiJudgeResults[item.id].suggestions" :key="suggestion">
                      {{ suggestion }}
                    </li>
                  </ul>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- AI Judge Request Button for items without results -->
          <div v-else-if="item.answer && item.answer.trim() !== ''" class="ai-judge-request">
            <el-row :gutter="24" style="margin-top: 10px;">
              <el-col :span="24" style="text-align: center;">
                <el-button 
                  type="primary" 
                  icon="el-icon-cpu" 
                  @click="requestAIJudge(item)"
                  :loading="judgingItems[item.id]">
                  {{ judgingItems[item.id] ? 'AI评判中...' : '请求AI智能评判' }}
                </el-button>
              </el-col>
            </el-row>
          </div>

        </div>

        <div v-if="item.quType === 2">
          <el-checkbox-group v-model="multiValues[item.id]">
            <el-checkbox v-for="an in item.answerList" :key="an.id" :label="an.id">{{ an.abc }}.{{ an.content }}
              <div v-if="an.image!=null && an.image!=''" style="clear: both">
                <el-image :src="an.image" style="max-width:100%;" />
              </div>
            </el-checkbox>
          </el-checkbox-group>

          <el-row :gutter="24">

            <el-col :span="12" style="color: #24da70">
              正确答案：{{ multiRights[item.id].join(',') }}
            </el-col>

            <el-col v-if="!item.answered" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：未答
            </el-col>

            <el-col v-if="item.answered && !item.isRight" :span="12" style="text-align: right; color: #ff0000;">
              答题结果：{{ myMulti[item.id].join(',') }}
            </el-col>

            <el-col v-if="item.answered && item.isRight" :span="12" style="text-align: right; color: #24da70;">
              答题结果：{{ myMulti[item.id].join(',') }}
            </el-col>

          </el-row>
        </div>

      </div>

    </el-card>

  </div>
</template>

<script>

import { paperResult } from '@/api/paper/exam'
import { post } from '@/utils/request'

export default {
  data() {
    return {
      // 试卷ID
      paperId: '',
      paperData: {
        quList: []
      },
      radioValues: {},
      multiValues: {},
      radioRights: {},
      multiRights: {},
      myRadio: {},
      myMulti: {},
      // AI Judge related data
      aiJudgeResults: {}, // Store AI judge results by question ID
      judgingItems: {} // Track which items are being judged
    }
  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== 'undefined') {
      this.paperId = id
      this.fetchData(id)
    }
  },
  methods: {

    fetchData(id) {
      const params = { id: id }
      paperResult(params).then(response => {
        // 试卷内容
        this.paperData = response.data

        // 填充该题目的答案
        this.paperData.quList.forEach((item) => {
          let radioValue = ''
          let radioRight = ''
          let myRadio = ''
          const multiValue = []
          const multiRight = []
          const myMulti = []

          item.answerList.forEach((an) => {
            // 用户选定的
            if (an.checked) {
              if (item.quType === 1 || item.quType === 3) {
                radioValue = an.id
                myRadio = an.abc
              } else {
                multiValue.push(an.id)
                myMulti.push(an.abc)
              }
            }

            // 正确答案
            if (an.isRight) {
              if (item.quType === 1 || item.quType === 3) {
                radioRight = an.abc
              } else {
                multiRight.push(an.abc)
              }
            }
          })

          this.multiValues[item.id] = multiValue
          this.radioValues[item.id] = radioValue

          this.radioRights[item.id] = radioRight
          this.multiRights[item.id] = multiRight

          this.myRadio[item.id] = myRadio
          this.myMulti[item.id] = myMulti
        })

        console.log(this.multiValues)
        console.log(this.radioValues)
      })
    },

    /**
     * Request AI judge for a short answer question
     */
    async requestAIJudge(item) {
      if (!item.answer || item.answer.trim() === '') {
        this.$message.warning('该题目没有答案，无法进行AI评判')
        return
      }

      // Set loading state
      this.$set(this.judgingItems, item.id, true)

      try {
        const requestData = {
          questionId: item.id.toString(),
          questionContent: item.content,
          userAnswer: item.answer,
          standardAnswer: item.analysis || '', // Use analysis as standard answer if available
          knowledgePoint: this.getFirstKnowledgePoint(item.knowledgePoints) || '基础知识',
          maxScore: item.score || 5 // Use question's max score
        }

        console.log('🎯 Requesting AI judge for question:', requestData)

        const response = await post('/exam/api/judge/short-answer', requestData)

        if (response.code === 0 && response.data) {
          // Store the AI judge result
          this.$set(this.aiJudgeResults, item.id, response.data)
          
          this.$message.success('AI评判完成！')
          console.log('✅ AI judge result received:', response.data)
        } else {
          throw new Error(response.msg || 'AI评判失败')
        }

      } catch (error) {
        console.error('❌ AI judge request failed:', error)
        this.$message.error('AI评判失败: ' + error.message)
      } finally {
        // Clear loading state
        this.$set(this.judgingItems, item.id, false)
      }
    },

    /**
     * Get score tag type based on score ratio
     */
    getScoreTagType(score, maxScore) {
      if (!score || !maxScore) return 'info'
      
      const ratio = score / maxScore
      if (ratio >= 0.8) return 'success'
      if (ratio >= 0.6) return 'warning'
      return 'danger'
    },

    /**
     * Extract first knowledge point from knowledge points string
     */
    getFirstKnowledgePoint(knowledgePointsStr) {
      if (!knowledgePointsStr || knowledgePointsStr.trim() === '') {
        return null
      }
      try {
        const parsed = JSON.parse(knowledgePointsStr)
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed[0]
        }
        return null
      } catch (e) {
        console.warn('Failed to parse knowledge points:', knowledgePointsStr)
        return null
      }
    }
  }
}
</script>

<style scoped>

  .qu-content{
    border-bottom: #eee 1px solid;
    padding-bottom: 10px;
  }

  .qu-content div{
    line-height: 30px;
  }

  .el-checkbox-group label,.el-radio-group label{
    width: 100%;
  }

  .card-title{
    background: #eee;
    line-height: 35px;
    text-align: center;
    font-size: 14px;
  }
  
  .card-line{
    padding-left: 10px
  }
  
  .card-line span {
    cursor: pointer;
    margin: 2px;
  }

  /* AI Judge Result Styles */
  .ai-judge-result {
    background: #f8f9fa;
    border: 1px solid #e9ecef;
    border-radius: 8px;
    padding: 15px;
    margin-top: 15px;
  }

  .ai-judge-request {
    background: #f0f8ff;
    border: 1px dashed #4dabf7;
    border-radius: 8px;
    padding: 15px;
    margin-top: 10px;
  }

  .feedback-section {
    background: #fff;
    border-left: 4px solid #409eff;
    padding: 10px 15px;
    margin-bottom: 10px;
  }

  .feedback-section h4 {
    color: #409eff;
    margin: 0 0 8px 0;
    font-size: 14px;
  }

  .feedback-text {
    color: #606266;
    line-height: 1.6;
    margin: 0;
    white-space: pre-line;
  }

  .key-points-section {
    background: #fff;
    padding: 10px 15px;
    margin-bottom: 10px;
    border-radius: 4px;
  }

  .key-points-section h4 {
    margin: 0 0 8px 0;
    font-size: 14px;
  }

  .key-points-section h4 .el-icon-check {
    color: #67c23a;
  }

  .key-points-section h4 .el-icon-close {
    color: #f56c6c;
  }

  .suggestions-section {
    background: #fff;
    border-left: 4px solid #e6a23c;
    padding: 10px 15px;
    border-radius: 4px;
  }

  .suggestions-section h4 {
    color: #e6a23c;
    margin: 0 0 8px 0;
    font-size: 14px;
  }

  .suggestions-list {
    margin: 0;
    padding-left: 20px;
    color: #606266;
  }

  .suggestions-list li {
    line-height: 1.6;
    margin-bottom: 5px;
  }

  /* Responsive adjustments */
  @media (max-width: 768px) {
    .ai-judge-result {
      padding: 10px;
    }
    
    .feedback-section, 
    .key-points-section, 
    .suggestions-section {
      padding: 8px 12px;
    }
  }

</style>

