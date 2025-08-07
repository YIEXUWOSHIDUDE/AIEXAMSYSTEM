package com.yf.exam.config;


public class PromptConfig {
    public static final String EXTRACT_QUESTION_PROMPT =
            "请从以下文本中提取题目，并按照JSON格式返回。支持的题目类型：\n" +
                    "1=单选题，2=多选题，3=判断题，4=简答题，5=填空题\n" +
                    "\n" +
                    "严格按照以下JSON格式返回题目数组：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"quType\": 1,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"题目内容\",\n" +
                    "    \"image\": \"题目图片URL（如有）\",\n" +
                    "    \"analysis\": \"解析说明\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"选项A\", \"image\": \"选项图片URL（如有）\", \"isRight\": false, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项B\", \"image\": null, \"isRight\": true, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项C\", \"image\": null, \"isRight\": false, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项D\", \"image\": null, \"isRight\": false, \"analysis\": \"选项解析（可选）\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"quType\": 4,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"请简述面向对象编程的三大特性。\",\n" +
                    "    \"image\": null,\n" +
                    "    \"analysis\": \"面向对象编程的三大特性是封装、继承和多态\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"封装、继承、多态。封装是隐藏内部实现细节；继承是子类继承父类的属性和方法；多态是同一接口的不同实现。\", \"image\": null, \"isRight\": true, \"analysis\": \"标准答案\"}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"quType\": 5,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"填空题：Java中用于定义类的关键字是_____。\",\n" +
                    "    \"image\": null,\n" +
                    "    \"analysis\": \"class关键字用于定义类\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"class\", \"image\": null, \"isRight\": true, \"analysis\": \"正确答案\"}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]\n" +
                    "\n" +
                    "要求：\n" +
                    "- 自动识别题目类型并设置正确的quType值\n" +
                    "- 选择题要提取所有选项并标记正确答案\n" +
                    "- 填空题的options数组包含正确答案\n" +
                    "- 简答题必须在options中包含参考答案，isRight设为true\n" +
                    "- 必须包含有意义的analysis解析\n" +
                    "- 如果题目或选项包含图片，提取图片URL到image字段；没有图片时设为null\n" +
                    "- 支持图片+文字混合的选项内容\n" +
                    "- 每个选项可以包含独立的analysis解析说明\n" +
                    "- 【重要】图片标记如{{IMG_001}}、{{IMG_002}}是真实图片的占位符，绝对不能删除、修改或重新编号\n" +
                    "- 这些标记代表MinIO服务器上的真实图片，必须在题目content中100%原样保留\n" +
                    "- 即使标记看起来格式不规范，也绝对不要'修复'或'美化'这些标记\n" +
                    "- 标记中的数字序号(IMG_001, IMG_002)具有关键匹配意义，不得更改\n" +
                    "- 如果遇到其他图片引用（如\"图K-19-4\"、\"如图所示\"等），可以转换为描述性文字\n" +
                    "- 所有{{IMG_XXX}}格式的标记是系统生成的，必须完整保留到最终输出\n" +
                    "- 只返回JSON数组，不要其他内容";

    /**
     * 题目提取提示词（带知识点约束）
     */
    public static final String EXTRACT_QUESTION_WITH_CONSTRAINTS_PROMPT =
            "请从以下文本中提取题目，并按照JSON格式返回。支持的题目类型：\n" +
                    "1=单选题，2=多选题，3=判断题，4=简答题，5=填空题\n" +
                    "\n" +
                    "【严格约束】：\n" +
                    "1. 只能提取与指定知识点直接相关的题目\n" +
                    "2. 如果题目不涉及指定的知识点，必须跳过该题目\n" +
                    "3. knowledgePoint字段必须从提供的可选知识点中选择，不能自创知识点\n" +
                    "4. 每个题目的knowledgePoint必须是指定知识点中的一个\n" +
                    "5. 绝对不允许返回与指定知识点无关的题目\n" +
                    "\n" +
                    "严格按照以下JSON格式返回题目数组：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"quType\": 1,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"题目内容\",\n" +
                    "    \"image\": \"题目图片URL（如有）\",\n" +
                    "    \"analysis\": \"解析说明\",\n" +
                    "    \"knowledgePoint\": \"必须从指定知识点中选择一个\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"选项A\", \"image\": \"选项图片URL（如有）\", \"isRight\": false, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项B\", \"image\": null, \"isRight\": true, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项C\", \"image\": null, \"isRight\": false, \"analysis\": \"选项解析（可选）\"},\n" +
                    "      {\"content\": \"选项D\", \"image\": null, \"isRight\": false, \"analysis\": \"选项解析（可选）\"}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]\n" +
                    "\n" +
                    "要求：\n" +
                    "- 自动识别题目类型并设置正确的quType值\n" +
                    "- 选择题要提取所有选项并标记正确答案\n" +
                    "- knowledgePoint必须且只能从指定的知识点中选择\n" +
                    "- 如果题目与指定知识点无关，直接跳过不要提取\n" +
                    "- 必须包含有意义的analysis解析\n" +
                    "- 如果题目或选项包含图片，提取图片URL到image字段；没有图片时设为null\n" +
                    "- 支持图片+文字混合的选项内容\n" +
                    "- 每个选项可以包含独立的analysis解析说明\n" +
                    "- 【重要】图片标记如{{IMG_001}}、{{IMG_002}}是真实图片的占位符，绝对不能删除、修改或重新编号\n" +
                    "- 这些标记代表MinIO服务器上的真实图片，必须在题目content中100%原样保留\n" +
                    "- 即使标记看起来格式不规范，也绝对不要'修复'或'美化'这些标记\n" +
                    "- 标记中的数字序号(IMG_001, IMG_002)具有关键匹配意义，不得更改\n" +
                    "- 如果遇到其他图片引用（如\"图K-19-4\"、\"如图所示\"等），可以转换为描述性文字\n" +
                    "- 所有{{IMG_XXX}}格式的标记是系统生成的，必须完整保留到最终输出\n" +
                    "- 只返回JSON数组，不要其他内容\n" +
                    "- 宁可返回较少的高质量题目，也不要返回不相关的题目";

    /**
     * 智能文档结构分析提示词 - 检测分离式答案格式
     */
    public static final String DOCUMENT_STRUCTURE_ANALYSIS_PROMPT =
            "请分析以下文档的结构，判断题目和答案的组织方式。\n" +
                    "\n" +
                    "【分析要点】：\n" +
                    "1. 题目和答案是内联格式（答案紧跟在每个题目后面）还是分离格式（所有题目在前，答案统一在后）\n" +
                    "2. 如果是分离格式，识别题目区域和答案区域的分界点\n" +
                    "3. 识别编号格式：1. 2. 3. / (1) (2) (3) / A. B. C. / 第1题 第2题 等\n" +
                    "4. 检测答案区域的标识：答案、参考答案、Answer Key、答案解析等\n" +
                    "\n" +
                    "【返回格式】：\n" +
                    "{\n" +
                    "  \"documentType\": \"inline\" | \"separated\",\n" +
                    "  \"questionNumberingStyle\": \"1.\" | \"(1)\" | \"A.\" | \"第1题\" | \"Question 1\" | \"other\",\n" +
                    "  \"answerSectionStart\": \"答案区域开始的文本位置描述\",\n" +
                    "  \"answerSectionKeywords\": [\"答案\", \"参考答案\", \"Answer Key\"],\n" +
                    "  \"totalQuestions\": 估计的题目总数,\n" +
                    "  \"confidence\": 0.0-1.0的置信度\n" +
                    "}\n" +
                    "\n" +
                    "【重要】：只返回JSON对象，不要其他内容。";

    /**
     * 分离式答案提取提示词 - 处理题目和答案分开的文档
     */
    public static final String SEPARATED_ANSWER_EXTRACTION_PROMPT =
            "这是一个题目和答案分离的文档。请按照以下步骤提取：\n" +
                    "\n" +
                    "【第一步】：提取所有题目\n" +
                    "- 识别题目编号和内容\n" +
                    "- 保持题目的原始编号\n" +
                    "- 注意题目可能包含选项A、B、C、D\n" +
                    "\n" +
                    "【第二步】：提取对应答案\n" +
                    "- 根据编号匹配找到每个题目的答案\n" +
                    "- 答案可能是：选项字母（A、B、C）、具体内容、数值等\n" +
                    "- 如果是选择题答案（如\"1.B\"），需要找到对应的选项内容\n" +
                    "\n" +
                    "【第三步】：组合成完整题目\n" +
                    "严格按照以下JSON格式返回：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"originalNumber\": \"1\",\n" +
                    "    \"quType\": 1,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"题目内容\",\n" +
                    "    \"image\": \"题目图片URL（如有）\",\n" +
                    "    \"analysis\": \"解析说明\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"选项A\", \"image\": null, \"isRight\": false, \"analysis\": \"选项解析\"},\n" +
                    "      {\"content\": \"选项B\", \"image\": null, \"isRight\": true, \"analysis\": \"正确答案\"}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]\n" +
                    "\n" +
                    "【重要要求】：\n" +
                    "- 必须确保每个题目都有对应的答案\n" +
                    "- 如果答案是选项字母，要标记对应选项为isRight: true\n" +
                    "- 如果是填空题或简答题，答案放在options数组中，isRight设为true\n" +
                    "- 保持originalNumber字段记录原始编号\n" +
                    "- 如果某个题目找不到答案，在analysis中说明\n" +
                    "- 只返回JSON数组，不要其他内容";

    // ===========================================
    // DIFFICULTY RATIO CONFIGURATION - ABSOLUTELY ENFORCED
    // ===========================================
    
    /**
     * 难度分布比例配置 - 系统强制执行
     * 支持4个难度等级和多种分布方案
     */
    public static final class DifficultyRatio {
        
        // 难度等级映射
        public static final int EASY_LEVEL = 1;      // 简单（1档）
        public static final int MEDIUM_LEVEL = 2;    // 普通（2档）
        public static final int HARD_LEVEL = 3;      // 难题（3档）
        public static final int SUPER_HARD_LEVEL = 4; // 超难（4档）
        
        // 难度分布方案枚举
        public enum DifficultyScheme {
            BALANCED("均衡型", 0.25, 0.35, 0.25, 0.15, "🌟 适合日常考察，难度适中，整体覆盖面广"),
            ADVANCED("进阶挑战型", 0.15, 0.30, 0.35, 0.20, "🚀 适合稍具挑战性的考试，突出学生能力差异"),
            COMPETITION("高难度竞赛型", 0.10, 0.20, 0.35, 0.35, "🔥 适合选拔或竞赛类考试，突出难题和超难题的区分度"),
            FOUNDATION("基础巩固型", 0.40, 0.40, 0.15, 0.05, "✨ 适合阶段性复习或基础考核，以巩固学生基础知识为主");
            
            private final String name;
            private final double easyRatio;
            private final double mediumRatio;
            private final double hardRatio;
            private final double superHardRatio;
            private final String description;
            
            DifficultyScheme(String name, double easyRatio, double mediumRatio, double hardRatio, double superHardRatio, String description) {
                this.name = name;
                this.easyRatio = easyRatio;
                this.mediumRatio = mediumRatio;
                this.hardRatio = hardRatio;
                this.superHardRatio = superHardRatio;
                this.description = description;
            }
            
            public String getName() { return name; }
            public double getEasyRatio() { return easyRatio; }
            public double getMediumRatio() { return mediumRatio; }
            public double getHardRatio() { return hardRatio; }
            public double getSuperHardRatio() { return superHardRatio; }
            public String getDescription() { return description; }
        }
        
        // 默认方案
        public static final DifficultyScheme DEFAULT_SCHEME = DifficultyScheme.BALANCED;
        
        /**
         * 根据指定方案计算各难度等级所需题目数量
         */
        public static int[] calculateQuestionCounts(int totalQuestions, DifficultyScheme scheme) {
            int easyCount = (int) Math.round(totalQuestions * scheme.getEasyRatio());
            int mediumCount = (int) Math.round(totalQuestions * scheme.getMediumRatio());
            int hardCount = (int) Math.round(totalQuestions * scheme.getHardRatio());
            int superHardCount = totalQuestions - easyCount - mediumCount - hardCount; // 确保总数正确
            
            return new int[]{easyCount, mediumCount, hardCount, superHardCount};
        }
        
        /**
         * 使用默认方案计算题目数量（向后兼容）
         */
        public static int[] calculateQuestionCounts(int totalQuestions) {
            return calculateQuestionCounts(totalQuestions, DEFAULT_SCHEME);
        }
        
        /**
         * 获取难度分布描述文本 - 用于AI提示
         */
        public static String getRatioDescription(DifficultyScheme scheme) {
            return String.format("严格按照以下难度比例选题：简单题(level=1) %d%%, 普通题(level=2) %d%%, 难题(level=3) %d%%, 超难题(level=4) %d%%",
                (int)(scheme.getEasyRatio() * 100), (int)(scheme.getMediumRatio() * 100), 
                (int)(scheme.getHardRatio() * 100), (int)(scheme.getSuperHardRatio() * 100));
        }
        
        /**
         * 使用默认方案获取描述（向后兼容）
         */
        public static String getRatioDescription() {
            return getRatioDescription(DEFAULT_SCHEME);
        }
    }

    // 更新问题选择提示，强制包含难度比例要求
    public static final String DIFFICULTY_ENFORCED_SELECTION_PROMPT =
            "你是专业的智能组卷助手。必须严格按照指定的难度分布比例和知识点约束选择题目。\n\n" +
            "【强制难度比例】：\n" +
            DifficultyRatio.getRatioDescription() + "\n" +
            "此比例绝对不可违反，必须精确执行！\n\n" +
            "【知识点约束】：\n" +
            "1. 如果指定了知识点，只能选择包含这些知识点的题目\n" +
            "2. 绝不允许选择包含其他知识点的题目\n" +
            "3. 每个选中的题目，其所有知识点都必须在指定的知识点范围内\n" +
            "4. 知识点过滤优先级高于其他所有选择标准\n" +
            "5. 宁可返回较少的符合要求的题目，也不要违反知识点约束\n\n" +
            "选题策略：\n" +
            "1. 首先严格按知识点过滤题目（如果有指定）\n" +
            "2. 然后按难度分组过滤后的题目\n" +
            "3. 在每个难度级别内选择最优题目\n" +
            "4. 避免选择相似或重复的题目\n" +
            "5. 优先选择表述清晰、答案明确的高质量题目\n\n" +
            "要求：严格按照难度比例和知识点约束返回题目ID，用英文逗号分隔\n" +
            "示例：1234567890,2345678901,3456789012";

    // ===========================================
    // KNOWLEDGE OUTLINE SYSTEM PROMPTS
    // ===========================================
    
    /**
     * 基于知识大纲的知识点识别提示
     * 要求LLM从预定义的知识大纲中识别最匹配的知识点
     */
    public static final String OUTLINE_BASED_KNOWLEDGE_IDENTIFICATION_PROMPT =
            "你是专业的教育内容分析师。请根据预定义的知识大纲识别题目对应的知识点。\n\n" +
            "【重要规则】：\n" +
            "1. 只能从提供的知识大纲列表中选择知识点\n" +
            "2. 如果题目不匹配任何大纲知识点，返回null\n" +
            "3. 每个题目只能匹配1个最核心的知识点\n" +
            "4. 优先选择最具体、最精确的知识点\n" +
            "5. 必须考虑学科和年级的匹配度\n\n" +
            "【返回格式】：\n" +
            "{\n" +
            "  \"outlineId\": \"MATH_G7_002\",\n" +
            "  \"knowledgePoint\": \"有理数加法\",\n" +
            "  \"confidence\": 0.95,\n" +
            "  \"reason\": \"题目明确考查有理数加法运算规则\"\n" +
            "}\n\n" +
            "如果不匹配任何知识点，返回：\n" +
            "{\n" +
            "  \"outlineId\": null,\n" +
            "  \"knowledgePoint\": null,\n" +
            "  \"confidence\": 0.0,\n" +
            "  \"reason\": \"题目内容不在预定义知识大纲范围内\"\n" +
            "}\n\n" +
            "要求：严格按照JSON格式返回，不要其他内容";


    /**
     * 大纲识别AI提示词
     * 用于识别题目对应的知识大纲
     */
    public static final String OUTLINE_IDENTIFICATION_PROMPT =
            "你是专业的教育内容分析师。请分析题目并识别对应的知识大纲。\n\n" +
            "分析要求：\n" +
            "1. 仔细分析题目考查的核心知识点\n" +
            "2. 如果能明确识别知识点，返回具体信息\n" +
            "3. 如果无法确定，返回null\n\n" +
            "返回JSON格式：\n" +
            "{\n" +
            "  \"outlineId\": \"MATH_001\",\n" +
            "  \"knowledgePoint\": \"有理数加法\",\n" +
            "  \"confidence\": 0.90,\n" +
            "  \"reason\": \"题目明确考查有理数加法运算\"\n" +
            "}\n\n" +
            "如果无法识别，返回：\n" +
            "{\n" +
            "  \"outlineId\": null,\n" +
            "  \"knowledgePoint\": null,\n" +
            "  \"confidence\": 0.0,\n" +
            "  \"reason\": \"无法确定具体知识点\"\n" +
            "}\n\n" +
            "要求：只返回JSON，不要其他内容。";

    // ===========================================
    // ADDITIONAL AI PROCESSING PROMPTS
    // ===========================================

    /**
     * 题干提取提示词
     */
    public static final String STEM_EXTRACTION_PROMPT = 
        "你是一个专业的题目题干提取器。请从给定的题目中提取最核心、最简洁的题干部分。\n\n" +
        "【核心要求】：\n" +
        "- 必须大幅缩短原题目长度\n" +
        "- 只保留题目的本质问题\n" +
        "- 彻底删除所有叙述性、描述性内容\n" +
        "- 删除不完整的句子或短语\n" +
        "- 绝对不要包含任何提取标记词汇\n\n" +
        "【严格禁止词汇】：\n" +
        "- 已提取、提取、摘要、总结、简化\n" +
        "- 题干、核心、本质、关键\n" +
        "- 如下、以下、上述、该题\n\n" +
        "【删除规则】：\n" +
        "1. 删除人物名称（小明、老师、同学等）\n" +
        "2. 删除场景描述（在课堂上、昨天等）\n" +
        "3. 删除引导语句（有一个问题、请思考等）\n" +
        "4. 删除不完整的尾句（如：\"则...\"、\"那么...\"等开头但没有完整表达的句子）\n" +
        "5. 删除冗余的修饰词和连接词\n" +
        "6. 删除任何提取过程的标记性词汇\n\n" +
        "【保留规则】：\n" +
        "1. 保留核心物理条件和数值关系\n" +
        "2. 保留完整的问题表述\n" +
        "3. 保留选项（如果有）\n\n" +
        "【重要】：你必须严格按照要求处理原文，不能直接返回原文不变！必须进行大幅度简化！\n" +
        "【输出格式】：直接返回提取后的简化题干，不要任何前缀、后缀、解释或标记性词汇！";

    /**
     * 知识点识别提示词 - 简化版，返回单一知识点名称
     */
    public static final String KNOWLEDGE_POINT_PROMPT = 
        "你是专业的教育内容分析师。请识别以下题目涉及的最核心知识点。\n\n" +
        "【识别要求】：\n" +
        "1. 准确识别题目考查的最核心知识点\n" +
        "2. 只能标注1个最重要的知识点\n" +
        "3. 使用标准化的知识点名称\n" +
        "4. 选择最具代表性和核心的知识点\n\n" +
        "【知识点类别参考】：\n" +
        "- 数学：代数运算、几何图形、函数关系、概率统计、二次函数、一元一次方程等\n" +
        "- 物理：力学、电学、光学、热学、牛顿定律、电路分析等\n" +
        "- 化学：原子结构、化学反应、有机化学、酸碱反应等\n" +
        "- 语文：阅读理解、语法知识、文学鉴赏、现代文阅读等\n\n" +
        "【重要】：你必须严格按照要求分析题目，不能直接返回原题目内容！\n" +
        "【输出格式】：直接返回知识点名称，不要任何前缀、后缀、解释或标点符号！\n" +
        "【示例】：如果题目考查二次函数，则返回：二次函数\n" +
        "要求：只返回知识点名称，不要其他任何内容。";

    /**
     * 简答题判分提示词
     */
    public static final String SHORT_ANSWER_JUDGE_PROMPT = 
        "你是专业的智能判题助手。请对学生的简答题答案进行评分和点评。\n\n" +
        "判分标准：\n" +
        "1. 准确性（60%）：答案内容是否正确、完整\n" +
        "2. 完整性（25%）：是否涵盖了所有关键知识点\n" +
        "3. 表达清晰度（15%）：语言表达是否清楚、逻辑是否清晰\n\n" +
        "请严格按照以下JSON格式返回结果：\n" +
        "{\n" +
        "  \"score\": 8.5,\n" +
        "  \"feedback\": \"答案基本正确，涵盖了主要知识点，但在某些细节上还需完善...\",\n" +
        "  \"keyPoints\": [\"要点1\", \"要点2\", \"要点3\"],\n" +
        "  \"suggestions\": [\"建议1\", \"建议2\"]\n" +
        "}\n\n" +
        "要求：只返回JSON，不要其他内容。";

    /**
     * 整体测试判分提示词
     */
    public static final String OVERALL_TEST_JUDGE_PROMPT = 
        "你是专业的教育评估专家。请对整个测试进行综合评价和建议。\n\n" +
        "评估维度：\n" +
        "1. 整体表现：学生在各题型上的表现情况\n" +
        "2. 知识掌握：对相关知识点的掌握程度\n" +
        "3. 能力分析：解题能力和思维逻辑\n" +
        "4. 改进建议：针对性的学习提升建议\n\n" +
        "请按照以下JSON格式返回结果：\n" +
        "{\n" +
        "  \"overallScore\": 85.5,\n" +
        "  \"overallFeedback\": \"整体表现良好，基础知识扎实...\",\n" +
        "  \"strengthAreas\": [\"优势领域1\", \"优势领域2\"],\n" +
        "  \"weaknessAreas\": [\"薄弱环节1\", \"薄弱环节2\"],\n" +
        "  \"studyRecommendations\": [\"学习建议1\", \"学习建议2\", \"学习建议3\"]\n" +
        "}\n\n" +
        "要求：只返回JSON，不要其他内容。";

    /**
     * 结构化文档题目提取提示词 - 用于处理DoclingDocument格式
     */
    public static final String STRUCTURED_EXTRACTION_PROMPT =
        "你是专业的结构化题目提取专家。你将接收一个DoclingDocument格式的JSON，其中包含结构化的内容块(content_blocks)和图片信息(images)。\n\n" +
        "【重要说明】：\n" +
        "- 输入是预处理过的结构化文档，不是原始文本\n" +
        "- content_blocks已经识别了题目、选项、文本等类型\n" +
        "- images包含真实的MinIO服务器图片URL\n" +
        "- 你的任务是将这些结构化信息转换为标准题目JSON\n\n" +
        "【块类型说明】：\n" +
        "- question: 真实题目，需要提取\n" +
        "- option: 选项，通过parent_block关联到题目\n" +
        "- section_title: 章节标题，忽略不提取\n" +
        "- answer: 答案解析，可选提取\n" +
        "- sub_question: 子问题，合并到父题目\n" +
        "- text: 普通文本，通常忽略\n\n" +
        "【关键规则】：\n" +
        "1. **保持结构关联性**：\n" +
        "   - 每个content_block都有唯一的id，必须在输出中保留这种关联\n" +
        "   - 如果block有image_refs，这些是真实的图片引用，必须100%保留\n" +
        "   - 不要修改、删除或重新编号任何image_refs\n\n" +
        "2. **图片处理原则**：\n" +
        "   - image_refs中的值(如\"img_001\")对应images字典中的真实图片\n" +
        "   - 直接使用image_refs，不要尝试匹配或猜测图片归属\n" +
        "   - 保持图片ID的原始格式和大小写\n\n" +
        "3. **题目类型识别**：\n" +
        "   - type=\"question\"的block是题目主体\n" +
        "   - type=\"option\"的block是选项，通过parent_block关联到题目\n" +
        "   - 根据content_blocks的结构确定题目类型\n\n" +
        "4. **输出格式要求**：\n" +
        "   - 返回标准的题目JSON数组\n" +
        "   - 每个题目包含block_id字段，值为对应的content_block.id\n" +
        "   - 图片URL从images字典中根据image_refs获取\n" +
        "   - 选项的parent_block关系必须正确反映\n\n" +
        "【处理步骤】：\n" +
        "1. 解析输入的DoclingDocument JSON\n" +
        "2. 按sequence排序处理content_blocks\n" +
        "3. 识别question类型的块作为题目\n" +
        "4. 找到该题目的所有option子块\n" +
        "5. 根据image_refs从images字典获取图片URL\n" +
        "6. 构造标准题目JSON格式\n" +
        "7. 确保保留所有block_id和图片关联\n\n" +
        "【重要】：\n" +
        "- 绝对不要丢失或修改image_refs\n" +
        "- 必须保留block_id用于后续追踪\n" +
        "- 图片URL直接从images字典获取，不需要推测\n" +
        "- 只返回JSON数组，不要其他内容";

    /**
     * 知识大纲结构提取提示词
     */
    public static final String OUTLINE_STRUCTURE_EXTRACTION_PROMPT =
        "你是专业的教育内容分析师。请从文档中提取知识大纲结构，按照JSON格式返回。\n\n" +
        "提取要求：\n" +
        "1. 识别文档中的知识点层次结构\n" +
        "2. 提取每个知识点的名称和描述\n" +
        "3. 如果有示例或练习，一并提取\n\n" +
        "返回JSON格式：\n" +
        "{\n" +
        "  \"outlines\": [\n" +
        "    {\n" +
        "      \"knowledgePoint\": \"知识点名称\",\n" +
        "      \"description\": \"知识点描述\",\n" +
        "      \"sortOrder\": 1\n" +
        "    }\n" +
        "  ]\n" +
        "}\n\n" +
        "要求：只返回JSON，不要其他内容。";
}
