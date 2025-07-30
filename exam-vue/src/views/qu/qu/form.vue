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
              <el-select v-model="outlineGrade" placeholder="选择年级" @change="loadOutlines" size="small" style="width: 100px; margin-right: 10px;">
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
            <div class="outline-select" style="margin-top: 10px;">
              <el-select v-model="postForm.outlineId" placeholder="选择知识大纲" clearable style="width: 100%;">
                <el-option
                  v-for="outline in availableOutlines"
                  :key="outline.id"
                  :label="`${outline.subject} - ${outline.grade} - ${outline.knowledgePoint}`"
                  :value="outline.id"
                />
              </el-select>
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
            <div class="knowledge-input" v-if="knowledgePointsArray.length === 0">
              <el-input
                v-model="newKnowledgePoint"
                placeholder="输入一个知识点"
                @keyup.enter.native="addKnowledgePoint"
                size="small"
                style="width: 200px; margin-right: 10px;" />
              <el-button size="small" @click="addKnowledgePoint">设置</el-button>
              <el-button size="small" type="primary" @click="identifyKnowledgePoints" :loading="knowledgeIdentifying">
                <i class="el-icon-magic-stick"></i>
                AI识别
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

      <div v-if="postForm.quType!==4" class="filter-container" style="margin-top: 25px">

        <el-button class="filter-item" type="primary" icon="el-icon-plus" size="small" plain @click="handleAdd">
          添加
        </el-button>

        <el-table
          :data="postForm.answerList"
          :border="true"
          style="width: 100%;"
        >
          <el-table-column
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
            label="答案内容"
          >
            <template v-slot="scope">
              <el-input v-model="scope.row.content" type="textarea" />
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
import { getOutlinesBySubjectAndGrade, identifySingleOutline } from '@/api/outline'

export default {
  name: 'QuDetail',
  components: { FileUpload, RepoSelect },
  data() {
    return {

      quTypeDisabled: false,
      itemImage: true,

      levels: [
        { value: 1, label: '普通' },
        { value: 2, label: '较难' }
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
        outlineId: ''
      },
      
      // New data for enhanced features
      newKnowledgePoint: '',
      stemExtracting: false,
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
        this.postForm.extractionStatus = 2 // Mark as manually edited
        this.newKnowledgePoint = ''
      }
    },

    removeKnowledgePoint(point) {
      // Clear the knowledge point
      this.postForm.knowledgePoints = JSON.stringify([])
      this.postForm.extractionStatus = 2 // Mark as manually edited
    },

    async extractStem() {
      if (!this.postForm.content.trim()) {
        this.$message.warning('请先输入题目内容')
        return
      }

      this.stemExtracting = true
      try {
        const response = await post('/api/llm/extractstem', {
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

    async identifyKnowledgePoints() {
      if (!this.postForm.content.trim()) {
        this.$message.warning('请先输入题目内容')
        return
      }

      this.knowledgeIdentifying = true
      try {
        const response = await post('/api/llm/identifyknowledge', {
          questionContent: this.postForm.content
        })
        
        if (response.data) {
          const identified = JSON.parse(response.data)
          
          if (Array.isArray(identified) && identified.length > 0) {
            // Only use the first knowledge point
            this.$set(this.postForm, 'knowledgePoints', JSON.stringify([identified[0]]))
            this.postForm.extractionStatus = 1 // Mark as AI extracted
            this.$message.success(`识别出知识点: ${identified[0]}`)
          } else {
            this.$message.warning('未能识别出有效的知识点')
          }
        } else {
          this.$message.warning('AI服务返回空数据')
        }
      } catch (error) {
        console.error('Knowledge identification failed:', error)
        this.$message.error('知识点识别失败: ' + (error.response && error.response.data ? error.response.data : error.message))
      } finally {
        this.knowledgeIdentifying = false
      }
    },

    // Outline related methods
    onOutlineSubjectChange() {
      this.outlineGrade = ''
      this.availableOutlines = []
      this.postForm.outlineId = ''
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
          if (result.outlineId) {
            this.postForm.outlineId = result.outlineId
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

    fetchData(id) {
      fetchDetail(id).then(response => {
        this.postForm = response.data
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

