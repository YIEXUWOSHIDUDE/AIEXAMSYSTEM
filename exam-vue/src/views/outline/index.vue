<template>
  <div class="app-container">
    <!-- 搜索区域 -->
    <el-card class="filter-container" shadow="never">
      <el-form :inline="true" :model="listQuery" class="demo-form-inline">
        <el-form-item label="学科">
          <el-select v-model="listQuery.subject" placeholder="选择学科" clearable @change="handleFilter">
            <el-option
              v-for="subject in subjects"
              :key="subject"
              :label="subject"
              :value="subject"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="年级">
          <el-select v-model="listQuery.grade" placeholder="选择年级" clearable @change="handleFilter">
            <el-option
              v-for="grade in grades"
              :key="grade"
              :label="grade"
              :value="grade"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
          <el-button type="success" @click="handleCreate">新建大纲</el-button>
          <el-button type="warning" @click="handleUpload">上传大纲文档</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table v-loading="listLoading" :data="list" border>
        <el-table-column label="大纲编码" prop="outlineCode" width="150" />
        <el-table-column label="学科" prop="subject" width="100" />
        <el-table-column label="年级" prop="grade" width="100" />
        <el-table-column label="知识点" prop="knowledgePoint" />
        <el-table-column label="创建时间" prop="createTime" width="160">
          <template slot-scope="{row}">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="{row}">
            <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog :title="dialogStatus === 'create' ? '新建大纲' : '编辑大纲'" :visible.sync="dialogFormVisible" width="500px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="80px">
        <el-form-item label="学科" prop="subject">
          <el-select v-model="temp.subject" placeholder="请选择学科" style="width: 100%">
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
        <el-form-item label="年级" prop="grade">
          <el-select v-model="temp.grade" placeholder="请选择年级" style="width: 100%">
            <el-option label="七年级" value="七年级" />
            <el-option label="八年级" value="八年级" />
            <el-option label="九年级" value="九年级" />
            <el-option label="高一" value="高一" />
            <el-option label="高二" value="高二" />
            <el-option label="高三" value="高三" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点" prop="knowledgePoint">
          <el-input v-model="temp.knowledgePoint" placeholder="请输入知识点名称" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">取消</el-button>
        <el-button type="primary" @click="dialogStatus === 'create' ? createData() : updateData()">
          确定
        </el-button>
      </div>
    </el-dialog>

    <!-- 上传大纲文档对话框 -->
    <el-dialog title="上传知识大纲文档" :visible.sync="uploadDialogVisible" width="600px">
      <div class="upload-info">
        <p>支持上传Word文档(.docx)或PDF文件(.pdf)，系统将自动解析文档中的知识大纲结构。</p>
        <p><strong>文档格式要求：</strong></p>
        <ul>
          <li>包含学科和年级信息</li>
          <li>清晰的知识点层次结构</li>
          <li>每个知识点单独一行或段落</li>
        </ul>
      </div>
      
      <el-form :model="uploadForm" label-width="80px" style="margin-top: 20px;">
        <el-form-item label="学科">
          <el-select v-model="uploadForm.subject" placeholder="请选择学科" style="width: 100%">
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
          <el-select v-model="uploadForm.grade" placeholder="请选择年级" style="width: 100%">
            <el-option label="七年级" value="七年级" />
            <el-option label="八年级" value="八年级" />
            <el-option label="九年级" value="九年级" />
            <el-option label="高一" value="高一" />
            <el-option label="高二" value="高二" />
            <el-option label="高三" value="高三" />
          </el-select>
        </el-form-item>
      </el-form>
      
      <el-upload 
        class="upload-demo"
        drag
        :action="uploadAction"
        :data="uploadForm"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :show-file-list="false"
        accept=".docx,.pdf">
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">只能上传docx/pdf文件，且不超过10MB</div>
      </el-upload>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="uploadDialogVisible = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOutlineList, getSubjects, getGrades, createOutline, updateOutline, deleteOutline } from '@/api/outline'

export default {
  name: 'OutlineIndex',
  data() {
    return {
      list: [],
      subjects: [],
      grades: [],
      listLoading: true,
      listQuery: {
        subject: '',
        grade: ''
      },
      dialogFormVisible: false,
      dialogStatus: '',
      temp: {
        id: undefined,
        subject: '',
        grade: '',
        knowledgePoint: ''
      },
      rules: {
        subject: [{ required: true, message: '请选择学科', trigger: 'change' }],
        grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
        knowledgePoint: [{ required: true, message: '请输入知识点名称', trigger: 'blur' }]
      },
      uploadDialogVisible: false,
      uploadForm: {
        subject: '',
        grade: ''
      },
      uploadAction: process.env.VUE_APP_BASE_API + '/exam/api/outline/upload'
    }
  },
  created() {
    this.getList()
    this.getSubjects()
  },
  methods: {
    getList() {
      this.listLoading = true
      getOutlineList(this.listQuery).then(response => {
        this.list = response.data
        this.listLoading = false
      })
    },
    getSubjects() {
      getSubjects().then(response => {
        this.subjects = response.data
      })
    },
    getGrades() {
      if (this.listQuery.subject) {
        getGrades(this.listQuery.subject).then(response => {
          this.grades = response.data
        })
      }
    },
    handleFilter() {
      if (this.listQuery.subject && !this.listQuery.grade) {
        this.getGrades()
      }
      this.getList()
    },
    resetFilter() {
      this.listQuery = {
        subject: '',
        grade: ''
      }
      this.grades = []
      this.getList()
    },
    resetTemp() {
      this.temp = {
        id: undefined,
        subject: '',
        grade: '',
        knowledgePoint: ''
      }
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = 'create'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    createData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          createOutline(this.temp).then(() => {
            this.dialogFormVisible = false
            this.$notify({
              title: '成功',
              message: '创建成功',
              type: 'success',
              duration: 2000
            })
            this.getList()
          })
        }
      })
    },
    handleEdit(row) {
      this.temp = Object.assign({}, row)
      this.dialogStatus = 'update'
      this.dialogFormVisible = true
      this.$nextTick(() => {
        this.$refs['dataForm'].clearValidate()
      })
    },
    updateData() {
      this.$refs['dataForm'].validate((valid) => {
        if (valid) {
          updateOutline(this.temp).then(() => {
            this.dialogFormVisible = false
            this.$notify({
              title: '成功',
              message: '更新成功',
              type: 'success',
              duration: 2000
            })
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      this.$confirm('此操作将永久删除该大纲, 是否继续?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteOutline(row.id).then(() => {
          this.$notify({
            title: '成功',
            message: '删除成功',
            type: 'success',
            duration: 2000
          })
          this.getList()
        })
      })
    },
    formatTime(time) {
      if (!time) return ''
      return new Date(time).toLocaleString()
    },
    handleUpload() {
      this.uploadForm = {
        subject: '',
        grade: ''
      }
      this.uploadDialogVisible = true
    },
    beforeUpload(file) {
      const isDocxOrPdf = file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' || file.type === 'application/pdf'
      const isLt10M = file.size / 1024 / 1024 < 10

      if (!isDocxOrPdf) {
        this.$message.error('只能上传 DOCX 或 PDF 格式的文件!')
      }
      if (!isLt10M) {
        this.$message.error('上传文件大小不能超过 10MB!')
      }
      if (!this.uploadForm.subject || !this.uploadForm.grade) {
        this.$message.error('请先选择学科和年级!')
        return false
      }
      return isDocxOrPdf && isLt10M
    },
    handleUploadSuccess(response) {
      this.uploadDialogVisible = false
      if (response.code === 0) {
        this.$notify({
          title: '成功',
          message: `成功解析并导入了 ${response.data.importedCount} 个知识点`,
          type: 'success',
          duration: 3000
        })
        this.getList()
      } else {
        this.$message.error(response.msg || '上传失败')
      }
    },
    handleUploadError() {
      this.$message.error('上传失败，请重试')
    }
  }
}
</script>

<style scoped>
.filter-container {
  margin-bottom: 20px;
}

.upload-info {
  background-color: #f4f4f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.upload-info p {
  margin: 0 0 10px 0;
  color: #606266;
}

.upload-info ul {
  margin: 5px 0 0 20px;
  color: #909399;
}

.upload-demo {
  margin-top: 20px;
}
</style>