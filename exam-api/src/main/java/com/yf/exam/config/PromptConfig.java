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
                    "    \"analysis\": \"解析说明\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"选项A\", \"isRight\": false},\n" +
                    "      {\"content\": \"选项B\", \"isRight\": true},\n" +
                    "      {\"content\": \"选项C\", \"isRight\": false},\n" +
                    "      {\"content\": \"选项D\", \"isRight\": false}\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"quType\": 5,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"填空题：Java中用于定义类的关键字是_____。\",\n" +
                    "    \"analysis\": \"class关键字用于定义类\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"class\", \"isRight\": true}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]\n" +
                    "\n" +
                    "要求：\n" +
                    "- 自动识别题目类型并设置正确的quType值\n" +
                    "- 选择题要提取所有选项并标记正确答案\n" +
                    "- 填空题的options数组包含正确答案\n" +
                    "- 简答题可以不设置options或设置为空数组\n" +
                    "- 必须包含有意义的analysis解析\n" +
                    "- 只返回JSON数组，不要其他内容";

    /**
     * 题目提取提示词（带知识点约束）
     */
    public static final String EXTRACT_QUESTION_WITH_CONSTRAINTS_PROMPT =
            "请从以下文本中提取题目，并按照JSON格式返回。支持的题目类型：\n" +
                    "1=单选题，2=多选题，3=判断题，4=简答题，5=填空题\n" +
                    "\n" +
                    "【重要约束】：knowledgePoint字段必须从提供的可选知识点中选择，不能自创知识点！\n" +
                    "\n" +
                    "严格按照以下JSON格式返回题目数组：\n" +
                    "[\n" +
                    "  {\n" +
                    "    \"quType\": 1,\n" +
                    "    \"level\": 1,\n" +
                    "    \"content\": \"题目内容\",\n" +
                    "    \"analysis\": \"解析说明\",\n" +
                    "    \"knowledgePoint\": \"从可选知识点中选择最匹配的一个\",\n" +
                    "    \"options\": [\n" +
                    "      {\"content\": \"选项A\", \"isRight\": false},\n" +
                    "      {\"content\": \"选项B\", \"isRight\": true},\n" +
                    "      {\"content\": \"选项C\", \"isRight\": false},\n" +
                    "      {\"content\": \"选项D\", \"isRight\": false}\n" +
                    "    ]\n" +
                    "  }\n" +
                    "]\n" +
                    "\n" +
                    "要求：\n" +
                    "- 自动识别题目类型并设置正确的quType值\n" +
                    "- 选择题要提取所有选项并标记正确答案\n" +
                    "- knowledgePoint必须从可选知识点中选择\n" +
                    "- 必须包含有意义的analysis解析\n" +
                    "- 只返回JSON数组，不要其他内容";



    // ===========================================
    // DIFFICULTY RATIO CONFIGURATION - ABSOLUTELY ENFORCED
    // ===========================================
    
    /**
     * 难度分布比例配置 - 系统强制执行
     * 所有智能组卷必须严格按照此比例分配题目难度
     */
    public static final class DifficultyRatio {
        public static final double EASY_RATIO = 0.50;      // 简单题 50%
        public static final double MEDIUM_RATIO = 0.30;    // 中等题 30%  
        public static final double HARD_RATIO = 0.20;      // 困难题 20%
        
        // 难度等级映射
        public static final int EASY_LEVEL = 1;
        public static final int MEDIUM_LEVEL = 2;
        public static final int HARD_LEVEL = 3;
        
        /**
         * 计算各难度等级所需题目数量
         */
        public static int[] calculateQuestionCounts(int totalQuestions) {
            int easyCount = (int) Math.round(totalQuestions * EASY_RATIO);
            int mediumCount = (int) Math.round(totalQuestions * MEDIUM_RATIO);
            int hardCount = totalQuestions - easyCount - mediumCount; // 确保总数正确
            
            return new int[]{easyCount, mediumCount, hardCount};
        }
        
        /**
         * 获取难度分布描述文本 - 用于AI提示
         */
        public static String getRatioDescription() {
            return String.format("严格按照以下难度比例选题：简单题(level=1) %d%%, 中等题(level=2) %d%%, 困难题(level=3) %d%%",
                (int)(EASY_RATIO * 100), (int)(MEDIUM_RATIO * 100), (int)(HARD_RATIO * 100));
        }
    }

    // 更新问题选择提示，强制包含难度比例要求
    public static final String DIFFICULTY_ENFORCED_SELECTION_PROMPT =
            "你是专业的智能组卷助手。必须严格按照指定的难度分布比例选择题目。\n\n" +
            "【强制难度比例】：\n" +
            DifficultyRatio.getRatioDescription() + "\n" +
            "此比例绝对不可违反，必须精确执行！\n\n" +
            "选题策略：\n" +
            "1. 首先按难度分组题目\n" +
            "2. 在每个难度级别内选择最优题目\n" +
            "3. 确保知识点覆盖全面\n" +
            "4. 避免选择相似或重复的题目\n" +
            "5. 优先选择表述清晰、答案明确的高质量题目\n\n" +
            "要求：严格按照难度比例返回题目ID，用英文逗号分隔\n" +
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
