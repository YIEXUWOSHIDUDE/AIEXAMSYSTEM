<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- AI Grading System Overview -->
      <el-col :span="16">
        <el-card class="box">
          <div slot="header" class="clearfix">
            <span>AI智能评判系统</span>
            <el-button style="float: right; padding: 3px 0; font-size: 12px" type="text">系统版本：{{ version }}</el-button>
          </div>

          <div style="line-height: 32px;">
            <div class="title" style="padding-top: 0px">系统介绍</div>
            AI智能评判系统是一款基于大语言模型(LLM)的智能考试评判系统，专注于简答题自动评分和智能反馈，显著提升评卷效率和评分客观性。

            <div class="title">核心技术</div>
            <div class="tags">
              <el-tag type="success">Qwen3-32B</el-tag>
              <el-tag type="primary">Spring Boot</el-tag>
              <el-tag type="warning">Vue.js</el-tag>
              <el-tag type="info">Element UI</el-tag>
              <el-tag type="danger">AI评判</el-tag>
              <el-tag>MinIO存储</el-tag>
            </div>

            <div class="title">AI评判功能</div>
            <div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 简答题自动评分：基于Qwen3-32B的语义理解评分</div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 实时评判反馈：毫秒级响应，即时获得评分结果</div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 多维度分析：准确性(60%)、完整性(25%)、表达清晰度(15%)</div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 智能文档解析：Word/PDF文档自动题目提取</div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 图片智能处理：支持图文混合题目的智能识别</div>
              <div><i class="el-icon-check" style="color: #67C23A;"></i> 客观公正评分：消除主观偏见，确保评分一致性</div>
            </div>

            <div class="title">适用场景</div>
            <div>
              <div>🎓 教育培训：学校、培训机构的简答题自动评分</div>
              <div>🏢 企业考核：员工技能评估的智能化评判</div>
              <div>📚 在线学习：MOOC平台的实时评判反馈</div>
              <div>🔬 学术研究：教育测量与评判质量研究</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Additional Information -->
      <el-col :span="8">
        <el-card class="box">
          <div slot="header" class="clearfix">
            <span>系统特色</span>
          </div>

          <div>
            <div class="title" style="padding-top: 0px">技术优势</div>
            <div class="feature-list">
              <div class="feature-item">
                <i class="el-icon-cpu" style="color: #409eff;"></i>
                <span>大模型驱动，语义理解精准</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-timer" style="color: #67c23a;"></i>
                <span>毫秒级响应，实时评判反馈</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-pie-chart" style="color: #e6a23c;"></i>
                <span>多维度分析，评分更全面</span>
              </div>
              <div class="feature-item">
                <i class="el-icon-shield" style="color: #f56c6c;"></i>
                <span>客观公正，消除主观偏见</span>
              </div>
            </div>

          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import config from '../../../package.json'

export default {
  name: 'Dashboard',
  data() {
    return {
      currentRole: 'adminDashboard',
      version: config.version
    }
  },
  computed: {
    ...mapGetters([
      'roles',
      'siteData'
    ])
  },
  created() {
    // Dashboard initialization
  }
}
</script>

<style scoped>
.box {
  height: calc(100vh - 120px);
  font-size: 14px;
  line-height: 28px;
  overflow-y: auto;
}

.title {
  font-size: 18px;
  font-weight: 700;
  padding-bottom: 10px;
  padding-top: 20px;
  margin-bottom: 10px;
  border-bottom: #eee 1px dotted;
  color: #303133;
}

.tags span {
  margin-right: 10px;
  margin-bottom: 8px;
}

.feature-list {
  margin-bottom: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 0;
}

.feature-item i {
  margin-right: 12px;
  font-size: 16px;
}

.feature-item span {
  color: #606266;
  font-size: 14px;
}

</style>