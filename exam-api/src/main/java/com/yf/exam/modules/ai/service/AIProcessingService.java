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
import com.yf.exam.modules.outline.service.KnowledgeOutlineService;
import java.util.stream.Collectors;
import java.util.*;
import java.util.Arrays;

/**
 * 统一AI处理服务 - 集成原LLM模块功能
 * Unified AI Processing Service - Integrated from LLM module
 */
@Service
public class AIProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AIProcessingService.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private KnowledgeOutlineService knowledgeOutlineService;
    
    // Qwen3-32B API配置
    private static final String QWEN3_API_URL = "http://10.0.201.81:10031/v1/chat/completions";
    private static final String MODEL_NAME = "qwen3_32b";

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
     * 题目提取 - 从文档中提取题目（带知识点约束）
     */
    public String extractQuestions(String content, String subject, String grade) {
        try {
            // 获取该学科年级的所有知识点作为约束
            List<String> validKnowledgePoints = getValidKnowledgePoints(subject, grade);
            
            String prompt = PromptConfig.EXTRACT_QUESTION_WITH_CONSTRAINTS_PROMPT 
                + "\n\n可选知识点：" + String.join("、", validKnowledgePoints)
                + "\n\n文档内容：\n" + content;
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
            logger.info("🎯 开始知识点识别，题目内容长度: {}", questionContent.length());
            String prompt = PromptConfig.KNOWLEDGE_POINT_PROMPT + "\n\n题目内容：\n" + questionContent;
            logger.info("📝 知识点识别提示词: {}", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);
            
            String result = callQwen3API(prompt);
            
            if (result != null) {
                logger.info("✅ 知识点识别AI响应成功，内容长度: {}", result.length());
                logger.info("📄 AI返回内容: {}", result);
                return result;
            } else {
                logger.error("❌ 知识点识别AI返回null");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 知识点识别失败", e);
            return null;
        }
    }

    /**
     * 知识点识别 - 带学科年级约束
     */
    public String identifyKnowledgeWithConstraints(String questionContent, String subject, String grade) {
        try {
            logger.info("🎯 开始约束知识点识别，学科: {}, 年级: {}", subject, grade);
            
            // 获取该学科年级的所有知识点作为约束
            List<String> validKnowledgePoints = getValidKnowledgePoints(subject, grade);
            
            String prompt = PromptConfig.KNOWLEDGE_POINT_PROMPT 
                + "\n\n【重要约束】：知识点必须从以下列表中选择，不能自创：\n"
                + String.join("、", validKnowledgePoints)
                + "\n\n题目内容：\n" + questionContent;
            
            String result = callQwen3API(prompt);
            
            if (result != null) {
                logger.info("✅ 约束知识点识别成功: {}", result);
                return result;
            } else {
                logger.error("❌ 约束知识点识别AI返回null");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 约束知识点识别失败", e);
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
     * 知识大纲结构提取 - 从文档中提取知识大纲（带知识点约束）
     */
    public String extractOutlineStructure(String prompt, String subject, String grade) {
        try {
            // 获取该学科年级的所有知识点作为约束
            List<String> validKnowledgePoints = getValidKnowledgePoints(subject, grade);
            
            String constrainedPrompt = prompt;
            if (!validKnowledgePoints.isEmpty()) {
                constrainedPrompt += "\n\n可选知识点约束：" + String.join("、", validKnowledgePoints)
                    + "\n注意：提取的知识点必须从上述列表中选择，不能自创新的知识点。";
            }
            
            return callQwen3API(constrainedPrompt);
        } catch (Exception e) {
            logger.error("知识大纲结构提取失败", e);
            return null;
        }
    }

    /**
     * 获取有效的知识点列表
     */
    private List<String> getValidKnowledgePoints(String subject, String grade) {
        try {
            return knowledgeOutlineService.getBySubjectAndGrade(subject, grade)
                .stream()
                .map(outline -> outline.getKnowledgePoint())
                .filter(kp -> kp != null && !kp.trim().isEmpty())
                .flatMap(kp -> Arrays.stream(kp.split("\\s+"))) // Split by spaces
                .filter(point -> !point.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("获取知识点列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 调用Qwen3-32B API
     */
    private String callQwen3API(String prompt) {
        try {
            logger.info("🚀 调用Qwen3 API: {}", QWEN3_API_URL);
            logger.info("🔍 使用模型: {}", MODEL_NAME);
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL_NAME);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 32768);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer EMPTY");
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            logger.info("📤 发送请求到: {}", QWEN3_API_URL);
            logger.debug("📤 请求参数: {}", requestBody.toString());
            
            ResponseEntity<String> response = restTemplate.postForEntity(QWEN3_API_URL, entity, String.class);
            
            logger.info("📥 响应状态: {}", response.getStatusCode());
            logger.info("📥 响应Headers: {}", response.getHeaders());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("✅ HTTP请求成功，解析响应内容");
                logger.debug("📥 完整响应: {}", response.getBody());
                
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
                    logger.error("❌ AI响应格式错误：choices为空，完整响应: {}", response.getBody());
                    return null;
                }
            } else {
                logger.error("❌ API调用失败");
                logger.error("状态码: {}", response.getStatusCode());
                logger.error("响应体: {}", response.getBody());
                throw new RuntimeException("AI服务返回错误状态: " + response.getStatusCode());
            }
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("❌ 无法连接到AI服务器: {}", QWEN3_API_URL);
            logger.error("连接错误: {}", e.getMessage());
            throw new RuntimeException("AI服务连接失败: 服务器不可达或服务未启动");
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            logger.error("❌ AI服务器内部错误: {}", e.getStatusCode());
            logger.error("错误响应: {}", e.getResponseBodyAsString());
            throw new RuntimeException("AI服务内部错误: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("❌ Qwen3 API调用异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logger.error("详细错误: ", e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
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