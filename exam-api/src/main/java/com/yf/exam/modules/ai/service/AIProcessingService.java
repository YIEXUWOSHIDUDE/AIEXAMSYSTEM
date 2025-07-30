package com.yf.exam.modules.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.config.PromptConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 统一AI处理服务 - 集成原LLM模块功能
 * Unified AI Processing Service - Integrated from LLM module
 */
@Service
public class AIProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AIProcessingService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    // Qwen3-32B API配置
    private static final String QWEN3_API_URL = "http://10.0.201.81:10031/v1/chat/completions";
    private static final String MODEL_NAME = "Qwen/Qwen2-72B-Instruct";
    
    // 备用配置或Mock响应
    private static final boolean ENABLE_MOCK_FALLBACK = true;

    /**
     * 题目提取 - 从文档中提取题目
     */
    public String extractQuestions(String content) {
        try {
            String prompt = PromptConfig.EXTRACT_QUESTION_PROMPT + "\n\n文档内容：\n" + content;
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("题目提取失败", e);
            return null;
        }
    }

    /**
     * 题目选择 - 智能选择题目
     */
    public String selectQuestions(Map<String, Object> request) {
        try {
            String prompt = buildSelectionPrompt(request);
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("题目选择失败", e);
            return null;
        }
    }

    /**
     * 轻量级题目选择
     */
    public String selectLightweightQuestions(Map<String, Object> request) {
        try {
            String prompt = buildLightweightSelectionPrompt(request);
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("轻量级题目选择失败", e);
            return null;
        }
    }

    /**
     * 题干提取
     */
    public String extractStem(String questionContent) {
        try {
            String prompt = PromptConfig.STEM_EXTRACTION_PROMPT + "\n\n题目内容：\n" + questionContent;
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("题干提取失败", e);
            return null;
        }
    }

    /**
     * 知识点识别
     */
    public String identifyKnowledge(String questionContent) {
        try {
            String prompt = PromptConfig.KNOWLEDGE_POINT_PROMPT + "\n\n题目内容：\n" + questionContent;
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("知识点识别失败", e);
            return null;
        }
    }

    /**
     * 增强提取
     */
    public String enhancedExtract(String content) {
        try {
            String prompt = PromptConfig.EXTRACT_QUESTION_PROMPT + "\n\n【增强模式】\n" + content;
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("增强提取失败", e);
            return null;
        }
    }

    /**
     * 简答题判分
     */
    public String judgeShortAnswer(Map<String, Object> request) {
        try {
            String prompt = buildShortAnswerJudgePrompt(request);
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("简答题判分失败", e);
            return null;
        }
    }

    /**
     * 整体测试判分
     */
    public String judgeOverallTest(Map<String, Object> request) {
        try {
            String prompt = buildOverallTestJudgePrompt(request);
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("整体测试判分失败", e);
            return null;
        }
    }

    /**
     * 大纲识别
     */
    public String identifyOutline(Map<String, Object> request) {
        try {
            String prompt = buildOutlineIdentificationPrompt(request);
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("大纲识别失败", e);
            return null;
        }
    }

    /**
     * 知识大纲结构提取 - 从文档中提取知识大纲
     */
    public String extractOutlineStructure(String prompt) {
        try {
            return callQwen3API(prompt);
        } catch (Exception e) {
            logger.error("知识大纲结构提取失败", e);
            return null;
        }
    }

    /**
     * 调用Qwen3-32B API
     */
    private String callQwen3API(String prompt) {
        try {
            logger.info("🚀 调用Qwen3 API: {}", QWEN3_API_URL);
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL_NAME);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 4000);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            logger.debug("📤 请求参数: {}", requestBody.toString());
            
            ResponseEntity<String> response = restTemplate.postForEntity(QWEN3_API_URL, entity, String.class);
            
            logger.info("📥 响应状态: {}", response.getStatusCode());
            logger.debug("📥 响应内容: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject responseObj = JSON.parseObject(response.getBody());
                JSONArray choices = responseObj.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    String content = messageObj.getString("content");
                    
                    if (content != null && !content.trim().isEmpty()) {
                        logger.info("✅ AI响应成功，内容长度: {}", content.length());
                        return content;
                    } else {
                        logger.error("❌ AI返回内容为空");
                        return null;
                    }
                } else {
                    logger.error("❌ AI响应格式错误：choices为空");
                    return null;
                }
            } else {
                logger.error("❌ API调用失败，状态码: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ 调用Qwen3 API异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logger.debug("异常详情", e);
            
            // 如果启用了Mock回退，返回示例响应
            if (ENABLE_MOCK_FALLBACK) {
                logger.warn("⚠️ 启用Mock回退模式");
                return getMockResponse(prompt);
            }
            
            return null;
        }
    }

    /**
     * Mock响应 - 当Qwen3 API不可用时的回退方案
     */
    private String getMockResponse(String prompt) {
        logger.info("🔧 生成Mock响应");
        
        // 根据不同的提示类型返回不同的Mock响应
        if (prompt.contains("提取题目") || prompt.contains("EXTRACT_QUESTION_PROMPT")) {
            return "[\n" +
                   "  {\n" +
                   "    \"quType\": 1,\n" +
                   "    \"level\": 1,\n" +
                   "    \"content\": \"以下哪个是Java的基本数据类型？\",\n" +
                   "    \"analysis\": \"Java有8种基本数据类型，int是其中之一\",\n" +
                   "    \"options\": [\n" +
                   "      {\"content\": \"String\", \"isRight\": false},\n" +
                   "      {\"content\": \"int\", \"isRight\": true},\n" +
                   "      {\"content\": \"Array\", \"isRight\": false},\n" +
                   "      {\"content\": \"Object\", \"isRight\": false}\n" +
                   "    ]\n" +
                   "  }\n" +
                   "]";
        } else if (prompt.contains("题干提取")) {
            return "以下哪个是Java的基本数据类型？";
        } else if (prompt.contains("知识点识别")) {
            return "[\"Java基础语法\"]";
        } else {
            return "Mock响应：AI服务暂时不可用，请联系管理员检查Qwen3服务状态";
        }
    }

    /**
     * 构建题目选择提示
     */
    private String buildSelectionPrompt(Map<String, Object> request) {
        return PromptConfig.DIFFICULTY_ENFORCED_SELECTION_PROMPT + "\n\n" +
                "请求参数：\n" + JSON.toJSONString(request);
    }

    /**
     * 构建轻量级选择提示
     */
    private String buildLightweightSelectionPrompt(Map<String, Object> request) {
        return "请根据以下条件选择题目：\n" + JSON.toJSONString(request);
    }

    /**
     * 构建简答题判分提示
     */
    private String buildShortAnswerJudgePrompt(Map<String, Object> request) {
        String questionContent = (String) request.get("questionContent");
        String standardAnswer = (String) request.get("standardAnswer");
        String userAnswer = (String) request.get("userAnswer");
        String knowledgePoint = (String) request.get("knowledgePoint");
        Integer maxScore = (Integer) request.get("maxScore");

        return PromptConfig.SHORT_ANSWER_JUDGE_PROMPT + "\n\n" +
                "题目内容：" + questionContent + "\n" +
                "标准答案：" + standardAnswer + "\n" +
                "知识点：" + knowledgePoint + "\n" +
                "满分：" + maxScore + "分\n" +
                "学生答案：" + userAnswer;
    }

    /**
     * 构建整体测试判分提示
     */
    private String buildOverallTestJudgePrompt(Map<String, Object> request) {
        return PromptConfig.OVERALL_TEST_JUDGE_PROMPT + "\n\n" +
                "测试信息：\n" + JSON.toJSONString(request);
    }

    /**
     * 构建大纲识别提示
     */
    private String buildOutlineIdentificationPrompt(Map<String, Object> request) {
        String questionContent = (String) request.get("questionContent");
        String subject = (String) request.get("subject");
        String grade = (String) request.get("grade");

        return PromptConfig.OUTLINE_IDENTIFICATION_PROMPT + "\n\n" +
                "题目内容：" + questionContent + "\n" +
                "学科：" + subject + "\n" +
                "年级：" + grade;
    }
}