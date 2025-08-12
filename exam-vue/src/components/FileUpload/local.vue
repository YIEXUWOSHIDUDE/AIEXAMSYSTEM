<template>
  <div class="content">
    <!-- Show existing image if available -->
    <div v-if="fileUrl && isImageFile(fileUrl)" class="image-container" style="display: flex; align-items: center; gap: 10px; margin-bottom: 10px;">
      <el-image 
        :src="fileUrl" 
        style="width: 150px; height: 100px; border-radius: 6px; border: 1px solid #dcdfe6; flex-shrink: 0;"
        fit="cover"
        :preview-src-list="[fileUrl]">
        <div slot="error" class="image-slot">
          <i class="el-icon-picture-outline"></i>
        </div>
      </el-image>
      <div class="image-actions" style="display: flex; flex-direction: column; gap: 8px;">
        <el-upload
          :action="server"
          :accept="accept"
          :on-success="handleSuccess"
          :on-exceed="handleExceed"
          :show-file-list="false"
          :limit="limit"
          :headers="header"
        >
          <el-button size="mini" type="primary">更换图片</el-button>
        </el-upload>
        <el-button size="mini" type="danger" @click="handleRemoveImage">删除图片</el-button>
      </div>
    </div>

    <!-- Upload button when no image -->
    <el-upload
      v-if="!fileUrl || !isImageFile(fileUrl)"
      v-model="fileUrl"
      :action="server"
      :accept="accept"
      :before-remove="beforeRemove"
      :on-remove="handleRemove"
      :on-success="handleSuccess"
      :on-exceed="handleExceed"
      :show-file-list="false"
      :limit="limit"
      :headers="header"
    >
      <el-button size="small" type="primary">上传图片</el-button>
      <div v-if="tips" slot="tip" class="el-upload__tip">{{ tips }}</div>
    </el-upload>

  </div>

</template>

<script>

import { getToken } from '@/utils/auth'

export default {
  name: 'FileUploadLocal',
  props: {
    value: String,
    accept: String,
    tips: String,
    listType: String,
    limit: {
      type: Number,
      default: 1
    }
  },
  data() {
    return {
      server: `${process.env.VUE_APP_BASE_API}/common/api/file/upload`,
      fileList: [],
      fileUrl: '',
      header: {}
    }
  },

  watch: {
    // 检测查询变化
    value: {
      handler() {
        this.fillValue()
      }
    }
  },

  created() {
    this.fillValue()
    this.header = { token: getToken() }
  },

  methods: {

    fillValue() {
      this.fileList = []
      this.fileUrl = this.value
      if (this.fileUrl) {
        // Extract filename from URL instead of showing full URL
        const fileName = this.fileUrl.split('/').pop() || 'image.jpg'
        this.fileList = [{ name: fileName, url: this.fileUrl }]
      }
    },

    // Check if the file is an image
    isImageFile(url) {
      if (!url) return false
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']
      const lowerUrl = url.toLowerCase()
      return imageExtensions.some(ext => lowerUrl.includes(ext))
    },

    // Handle image removal
    handleRemoveImage() {
      this.$confirm('确定删除这张图片吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$emit('input', '')
        this.fileUrl = ''
        this.fileList = []
        this.$message.success('图片已删除')
      }).catch(() => {})
    },

    // 文件超出个数限制时的钩子
    handleExceed() {
      this.$message.warning(`每次只能上传 ${this.limit} 个文件`)
    },
    // 删除文件之前的钩子
    beforeRemove() {
      return this.$confirm(`确定移除文件吗？`)
    },

    // 文件列表移除文件时的钩子
    handleRemove() {
      this.$emit('input', '')
      this.fileList = []
    },

    // 文件上传成功时的钩子
    handleSuccess(response) {
      if (response.code === 1) {
        this.$message({
          type: 'error',
          message: response.msg
        })

        this.fileList = []
        return
      }
      this.$emit('input', response.data.url)
    }

  }
}
</script>
