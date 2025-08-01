package com.yf.exam.ability.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.ability.ai.dto.LightweightQuestionDTO;
import com.yf.exam.config.PromptConfig;
import com.yf.exam.modules.ai.service.AIProcessingService;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.enums.QuType;
import com.yf.exam.modules.qu.service.QuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI智能组卷服务
 * 使用现有LLM模块进行智能题目选择
 * 
 * @author AI Assistant
 * @date 2025-07-24
 */
@Service
public class AIExamGenerationService {

    @Autowired
    private AIProcessingService aiProcessingService;
    
    @Autowired
    private QuService quService;

    /**
     * AI智能选择题目 - 优化版本（仅使用知识点和题干）
     * 支持所有5种题型：单选题、多选题、判断题、简答题、填空题
     */
    public List<Qu> intelligentQuestionSelection(String repoId, Integer quType, 
                                                List<String> excludes, Integer size) {
        return intelligentQuestionSelectionWithKnowledgePoints(repoId, quType, excludes, size, null);
    }

    /**
     * AI智能选择题目 - 支持知识点筛选 + 强制难度比例分配
     * @param repoId 题库ID
     * @param quType 题目类型
     * @param excludes 排除的题目ID列表
     * @param size 需要选择的题目数量
     * @param selectedKnowledgePoints 选定的知识点列表（可为null）
     * @return 选中的题目列表
     */
    public List<Qu> intelligentQuestionSelectionWithKnowledgePoints(String repoId, Integer quType, 
                                                                   List<String> excludes, Integer size,
                                                                   List<String> selectedKnowledgePoints) {
        return intelligentQuestionSelectionWithDifficultyRatio(repoId, quType, excludes, size, selectedKnowledgePoints, true);
    }

    /**
     * AI智能选择题目 - 核心方法，支持知识点筛选 + 可选难度比例强制执行
     * @param repoId 题库ID
     * @param quType 题目类型
     * @param excludes 排除的题目ID列表
     * @param size 需要选择的题目数量
     * @param selectedKnowledgePoints 选定的知识点列表（可为null）
     * @param enforceDifficultyRatio 是否强制执行难度比例
     * @return 选中的题目列表
     */
    public List<Qu> intelligentQuestionSelectionWithDifficultyRatio(String repoId, Integer quType, 
                                                                   List<String> excludes, Integer size,
                                                                   List<String> selectedKnowledgePoints,
                                                                   boolean enforceDifficultyRatio) {
        try {
            // 1. 根据是否有知识点筛选条件选择不同的查询方法
            List<Qu> allQuestions;
            if (CollectionUtils.isEmpty(selectedKnowledgePoints)) {
                allQuestions = quService.listByType(repoId, quType, excludes);
            } else {
                allQuestions = quService.listByTypeAndKnowledgePoints(repoId, quType, excludes, selectedKnowledgePoints);
                System.out.println("根据知识点筛选后找到 " + allQuestions.size() + " 道题目: " + selectedKnowledgePoints);
            }
            
            if (CollectionUtils.isEmpty(allQuestions)) {
                System.out.println("未找到符合条件的题目");
                return new ArrayList<>();
            }
            
            // 如果题目数量不足，直接返回所有题目
            if (allQuestions.size() <= size) {
                System.out.println("题目数量不足，返回所有 " + allQuestions.size() + " 道题目");
                return allQuestions;
            }
            
            // 2. 难度比例强制执行逻辑
            if (enforceDifficultyRatio) {
                System.out.println("🎯 执行强制难度比例分配 - 总题数: " + size);
                return selectQuestionsWithDifficultyRatio(allQuestions, size, quType, selectedKnowledgePoints);
            }
            
            // 3. 不强制难度比例时，使用轻量级AI选择（向后兼容）
            try {
                return lightweightIntelligentSelection(allQuestions, size, quType, selectedKnowledgePoints);
            } catch (Exception lightweightError) {
                System.err.println("轻量级AI选题失败，尝试传统方法: " + lightweightError.getMessage());
                
                // 4. 回退到传统AI选择
                String questionList = buildQuestionSelectionPrompt(allQuestions, size, quType);
                String selectedIds = callLLMService(questionList, size, selectedKnowledgePoints);
                return parseAndReturnQuestions(selectedIds, allQuestions);
            }
            
        } catch (Exception e) {
            System.err.println("AI智能选题失败，回退到随机选择: " + e.getMessage());
            // 发生异常时回退到随机选择
            return quService.listByRandom(repoId, quType, excludes, size);
        }
    }

    /**
     * 🎯 按难度比例强制分配题目 - 核心实现方法
     * 严格按照 PromptConfig.DifficultyRatio 中定义的比例分配题目
     * @param allQuestions 所有可选题目
     * @param totalSize 总题目数量
     * @param quType 题目类型
     * @return 按难度比例分配的题目列表
     */
    private List<Qu> selectQuestionsWithDifficultyRatio(List<Qu> allQuestions, Integer totalSize, Integer quType, List<String> selectedKnowledgePoints) {
        System.out.println("🎯 开始按难度比例强制分配题目");
        
        // 1. 计算各难度级别需要的题目数量
        int[] difficultyCount = PromptConfig.DifficultyRatio.calculateQuestionCounts(totalSize);
        int easyCount = difficultyCount[0];
        int mediumCount = difficultyCount[1]; 
        int hardCount = difficultyCount[2];
        
        System.out.println("📊 难度分配计划: 简单题(" + easyCount + ") + 中等题(" + mediumCount + ") + 困难题(" + hardCount + ") = " + totalSize);
        
        // 2. 按难度分组题目
        Map<Integer, List<Qu>> questionsByDifficulty = allQuestions.stream()
            .collect(Collectors.groupingBy(Qu::getLevel));
            
        // 确保所有难度级别都有对应的列表
        questionsByDifficulty.putIfAbsent(PromptConfig.DifficultyRatio.EASY_LEVEL, new ArrayList<>());
        questionsByDifficulty.putIfAbsent(PromptConfig.DifficultyRatio.MEDIUM_LEVEL, new ArrayList<>());
        questionsByDifficulty.putIfAbsent(PromptConfig.DifficultyRatio.HARD_LEVEL, new ArrayList<>());
        
        System.out.println("📈 题目难度分布统计:");
        questionsByDifficulty.forEach((level, questions) -> 
            System.out.println("  Level " + level + ": " + questions.size() + " 道题目"));
        
        // 3. 分别从各难度级别选择题目
        List<Qu> selectedQuestions = new ArrayList<>();
        
        try {
            // 选择简单题
            List<Qu> selectedEasy = selectQuestionsFromDifficultyGroup(
                questionsByDifficulty.get(PromptConfig.DifficultyRatio.EASY_LEVEL), 
                easyCount, "简单题", quType, selectedKnowledgePoints);
            selectedQuestions.addAll(selectedEasy);
            
            // 选择中等题
            List<Qu> selectedMedium = selectQuestionsFromDifficultyGroup(
                questionsByDifficulty.get(PromptConfig.DifficultyRatio.MEDIUM_LEVEL), 
                mediumCount, "中等题", quType, selectedKnowledgePoints);
            selectedQuestions.addAll(selectedMedium);
            
            // 选择困难题
            List<Qu> selectedHard = selectQuestionsFromDifficultyGroup(
                questionsByDifficulty.get(PromptConfig.DifficultyRatio.HARD_LEVEL), 
                hardCount, "困难题", quType, selectedKnowledgePoints);
            selectedQuestions.addAll(selectedHard);
            
            System.out.println("✅ 难度比例分配完成！实际选择: " + selectedQuestions.size() + " 道题目");
            System.out.println("📊 最终分布: 简单(" + selectedEasy.size() + ") + 中等(" + selectedMedium.size() + ") + 困难(" + selectedHard.size() + ")");
            
            return selectedQuestions;
            
        } catch (Exception e) {
            System.err.println("❌ 难度比例分配失败: " + e.getMessage());
            // 回退到轻量级AI选择
            return lightweightIntelligentSelection(allQuestions, totalSize, quType, selectedKnowledgePoints);
        }
    }
    
    /**
     * 从特定难度组中选择指定数量的题目
     * @param questionsInGroup 该难度组的所有题目
     * @param requiredCount 需要选择的数量
     * @param difficultyName 难度名称（用于日志）
     * @param quType 题目类型
     * @return 选中的题目列表
     */
    private List<Qu> selectQuestionsFromDifficultyGroup(List<Qu> questionsInGroup, int requiredCount, 
                                                       String difficultyName, Integer quType, List<String> selectedKnowledgePoints) {
        if (requiredCount <= 0) {
            System.out.println("⏭️ " + difficultyName + " 需要数量为0，跳过");
            return new ArrayList<>();
        }
        
        if (CollectionUtils.isEmpty(questionsInGroup)) {
            System.out.println("⚠️ " + difficultyName + " 题库为空，无法选择");
            return new ArrayList<>();
        }
        
        // 如果题目数量不足，返回所有可用题目
        if (questionsInGroup.size() <= requiredCount) {
            System.out.println("⚠️ " + difficultyName + " 题目不足，返回所有 " + questionsInGroup.size() + " 道");
            return new ArrayList<>(questionsInGroup);
        }
        
        try {
            // 使用AI从该难度组中选择最优题目
            System.out.println("🤖 使用AI从 " + questionsInGroup.size() + " 道" + difficultyName + "中选择 " + requiredCount + " 道");
            return lightweightIntelligentSelection(questionsInGroup, requiredCount, quType, selectedKnowledgePoints);
            
        } catch (Exception e) {
            System.err.println("❌ AI选择" + difficultyName + "失败，使用随机选择: " + e.getMessage());
            // 回退到随机选择
            Collections.shuffle(questionsInGroup);
            return questionsInGroup.subList(0, requiredCount);
        }
    }

    /**
     * 轻量级AI智能选题方法
     * 只发送题目ID、题干和知识点给LLM，节省token和时间
     */
    public List<Qu> lightweightIntelligentSelection(List<Qu> allQuestions, Integer size, Integer quType, List<String> selectedKnowledgePoints) {
        try {
            // 1. 构建轻量级题目信息
            String lightweightQuestions = buildLightweightQuestionList(allQuestions, size, quType);
            
            // 2. 调用轻量级LLM服务
            String selectedIds = callLightweightLLMService(lightweightQuestions, size, selectedKnowledgePoints);
            
            // 3. 根据ID获取完整题目信息
            return parseAndReturnQuestions(selectedIds, allQuestions);
            
        } catch (Exception e) {
            throw new RuntimeException("轻量级AI选题失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建轻量级题目列表（仅包含ID、题干、知识点）
     * 大幅减少LLM输入token数量，提升处理效率
     */
    private String buildLightweightQuestionList(List<Qu> questions, Integer size, Integer quType) {
        StringBuilder lightweightList = new StringBuilder();
        
        String typeName = getQuestionTypeName(quType);
        lightweightList.append("题型：").append(typeName).append("\n");
        lightweightList.append("从以下").append(questions.size()).append("道题目中选择").append(size).append("道最优题目：\n\n");
        
        // 构建轻量级题目列表
        for (Qu qu : questions) {
            lightweightList.append("ID: ").append(qu.getId()).append("\n");
            lightweightList.append("题干: ").append(getSafeQuestionStem(qu)).append("\n");
            lightweightList.append("知识点: ").append(getSafeKnowledgePoints(qu)).append("\n");
            lightweightList.append("难度: ").append(qu.getLevel() != null ? qu.getLevel() : 1).append("\n");
            lightweightList.append("---\n");
        }
        
        return lightweightList.toString();
    }

    /**
     * 构建AI选题提示词和题目列表
     * 使用PromptConfig中的标准提示词
     */
    private String buildQuestionSelectionPrompt(List<Qu> questions, Integer size, Integer quType) {
        StringBuilder questionList = new StringBuilder();
        
        String typeName = getQuestionTypeName(quType);
        questionList.append("题型：").append(typeName).append("\n");
        questionList.append("从以下").append(questions.size()).append("道题目中选择").append(size).append("道：\n\n");
        
        // 构建题目列表
        for (int i = 0; i < questions.size(); i++) {
            Qu qu = questions.get(i);
            questionList.append(String.format("%d. [ID:%s] %s\n", 
                i + 1, qu.getId(), truncateContent(qu.getContent(), 100)));
        }
        
        return questionList.toString();
    }

    /**
     * 调用轻量级LLM服务进行智能选择
     * 使用新的selectlightweight端点，只发送关键信息
     */
    private String callLightweightLLMService(String lightweightQuestions, Integer targetCount, List<String> selectedKnowledgePoints) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("selectionPrompt", buildSelectionPromptWithKnowledgePoints(selectedKnowledgePoints));
            requestBody.put("lightweightQuestions", lightweightQuestions);
            requestBody.put("targetCount", targetCount);
            requestBody.put("selectedKnowledgePoints", selectedKnowledgePoints);
            
            String result = aiProcessingService.selectLightweightQuestions(requestBody);
            
            if (result != null) {
                System.out.println("轻量级AI选题成功");
                return result;
            } else {
                throw new RuntimeException("轻量级AI服务调用失败");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("调用轻量级LLM服务异常: " + e.getMessage(), e);
        }
    }

    /**
     * 调用LLM服务进行智能选择
     * 使用专门的selectquestions端点
     */
    private String callLLMService(String questionList, Integer targetCount, List<String> selectedKnowledgePoints) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("selectionPrompt", buildSelectionPromptWithKnowledgePoints(selectedKnowledgePoints));
            requestBody.put("questionList", questionList);
            requestBody.put("targetCount", targetCount);
            requestBody.put("selectedKnowledgePoints", selectedKnowledgePoints);
            
            String result = aiProcessingService.selectQuestions(requestBody);
            
            if (result != null) {
                return result;
            } else {
                throw new RuntimeException("AI服务调用失败");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("调用LLM服务异常: " + e.getMessage(), e);
        }
    }

    /**
     * 构建带知识点约束的选择提示词
     */
    private String buildSelectionPromptWithKnowledgePoints(List<String> selectedKnowledgePoints) {
        String basePrompt = PromptConfig.DIFFICULTY_ENFORCED_SELECTION_PROMPT;
        
        if (CollectionUtils.isEmpty(selectedKnowledgePoints)) {
            return basePrompt;
        }
        
        // 添加知识点约束信息
        StringBuilder promptWithKnowledge = new StringBuilder(basePrompt);
        promptWithKnowledge.append("\n\n【当前选定的知识点】：\n");
        for (String point : selectedKnowledgePoints) {
            promptWithKnowledge.append("- ").append(point).append("\n");
        }
        promptWithKnowledge.append("\n【重要提醒】：只能选择包含上述知识点的题目，绝不允许选择包含其他知识点的题目！");
        
        return promptWithKnowledge.toString();
    }

    /**
     * 解析LLM返回结果并获取对应题目
     * LLM服务返回逗号分隔的题目ID字符串
     */
    private List<Qu> parseAndReturnQuestions(String llmResponse, List<Qu> allQuestions) {
        try {
            // 创建ID到题目的映射
            Map<String, Qu> questionMap = allQuestions.stream()
                .collect(Collectors.toMap(Qu::getId, q -> q));
            
            List<Qu> selectedQuestions = new ArrayList<>();
            
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                // 解析逗号分隔的ID列表
                String[] ids = llmResponse.trim().split("[,，\\s]+");
                for (String id : ids) {
                    id = id.trim();
                    if (!id.isEmpty() && questionMap.containsKey(id)) {
                        selectedQuestions.add(questionMap.get(id));
                    }
                }
            }
            
            // 如果解析失败或结果为空，返回前N个题目作为fallback
            if (selectedQuestions.isEmpty()) {
                System.err.println("AI选题结果解析失败，使用随机选择策略");
                Collections.shuffle(allQuestions);
                return allQuestions.stream().limit(5).collect(Collectors.toList());
            }
            
            System.out.println("AI成功选择了 " + selectedQuestions.size() + " 道题目");
            return selectedQuestions;
            
        } catch (Exception e) {
            System.err.println("解析AI选题结果失败: " + e.getMessage());
            // 解析失败时返回随机选择的题目
            Collections.shuffle(allQuestions);
            return allQuestions.stream().limit(5).collect(Collectors.toList());
        }
    }

    /**
     * 获取题型名称
     */
    private String getQuestionTypeName(Integer quType) {
        switch (quType) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "简答题";
            case 5: return "填空题";
            default: return "未知题型";
        }
    }

    /**
     * 截断题目内容以适应提示词长度限制
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 安全获取题目题干，如果为空则使用原题目内容的简化版
     */
    private String getSafeQuestionStem(Qu qu) {
        if (qu.getQuestionStem() != null && !qu.getQuestionStem().trim().isEmpty()) {
            return qu.getQuestionStem();
        }
        // 如果题干为空，使用截断的原内容
        return truncateContent(qu.getContent(), 50);
    }

    /**
     * 安全获取知识点，如果为空则返回空数组
     */
    private String getSafeKnowledgePoints(Qu qu) {
        if (qu.getKnowledgePoints() != null && !qu.getKnowledgePoints().trim().isEmpty()) {
            return qu.getKnowledgePoints();
        }
        return "[]";
    }
}