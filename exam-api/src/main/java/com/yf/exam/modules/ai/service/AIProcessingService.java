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
     * 增强题目提取 - 支持图片和文字混合内容
     * @param content 文档文字内容
     * @param images 图片数据列表（base64编码或URL）
     * @return 提取结果
     */
    public String extractQuestionsWithImages(String content, List<String> images) {
        try {
            logger.info("🖼️ 开始多模态题目提取，文字内容长度: {}, 图片数量: {}", 
                content != null ? content.length() : 0, 
                images != null ? images.size() : 0);
            
            String prompt = PromptConfig.EXTRACT_QUESTION_PROMPT + "\n\n" +
                "【重要说明】：\n" +
                "- 本次提取包含图片内容，请仔细分析图片中的信息\n" +
                "- 如果题目或选项中包含图片，请在相应的image字段中描述图片内容或提供图片标识\n" +
                "- 图片中的文字、图表、公式等都需要准确识别\n" +
                "- 支持数学公式、化学方程式、物理图表等专业内容\n\n" +
                "文档内容：\n" + (content != null ? content : "");
            
            // 对于包含图片的内容，使用增强的API调用
            if (images != null && !images.isEmpty()) {
                return callQwen3APIWithImages(prompt, images);
            } else {
                return callQwen3API(prompt);
            }
        } catch (Exception e) {
            logger.error("增强题目提取失败", e);
            return null;
        }
    }

    /**
     * 智能题目提取 - 自动检测文档结构并选择合适的提取方法
     * @param content 文档内容
     * @return 提取结果
     */
    public String extractQuestionsIntelligent(String content) {
        try {
            logger.info("🔍 开始智能题目提取，文档长度: {}", content != null ? content.length() : 0);
            
            // 检查文档大小，如果过大考虑分块处理
            if (content != null && content.length() > 15000) {
                logger.info("📄 文档较大({} 字符)，使用增强处理策略", content.length());
                return extractLargeDocument(content);
            }
            
            // 第一步：分析文档结构
            String structureAnalysis = analyzeDocumentStructure(content);
            if (structureAnalysis == null) {
                logger.warn("⚠️ 文档结构分析失败，回退到传统提取方法");
                return extractQuestions(content);
            }
            
            // 解析结构分析结果
            JSONObject structure = JSON.parseObject(structureAnalysis);
            String documentType = structure.getString("documentType");
            double confidence = structure.getDoubleValue("confidence");
            
            logger.info("📊 文档结构分析结果: 类型={}, 置信度={}", documentType, confidence);
            
            // 第二步：根据文档类型选择提取方法
            if ("separated".equals(documentType) && confidence > 0.7) {
                logger.info("🎯 检测到分离式答案格式，使用专门的提取方法");
                return extractQuestionsWithSeparatedAnswers(content, structure);
            } else {
                logger.info("📝 使用传统内联提取方法");
                return extractQuestions(content);
            }
            
        } catch (Exception e) {
            logger.error("❌ 智能题目提取失败: {}", e.getMessage());
            logger.info("🔄 回退到传统提取方法");
            return extractQuestions(content);
        }
    }

    /**
     * 处理大型文档 - 使用优化策略减少超时风险
     * @param content 大型文档内容
     * @return 提取结果
     */
    private String extractLargeDocument(String content) {
        try {
            logger.info("📚 开始大型文档处理");
            
            // 对于大型文档，直接使用分离式提取（更高效）
            // 因为大文档通常是正式的试题，更可能使用分离式答案
            String structurePrompt = "这是一个大型题目文档。请快速判断：\n" +
                "1. 题目和答案是否分离（所有题目在前，答案在后）？\n" +
                "2. 如果是分离格式，返回 {\"documentType\": \"separated\", \"confidence\": 0.9}\n" +
                "3. 如果不确定，返回 {\"documentType\": \"inline\", \"confidence\": 0.5}\n" +
                "只返回JSON，不要其他内容。\n\n" +
                "文档前1000字符：\n" + content.substring(0, Math.min(1000, content.length())) + "\n\n" +
                "文档后1000字符：\n" + content.substring(Math.max(0, content.length() - 1000));
            
            String quickAnalysis = callQwen3API(structurePrompt);
            
            if (quickAnalysis != null) {
                JSONObject structure = JSON.parseObject(quickAnalysis);
                String documentType = structure.getString("documentType");
                
                if ("separated".equals(documentType)) {
                    logger.info("🎯 大文档检测为分离式答案，使用专门处理");
                    // 为大文档添加预估题目数量
                    structure.put("totalQuestions", estimateQuestionCount(content));
                    return extractQuestionsWithSeparatedAnswers(content, structure);
                }
            }
            
            // 回退到传统方法
            logger.info("📝 大文档使用传统方法处理");
            return extractQuestions(content);
            
        } catch (Exception e) {
            logger.error("❌ 大文档处理失败: {}", e.getMessage());
            return extractQuestions(content);
        }
    }

    /**
     * 估算文档中的题目数量
     * @param content 文档内容
     * @return 预估题目数量
     */
    private int estimateQuestionCount(String content) {
        try {
            // 简单的题目数量估算逻辑
            int count = 0;
            
            // 匹配常见的题目编号模式
            String[] patterns = {
                "\\d+\\.", // 1. 2. 3.
                "\\(\\d+\\)", // (1) (2) (3)
                "[A-Z]\\.", // A. B. C.
                "第\\d+题", // 第1题 第2题
                "Question \\d+" // Question 1
            };
            
            for (String pattern : patterns) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(content);
                int matches = 0;
                while (m.find()) {
                    matches++;
                }
                count = Math.max(count, matches);
            }
            
            logger.info("📊 预估题目数量: {}", count);
            return Math.max(count, 10); // 至少估算为10题
            
        } catch (Exception e) {
            logger.warn("⚠️ 题目数量估算失败: {}", e.getMessage());
            return 15; // 默认估算值
        }
    }

    /**
     * 分析文档结构 - 检测题目和答案的组织方式
     * @param content 文档内容
     * @return 结构分析JSON结果
     */
    private String analyzeDocumentStructure(String content) {
        try {
            logger.info("🔍 开始文档结构分析");
            String prompt = PromptConfig.DOCUMENT_STRUCTURE_ANALYSIS_PROMPT + "\n\n文档内容：\n" + content;
            
            String result = callQwen3API(prompt);
            
            if (result != null) {
                logger.info("✅ 文档结构分析完成");
                logger.debug("📄 分析结果: {}", result);
                return result;
            } else {
                logger.error("❌ 文档结构分析返回null");
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 文档结构分析异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 提取分离式答案格式的题目
     * @param content 文档内容
     * @param structure 文档结构信息
     * @return 提取结果
     */
    private String extractQuestionsWithSeparatedAnswers(String content, JSONObject structure) {
        try {
            logger.info("🎯 开始分离式答案提取");
            
            String numberingStyle = structure.getString("questionNumberingStyle");
            int totalQuestions = structure.getIntValue("totalQuestions");
            
            logger.info("📋 提取参数: 编号格式={}, 预估题目数={}", numberingStyle, totalQuestions);
            
            String prompt = PromptConfig.SEPARATED_ANSWER_EXTRACTION_PROMPT + "\n\n" +
                "【文档结构信息】：\n" +
                "编号格式：" + numberingStyle + "\n" +
                "预估题目数：" + totalQuestions + "\n\n" +
                "文档内容：\n" + content;
            
            String result = callQwen3API(prompt);
            
            if (result != null) {
                logger.info("✅ 分离式答案提取成功，开始验证质量");
                
                // 验证提取质量
                boolean isValid = validateQuestionAnswerMatching(result, totalQuestions);
                if (!isValid) {
                    logger.warn("⚠️ 分离式答案提取质量不佳，回退到传统方法");
                    return extractQuestions(content);
                }
                
                return result;
            } else {
                logger.error("❌ 分离式答案提取失败");
                throw new RuntimeException("分离式答案提取返回null");
            }
            
        } catch (Exception e) {
            logger.error("❌ 分离式答案提取异常: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证题目-答案匹配质量
     * @param extractionResult 提取结果JSON
     * @param expectedQuestions 预期题目数量
     * @return 是否通过验证
     */
    private boolean validateQuestionAnswerMatching(String extractionResult, int expectedQuestions) {
        try {
            logger.info("🔍 开始验证题目-答案匹配质量");
            
            // 检查提取结果是否为空或null
            if (extractionResult == null || extractionResult.trim().isEmpty()) {
                logger.warn("❌ 提取结果为空或null");
                return false;
            }
            
            // 记录原始数据用于调试
            logger.info("📋 验证数据长度: {}", extractionResult.length());
            logger.debug("📋 验证数据前200字符: {}", 
                extractionResult.length() > 200 ? extractionResult.substring(0, 200) + "..." : extractionResult);
            
            JSONArray questions;
            try {
                questions = JSON.parseArray(extractionResult);
            } catch (Exception jsonException) {
                logger.error("❌ JSON解析失败: {}", jsonException.getMessage());
                logger.error("❌ 无法解析的数据前500字符: {}", 
                    extractionResult.length() > 500 ? extractionResult.substring(0, 500) + "..." : extractionResult);
                return false;
            }
            
            if (questions == null || questions.isEmpty()) {
                logger.warn("❌ 提取结果为空");
                return false;
            }
            
            int actualQuestions = questions.size();
            logger.info("📊 提取统计: 预期{}题，实际{}题", expectedQuestions, actualQuestions);
            
            // 检查题目数量合理性
            if (expectedQuestions > 0) {
                double ratio = (double) actualQuestions / expectedQuestions;
                if (ratio < 0.5 || ratio > 1.5) {
                    logger.warn("⚠️ 题目数量偏差过大: 预期{}, 实际{}, 比例{}", 
                        expectedQuestions, actualQuestions, ratio);
                    return false;
                }
            }
            
            // 检查每个题目的完整性
            int validQuestions = 0;
            int questionsWithAnswers = 0;
            
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                
                // 基本字段检查
                if (question.containsKey("content") && 
                    question.containsKey("quType") && 
                    question.containsKey("options")) {
                    validQuestions++;
                    
                    // 检查是否有正确答案
                    JSONArray options = question.getJSONArray("options");
                    if (options != null && !options.isEmpty()) {
                        boolean hasCorrectAnswer = false;
                        for (int j = 0; j < options.size(); j++) {
                            JSONObject option = options.getJSONObject(j);
                            if (option.getBooleanValue("isRight")) {
                                hasCorrectAnswer = true;
                                break;
                            }
                        }
                        if (hasCorrectAnswer) {
                            questionsWithAnswers++;
                        }
                    }
                }
            }
            
            logger.info("📈 质量统计: 有效题目{}/{}, 有答案题目{}/{}", 
                validQuestions, actualQuestions, questionsWithAnswers, actualQuestions);
            
            // 验证通过条件
            double validRatio = (double) validQuestions / actualQuestions;
            double answerRatio = (double) questionsWithAnswers / actualQuestions;
            
            boolean passed = validRatio >= 0.8 && answerRatio >= 0.7;
            
            if (passed) {
                logger.info("✅ 题目-答案匹配质量验证通过");
            } else {
                logger.warn("❌ 题目-答案匹配质量不达标: 有效比例={}, 答案比例={}", validRatio, answerRatio);
            }
            
            return passed;
            
        } catch (Exception e) {
            logger.error("❌ 验证过程异常: {}", e.getMessage());
            return false;
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
     * 调用Qwen3-32B API - 支持图片的多模态版本
     * @param prompt 文字提示
     * @param images 图片列表（base64编码或URL）
     * @return AI响应内容
     */
    private String callQwen3APIWithImages(String prompt, List<String> images) {
        try {
            logger.info("🚀 调用Qwen3多模态API: {}", QWEN3_API_URL);
            logger.info("🔍 使用模型: {}, 图片数量: {}", MODEL_NAME, images.size());
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL_NAME);
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            
            // 构建多模态内容
            JSONArray contentArray = new JSONArray();
            
            // 添加文字内容
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", prompt);
            contentArray.add(textContent);
            
            // 添加图片内容
            for (String imageData : images) {
                JSONObject imageContent = new JSONObject();
                imageContent.put("type", "image_url");
                JSONObject imageUrl = new JSONObject();
                
                // 判断是base64还是URL
                if (imageData.startsWith("data:image") || imageData.startsWith("http")) {
                    imageUrl.put("url", imageData);
                } else {
                    // 假设是base64编码，添加前缀
                    imageUrl.put("url", "data:image/jpeg;base64," + imageData);
                }
                
                imageContent.put("image_url", imageUrl);
                contentArray.add(imageContent);
            }
            
            message.put("content", contentArray);
            messages.add(message);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 32768);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
          
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            logger.info("📤 发送多模态请求到: {}", QWEN3_API_URL);
            logger.debug("📤 请求参数: {}", requestBody.toString());
            
            ResponseEntity<String> response = restTemplate.postForEntity(QWEN3_API_URL, entity, String.class);
            
            return parseQwen3Response(response);
            
        } catch (Exception e) {
            logger.error("❌ Qwen3多模态API调用异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logger.error("详细错误: ", e);
            throw new RuntimeException("多模态AI服务调用失败: " + e.getMessage());
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
            
            return parseQwen3Response(response);
            
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("❌ 无法连接到AI服务器: {}", QWEN3_API_URL);
            logger.error("连接错误: {}", e.getMessage());
            
            // 检查是否是超时错误
            if (e.getMessage().contains("Read timed out")) {
                throw new RuntimeException("AI服务处理超时: 文档可能过于复杂，请尝试分段处理或简化内容");
            } else {
                throw new RuntimeException("AI服务连接失败: 服务器不可达或服务未启动");
            }
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            logger.error("❌ AI服务器内部错误: {}", e.getStatusCode());
            logger.error("错误响应: {}", e.getResponseBodyAsString());
            throw new RuntimeException("AI服务内部错误: " + e.getStatusCode());
        } catch (Exception e) {
            logger.error("❌ Qwen3 API调用异常: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            logger.error("详细错误: ", e);
            
            // 对于超时类异常提供更有用的错误信息
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                throw new RuntimeException("AI服务处理超时: 文档复杂度较高，请考虑：1)分段处理 2)简化文档内容 3)稍后重试");
            } else {
                throw new RuntimeException("AI服务调用失败: " + e.getMessage());
            }
        }
    }

    /**
     * 解析Qwen3 API响应
     * @param response HTTP响应
     * @return 解析出的内容
     */
    private String parseQwen3Response(ResponseEntity<String> response) {
        logger.info("📥 响应状态: {}", response.getStatusCode());
        logger.info("📥 响应Headers: {}", response.getHeaders());
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            logger.info("✅ HTTP请求成功，解析响应内容");
            logger.debug("📥 完整响应: {}", response.getBody());
            
            try {
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
            } catch (Exception e) {
                logger.error("❌ JSON解析失败: {}", e.getMessage());
                logger.error("❌ 响应前500字符: {}", 
                    response.getBody().length() > 500 ? response.getBody().substring(0, 500) : response.getBody());
                return null;
            }
        } else {
            logger.error("❌ API调用失败");
            logger.error("状态码: {}", response.getStatusCode());
            logger.error("响应体: {}", response.getBody());
            throw new RuntimeException("AI服务返回错误状态: " + response.getStatusCode());
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
        Object maxScoreObj = request.get("maxScore");
        
        // Handle both Integer and Double types for maxScore
        double maxScore;
        if (maxScoreObj instanceof Integer) {
            maxScore = ((Integer) maxScoreObj).doubleValue();
        } else if (maxScoreObj instanceof Double) {
            maxScore = (Double) maxScoreObj;
        } else {
            maxScore = 10.0; // Default fallback
        }

        return PromptConfig.QUICK_SHORT_ANSWER_GRADING_PROMPT + "\n\n" +
                "题目内容：" + questionContent + "\n" +
                "标准答案：" + (standardAnswer != null ? standardAnswer : "无标准答案") + "\n" +
                "知识点：" + (knowledgePoint != null ? knowledgePoint : "基础知识") + "\n" +
                "满分：" + maxScore + "分\n" +
                "学生答案：" + (userAnswer != null ? userAnswer : "");
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
