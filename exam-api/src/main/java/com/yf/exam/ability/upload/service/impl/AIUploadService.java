package com.yf.exam.ability.upload.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.yf.exam.ability.upload.utils.FileUtils.MultipartInputStreamFileResource;
import com.yf.exam.config.PromptConfig;
import com.yf.exam.core.api.ApiRest;
import com.yf.exam.modules.ai.service.AIProcessingService;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.entity.QuAnswer;
import com.yf.exam.modules.qu.service.QuService;
import com.yf.exam.modules.qu.service.QuAnswerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AIUploadService {

    private static final Logger logger = LoggerFactory.getLogger(AIUploadService.class);

    @Autowired
    private AIProcessingService aiProcessingService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private QuService quService;
    
    @Autowired
    private QuAnswerService quAnswerService;


    /**
     * 1. 先抽图片和文本（Python微服务） - 支持结构化和兼容格式
     */
    public String extractTextFromFile(MultipartFile file) {
        return extractTextFromFile(file, true); // 默认使用兼容格式
    }
    
    /**
     * 1. 先抽图片和文本（Python微服务） - 支持选择输出格式
     */
    public String extractTextFromFile(MultipartFile file, boolean legacyFormat) {
        try {
            String pythonUrl = "http://localhost:8003/api/extract_questions_with_images";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            Resource fileResource = new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename());
            body.add("file", fileResource);
            body.add("legacy_format", legacyFormat); // 传递格式参数

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("抽取图片/文本失败: " + response.getBody());
            }
            
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("文件解析异常: " + e.getMessage(), e);
        }
    }

    /**
     * 2. 调用大模型，拆题 - 通过HTTP接口调用LLM模块（增强版）
     */
    public JSONArray callAiExtractQuestions(String textContent) {
        return callAiExtractQuestions(textContent, null, null);
    }
    
    /**
     * 2. 调用大模型，拆题 - 带学科年级约束
     */
    public JSONArray callAiExtractQuestions(String textContent, String subject, String grade) {
        return callAiExtractQuestions(textContent, subject, grade, null);
    }
    
    /**
     * 2. 调用大模型，拆题 - 带学科年级约束和图片信息
     */
    public JSONArray callAiExtractQuestions(String textContent, String subject, String grade, JSONArray extractedImages) {
        // Direct extraction - no need for complex enhanced/original fallback
        return callOriginalExtraction(textContent, subject, grade, extractedImages);
    }
    
    /**
     * 2. 调用大模型，拆题 - 结构化提取版本
     * 处理DoclingDocument格式的输入
     */
    public JSONArray callAiExtractQuestionsStructured(String doclingDocumentJson, String subject, String grade) {
        try {
            logger.info("🏗️ 开始结构化题目提取");
            
            // 构建结构化提取提示词
            String prompt = PromptConfig.STRUCTURED_EXTRACTION_PROMPT + "\n\n文档内容：\n" + doclingDocumentJson;
            
            // 如果有学科年级约束，添加到提示词中
            if (subject != null && grade != null) {
                prompt += "\n\n【学科年级约束】：\n" +
                         "学科：" + subject + "\n" +
                         "年级：" + grade + "\n" +
                         "只提取与指定学科年级相关的题目，其他题目请忽略。";
            }
            
            // 🔍 DIAGNOSTIC: 记录发送给AI的内容
            logger.info("🔍 AI输入诊断:");
            logger.info("  📝 提示词长度: {}", prompt.length());
            logger.info("  📄 输入内容前500字符: {}", prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt);
            
            // 检查输入中的图片标记
            java.util.regex.Pattern inputMarkerPattern = java.util.regex.Pattern.compile("\\{\\{[A-Z_0-9]+\\}\\}");
            java.util.regex.Matcher inputMatcher = inputMarkerPattern.matcher(prompt);
            int inputMarkerCount = 0;
            StringBuilder inputMarkers = new StringBuilder();
            while (inputMatcher.find()) {
                inputMarkerCount++;
                if (inputMarkers.length() > 0) inputMarkers.append(", ");
                inputMarkers.append(inputMatcher.group());
            }
            logger.info("  🏷️ 输入中发现的图片标记数量: {}", inputMarkerCount);
            if (inputMarkerCount > 0) {
                logger.info("  📋 输入标记列表: {}", inputMarkers.toString());
            }
            
            // 调用AI服务 - 直接使用智能提取接口
            String response = aiProcessingService.extractQuestionsIntelligent(prompt);
            
            if (response == null) {
                throw new RuntimeException("结构化AI提取失败: AI服务返回空结果");
            }
            
            // 🔍 DIAGNOSTIC: 记录AI返回的内容
            logger.info("🔍 AI输出诊断:");
            logger.info("  📝 响应长度: {}", response.length());
            logger.info("  📄 响应前500字符: {}", response.length() > 500 ? response.substring(0, 500) + "..." : response);
            
            // 检查响应中的图片标记保留情况
            java.util.regex.Matcher outputMatcher = inputMarkerPattern.matcher(response);
            int outputMarkerCount = 0;
            StringBuilder outputMarkers = new StringBuilder();
            while (outputMatcher.find()) {
                outputMarkerCount++;
                if (outputMarkers.length() > 0) outputMarkers.append(", ");
                outputMarkers.append(outputMatcher.group());
            }
            logger.info("  🏷️ 响应中保留的图片标记数量: {}", outputMarkerCount);
            if (outputMarkerCount > 0) {
                logger.info("  📋 响应标记列表: {}", outputMarkers.toString());
            }
            
            // 标记保留率分析
            if (inputMarkerCount > 0) {
                double preservationRate = (double) outputMarkerCount / inputMarkerCount * 100;
                logger.info("  📊 标记保留率: {:.1f}% ({}/{})", preservationRate, outputMarkerCount, inputMarkerCount);
                
                if (preservationRate < 100) {
                    logger.warn("⚠️ 警告: AI未完全保留图片标记! 丢失了{}个标记", inputMarkerCount - outputMarkerCount);
                }
            }
            
            // 解析响应
            JSONArray questions = parseAIResponse(response);
            logger.info("✅ 结构化提取成功，获得{}道题目", questions.size());
            
            // 🔍 DIAGNOSTIC: 分析每个题目中的图片信息
            logger.info("🔍 题目图片分析:");
            for (int i = 0; i < questions.size(); i++) {
                JSONObject question = questions.getJSONObject(i);
                String content = question.getString("content");
                String imageUrl = question.getString("image");
                String blockId = question.getString("block_id");
                
                // 检查题目内容中的图片标记
                java.util.regex.Matcher questionMatcher = inputMarkerPattern.matcher(content != null ? content : "");
                int questionMarkerCount = 0;
                while (questionMatcher.find()) {
                    questionMarkerCount++;
                }
                
                logger.info("  题目{}: Block={}, 内容标记数={}, 图片URL={}", 
                    i + 1, blockId, questionMarkerCount, 
                    (imageUrl != null && !imageUrl.trim().isEmpty()) ? "有图片" : "无图片");
            }
            
            return questions;
            
        } catch (Exception e) {
            logger.error("❌ 结构化题目提取异常", e);
            throw new RuntimeException("结构化拆题异常: " + e.getMessage(), e);
        }
    }


    /**
     * 原始版题目提取 - 兼容性保证
     */
    private JSONArray callOriginalExtraction(String textContent) {
        return callOriginalExtraction(textContent, null, null);
    }
    
    /**
     * 原始版题目提取 - 带学科年级约束
     */
    private JSONArray callOriginalExtraction(String textContent, String subject, String grade) {
        return callOriginalExtraction(textContent, subject, grade, null);
    }
    
    /**
     * 原始版题目提取 - 带学科年级约束和图片信息
     */
    private JSONArray callOriginalExtraction(String textContent, String subject, String grade, JSONArray extractedImages) {
        try {
            // 调用智能提取接口 - 自动检测文档结构并选择最佳方法
            String response = aiProcessingService.extractQuestionsIntelligent(textContent);
            
            if (response == null) {
                throw new RuntimeException("调用AI接口失败: AI服务返回空结果");
            }
            
            // 解析响应 - 提取JSON数组
            String responseBody = response;
            JSONArray questions = parseAIResponse(responseBody);
            
            // 为原始提取的题目也进行个别处理（简化版）
            for (Object item : questions) {
                if (item instanceof JSONObject) {
                    JSONObject question = (JSONObject) item;
                    String questionContent = question.getString("content");
                    
                    // Simple approach - same as enhanced method
                    String extractedStem = aiProcessingService.extractStem(questionContent);
                    
                    // Use constrained knowledge point extraction if subject/grade provided
                    String knowledgePoint;
                    if (subject != null && grade != null) {
                        knowledgePoint = aiProcessingService.identifyKnowledgeWithConstraints(questionContent, subject, grade);
                    } else {
                        knowledgePoint = aiProcessingService.identifyKnowledge(questionContent);
                    }
                    
                    question.put("questionStem", extractedStem != null ? extractedStem.trim() : questionContent);
                    question.put("knowledgePoints", knowledgePoint != null ? "[\"" + knowledgePoint.trim() + "\"]" : "[]");
                    
                    // 设置提取状态为已处理（因为我们已经尝试了处理）
                    question.put("extractionStatus", 1);
                }
            }
            
            
            return questions;
            
        } catch (Exception e) {
            throw new RuntimeException("原始拆题异常: " + e.getMessage(), e);
        }
    }

    /**
     * 3. 存数据库
     */
    public ApiRest<?> saveQuestions(JSONArray questions) {
        return saveQuestionsWithImages(questions, null);
    }
    
    /**
     * 3. 存数据库（带图片信息）
     */
    public ApiRest<?> saveQuestionsWithImages(JSONArray questions, JSONArray extractedImages) {
        return saveQuestionsWithImages(questions, extractedImages, null, null);
    }
    
    /**
     * 3. 存数据库（带图片信息和学科年级） - 结构化版本
     */
    public ApiRest<?> saveQuestionsWithImagesStructured(JSONArray questions, JSONObject doclingDocument, String subject, String grade) {
        try {
            logger.info("💾 开始结构化保存，题目数：{}", questions.size());
            
            // 从DoclingDocument中提取图片映射
            JSONObject imagesDict = doclingDocument.getJSONObject("images");
            Map<String, String> imageUrlMap = new HashMap<>();
            
            if (imagesDict != null) {
                for (String imageId : imagesDict.keySet()) {
                    JSONObject imageInfo = imagesDict.getJSONObject(imageId);
                    String imageUrl = imageInfo.getString("url");
                    imageUrlMap.put(imageId, imageUrl);
                    logger.debug("🖼️ 图片映射: {} → {}", imageId, imageUrl);
                }
            }
            
            int savedCount = 0;
            
            for (Object questionObj : questions) {
                JSONObject questionJson = (JSONObject) questionObj;
                
                // 创建题目实体
                Qu qu = new Qu();
                qu.setQuType(questionJson.getInteger("quType"));
                qu.setLevel(questionJson.getInteger("level") != null ? questionJson.getInteger("level") : 1);
                
                // 结构化图片处理 - 直接从block_id和image_refs获取
                String blockId = questionJson.getString("block_id");
                String questionImageUrl = questionJson.getString("image");
                
                if (questionImageUrl != null && !questionImageUrl.trim().isEmpty()) {
                    // 如果AI已经返回了完整URL，直接使用
                    qu.setImage(convertImageUrl(questionImageUrl));
                    logger.info("🖼️ 题目图片: {} → {}", blockId, questionImageUrl);
                } else {
                    qu.setImage("");
                }
                
                qu.setContent(questionJson.getString("content"));
                qu.setCreateTime(new Date());
                qu.setUpdateTime(new Date());
                qu.setRemark(questionJson.getString("remark") != null ? questionJson.getString("remark") : "");
                qu.setAnalysis(questionJson.getString("analysis") != null ? questionJson.getString("analysis") : "");
                
                // 设置增强字段
                String questionStem = questionJson.getString("questionStem") != null ? 
                    questionJson.getString("questionStem") : questionJson.getString("content");
                String knowledgePoints = questionJson.getString("knowledgePoints") != null ? 
                    questionJson.getString("knowledgePoints") : "[]";
                Integer extractionStatus = questionJson.getInteger("extractionStatus") != null ? 
                    questionJson.getInteger("extractionStatus") : 1; // 结构化提取默认为已处理
                
                qu.setQuestionStem(questionStem);
                qu.setKnowledgePoints(knowledgePoints);
                qu.setExtractionStatus(extractionStatus);
                
                // 设置学科年级
                if (subject != null && !subject.trim().isEmpty()) {
                    qu.setSubject(subject);
                }
                if (grade != null && !grade.trim().isEmpty()) {
                    qu.setGrade(grade);
                }
                
                logger.info("💾 保存结构化题目:");
                logger.info("  📋 Block ID: {}", blockId);
                logger.info("  📝 Content: {}", qu.getContent().substring(0, Math.min(50, qu.getContent().length())) + "...");
                
                // 保存题目
                boolean saved = quService.save(qu);
                if (saved) {
                    savedCount++;
                    logger.info("  ✅ 题目保存成功，ID: {}", qu.getId());
                    
                    // 保存答案选项
                    JSONArray options = questionJson.getJSONArray("options");
                    if (options != null && !options.isEmpty()) {
                        for (Object optionObj : options) {
                            JSONObject optionJson = (JSONObject) optionObj;
                            
                            QuAnswer answer = new QuAnswer();
                            answer.setQuId(qu.getId());
                            answer.setIsRight(optionJson.getBoolean("isRight") != null ? optionJson.getBoolean("isRight") : false);
                            
                            // 结构化选项图片处理
                            String optionImageUrl = optionJson.getString("image");
                            if (optionImageUrl != null && !optionImageUrl.trim().isEmpty()) {
                                answer.setImage(convertImageUrl(optionImageUrl));
                                logger.info("    🖼️ 选项图片: {}", optionImageUrl);
                            } else {
                                answer.setImage("");
                            }
                            
                            answer.setContent(optionJson.getString("content"));
                            answer.setAnalysis(optionJson.getString("analysis") != null ? optionJson.getString("analysis") : "");
                            
                            quAnswerService.save(answer);
                        }
                    }
                } else {
                    logger.error("  ❌ 题目保存失败");
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("savedCount", savedCount);
            result.put("totalCount", questions.size());
            result.put("imageCount", imageUrlMap.size());
            result.put("extractionMethod", "structured");
            
            ApiRest<Map<String, Object>> apiRest = new ApiRest<>();
            apiRest.setCode(0);
            String message = "结构化导入成功: " + savedCount + " 道题目";
            if (imageUrlMap.size() > 0) {
                message += "，" + imageUrlMap.size() + " 张图片";
            }
            apiRest.setMsg(message);
            apiRest.setData(result);
            
            return apiRest;
            
        } catch (Exception e) {
            logger.error("❌ 结构化保存失败", e);
            throw new RuntimeException("结构化保存题目失败: " + e.getMessage(), e);
        }
    }

    /**
     * 3. 存数据库（带图片信息和学科年级）
     */
    public ApiRest<?> saveQuestionsWithImages(JSONArray questions, JSONArray extractedImages, String subject, String grade) {
        try {
            int savedCount = 0;
            int questionIndex = 0; // Question index for document position matching
            
            for (Object questionObj : questions) {
                JSONObject questionJson = (JSONObject) questionObj;
                
                // 创建题目实体
                Qu qu = new Qu();
                qu.setQuType(questionJson.getInteger("quType"));
                qu.setLevel(questionJson.getInteger("level") != null ? questionJson.getInteger("level") : 1);
                
                // Handle image URL - match by reference or sequential assignment
                String imageUrl = questionJson.getString("image");
                
                // Simple: extract image marker from question content and match directly
                String questionContent = questionJson.getString("content");
                String imageMarker = extractImageMarkerFromContent(questionContent);
                
                System.out.println("  🔍 Question " + questionIndex + " content: " + (questionContent != null ? questionContent.substring(0, Math.min(100, questionContent.length())) + "..." : "null"));
                System.out.println("  🏷️ Extracted marker: " + imageMarker);
                
                if (imageMarker != null && extractedImages != null) {
                    // Direct match by image_id
                    String matchedUrl = findImageByReference(imageMarker, extractedImages);
                    imageUrl = matchedUrl != null ? matchedUrl : "";
                    if (matchedUrl != null) {
                        System.out.println("  ✅ Direct match: " + imageMarker + " → " + matchedUrl.substring(matchedUrl.lastIndexOf('/') + 1));
                    } else {
                        System.out.println("  ❌ No match found for: " + imageMarker);
                    }
                } else {
                    imageUrl = "";
                    if (imageMarker == null) {
                        System.out.println("  ⚪ No image marker found in question content");
                    }
                }
                
                // 转换图片URL为浏览器兼容格式
                imageUrl = convertImageUrl(imageUrl);
                qu.setImage(imageUrl != null ? imageUrl : "");
                
                qu.setContent(questionJson.getString("content"));
                qu.setCreateTime(new Date());
                qu.setUpdateTime(new Date());
                qu.setRemark(questionJson.getString("remark") != null ? questionJson.getString("remark") : "");
                qu.setAnalysis(questionJson.getString("analysis") != null ? questionJson.getString("analysis") : "");
                
                // 设置增强字段
                String questionStem = questionJson.getString("questionStem") != null ? 
                    questionJson.getString("questionStem") : questionJson.getString("content");
                String knowledgePoints = questionJson.getString("knowledgePoints") != null ? 
                    questionJson.getString("knowledgePoints") : "[]";
                Integer extractionStatus = questionJson.getInteger("extractionStatus") != null ? 
                    questionJson.getInteger("extractionStatus") : 0;
                
                qu.setQuestionStem(questionStem);
                qu.setKnowledgePoints(knowledgePoints); // Keep for backward compatibility
                qu.setExtractionStatus(extractionStatus);
                
                // Set subject and grade if provided
                if (subject != null && !subject.trim().isEmpty()) {
                    qu.setSubject(subject);
                }
                if (grade != null && !grade.trim().isEmpty()) {
                    qu.setGrade(grade);
                }
                
                System.out.println("💾 Saving question with enhanced data:");
                System.out.println("  📝 Content: " + qu.getContent().substring(0, Math.min(50, qu.getContent().length())) + "...");
                System.out.println("  🔍 Stem: " + questionStem.substring(0, Math.min(50, questionStem.length())) + "...");
                System.out.println("  🏷️ Knowledge: " + knowledgePoints);
                System.out.println("  📊 Status: " + extractionStatus);
                
                // 保存题目
                boolean saved = quService.save(qu);
                if (saved) {
                    savedCount++;
                    System.out.println("  ✅ Question saved successfully with ID: " + qu.getId());
                    
                    // 保存答案选项
                    JSONArray options = questionJson.getJSONArray("options");
                    if (options != null && !options.isEmpty()) {
                        for (Object optionObj : options) {
                            JSONObject optionJson = (JSONObject) optionObj;
                            
                            QuAnswer answer = new QuAnswer();
                            answer.setQuId(qu.getId());
                            answer.setIsRight(optionJson.getBoolean("isRight") != null ? optionJson.getBoolean("isRight") : false);
                            
                            // Handle answer image URL - check both content and original image field
                            String answerContent = optionJson.getString("content");
                            String originalAnswerImage = optionJson.getString("image");
                            String answerImageUrl = "";
                            
                            // First try to extract marker from answer content
                            String answerImageMarker = extractImageMarkerFromContent(answerContent);
                            if (answerImageMarker != null && extractedImages != null) {
                                String matchedUrl = findImageByReference(answerImageMarker, extractedImages);
                                if (matchedUrl != null) {
                                    answerImageUrl = matchedUrl;
                                    System.out.println("    ✅ Answer content marker: " + answerImageMarker);
                                }
                            }
                            
                            // Fallback: try original image field if it contains a marker
                            if (answerImageUrl.isEmpty() && originalAnswerImage != null) {
                                String originalMarker = extractImageMarkerFromContent(originalAnswerImage);
                                if (originalMarker != null && extractedImages != null) {
                                    String matchedUrl = findImageByReference(originalMarker, extractedImages);
                                    if (matchedUrl != null) {
                                        answerImageUrl = matchedUrl;
                                        System.out.println("    ✅ Answer image field: " + originalMarker);
                                    }
                                }
                            }
                            
                            answer.setImage(convertImageUrl(answerImageUrl) != null ? convertImageUrl(answerImageUrl) : "");
                            answer.setContent(optionJson.getString("content"));
                            answer.setAnalysis(optionJson.getString("analysis") != null ? optionJson.getString("analysis") : "");
                            
                            quAnswerService.save(answer);
                        }
                    }
                } else {
                    System.err.println("  ❌ Failed to save question to database");
                }
                
                questionIndex++; // Move to next question
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("savedCount", savedCount);
            result.put("totalCount", questions.size());
            
            // Add image information to result
            int imageCount = extractedImages != null ? extractedImages.size() : 0;
            result.put("imageCount", imageCount);
            
            // Return proper ApiRest format
            ApiRest<Map<String, Object>> apiRest = new ApiRest<>();
            apiRest.setCode(0);
            String message = "成功导入 " + savedCount + " 道题目";
            if (imageCount > 0) {
                message += "，提取了 " + imageCount + " 张图片";
            }
            apiRest.setMsg(message);
            apiRest.setData(result);
            
            return apiRest;
        } catch (Exception e) {
            throw new RuntimeException("保存题目失败: " + e.getMessage(), e);
        }
    }

    /**
     * 4. 全流程入口（给 Controller 用）
     */
    public ApiRest<?> handleUploadAndSplit(MultipartFile file) {
        return handleUploadAndSplit(file, null, null);
    }
    
    /**
     * 4. 全流程入口（结构化版本）- 使用DoclingDocument格式
     */
    public ApiRest<?> handleUploadAndSplitStructured(MultipartFile file, String subject, String grade) {
        try {
            logger.info("🏗️ 开始结构化文档处理流程");
            
            // 1. 获取结构化文档
            String doclingJsonStr = extractTextFromFile(file, false); // 使用结构化格式
            JSONObject doclingDocument = JSONObject.parseObject(doclingJsonStr);
            
            // 检查Python服务错误
            if (doclingDocument.containsKey("error")) {
                ApiRest<Object> apiRest = new ApiRest<>();
                apiRest.setCode(1);
                apiRest.setMsg("结构化文档解析失败: " + doclingDocument.getString("error"));
                apiRest.setData(null);
                return apiRest;
            }
            
            logger.info("📋 结构化文档信息:");
            logger.info("  📝 内容块数: {}", doclingDocument.getJSONArray("content_blocks").size());
            logger.info("  🖼️ 图片数量: {}", doclingDocument.getJSONObject("images").size());
            
            // 2. 使用结构化AI提取题目
            JSONArray questions = callAiExtractQuestionsStructured(doclingJsonStr, subject, grade);
            
            // 3. 结构化保存到数据库
            return saveQuestionsWithImagesStructured(questions, doclingDocument, subject, grade);
            
        } catch (Exception e) {
            logger.error("❌ 结构化流程异常", e);
            ApiRest<Object> apiRest = new ApiRest<>();
            apiRest.setCode(1);
            apiRest.setMsg("结构化处理失败: " + e.getMessage());
            apiRest.setData(null);
            return apiRest;
        }
    }

    /**
     * 4. 全流程入口（带学科年级约束）
     */
    public ApiRest<?> handleUploadAndSplit(MultipartFile file, String subject, String grade) {
        try {
            logger.info("🔍 开始完整流程诊断 - 文件: {}", file.getOriginalFilename());
            
            // 1. 先抽文件内容
            String extractJsonStr = extractTextFromFile(file);
            JSONObject extractBody = JSONObject.parseObject(extractJsonStr);
            
            // 🔍 DIAGNOSTIC: 检查Python返回的内容
            logger.info("🔍 Python提取结果诊断:");
            logger.info("  📝 响应长度: {}", extractJsonStr.length());
            
            if (extractBody.containsKey("textContent")) {
                String textContent = extractBody.getString("textContent");
                logger.info("  📄 文本内容长度: {}", textContent.length());
                
                // 检查Python返回文本中的图片标记
                java.util.regex.Pattern pythonMarkerPattern = java.util.regex.Pattern.compile("\\{\\{[A-Z_0-9]+\\}\\}");
                java.util.regex.Matcher pythonMatcher = pythonMarkerPattern.matcher(textContent);
                int pythonMarkerCount = 0;
                StringBuilder pythonMarkers = new StringBuilder();
                while (pythonMatcher.find()) {
                    pythonMarkerCount++;
                    if (pythonMarkers.length() > 0) pythonMarkers.append(", ");
                    pythonMarkers.append(pythonMatcher.group());
                }
                logger.info("  🏷️ Python返回文本中的图片标记数量: {}", pythonMarkerCount);
                if (pythonMarkerCount > 0) {
                    logger.info("  📋 Python标记列表: {}", pythonMarkers.toString());
                }
                
                // 显示文本中的标记位置
                if (pythonMarkerCount > 0) {
                    logger.info("  📍 标记位置示例:");
                    java.util.regex.Matcher positionMatcher = pythonMarkerPattern.matcher(textContent);
                    int count = 0;
                    while (positionMatcher.find() && count < 3) { // 只显示前3个
                        int start = Math.max(0, positionMatcher.start() - 30);
                        int end = Math.min(textContent.length(), positionMatcher.end() + 30);
                        String context = textContent.substring(start, end).replaceAll("\n", "\\\\n");
                        logger.info("    {}: ...{}...", positionMatcher.group(), context);
                        count++;
                    }
                } else {
                    logger.warn("⚠️ 警告: Python返回的文本中未发现图片标记!");
                    logger.info("  📄 文本前300字符: {}", textContent.substring(0, Math.min(300, textContent.length())));
                }
            }
            
            if (extractBody.containsKey("images")) {
                JSONArray images = extractBody.getJSONArray("images");
                logger.info("  🖼️ Python返回图片数量: {}", images != null ? images.size() : 0);
                if (images != null && images.size() > 0) {
                    for (int i = 0; i < Math.min(3, images.size()); i++) { // 显示前3个图片
                        JSONObject img = images.getJSONObject(i);
                        logger.info("    图片{}: ID={}, URL={}", i+1, img.getString("image_id"), img.getString("image_url"));
                    }
                }
            }
            
            if (extractBody.containsKey("structure_info")) {
                JSONObject structureInfo = extractBody.getJSONObject("structure_info");
                logger.info("  📊 结构化信息: 总块数={}, 题目块数={}, 选项块数={}, 关联数={}", 
                    structureInfo.getInteger("total_blocks"),
                    structureInfo.getInteger("question_blocks"), 
                    structureInfo.getInteger("option_blocks"),
                    structureInfo.getInteger("relationships"));
            }
            
            // Check for Python service errors
            if (extractBody.containsKey("error")) {
                ApiRest<Object> apiRest = new ApiRest<>();
                apiRest.setCode(1);
                apiRest.setMsg("文件解析失败: " + extractBody.getString("error"));
                apiRest.setData(null);
                return apiRest;
            }
            
            String textContent = extractBody.getString("textContent");
            
            // Extract image information if available
            JSONArray extractedImages = extractBody.getJSONArray("images");
            int imageCount = extractBody.getInteger("imageCount") != null ? extractBody.getInteger("imageCount") : 0;
            
            // Check if textContent is null or empty
            if (textContent == null || textContent.trim().isEmpty()) {
                ApiRest<Object> apiRest = new ApiRest<>();
                apiRest.setCode(1);
                apiRest.setMsg("文件中未找到任何文本内容，请检查文件格式");
                apiRest.setData(null);
                return apiRest;
            }
            
            // Log image extraction results
            if (imageCount > 0) {
                System.out.println("Successfully extracted " + imageCount + " images from the document");
            }
            
            // 2. 调 LLM 拆题 (pass extracted images info to AI)
            JSONArray questions = callAiExtractQuestions(textContent, subject, grade, extractedImages);
            
            
            // 3. 存库并返回正确格式（包含图片信息）
            return saveQuestionsWithImages(questions, extractedImages, subject, grade);
        } catch (Exception e) {
            // Return proper ApiRest error format
            ApiRest<Object> apiRest = new ApiRest<>();
            apiRest.setCode(1);
            apiRest.setMsg("AI解析失败: " + e.getMessage());
            apiRest.setData(null);
            return apiRest;
        }
    }

    /**
     * 解析AI响应，提取JSON数组
     * AI可能返回额外的文本，需要提取纯JSON部分
     */
    private JSONArray parseAIResponse(String response) {
        try {
            // 尝试直接解析
            return JSONArray.parseArray(response);
        } catch (Exception e) {
            logger.warn("直接解析JSON失败，尝试提取JSON数组: {}", e.getMessage());
            
            try {
                // 处理markdown代码块格式 ```json ... ```
                String cleanedResponse = response;
                if (response.contains("```json")) {
                    int startIndex = response.indexOf("```json") + 7;
                    int endIndex = response.lastIndexOf("```");
                    if (startIndex < endIndex) {
                        cleanedResponse = response.substring(startIndex, endIndex).trim();
                    }
                }
                
                // 查找JSON数组的开始和结束位置
                int startIndex = cleanedResponse.indexOf('[');
                int endIndex = cleanedResponse.lastIndexOf(']');
                
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    String jsonString = cleanedResponse.substring(startIndex, endIndex + 1);
                    logger.info("提取到JSON字符串，长度: {}", jsonString.length());
                    return JSONArray.parseArray(jsonString);
                } else {
                    throw new RuntimeException("响应中未找到有效的JSON数组");
                }
                
            } catch (Exception parseError) {
                logger.error("JSON解析失败，响应前500字符: {}", 
                    response.length() > 500 ? response.substring(0, 500) : response);
                throw new RuntimeException("AI响应格式错误，无法解析JSON: " + parseError.getMessage());
            }
        }
    }


    /**
     * 根据图片标记查找匹配的图片URL
     * 例如：imageMarker = "[IMAGE_1]" 在extractedImages中找到对应的图片
     */
    private String findImageByReference(String imageMarker, JSONArray extractedImages) {
        if (imageMarker == null || imageMarker.trim().isEmpty() || extractedImages == null) {
            return null;
        }
        
        // 如果imageMarker是标准URL格式，直接返回
        if (imageMarker.startsWith("http://") || imageMarker.startsWith("https://")) {
            return imageMarker;
        }
        
        // 查找图片标记，例如 {{IMG_001}} 等
        String cleanMarker = imageMarker.replaceAll("[\\{\\}]", "").trim(); // 移除花括号
        
        for (Object imgObj : extractedImages) {
            JSONObject imageInfo = (JSONObject) imgObj;
            String imageId = imageInfo.getString("image_id");
            
            if (imageId != null && imageId.equals(cleanMarker)) {
                System.out.println("  🎯 Found perfect match for image marker: " + cleanMarker);
                return imageInfo.getString("image_url");
            }
        }
        
        // 如果没有找到精确匹配，尝试模糊匹配
        if (cleanMarker.startsWith("IMG_")) {
            try {
                String numberPart = cleanMarker.replace("IMG_", "");
                int imageNum = Integer.parseInt(numberPart);
                
                // 尝试直接按编号匹配 (IMG_001 format)
                for (Object imgObj : extractedImages) {
                    JSONObject imageInfo = (JSONObject) imgObj;
                    String imageId = imageInfo.getString("image_id");
                    if (imageId != null && imageId.equals(String.format("IMG_%03d", imageNum))) {
                        System.out.println("  🎯 Found numbered match for: IMG_" + String.format("%03d", imageNum));
                        return imageInfo.getString("image_url");
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }
        
        return null; // 未找到匹配，使用fallback逻辑
    }
    

    /**
     * 从题目内容中提取图片标记
     * 例如：从 "如图所示 {{IMG_001}} 求解..." 中提取 "IMG_001"
     */
    private String extractImageMarkerFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 使用正则表达式查找新格式的图片标记
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{IMG_\\d{3}\\}\\}");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        if (matcher.find()) {
            String marker = matcher.group();
            System.out.println("  📝 Extracted image marker from content: " + marker);
            return marker;
        }
        
        return null;
    }

    /**
     * 转换图片URL为浏览器兼容格式
     * WMF格式无法在浏览器中显示，需要转换
     */
    private String convertImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }

        // WMF转换已在Python服务中处理，这里收到的应该是PNG URL
        return imageUrl;
    }
}
