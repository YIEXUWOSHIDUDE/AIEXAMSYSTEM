<template>

  <div>

    <data-table
      ref="pagingTable"
      :options="options"
      :list-query="listQuery"
      @multi-actions="handleMultiAction"
    >
      <template #filter-content>

        <el-row>
          <el-col :span="24">

            <el-select v-model="listQuery.params.quType" class="filter-item" clearable>
              <el-option
                v-for="item in quTypes"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>

            <repo-select v-model="listQuery.params.repoIds" :multi="true" />

            <el-input v-model="listQuery.params.content" placeholder="题目内容" style="width: 200px;" class="filter-item" />
            
            <el-input v-model="listQuery.params.knowledgePoint" placeholder="知识点搜索" style="width: 150px;" class="filter-item" />

            <el-select v-model="listQuery.params.subject" placeholder="选择学科" style="width: 120px;" class="filter-item" clearable>
              <el-option label="数学" value="数学" />
              <el-option label="物理" value="物理" />
              <el-option label="化学" value="化学" />
              <el-option label="语文" value="语文" />
              <el-option label="英语" value="英语" />
              <el-option label="生物" value="生物" />
              <el-option label="历史" value="历史" />
              <el-option label="地理" value="地理" />
            </el-select>

            <el-select v-model="listQuery.params.grade" placeholder="选择年级" style="width: 120px;" class="filter-item" clearable>
              <el-option label="七年级" value="七年级" />
              <el-option label="八年级" value="八年级" />
              <el-option label="九年级" value="九年级" />
              <el-option label="高一" value="高一" />
              <el-option label="高二" value="高二" />
              <el-option label="高三" value="高三" />
            </el-select>

            <el-button-group class="filter-item" style="float:  right">
              <el-button size="mini" class="ai-upload-btn" type="primary" @click="showAIImport">
                <i class="el-icon-magic-stick" />
                AI智能导入
              </el-button>
              <el-button size="mini" icon="el-icon-upload2" @click="showImport">导入</el-button>
              <el-button size="mini" icon="el-icon-download" @click="exportExcel">导出</el-button>
            </el-button-group>

          </el-col>
        </el-row>

      </template>

      <template #data-columns>

        <el-table-column
          label="题目类型"
          align="center"
          width="100px"
        >
          <template v-slot="scope">
            {{ scope.row.quType | quTypeFilter() }}
          </template>
        </el-table-column>

        <el-table-column
          label="难度等级"
          align="center"
          width="80px"
        >
          <template v-slot="scope">
            <el-tag 
              :type="getLevelTagType(scope.row.level)"
              size="mini">
              {{ scope.row.level | levelFilter }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="学科"
          align="center"
          width="80px"
        >
          <template v-slot="scope">
            {{ scope.row.subject || '未设置' }}
          </template>
        </el-table-column>

        <el-table-column
          label="年级"
          align="center"
          width="80px"
        >
          <template v-slot="scope">
            {{ scope.row.grade || '未设置' }}
          </template>
        </el-table-column>

        <el-table-column
          label="题目内容"
          show-overflow-tooltip
          min-width="200px"
        >
          <template v-slot="scope">
            <router-link :to="{ name: 'UpdateQu', params:{ id: scope.row.id}}">
              {{ scope.row.content }}
            </router-link>
          </template>
        </el-table-column>

        <el-table-column
          label="题干"
          show-overflow-tooltip
          min-width="180px"
        >
          <template v-slot="scope">
            <div class="question-stem">
              <span v-if="scope.row.questionStem && scope.row.questionStem !== scope.row.content" 
                    class="stem-text">{{ scope.row.questionStem }}</span>
              <span v-else class="stem-placeholder">—</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          label="知识点"
          min-width="200px"
        >
          <template v-slot="scope">
            <div class="knowledge-points">
              <template v-if="getFirstKnowledgePoint(scope.row.knowledgePoints)">
                <el-tag 
                  size="mini" 
                  type="primary" 
                  class="knowledge-tag">
                  {{ getFirstKnowledgePoint(scope.row.knowledgePoints) }}
                </el-tag>
              </template>
              <span v-else class="no-knowledge">暂无标签</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          label="创建时间"
          align="center"
          prop="createTime"
          width="180px"
        />

      </template>

    </data-table>

    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="30%"
    >

      <el-form label-position="left" label-width="100px">

        <el-form-item label="操作题库" prop="repoIds">
          <repo-select v-model="dialogRepos" :multi="true" />
        </el-form-item>

        <el-row>
          <el-button type="primary" @click="handlerRepoAction">保存</el-button>
        </el-row>

      </el-form>

    </el-dialog>

    <el-dialog
      :visible.sync="importVisible"
      title="导入试题"
      width="30%"
    >

      <el-row>
        <el-button type="primary" @click="chooseFile">上传导入</el-button>
        <el-button type="warning" @click="downloadTemplate">下载导入模板</el-button>
        <input ref="upFile" class="file" name="file" type="file" style="display: none" @change="doImport">
      </el-row>

    </el-dialog>

    <!-- AI智能导入对话框 -->
    <ai-upload
      :visible.sync="aiImportVisible"
      @import-success="handleAIImportSuccess"
    />

  </div>

</template>

<script>
import DataTable from '@/components/DataTable'
import RepoSelect from '@/components/RepoSelect'
import AIUpload from '@/components/AIUpload'
import { batchAction } from '@/api/qu/repo'
import { exportExcel, importExcel, importTemplate } from '@/api/qu/qu'

console.log('AIUpload组件:', AIUpload)

export default {
  name: 'QuList',
  components: { 
    RepoSelect, 
    DataTable, 
    'ai-upload': AIUpload 
  },
  data() {
    return {

      dialogTitle: '加入题库',
      dialogVisible: false,
      importVisible: false,
      aiImportVisible: false,
      dialogRepos: [],
      dialogQuIds: [],
      dialogFlag: false,

      listQuery: {
        current: 1,
        size: 10,
        params: {
          content: '',
          quType: '',
          repoIds: [],
          subject: '',
          grade: ''
        }
      },

      quTypes: [
        {
          value: 1,
          label: '单选题'
        },
        {
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

      options: {

        // 可批量操作
        multi: true,

        // 批量操作列表
        multiActions: [
          {
            value: 'add-repo',
            label: '加入题库..'
          },
          {
            value: 'remove-repo',
            label: '从..题库移除'
          },
          {
            value: 'delete',
            label: '删除'
          }
        ],
        // 列表请求URL
        listUrl: '/exam/api/qu/qu/paging',
        // 删除请求URL
        deleteUrl: '/exam/api/qu/qu/delete',
        // 添加数据路由
        addRoute: 'AddQu'
      }
    }
  },
  methods: {

    // Helper method to get only the first knowledge point
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
    },

    // 根据难度等级返回标签颜色类型
    getLevelTagType(level) {
      const typeMap = {
        1: 'success',    // 简单 - 绿色
        2: '',           // 普通 - 默认色
        3: 'warning',    // 难题 - 橙色
        4: 'danger'      // 超难 - 红色
      }
      return typeMap[level] || ''
    },

    handleMultiAction(obj) {
      if (obj.opt === 'add-repo') {
        this.dialogTitle = '加入题库'
        this.dialogFlag = false
      }

      if (obj.opt === 'remove-repo') {
        this.dialogTitle = '从题库移除'
        this.dialogFlag = true
      }

      this.dialogVisible = true
      this.dialogQuIds = obj.ids
    },

    handlerRepoAction() {
      const postForm = { repoIds: this.dialogRepos, quIds: this.dialogQuIds, remove: this.dialogFlag }

      batchAction(postForm).then(() => {
        this.$notify({
          title: '成功',
          message: '批量操作成功！',
          type: 'success',
          duration: 2000
        })

        this.dialogVisible = false
        this.$refs.pagingTable.getList()
      })
    },

    exportExcel() {
      // 导出当前查询的数据
      exportExcel(this.listQuery.params)
    },

    downloadTemplate() {
      importTemplate()
    },

    showImport() {
      this.importVisible = true
    },

    showAIImport() {
      this.aiImportVisible = true
    },

    // 只是为了美化一下导入按钮
    chooseFile: function() {
      this.$refs.upFile.dispatchEvent(new MouseEvent('click'))
    },

    doImport(e) {
      const file = e.target.files[0]

      importExcel(file).then(res => {
        if (res.code !== 0) {
          this.$alert(res.data.msg, '导入信息', {
            dangerouslyUseHTMLString: true
          })
        } else {
          this.$message({
            message: '数据导入成功！',
            type: 'success'
          })

          this.importVisible = false
          this.$refs.pagingTable.getList()
        }
      })
    },

    // AI导入成功处理
    handleAIImportSuccess(questions) {
      this.$message.success(`AI智能导入成功！共导入 ${questions.length} 道题目`)
      this.aiImportVisible = false
      // 刷新题目列表
      this.$refs.pagingTable.getList()
    }
  }
}
</script>

<style scoped>
/* Question stem display styles */
.question-stem {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stem-text {
  color: #303133;
  font-size: 13px;
  line-height: 1.4;
}

.stem-placeholder {
  color: #C0C4CC;
  font-style: italic;
}


/* Knowledge points display styles */
.knowledge-points {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.knowledge-tag {
  margin: 0;
  font-size: 11px;
  padding: 2px 6px;
  line-height: 1.2;
  background-color: #f0f9ff;
  border-color: #b3d8ff;
  color: #409eff;
}

.no-knowledge {
  color: #C0C4CC;
  font-style: italic;
  font-size: 12px;
}
.ai-upload-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3) !important;
  transition: all 0.3s ease !important;
}

.ai-upload-btn:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%) !important;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4) !important;
  transform: translateY(-1px) !important;
}

.ai-upload-btn i {
  margin-right: 6px;
  animation: magical 2s infinite ease-in-out;
}

@keyframes magical {
  0%, 100% {
    transform: rotate(0deg) scale(1);
  }
  50% {
    transform: rotate(5deg) scale(1.1);
  }
}

.filter-item {
  margin-right: 10px;
}
</style>
