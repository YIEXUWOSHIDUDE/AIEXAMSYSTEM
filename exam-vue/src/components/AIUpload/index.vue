<template>
  <div class="ai-upload">
    <el-dialog
      :close-on-click-modal="false"
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="50%"
    >
      <div class="upload-container">
        
        <!-- 上传区域 -->
        <div class="upload-area">
          <el-upload
            ref="upload"
            :accept="acceptTypes"
            :action="uploadUrl"
            :auto-upload="false"
            :before-upload="beforeUpload"
            :headers="headers"
            :limit="1"
            :on-change="onFileChange"
            :on-error="onError"
            :on-progress="onProgress"
            :on-success="onSuccess"
            class="upload-demo"
            drag
          >
            <i class="el-icon-upload" />
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <div slot="tip" class="el-upload__tip">
              <div class="tips-content">
                <p><i class="el-icon-info" /> 支持的文件格式：PDF, DOC, DOCX, TXT</p>
                <p><i class="el-icon-magic-stick" /> AI将自动识别并提取试题内容</p>
                <p><i class="el-icon-warning" /> 文件大小不超过50MB</p>
              </div>
            </div>
          </el-upload>
        </div>

        <!-- 上传进度 -->
        <div v-if="uploading" class="upload-progress">
          <div class="progress-info">
            <i class="el-icon-loading" />
            <span>{{ progressText }}</span>
          </div>
          <el-progress
            :percentage="uploadPercentage"
            :status="progressStatus"
            :stroke-width="8"
          />
        </div>

        <!-- 结果显示 -->
        <div v-if="uploadResult" class="upload-result">
          <el-alert
            :closable="false"
            :description="resultDescription"
            :title="resultTitle"
            :type="resultType"
            show-icon
          />

          <div v-if="extractedQuestions.length > 0" class="questions-preview">
            <h4><i class="el-icon-document" /> 提取到的题目预览 (共{{ extractedQuestions.length }}道)</h4>
            <div class="question-list">
              <div
                v-for="(question, index) in extractedQuestions"
                :key="index"
                class="question-item"
              >
                <div class="question-header">
                  <span class="question-number">{{ index + 1 }}.</span>
                  <span v-if="shouldShowQuestionType(question.quType)" class="question-type">{{ getQuestionTypeName(question.quType) }}</span>
                </div>
                <div class="question-content">{{ question.content }}</div>
                <div v-if="question.options && question.options.length > 0" class="question-options">
                  <div
                    v-for="(option, optIndex) in question.options"
                    :key="optIndex"
                    class="option-item"
                  >
                    <span class="option-label">{{ String.fromCharCode(65 + optIndex) }}.</span>
                    <span class="option-content">{{ option.content }}</span>
                    <i v-if="option.isRight" class="el-icon-check correct-answer" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 提取到的图片预览 -->
          <div v-if="extractedImages.length > 0" class="images-preview">
            <h4><i class="el-icon-picture" /> 提取到的图片 (共{{ extractedImages.length }}张)</h4>
            <div class="image-gallery">
              <div
                v-for="(image, index) in extractedImages"
                :key="index"
                class="image-item"
              >
                <div class="image-container">
                  <img :src="image.image_url" :alt="`提取图片 ${index + 1}`" class="extracted-image" />
                  <div class="image-info">
                    <span class="image-type">{{ getImageTypeText(image.image_type) }}</span>
                    <span v-if="image.page_number" class="page-info">第{{ image.page_number }}页</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

      </div>

      <span slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button
          v-if="!uploading && !uploadResult"
          :disabled="!hasFile"
          type="primary"
          @click="startUpload"
        >
          开始AI解析
        </el-button>
        <el-button
          v-if="uploadResult && resultType === 'success'"
          type="success"
          @click="confirmImport"
        >
          确认导入
        </el-button>
        <el-button
          v-if="uploadResult && resultType !== 'success'"
          type="primary"
          @click="resetUpload"
        >
          重新上传
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'
import { aiUploadQuestions } from '@/api/qu/qu'

export default {
  name: 'AIUpload',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: 'AI智能导入题目',
      
      // 上传相关
      uploadUrl: `${process.env.VUE_APP_BASE_API}/exam/api/ai-upload/upload`,
      headers: {},
      acceptTypes: '.pdf,.doc,.docx,.txt',
      hasFile: false,
      
      // 进度相关
      uploading: false,
      uploadPercentage: 0,
      progressText: '准备上传...',
      progressStatus: '',
      
      // 结果相关
      uploadResult: false,
      resultTitle: '',
      resultType: '',
      resultDescription: '',
      extractedQuestions: [],
      extractedImages: [],
      
      // 题目类型映射
      questionTypeMap: {
        1: '单选题',
        2: '多选题',
        3: '判断题',
        4: '简答题',
        5: '填空题'
      }
    }
  },
  
  watch: {
    visible(val) {
      this.dialogVisible = val
      if (val) {
        this.resetUpload()
      }
    },
    
    dialogVisible(val) {
      this.$emit('update:visible', val)
    }
  },
  
  created() {
    this.headers = { token: getToken() }
  },
  
  methods: {
    // 文件变化监听
    onFileChange(file, fileList) {
      console.log('文件变化:', file, fileList)
      if (fileList.length > 0) {
        this.hasFile = true
      } else {
        this.hasFile = false
      }
    },

    // 上传前检查
    beforeUpload(file) {
      const isValidSize = file.size / 1024 / 1024 < 50 // 50MB
      const isValidType = this.isValidFileType(file.name)
      
      if (!isValidType) {
        this.$message.error('不支持的文件格式！请上传PDF、DOC、DOCX或TXT文件')
        return false
      }
      
      if (!isValidSize) {
        this.$message.error('文件大小不能超过50MB!')
        return false
      }
      
      this.hasFile = true
      return false // 阻止自动上传
    },
    
    // 检查文件类型
    isValidFileType(fileName) {
      const validExtensions = ['.pdf', '.doc', '.docx', '.txt']
      const extension = fileName.toLowerCase().substring(fileName.lastIndexOf('.'))
      return validExtensions.includes(extension)
    },
    
    // 开始上传
    startUpload() {
      console.log('开始上传，hasFile:', this.hasFile)
      const fileList = this.$refs.upload.uploadFiles
      console.log('文件列表:', fileList)
      
      if (fileList.length === 0) {
        this.$message.warning('请先选择文件')
        return
      }
      
      this.uploading = true
      this.uploadResult = false
      this.uploadPercentage = 0
      this.progressText = '正在上传文件...'
      this.progressStatus = null
      
      // 模拟进度更新
      this.simulateProgress()
      
      // 执行上传
      const file = fileList[0].raw
      console.log('准备上传文件:', file)
      
      aiUploadQuestions(file).then(response => {
        console.log('上传成功:', response)
        this.handleUploadSuccess(response)
      }).catch(error => {
        console.error('上传失败:', error)
        this.handleUploadError(error)
      })
    },
    
    // 模拟上传进度
    simulateProgress() {
      const progressSteps = [
        { percent: 10, text: '正在上传文件...' },
        { percent: 30, text: '文件上传完成，开始解析...' },
        { percent: 50, text: 'AI正在分析文档内容...' },
        { percent: 70, text: '正在提取题目信息...' },
        { percent: 85, text: '正在生成题目结构...' },
        { percent: 95, text: '即将完成...' }
      ]

      let stepIndex = 0
      const progressTimer = setInterval(() => {
        if (stepIndex < progressSteps.length && this.uploading) {
          const step = progressSteps[stepIndex]
          this.uploadPercentage = step.percent
          this.progressText = step.text
          stepIndex++
        } else {
          clearInterval(progressTimer)
        }
      }, 800)

      // 保存定时器引用以便清除
      this.progressTimer = progressTimer
    },
    
    // 上传进度
    onProgress(event, file, fileList) {
      if (this.uploading) {
        this.uploadPercentage = Math.round(event.percent * 0.3) // 上传占30%
        this.progressText = `上传进度 ${this.uploadPercentage}%`
      }
    },
    
    // 上传成功
    onSuccess(response, file, fileList) {
      // 由于使用自定义上传，这个方法可能不会被调用
    },
    
    // 处理上传成功
    handleUploadSuccess(response) {
      // 清除进度定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer)
      }
      
      this.uploading = false
      this.uploadResult = true
      this.uploadPercentage = 100
      this.progressStatus = 'success'
      this.progressText = 'AI解析完成！'
      
      if (response.code === 0) {
        // 成功
        const responseData = response.data || {}
        const questions = responseData.questions || responseData || []
        const savedCount = responseData.savedCount || questions.length
        const totalCount = responseData.totalCount || questions.length
        const imageCount = responseData.imageCount || 0
        
        this.extractedQuestions = questions
        this.extractedImages = responseData.images || []
        this.resultType = 'success'
        this.resultTitle = '题目提取成功！'
        
        if (savedCount > 0) {
          let description = `成功从文档中提取并保存了 ${savedCount} 道题目（共解析 ${totalCount} 道）`
          if (imageCount > 0) {
            description += `，提取了 ${imageCount} 张图片`
          }
          this.resultDescription = description + '。'
        } else if (questions.length > 0) {
          let description = `成功从文档中提取了 ${questions.length} 道题目`
          if (imageCount > 0) {
            description += `和 ${imageCount} 张图片`
          }
          this.resultDescription = description + '，请确认后导入系统。'
        } else {
          this.resultDescription = '文档解析完成，但未发现有效的题目内容。请检查文档格式或内容。'
          this.resultType = 'warning'
        }
      } else {
        // 失败
        this.resultType = 'error'
        this.resultTitle = '题目提取失败'
        this.resultDescription = response.msg || '文件解析失败，请检查文件格式或内容'
        this.extractedQuestions = []
        this.extractedImages = []
      }
    },
    
    // 上传错误
    onError(err, file, fileList) {
      this.handleUploadError(err)
    },
    
    // 处理上传错误
    handleUploadError(error) {
      // 清除进度定时器
      if (this.progressTimer) {
        clearInterval(this.progressTimer)
      }
      
      this.uploading = false
      this.uploadResult = true
      this.uploadPercentage = 0
      this.progressStatus = 'exception'
      this.progressText = '上传失败'
      
      this.resultType = 'error'
      this.resultTitle = '上传失败'
      this.resultDescription = error.message || '网络错误或服务器异常，请稍后重试'
      this.extractedQuestions = []
      this.extractedImages = []
    },
    
    // 获取题目类型名称
    getQuestionTypeName(type) {
      return this.questionTypeMap[type] || '未知类型'
    },
    
    // 判断是否显示题目类型（显示所有题目类型包括填空题）
    shouldShowQuestionType(type) {
      return true // 显示所有题目类型包括填空题
    },
    
    // 获取图片类型显示文本
    getImageTypeText(imageType) {
      const typeMap = {
        'docx_embedded': 'Word文档图片',
        'pdf_embedded': 'PDF文档图片'
      }
      return typeMap[imageType] || '文档图片'
    },
    
    // 确认导入
    confirmImport() {
      this.$emit('import-success', this.extractedQuestions)
      this.closeDialog()
      this.$message.success(`成功导入 ${this.extractedQuestions.length} 道题目！`)
    },
    
    // 重新上传
    resetUpload() {
      this.uploading = false
      this.uploadResult = false
      this.uploadPercentage = 0
      this.progressText = '准备上传...'
      this.progressStatus = null
      this.hasFile = false
      this.extractedQuestions = []
      this.extractedImages = []
      this.resultTitle = ''
      this.resultType = ''
      this.resultDescription = ''
      
      // 清空文件列表
      if (this.$refs.upload) {
        this.$refs.upload.clearFiles()
      }
    },
    
    // 关闭对话框
    closeDialog() {
      this.dialogVisible = false
      this.resetUpload()
    }
  }
}
</script>

<style scoped>
.ai-upload {
  .upload-container {
    padding: 20px 0;
  }
  
  .upload-area {
    margin-bottom: 20px;
    
    .upload-demo {
      .el-upload {
        border: 2px dashed #d9d9d9;
        border-radius: 6px;
        cursor: pointer;
        position: relative;
        overflow: hidden;
        transition: all 0.3s;
        
        &:hover {
          border-color: #409EFF;
          background-color: #fafafa;
        }
      }
      
      .el-icon-upload {
        font-size: 67px;
        color: #C0C4CC;
        margin: 40px 0 16px;
        line-height: 50px;
      }
      
      .el-upload__text {
        color: #606266;
        font-size: 14px;
        text-align: center;
        
        em {
          color: #409EFF;
          font-style: normal;
        }
      }
    }
    
    .tips-content {
      p {
        margin: 5px 0;
        font-size: 12px;
        color: #909399;
        
        i {
          margin-right: 5px;
          color: #409EFF;
        }
      }
    }
  }
  
  .upload-progress {
    margin: 20px 0;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 6px;
    
    .progress-info {
      display: flex;
      align-items: center;
      margin-bottom: 10px;
      
      i {
        margin-right: 10px;
        color: #409EFF;
      }
      
      span {
        font-size: 14px;
        color: #606266;
      }
    }
  }
  
  .upload-result {
    margin: 20px 0;
    
    .questions-preview {
      margin-top: 20px;
      
      h4 {
        color: #303133;
        margin-bottom: 15px;
        
        i {
          margin-right: 8px;
          color: #409EFF;
        }
      }
      
      .question-list {
        max-height: 400px;
        overflow-y: auto;
        border: 1px solid #EBEEF5;
        border-radius: 4px;
        
        .question-item {
          padding: 15px;
          border-bottom: 1px solid #EBEEF5;
          
          &:last-child {
            border-bottom: none;
          }
          
          .question-header {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            
            .question-number {
              font-weight: bold;
              color: #409EFF;
              margin-right: 10px;
              min-width: 25px;
            }
            
            .question-type {
              background: #409EFF;
              color: white;
              padding: 2px 8px;
              border-radius: 12px;
              font-size: 12px;
            }
          }
          
          .question-content {
            color: #303133;
            font-size: 14px;
            line-height: 1.6;
            margin-bottom: 10px;
          }
          
          .question-options {
            .option-item {
              display: flex;
              align-items: center;
              margin: 5px 0;
              font-size: 13px;
              
              .option-label {
                font-weight: bold;
                margin-right: 8px;
                min-width: 20px;
                color: #909399;
              }
              
              .option-content {
                flex: 1;
                color: #606266;
              }
              
              .correct-answer {
                color: #67C23A;
                font-weight: bold;
                margin-left: 10px;
              }
            }
          }
        }
      }
      
      .images-preview {
        margin-top: 20px;
        
        h4 {
          color: #409EFF;
          margin-bottom: 15px;
          
          i {
            margin-right: 8px;
          }
        }
        
        .image-gallery {
          display: flex;
          flex-wrap: wrap;
          gap: 15px;
          
          .image-item {
            .image-container {
              border: 1px solid #DCDFE6;
              border-radius: 8px;
              overflow: hidden;
              background: #fff;
              box-shadow: 0 2px 4px rgba(0,0,0,0.1);
              transition: transform 0.2s;
              
              &:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 8px rgba(0,0,0,0.15);
              }
              
              .extracted-image {
                width: 150px;
                height: 120px;
                object-fit: cover;
                display: block;
              }
              
              .image-info {
                padding: 8px 12px;
                background: #F5F7FA;
                font-size: 12px;
                color: #909399;
                
                .image-type {
                  display: block;
                  font-weight: 500;
                }
                
                .page-info {
                  color: #67C23A;
                  margin-left: 8px;
                }
              }
            }
          }
        }
      }
    }
  }
  
  .dialog-footer {
    text-align: right;
  }
}
</style>