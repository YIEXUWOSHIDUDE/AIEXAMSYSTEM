package com.yf.exam.modules.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.config.PromptConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final RestTemplate restTemplate = createRestTemplate();
    
    // Qwen3-32B API配置
    private static final String QWEN3_API_URL = "http://10.0.201.81:10031/v1/chat/completions";
    private static final String MODEL_NAME = "Qwen/Qwen2-72B-Instruct";
    
    private static RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();
        // Set connection and read timeouts to handle large responses
        template.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Connection", "keep-alive");
            return execution.execute(request, body);
        });
        return template;
    }

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
     * 调用Qwen3-32B API
     */
    private String callQwen3API(String prompt) {
        try {
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

            ResponseEntity<String> response = restTemplate.postForEntity(QWEN3_API_URL, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject responseObj = JSON.parseObject(response.getBody());
                JSONArray choices = responseObj.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    JSONObject messageObj = firstChoice.getJSONObject("message");
                    return messageObj.getString("content");
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("调用Qwen3 API失败", e);
            return null;
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