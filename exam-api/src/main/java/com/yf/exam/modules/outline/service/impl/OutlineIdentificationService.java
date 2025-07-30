package com.yf.exam.modules.outline.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.config.PromptConfig;
import com.yf.exam.modules.ai.service.AIProcessingService;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;
import com.yf.exam.modules.outline.entity.QuestionOutlineMapping;
import com.yf.exam.modules.outline.service.KnowledgeOutlineService;
import com.yf.exam.modules.outline.service.QuestionOutlineMappingService;
import com.yf.exam.modules.qu.entity.Qu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AI知识大纲识别服务
 * AI Knowledge Outline Identification Service
 * 
 * @author AI Assistant
 * @since 2025-07-29
 */
@Service
public class OutlineIdentificationService {

    @Autowired
    private AIProcessingService aiProcessingService;
    
    @Autowired
    private KnowledgeOutlineService knowledgeOutlineService;
    
    @Autowired
    private QuestionOutlineMappingService questionOutlineMappingService;

    /**
     * 🤖 使用AI识别题目对应的知识大纲
     * @param question 题目对象
     * @param subject 学科 (可选，用于过滤)
     * @param grade 年级 (可选，用于过滤)
     * @return 识别结果，包含大纲ID和置信度
     */
    public OutlineIdentificationResult identifyOutlineForQuestion(Qu question, String subject, String grade) {
        try {
            System.out.println("🎯 开始AI识别题目知识大纲:");
            System.out.println("  题目ID: " + question.getId());
            System.out.println("  题目内容: " + question.getContent().substring(0, Math.min(50, question.getContent().length())) + "...");
            
            // 1. 获取相关的知识大纲作为上下文
            List<KnowledgeOutline> availableOutlines = getAvailableOutlines(subject, grade);
            if (availableOutlines.isEmpty()) {
                System.out.println("⚠️ 没有找到可用的知识大纲");
                return OutlineIdentificationResult.noMatch("没有可用的知识大纲");
            }
            
            // 2. 构建AI识别请求
            String outlineContext = buildOutlineContextForAI(availableOutlines);
            String identificationPrompt = buildIdentificationPrompt(question, outlineContext);
            
            // 3. 调用LLM服务进行识别
            JSONObject aiResponse = callAIForOutlineIdentification(question.getContent(), subject, grade);
            
            // 4. 解析AI响应
            return parseAIResponse(aiResponse, availableOutlines);
            
        } catch (Exception e) {
            System.err.println("❌ AI知识大纲识别失败: " + e.getMessage());
            return OutlineIdentificationResult.error("AI识别失败: " + e.getMessage());
        }
    }

    /**
     * 🎯 批量识别题目的知识大纲并存储到数据库
     * @param questions 题目列表
     * @param subject 学科过滤条件
     * @param grade 年级过滤条件
     * @return 处理结果统计
     */
    public BatchIdentificationResult batchIdentifyAndStore(List<Qu> questions, String subject, String grade) {
        System.out.println("🚀 开始批量AI识别知识大纲，题目数量: " + questions.size());
        
        BatchIdentificationResult result = new BatchIdentificationResult();
        result.setTotalCount(questions.size());
        
        for (Qu question : questions) {
            try {
                // 检查是否已经存在映射关系
                if (questionOutlineMappingService.hasMapping(question.getId())) {
                    System.out.println("⏭️ 题目 " + question.getId() + " 已存在大纲映射，跳过");
                    result.incrementSkippedCount();
                    continue;
                }
                
                // AI识别知识大纲
                OutlineIdentificationResult identificationResult = identifyOutlineForQuestion(question, subject, grade);
                
                if (identificationResult.isSuccess() && identificationResult.getOutlineId() != null) {
                    // 存储映射关系到数据库
                    boolean stored = storeQuestionOutlineMapping(question.getId(), identificationResult);
                    
                    if (stored) {
                        result.incrementSuccessCount();
                        System.out.println("✅ 题目 " + question.getId() + " 成功映射到大纲: " + identificationResult.getKnowledgePoint());
                    } else {
                        result.incrementFailedCount();
                        System.err.println("❌ 题目 " + question.getId() + " 映射存储失败");
                    }
                } else {
                    result.incrementNoMatchCount();
                    System.out.println("⚠️ 题目 " + question.getId() + " 未找到匹配的知识大纲");
                }
                
                // 添加延迟避免API调用过频繁
                Thread.sleep(100);
                
            } catch (Exception e) {
                result.incrementFailedCount();
                System.err.println("❌ 处理题目 " + question.getId() + " 失败: " + e.getMessage());
            }
        }
        
        System.out.println("🎉 批量识别完成！统计: " + result.getSummary());
        return result;
    }

    /**
     * 获取可用的知识大纲列表
     */
    private List<KnowledgeOutline> getAvailableOutlines(String subject, String grade) {
        // 如果指定了学科和年级，只获取匹配的大纲
        if (subject != null && grade != null) {
            return knowledgeOutlineService.getOutlinesBySubjectAndGrade(subject, grade);
        }
        
        // 否则获取所有启用的大纲
        return knowledgeOutlineService.getAllActiveOutlines();
    }

    /**
     * 为AI构建知识大纲上下文
     */
    private String buildOutlineContextForAI(List<KnowledgeOutline> outlines) {
        StringBuilder context = new StringBuilder();
        context.append("可选择的知识大纲列表：\n\n");
        
        for (KnowledgeOutline outline : outlines) {
            context.append("大纲ID: ").append(outline.getId()).append("\n");
            context.append("编码: ").append(outline.getOutlineCode()).append("\n");
            context.append("学科: ").append(outline.getSubject()).append("\n");
            context.append("年级: ").append(outline.getGrade()).append("\n");
            context.append("知识点: ").append(outline.getKnowledgePoint()).append("\n");
            context.append("---\n");
        }
        
        return context.toString();
    }

    /**
     * 构建AI识别提示
     */
    private String buildIdentificationPrompt(Qu question, String outlineContext) {
        return PromptConfig.OUTLINE_BASED_KNOWLEDGE_IDENTIFICATION_PROMPT + "\n\n" +
               outlineContext + "\n" +
               "需要识别的题目：\n" +
               "题目内容：" + question.getContent() + "\n" +
               "题目类型：" + getQuestionTypeName(question.getQuType()) + "\n\n" +
               "请分析题目内容，从上述大纲列表中选择最匹配的知识点：";
    }

    /**
     * 调用AI服务进行大纲识别 (直接服务调用)
     */
    private JSONObject callAIForOutlineIdentification(String questionContent, String subject, String grade) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("questionContent", questionContent);
            requestBody.put("subject", subject);
            requestBody.put("grade", grade);
            
            String result = aiProcessingService.identifyOutline(requestBody);
            
            if (result != null && !result.trim().isEmpty()) {
                return JSONObject.parseObject(result);
            } else {
                throw new RuntimeException("AI大纲识别返回空结果");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("调用AI服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析AI响应结果
     */
    private OutlineIdentificationResult parseAIResponse(JSONObject aiResponse, List<KnowledgeOutline> availableOutlines) {
        try {
            String outlineId = aiResponse.getString("outlineId");
            String knowledgePoint = aiResponse.getString("knowledgePoint");
            Double confidence = aiResponse.getDouble("confidence");
            String reason = aiResponse.getString("reason");
            
            if (outlineId == null || "null".equals(outlineId)) {
                return OutlineIdentificationResult.noMatch(reason != null ? reason : "未找到匹配的知识点");
            }
            
            // 验证返回的大纲ID是否在可选列表中
            boolean validOutline = availableOutlines.stream()
                .anyMatch(outline -> outline.getId().equals(outlineId));
                
            if (!validOutline) {
                return OutlineIdentificationResult.noMatch("AI返回的大纲ID不在可选范围内");
            }
            
            return OutlineIdentificationResult.success(outlineId, knowledgePoint, confidence, reason);
            
        } catch (Exception e) {
            return OutlineIdentificationResult.error("解析AI响应失败: " + e.getMessage());
        }
    }

    /**
     * 存储题目与大纲的映射关系
     */
    private boolean storeQuestionOutlineMapping(String questionId, OutlineIdentificationResult result) {
        try {
            QuestionOutlineMapping mapping = new QuestionOutlineMapping();
            mapping.setQuestionId(questionId);
            mapping.setOutlineId(result.getOutlineId());
            mapping.setCreateTime(new Date());
            
            return questionOutlineMappingService.save(mapping);
            
        } catch (Exception e) {
            System.err.println("存储映射关系失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取题目类型名称
     */
    private String getQuestionTypeName(Integer quType) {
        switch (quType) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "简答题";
            case 5: return "填空题";
            default: return "未知类型";
        }
    }

    // 内部结果类
    public static class OutlineIdentificationResult {
        private boolean success;
        private String outlineId;
        private String knowledgePoint;
        private Double confidence;
        private String reason;
        private String errorMessage;

        public static OutlineIdentificationResult success(String outlineId, String knowledgePoint, Double confidence, String reason) {
            OutlineIdentificationResult result = new OutlineIdentificationResult();
            result.success = true;
            result.outlineId = outlineId;
            result.knowledgePoint = knowledgePoint;
            result.confidence = confidence;
            result.reason = reason;
            return result;
        }

        public static OutlineIdentificationResult noMatch(String reason) {
            OutlineIdentificationResult result = new OutlineIdentificationResult();
            result.success = false;
            result.reason = reason;
            return result;
        }

        public static OutlineIdentificationResult error(String errorMessage) {
            OutlineIdentificationResult result = new OutlineIdentificationResult();
            result.success = false;
            result.errorMessage = errorMessage;
            return result;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getOutlineId() { return outlineId; }
        public String getKnowledgePoint() { return knowledgePoint; }
        public Double getConfidence() { return confidence; }
        public String getReason() { return reason; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class BatchIdentificationResult {
        private int totalCount = 0;
        private int successCount = 0;
        private int failedCount = 0;
        private int noMatchCount = 0;
        private int skippedCount = 0;

        public void incrementSuccessCount() { successCount++; }
        public void incrementFailedCount() { failedCount++; }
        public void incrementNoMatchCount() { noMatchCount++; }
        public void incrementSkippedCount() { skippedCount++; }

        public String getSummary() {
            return String.format("总数:%d, 成功:%d, 失败:%d, 无匹配:%d, 跳过:%d", 
                totalCount, successCount, failedCount, noMatchCount, skippedCount);
        }

        // Getters and Setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public int getNoMatchCount() { return noMatchCount; }
        public int getSkippedCount() { return skippedCount; }
    }
}