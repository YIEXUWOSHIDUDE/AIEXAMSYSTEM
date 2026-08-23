# AI 智能考试系统

中文 | [English](README_EN.md)

这是一个面向考试管理场景的 AI 增强系统，在既有考试管理基础上加入文档拆题、简答题评判和智能选题流程。

系统由 Vue 管理界面、Spring Boot 业务服务、Python 文档处理服务、MySQL、MinIO，以及兼容 OpenAI 接口格式的 Qwen 模型服务组成。

> 本仓库属于工程原型。大模型生成的分数与反馈应作为人工复核的辅助信息，不应直接成为高风险考试的唯一评分依据。

## 本版本的主要增强

- **简答题 AI 评判**：将题目、参考答案、学生答案、知识点和满分传给模型，并返回结构化评分与反馈。
- **文档导入题库**：解析上传的 DOCX 文档，提取内嵌图片，保留题目与图片的对应关系，再进入题目导入流程。
- **题目与答案校验**：检查提取结果的字段完整性和答案匹配情况；校验失败时回退到较简单的提取路径。
- **AI 辅助选题**：结合知识点和难度筛选候选题，仅向模型发送精简后的题目信息；AI 服务异常时回退到随机选择。
- **考试管理流程**：沿用既有平台中的题库、试卷、考试、用户、角色、成绩和后台管理能力。

## 系统架构

```text
Vue 2 + Element UI（8080）
            |
            | HTTP / JSON
            v
Spring Boot API（8101）---------> MySQL
      |          |
      |          +--------------> 兼容 OpenAI API 的 Qwen 服务
      |
      +-------------------------> Python 文档服务（8003）
                                      |
                                      +----> MinIO 图片存储
```

### 主要数据流

```text
上传文档
  -> Python 提取文本与内嵌图片
  -> 图片写入 MinIO
  -> Spring Boot 调用模型生成结构化题目
  -> 校验题目与答案并写入数据库

提交简答题
  -> Spring Boot 构建带约束的评判提示词
  -> Qwen 返回结构化 JSON
  -> API 校验并返回分数、等级和反馈
```

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 2、Vue Router、Vuex、Element UI、Axios |
| 后端 | Java 8、Spring Boot、MyBatis-Plus、Apache Shiro、JWT、Maven |
| 数据 | MySQL、Druid 连接池 |
| AI 集成 | 通过兼容 OpenAI Chat Completions 的接口调用 Qwen3-32B |
| 文档处理 | Python、FastAPI、python-docx、MinIO |
| 运维支持 | Swagger、Quartz、Spring Boot Actuator |

## 目录结构

```text
AIEXAMSYSTEM/
├── exam-api/                         # Spring Boot API 与业务逻辑
├── exam-vue/                         # Vue 2 管理端与考试界面
├── extract_questions_with_images.py  # DOCX 文本与图片提取服务
└── docs/                             # 数据库及部署参考资料
```

## 环境要求

- JDK 8、Maven 3.6+
- MySQL 5.7+
- Node.js 14+、npm
- Python 3.8+
- MinIO 或兼容的对象存储
- 兼容 OpenAI 接口格式的 Qwen 模型服务

仓库内配置包含开发环境专用值。运行或部署前，请替换数据库、对象存储、上传目录和模型服务配置，不要在生产环境复用示例凭据。

## 本地启动

### 1. 初始化 MySQL

创建数据库并导入仓库内的 SQL：

```bash
mysql -u <用户名> -p <数据库名> < "docs/安装资源/数据库脚本.sql"
```

随后修改数据源配置：

```text
exam-api/src/main/resources/application-dev.yml
```

### 2. 配置模型和文档服务

为 `AIProcessingService` 配置模型地址和模型名称，并在 `extract_questions_with_images.py` 中配置 MinIO 地址、访问凭据和 bucket。

生产部署时，应将这些值迁移到环境变量或外部密钥管理服务，不要继续写在源码中。

### 3. 启动 Python 文档服务

```bash
python -m venv .venv
source .venv/bin/activate
pip install fastapi uvicorn minio python-docx python-multipart
python extract_questions_with_images.py
```

服务默认监听 `http://localhost:8003`。

### 4. 启动 Spring Boot API

```bash
cd exam-api
mvn spring-boot:run
```

API 默认监听 `http://localhost:8101`。

### 5. 启动 Vue 前端

```bash
cd exam-vue
npm install
npm run dev
```

开发界面默认位于 `http://localhost:8080`。

## 关键 API

### 上传并拆分文档

```http
POST /exam/api/ai-upload/upload
Content-Type: multipart/form-data

file=<文档>
subject=<可选学科>
grade=<可选年级>
```

### 评判简答题

```http
POST /exam/api/judge/short-answer
Content-Type: application/json
```

```json
{
  "questionId": "question-001",
  "questionContent": "请说明操作系统调度器的作用。",
  "userAnswer": "它从就绪进程中选择一个进程使用 CPU。",
  "standardAnswer": "调度器根据调度策略从就绪队列中选择下一项运行任务。",
  "knowledgePoint": "CPU 调度",
  "maxScore": 10
}
```

模型调用成功后，接口会整理为类似以下结构：

```json
{
  "score": 8.5,
  "percentage": 85,
  "grade": "B",
  "comment": "核心概念正确，可以进一步说明调度策略如何影响选择。",
  "maxScore": 10
}
```

### 分析整份考试

```http
POST /exam/api/judge/overall-test
Content-Type: application/json
```

## 可靠性与边界

- 系统将模型结果按 JSON 解析；响应格式错误或服务不可用时，接口会返回失败信息。
- 文档提取包含校验和回退路径，但复杂排版、公式和少见图片格式仍可能需要人工修正。
- AI 选题调用或解析失败时会回退到非 AI 选题逻辑。
- 仓库目前没有足够的基准测试证明评分准确率、延迟、公平性或重复一致性；实际使用前应在带人工评分的代表性数据集上评估。
- 学生答案和考试数据应遵循部署环境中的隐私与保留政策。

## 上线前安全检查

- 轮换所有曾经出现在源码或 Git 历史中的凭据；
- 将数据库、MinIO 和模型服务凭据移出仓库；
- 修改所有默认账号和密码；
- 限制 CORS、上传文件类型、文件大小和管理端接口；
- 避免在日志中记录学生答案或含敏感数据的提示词；
- 在 CI 中加入依赖漏洞与密钥扫描。

## 开源协议

本项目遵循 [MIT License](LICENSE)。

