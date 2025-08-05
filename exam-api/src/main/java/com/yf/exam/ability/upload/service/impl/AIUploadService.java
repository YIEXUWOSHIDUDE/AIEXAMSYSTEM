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
     * 1. 先抽图片和文本（Python微服务）
     */
    public String extractTextFromFile(MultipartFile file) {
        try {
            String pythonUrl = "http://localhost:8003/api/extract_questions_with_images";
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            Resource fileResource = new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename());
            body.add("file", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("抽取图片/文本失败: " + response.getBody());
            }
            // 这里你可以直接返回 body，或者只返回 text 部分（比如JSONObject.parseObject().getString("textContent")）
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
     * 3. 存数据库（带图片信息和学科年级）
     */
    public ApiRest<?> saveQuestionsWithImages(JSONArray questions, JSONArray extractedImages, String subject, String grade) {
        try {
            int savedCount = 0;
            int imageIndex = 0; // Sequential image assignment counter
            
            for (Object questionObj : questions) {
                JSONObject questionJson = (JSONObject) questionObj;
                
                // 创建题目实体
                Qu qu = new Qu();
                qu.setQuType(questionJson.getInteger("quType"));
                qu.setLevel(questionJson.getInteger("level") != null ? questionJson.getInteger("level") : 1);
                
                // Handle image URL - sequential assignment from extracted images
                String imageUrl = questionJson.getString("image");
                
                // If question has image reference and we have extracted images available
                if (extractedImages != null && imageUrl != null && !imageUrl.trim().isEmpty() && imageIndex < extractedImages.size()) {
                    JSONObject extractedImage = extractedImages.getJSONObject(imageIndex);
                    imageUrl = extractedImage.getString("image_url");
                    imageIndex++; // Move to next image for next question
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
                            answer.setImage(optionJson.getString("image") != null ? optionJson.getString("image") : "");
                            answer.setContent(optionJson.getString("content"));
                            answer.setAnalysis(optionJson.getString("analysis") != null ? optionJson.getString("analysis") : "");
                            
                            quAnswerService.save(answer);
                        }
                    }
                } else {
                    System.err.println("  ❌ Failed to save question to database");
                }
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
     * 4. 全流程入口（带学科年级约束）
     */
    public ApiRest<?> handleUploadAndSplit(MultipartFile file, String subject, String grade) {
        try {
            // 1. 先抽文件内容
            String extractJsonStr = extractTextFromFile(file);
            JSONObject extractBody = JSONObject.parseObject(extractJsonStr);
            
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
     * 转换图片URL为浏览器兼容格式
     * WMF格式无法在浏览器中显示，需要转换
     */
    private String convertImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        
        // 检查是否为WMF格式
        if (imageUrl.toLowerCase().endsWith(".wmf")) {
            // WMF格式浏览器无法显示，返回null让系统使用文本替代
            logger.warn("WMF格式图片无法在浏览器中显示: {}", imageUrl);
            return null;
        }
        
        return imageUrl;
    }
}
