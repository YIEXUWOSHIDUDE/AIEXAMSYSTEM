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

@Service
public class AIUploadService {

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
        try {
            // First try enhanced extraction
            return callEnhancedExtraction(textContent);
        } catch (Exception e) {
            System.err.println("Enhanced extraction failed, falling back to original method: " + e.getMessage());
            // Fallback to original extraction
            return callOriginalExtraction(textContent);
        }
    }

    /**
     * 增强版题目提取 - 包含题干提取和知识点识别
     */
    private JSONArray callEnhancedExtraction(String textContent) {
        try {
            // 调用增强提取接口 - 直接使用AI服务
            System.out.println("🚀 Calling enhanced extraction API...");
            System.out.println("⏱️ Using AI processing service for question extraction...");
            
            String response = aiProcessingService.enhancedExtract(textContent);
            
            if (response == null) {
                throw new RuntimeException("调用增强提取接口失败: AI服务返回空结果");
            }
            
            // 解析响应
            String responseBody = response;
            System.out.println("📥 Enhanced extraction response received, length: " + responseBody.length());
            System.out.println("📋 Response preview: " + responseBody.substring(0, Math.min(200, responseBody.length())) + "...");
            
            JSONArray questions = JSONArray.parseArray(responseBody);
            System.out.println("✅ Parsed " + questions.size() + " questions from enhanced extraction");
            
            // 处理增强数据
            for (Object item : questions) {
                if (item instanceof JSONObject) {
                    JSONObject question = (JSONObject) item;
                    
                    // 设置提取状态为已提取
                    question.put("extractionStatus", 1);
                    
                    // 确保知识点格式正确 - 处理从LLM返回的字符串格式
                    if (question.containsKey("knowledgePoints")) {
                        Object kpObj = question.get("knowledgePoints");
                        if (kpObj instanceof JSONArray) {
                            // 如果是JSONArray，转换为字符串
                            JSONArray knowledgePoints = (JSONArray) kpObj;
                            question.put("knowledgePoints", knowledgePoints.toJSONString());
                        } else if (kpObj instanceof String) {
                            // 如果已经是字符串，保持不变（来自增强提取）
                            String kpStr = (String) kpObj;
                            try {
                                // 验证是否为有效的JSON数组格式
                                JSONArray.parseArray(kpStr);
                                question.put("knowledgePoints", kpStr);
                            } catch (Exception e) {
                                // 如果不是有效JSON，设置为空数组
                                question.put("knowledgePoints", "[]");
                            }
                        } else {
                            // 其他情况设置为空数组
                            question.put("knowledgePoints", "[]");
                        }
                    } else {
                        // 如果没有知识点字段，设置为空数组
                        question.put("knowledgePoints", "[]");
                    }
                    
                    // 确保题干字段存在
                    if (!question.containsKey("questionStem") || question.getString("questionStem") == null) {
                        question.put("questionStem", question.getString("content"));
                    }
                }
            }
            
            System.out.println("Enhanced extraction successful with " + questions.size() + " questions");
            return questions;
            
        } catch (Exception e) {
            throw new RuntimeException("增强提取异常: " + e.getMessage(), e);
        }
    }

    /**
     * 原始版题目提取 - 兼容性保证
     */
    private JSONArray callOriginalExtraction(String textContent) {
        try {
            // 调用原始提取接口 - 直接使用AI服务  
            String response = aiProcessingService.extractQuestions(textContent);
            
            if (response == null) {
                throw new RuntimeException("调用AI接口失败: AI服务返回空结果");
            }
            
            // 解析响应
            String responseBody = response;
            JSONArray questions = JSONArray.parseArray(responseBody);
            
            // 为原始提取的题目设置默认值
            for (Object item : questions) {
                if (item instanceof JSONObject) {
                    JSONObject question = (JSONObject) item;
                    
                    // 设置默认题干为原内容
                    if (!question.containsKey("questionStem")) {
                        question.put("questionStem", question.getString("content"));
                    }
                    
                    // 设置默认知识点为空数组
                    if (!question.containsKey("knowledgePoints")) {
                        question.put("knowledgePoints", "[]");
                    }
                    
                    // 设置提取状态为未处理
                    question.put("extractionStatus", 0);
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
        try {
            int savedCount = 0;
            
            for (Object questionObj : questions) {
                JSONObject questionJson = (JSONObject) questionObj;
                
                // 创建题目实体
                Qu qu = new Qu();
                qu.setQuType(questionJson.getInteger("quType"));
                qu.setLevel(questionJson.getInteger("level") != null ? questionJson.getInteger("level") : 1);
                
                // Handle image URL - prioritize question-specific image, fallback to extracted images
                String imageUrl = questionJson.getString("image");
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    // Try to assign an extracted image if available
                    if (extractedImages != null && !extractedImages.isEmpty() && savedCount < extractedImages.size()) {
                        JSONObject imageInfo = extractedImages.getJSONObject(savedCount);
                        imageUrl = imageInfo.getString("image_url");
                    }
                }
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
                qu.setKnowledgePoints(knowledgePoints);
                qu.setExtractionStatus(extractionStatus);
                
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
            
            // 2. 调 LLM 拆题
            JSONArray questions = callAiExtractQuestions(textContent);
            
            // 3. 存库并返回正确格式（包含图片信息）
            return saveQuestionsWithImages(questions, extractedImages);
        } catch (Exception e) {
            // Return proper ApiRest error format
            ApiRest<Object> apiRest = new ApiRest<>();
            apiRest.setCode(1);
            apiRest.setMsg("AI解析失败: " + e.getMessage());
            apiRest.setData(null);
            return apiRest;
        }
    }
}
