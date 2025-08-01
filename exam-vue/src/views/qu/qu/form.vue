<template>
  <div class="app-container">

    <el-form ref="postForm" :model="postForm" :rules="rules" label-position="left" label-width="150px">

      <el-card>

        <el-form-item label="题目类型 " prop="quType">

          <el-select v-model="postForm.quType" :disabled="quTypeDisabled" class="filter-item" @change="handleTypeChange">
            <el-option
              v-for="item in quTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

        </el-form-item>

        <el-form-item label="难度等级 " prop="level">

          <el-select v-model="postForm.level" class="filter-item">
            <el-option
              v-for="item in levels"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>

        </el-form-item>


        <el-form-item label="归属题库" prop="repoIds">

          <repo-select v-model="postForm.repoIds" :multi="true" />

        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input v-model="postForm.content" type="textarea" />
        </el-form-item>

        <el-form-item label="题干内容" prop="questionStem">
          <div class="stem-input-container">
            <el-input 
              v-model="postForm.questionStem" 
              type="textarea" 
              :rows="3"
              placeholder="提取的题干内容，去除冗余描述" />
            <div class="stem-actions">
              <el-button size="mini" type="primary" @click="extractStem" :loading="stemExtracting">
                <i class="el-icon-magic-stick"></i>
                AI提取题干
              </el-button>
              <el-button size="mini" @click="resetStem">重置</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="知识大纲" prop="outlineId">
          <div class="outline-selection-container">
            <div class="outline-filters">
              <el-select v-model="outlineSubject" placeholder="选择学科" @change="onOutlineSubjectChange" size="small" style="width: 120px; margin-right: 10px;">
                <el-option label="数学" value="数学" />
                <el-option label="物理" value="物理" />
                <el-option label="化学" value="化学" />
                <el-option label="语文" value="语文" />
                <el-option label="英语" value="英语" />
                <el-option label="生物" value="生物" />
                <el-option label="历史" value="历史" />
                <el-option label="地理" value="地理" />
              </el-select>
              <el-select v-model="outlineGrade" placeholder="选择年级" @change="onOutlineGradeChange" size="small" style="width: 100px; margin-right: 10px;">
                <el-option label="七年级" value="七年级" />
                <el-option label="八年级" value="八年级" />
                <el-option label="九年级" value="九年级" />
                <el-option label="高一" value="高一" />
                <el-option label="高二" value="高二" />
                <el-option label="高三" value="高三" />
              </el-select>
              <el-button size="small" type="primary" @click="identifyOutline" :loading="outlineIdentifying">
                <i class="el-icon-magic-stick"></i>
                AI识别大纲
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="知识点标签" prop="knowledgePoints">
          <div class="knowledge-points-container">
            <div class="knowledge-tags">
              <el-tag
                v-if="knowledgePointsArray.length > 0"
                :closable="true"
                @close="removeKnowledgePoint(knowledgePointsArray[0])"
                size="medium"
                type="primary">
                {{ knowledgePointsArray[0] }}
              </el-tag>
            </div>
            <div class="knowledge-input" v-if="knowledgePointsArray.length === 0" style="display: flex; align-items: center;">
              <el-select
                v-model="newKnowledgePoint"
                placeholder="选择知识点"
                filterable
                :loading="knowledgePointsLoading"
                @change="addKnowledgePoint"
                size="small"
                style="width: 300px; margin-right: 10px;"
                clearable>
                <el-option
                  v-for="point in availableKnowledgePoints"
                  :key="point.id"
                  :label="point.knowledgePoint"
                  :value="point.knowledgePoint"
                />
              </el-select>
              <el-button 
                size="small" 
                type="success" 
                @click="identifyKnowledgePoint" 
                :loading="knowledgeIdentifying">
                <i class="el-icon-magic-stick"></i>
                AI识别知识点
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="试题图片">
          <file-upload v-model="postForm.image" accept=".jpg,.jepg,.png" />
        </el-form-item>

        <el-form-item label="整题解析" prop="oriPrice">
          <el-input v-model="postForm.analysis" :precision="1" :max="999999" type="textarea" />
        </el-form-item>

      </el-card>

      <div class="filter-container" style="margin-top: 25px">

        <el-button class="filter-item" type="primary" icon="el-icon-plus" size="small" plain @click="handleAdd">
          添加
        </el-button>

        <el-table
          :data="postForm.answerList"
          :border="true"
          style="width: 100%;"
        >
          <el-table-column
            v-if="postForm.quType !== 4"
            label="是否答案"
            width="120"
            align="center"
          >
            <template v-slot="scope">

              <el-checkbox v-model="scope.row.isRight">答案</el-checkbox>

            </template>

          </el-table-column>

          <el-table-column
            v-if="itemImage"
            label="选项图片"
            width="120px"
            align="center"
          >
            <template v-slot="scope">

              <file-upload
                v-model="scope.row.image"
                accept=".jpg,.jepg,.png"
              />

            </template>
          </el-table-column>

          <el-table-column
            :label="postForm.quType === 4 ? '参考答案' : '答案内容'"
          >
            <template v-slot="scope">
              <el-input v-model="scope.row.content" type="textarea" :rows="postForm.quType === 4 ? 4 : 2" />
            </template>
          </el-table-column>

          <el-table-column
            label="答案解析"
          >
            <template v-slot="scope">
              <el-input v-model="scope.row.analysis" type="textarea" />
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            align="center"
            width="100px"
          >
            <template v-slot="scope">
              <el-button type="danger" icon="el-icon-delete" circle @click="removeItem(scope.$index)" />
            </template>
          </el-table-column>

        </el-table>

      </div>

      <div style="margin-top: 20px">
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button type="info" @click="onCancel">返回</el-button>
      </div>

    </el-form>

  </div>
</template>

<script>
import { fetchDetail, saveData } from '@/api/qu/qu'
import { post } from '@/utils/request'
import RepoSelect from '@/components/RepoSelect'
import FileUpload from '@/components/FileUpload'
import { getOutlineList, getOutlinesBySubjectAndGrade, identifySingleOutline } from '@/api/outline'

export default {
  name: 'QuDetail',
  components: { FileUpload, RepoSelect },
  data() {
    return {

      quTypeDisabled: false,
      itemImage: true,

      levels: [
        { value: 1, label: '简单' },
        { value: 2, label: '普通' },
        { value: 3, label: '难题' },
        { value: 4, label: '超难' }
      ],

      quTypes: [{
        value: 1,
        label: '单选题'
      }, {
        value: 2,
        label: '多选题'
      },
      {
        value: 3,
        label: '判断题'
      },
      {
        value: 4,
        label: '简答题'
      },
      {
        value: 5,
        label: '填空题'
      }
      ],

      postForm: {
        repoIds: [],
        tagList: [],
        answerList: [],
        questionStem: '',
        knowledgePoints: '[]',
        extractionStatus: 0,
        outlineId: '',
        subject: '',
        grade: ''
      },
      
      // New data for enhanced features
      newKnowledgePoint: '',
      stemExtracting: false,
      
      // Knowledge point data
      availableKnowledgePoints: [],
      knowledgePointsLoading: false,
      knowledgeIdentifying: false,
      
      // Outline selection data
      outlineSubject: '',
      outlineGrade: '',
      availableOutlines: [],
      outlineIdentifying: false,
      rules: {
        content: [
          { required: true, message: '题目内容不能为空！' }
        ],

        quType: [
          { required: true, message: '题目类型不能为空！' }
        ],

        level: [
          { required: true, message: '必须选择难度等级！' }
        ],

        repoIds: [
          { required: true, message: '至少要选择一个题库！' }
        ]
      }
    }
  },
  created() {
    const id = this.$route.params.id
    if (typeof id !== 'undefined') {
      this.quTypeDisabled = true
      this.fetchData(id)
    }
  },
  computed: {
    knowledgePointsArray() {
      try {
        const parsed = JSON.parse(this.postForm.knowledgePoints || '[]')
        return Array.isArray(parsed) ? parsed : []
      } catch (e) {
        return []
      }
    }
  },
  methods: {

    handleTypeChange(v) {
      this.postForm.answerList = []
      if (v === 3) {
        this.postForm.answerList.push({ isRight: true, content: '正确', analysis: '' })
        this.postForm.answerList.push({ isRight: false, content: '错误', analysis: '' })
      }

      if (v === 1 || v === 2) {
        this.postForm.answerList.push({ isRight: false, content: '', analysis: '' })
        this.postForm.answerList.push({ isRight: false, content: '', analysis: '' })
        this.postForm.answerList.push({ isRight: false, content: '', analysis: '' })
        this.postForm.answerList.push({ isRight: false, content: '', analysis: '' })
      }

      if (v === 4) {
        // 简答题添加一个默认答案
        this.postForm.answerList.push({ isRight: true, content: '', analysis: '' })
      }
    },

    // 添加子项
    handleAdd() {
      this.postForm.answerList.push({ isRight: false, content: '', analysis: '' })
    },

    removeItem(index) {
      this.postForm.answerList.splice(index, 1)
    },

    // Enhanced features methods
    addKnowledgePoint() {
      if (this.newKnowledgePoint.trim()) {
        // Only allow one knowledge point
        this.postForm.knowledgePoints = JSON.stringify([this.newKnowledgePoint.trim()])
        this.postForm.subject = this.outlineSubject
        this.postForm.grade = this.outlineGrade
        this.postForm.extractionStatus = 2 // Mark as manually edited
        this.newKnowledgePoint = ''
      }
    },

    removeKnowledgePoint(point) {
      // Clear the knowledge point
      this.postForm.knowledgePoints = JSON.stringify([])
      this.postForm.extractionStatus = 2 // Mark as manually edited
    },

    // Load knowledge points for selected subject and grade
    async loadKnowledgePoints() {
      if (!this.outlineSubject || !this.outlineGrade) {
        this.availableKnowledgePoints = []
        return
      }
      
      console.log('Loading knowledge points for:', this.outlineSubject, this.outlineGrade)
      this.knowledgePointsLoading = true
      try {
        const response = await getOutlineList()
        console.log('API response:', response)
        
        if (response && response.data) {
          // Filter by subject and grade and split knowledge points
          const allKnowledgePoints = []
          response.data
            .filter(item => 
              item.subject === this.outlineSubject &&
              item.grade === this.outlineGrade
            )
            .forEach(item => {
              // Split knowledge points by spaces
              if (item.knowledgePoint && item.knowledgePoint.trim()) {
                const points = item.knowledgePoint.trim().split(/\s+/)
                points.forEach(point => {
                  if (point.trim()) {
                    allKnowledgePoints.push({
                      id: item.id + '_' + point, // Create unique ID
                      knowledgePoint: point.trim()
                    })
                  }
                })
              }
            })
          
          console.log('Split knowledge points:', allKnowledgePoints)
          
          // Remove duplicates
          const uniquePoints = allKnowledgePoints.filter((point, index, self) => 
            index === self.findIndex(p => p.knowledgePoint === point.knowledgePoint)
          )
          
          this.availableKnowledgePoints = uniquePoints
          console.log('Available knowledge points:', this.availableKnowledgePoints)
        }
      } catch (error) {
        console.error('Knowledge point loading failed:', error)
      } finally {
        this.knowledgePointsLoading = false
      }
    },

    async extractStem() {
      if (!this.postForm.content.trim()) {
        this.$message.warning('请先输入题目内容')
        return
      }

      this.stemExtracting = true
      try {
        const response = await post('/api/ai/extract-stem', {
          questionContent: this.postForm.content
        })
        
        if (response.data) {
          this.postForm.questionStem = response.data
          this.postForm.extractionStatus = 1 // Mark as AI extracted
          this.$message.success('题干提取成功')
        }
      } catch (error) {
        console.error('Stem extraction failed:', error)
        this.$message.error('题干提取失败: ' + (error.response && error.response.data ? error.response.data : error.message))
      } finally {
        this.stemExtracting = false
      }
    },

    resetStem() {
      this.postForm.questionStem = this.postForm.content
      this.postForm.extractionStatus = 0 // Mark as unprocessed
    },


    // Outline related methods
    onOutlineSubjectChange() {
      this.outlineGrade = ''
      this.availableOutlines = []
      this.availableKnowledgePoints = []
      this.postForm.outlineId = ''
    },

    onOutlineGradeChange() {
      this.loadOutlines()
      this.loadKnowledgePoints()
    },

    async loadOutlines() {
      if (!this.outlineSubject || !this.outlineGrade) {
        return
      }

      try {
        const response = await getOutlinesBySubjectAndGrade(this.outlineSubject, this.outlineGrade)
        if (response.success) {
          this.availableOutlines = response.data || []
        }
      } catch (error) {
        console.error('Failed to load outlines:', error)
        this.$message.error('加载大纲失败')
      }
    },

    async identifyOutline() {
      if (!this.postForm.content.trim()) {
        this.$message.warning('请先输入题目内容')
        return
      }

      if (!this.outlineSubject || !this.outlineGrade) {
        this.$message.warning('请先选择学科和年级')
        return
      }

      this.outlineIdentifying = true
      try {
        const response = await identifySingleOutline({
          questionId: this.postForm.id || 'temp',
          subject: this.outlineSubject,
          grade: this.outlineGrade
        })

        if (response.success && response.data) {
          const result = response.data
          if (result.outlineId && result.knowledgePoint) {
            // Set outline ID
            this.postForm.outlineId = result.outlineId
            
            // Set knowledge point and record subject/grade from outline selection
            this.postForm.knowledgePoints = JSON.stringify([result.knowledgePoint])
            this.postForm.subject = this.outlineSubject
            this.postForm.grade = this.outlineGrade
            this.postForm.extractionStatus = 1 // Mark as AI extracted
            
            this.$message.success(`AI识别成功: ${result.knowledgePoint}`)
            
            // Load outlines to show the selection
            await this.loadOutlines()
          } else {
            this.$message.warning('AI未能识别出匹配的大纲')
          }
        } else {
          this.$message.warning('AI识别失败')
        }
      } catch (error) {
        console.error('Outline identification failed:', error)
        this.$message.error('大纲识别失败')
      } finally {
        this.outlineIdentifying = false
      }
    },

    async identifyKnowledgePoint() {
      if (!this.postForm.content.trim()) {
        this.$message.warning('请先输入题目内容')
        return
      }

      this.knowledgeIdentifying = true
      try {
        // Use constrained API if subject/grade selected, otherwise use basic API
        let apiEndpoint = '/api/ai/identify-knowledge'
        let requestData = { questionContent: this.postForm.content }
        
        if (this.outlineSubject && this.outlineGrade) {
          apiEndpoint = '/api/ai/identify-knowledge-with-constraints'
          requestData = {
            questionContent: this.postForm.content,
            subject: this.outlineSubject,
            grade: this.outlineGrade
          }
        }
        
        const response = await post(apiEndpoint, requestData)
        
        if (response.data) {
          // Simple text processing - no complex JSON parsing needed
          const knowledgePoint = response.data.trim()
          if (knowledgePoint && knowledgePoint.length > 0) {
            // Set the knowledge point (stored as JSON array for consistency)
            this.postForm.knowledgePoints = JSON.stringify([knowledgePoint])
            this.postForm.subject = this.outlineSubject
            this.postForm.grade = this.outlineGrade
            this.postForm.extractionStatus = 1 // Mark as AI extracted
            this.$message.success(`AI识别成功: ${knowledgePoint}`)
            console.log('Knowledge point identified:', knowledgePoint)
          } else {
            this.$message.warning('AI返回了空的知识点')
            console.warn('Empty AI response for knowledge point identification')
          }
        } else {
          this.$message.warning('AI未能识别出知识点')
          console.warn('No data in AI response')
        }
      } catch (error) {
        console.error('Knowledge point identification failed:', error)
        this.$message.error('知识点识别失败: ' + (error.response && error.response.data ? error.response.data.msg || error.response.data : error.message))
      } finally {
        this.knowledgeIdentifying = false
      }
    },

    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data
        
        // If question has subject/grade, set outline selection to match
        if (this.postForm.subject && this.postForm.grade) {
          this.outlineSubject = this.postForm.subject
          this.outlineGrade = this.postForm.grade
          // Load knowledge points for the selected subject/grade
          this.loadKnowledgePoints()
          this.loadOutlines()
        }
      })
    },
    submitForm() {
      console.log(JSON.stringify(this.postForm))

      let rightCount = 0

      this.postForm.answerList.forEach(function(item) {
        if (item.isRight) {
          rightCount += 1
        }
      })

      if (this.postForm.quType === 1) {
        if (rightCount !== 1) {
          this.$message({
            message: '单选题答案只能有一个',
            type: 'warning'
          })

          return
        }
      }

      if (this.postForm.quType === 2) {
        if (rightCount < 2) {
          this.$message({
            message: '多选题至少要有两个正确答案！',
            type: 'warning'
          })

          return
        }
      }

      if (this.postForm.quType === 3) {
        if (rightCount !== 1) {
          this.$message({
            message: '判断题只能有一个正确项！',
            type: 'warning'
          })

          return
        }
      }

      if (this.postForm.quType === 4) {
        // 简答题需要至少有一个答案内容
        if (this.postForm.answerList.length === 0 || !this.postForm.answerList.some(item => item.content && item.content.trim())) {
          this.$message({
            message: '简答题至少需要有一个参考答案！',
            type: 'warning'
          })

          return
        }
        // 确保简答题的答案都标记为正确
        this.postForm.answerList.forEach(item => {
          if (item.content && item.content.trim()) {
            item.isRight = true
          }
        })
      }

      this.$refs.postForm.validate((valid) => {
        if (!valid) {
          return
        }

        saveData(this.postForm).then(response => {
          this.postForm = response.data
          this.$notify({
            title: '成功',
            message: '试题保存成功！',
            type: 'success',
            duration: 2000
          })

          this.$router.push({ name: 'ListQu' })
        })
      })
    },
    onCancel() {
      this.$router.push({ name: 'ListQu' })
    }

  }
}
</script>

<style scoped>
/* Enhanced form styling */
.stem-input-container {
  width: 100%;
}

.stem-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.knowledge-points-container {
  width: 100%;
}

.knowledge-tags {
  margin-bottom: 12px;
  min-height: 32px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.knowledge-tags .el-tag {
  margin: 0;
}

.knowledge-input {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.outline-selection-container {
  width: 100%;
}

.outline-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

</style>

