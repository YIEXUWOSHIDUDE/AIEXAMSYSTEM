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
        
        // 查找图片标记，例如 [IMAGE_1], IMAGE_2 等
        String cleanMarker = imageMarker.replaceAll("[\\[\\]]", "").trim(); // 移除方括号
        
        for (Object imgObj : extractedImages) {
            JSONObject imageInfo = (JSONObject) imgObj;
            String imageId = imageInfo.getString("image_id");
            
            if (imageId != null && imageId.equals(cleanMarker)) {
                System.out.println("  🎯 Found perfect match for image marker: " + cleanMarker);
                return imageInfo.getString("image_url");
            }
        }
        
        // 如果没有找到精确匹配，尝试模糊匹配
        if (cleanMarker.startsWith("IMAGE_")) {
            try {
                String numberPart = cleanMarker.replace("IMAGE_", "");
                int imageNum = Integer.parseInt(numberPart);
                
                // 尝试直接按编号匹配
                for (Object imgObj : extractedImages) {
                    JSONObject imageInfo = (JSONObject) imgObj;
                    String imageId = imageInfo.getString("image_id");
                    if (imageId != null && imageId.equals("IMAGE_" + imageNum)) {
                        System.out.println("  🎯 Found numbered match for: IMAGE_" + imageNum);
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
     * 根据文档位置获取最合适的图片
     * 用于为题目按文档顺序分配图片
     */
    private String getImageByDocumentPosition(JSONArray extractedImages, int questionIndex) {
        if (extractedImages == null || extractedImages.isEmpty()) {
            return null;
        }
        
        // 按document_position排序获取图片
        JSONArray sortedImages = new JSONArray();
        for (Object imgObj : extractedImages) {
            sortedImages.add(imgObj);
        }
        
        // 简单排序（按document_position）
        sortedImages.sort((a, b) -> {
            JSONObject imgA = (JSONObject) a;
            JSONObject imgB = (JSONObject) b;
            Integer posA = imgA.getInteger("document_position");
            Integer posB = imgB.getInteger("document_position");
            if (posA == null) posA = 0;
            if (posB == null) posB = 0;
            return posA.compareTo(posB);
        });
        
        // 根据题目索引分配图片（跳过第一张如果需要）
        int actualIndex = questionIndex;
        if (actualIndex >= 0 && actualIndex < sortedImages.size()) {
            JSONObject selectedImage = sortedImages.getJSONObject(actualIndex);
            return selectedImage.getString("image_url");
        }
        
        return null;
    }
    
    /**
     * 综合匹配策略 - 尝试多种方法找到最佳图片匹配
     */
    private String findBestImageMatch(String questionContent, String imageReference, JSONArray extractedImages, int questionIndex) {
        System.out.println("  🔍 Starting comprehensive image matching for question " + questionIndex);
        
        // Strategy 1: Extract and match image markers from content [IMAGE_1]
        String extractedImageMarker = extractImageMarkerFromContent(questionContent);
        if (extractedImageMarker != null) {
            String matchedUrl = findImageByReference(extractedImageMarker, extractedImages);
            if (matchedUrl != null) {
                System.out.println("  🎯 Strategy 1 SUCCESS: Found by content marker " + extractedImageMarker);
                return matchedUrl;
            }
        }
        
        // Strategy 2: Try exact matching with AI-provided image reference
        if (imageReference != null && !imageReference.trim().isEmpty()) {
            String matchedUrl = findImageByReference(imageReference, extractedImages);
            if (matchedUrl != null) {
                System.out.println("  🎯 Strategy 2 SUCCESS: Found by AI reference " + imageReference);
                return matchedUrl;
            }
        }
        
        // Strategy 3: Fuzzy matching - try to find references in nearby text
        String fuzzyMatchedUrl = findImageByFuzzyMatching(questionContent, imageReference, extractedImages);
        if (fuzzyMatchedUrl != null) {
            System.out.println("  🎯 Strategy 3 SUCCESS: Found by fuzzy matching");
            return fuzzyMatchedUrl;
        }
        
        // Strategy 4: Context-based matching using document position
        String contextMatchedUrl = findImageByContext(questionContent, extractedImages, questionIndex);
        if (contextMatchedUrl != null) {
            System.out.println("  🎯 Strategy 4 SUCCESS: Found by context matching");
            return contextMatchedUrl;
        }
        
        // Strategy 5: Smart positional assignment (improved)
        String positionUrl = getSmartPositionalImage(extractedImages, questionIndex);
        if (positionUrl != null) {
            System.out.println("  📍 Strategy 5 SUCCESS: Smart positional assignment");
            return positionUrl;
        }
        
        System.out.println("  ❌ All strategies failed for question " + questionIndex);
        return "";
    }
    
    /**
     * 文本锚点匹配策略 - 基于before/after text anchors精确匹配
     */
    private String findImageByFuzzyMatching(String questionContent, String imageReference, JSONArray extractedImages) {
        if (questionContent == null) return null;
        
        for (Object imgObj : extractedImages) {
            JSONObject imageInfo = (JSONObject) imgObj;
            String anchorBefore = imageInfo.getString("anchor_before");
            String anchorAfter = imageInfo.getString("anchor_after");
            
            // Strategy 1: Check if question content contains both anchors
            if (anchorBefore != null && anchorAfter != null && 
                !anchorBefore.trim().isEmpty() && !anchorAfter.trim().isEmpty()) {
                
                if (questionContent.contains(anchorBefore) && questionContent.contains(anchorAfter)) {
                    // Check if anchors appear in the right order
                    int beforePos = questionContent.indexOf(anchorBefore);
                    int afterPos = questionContent.indexOf(anchorAfter);
                    if (beforePos < afterPos && (afterPos - beforePos) < 100) { // Reasonable distance
                        System.out.println("  🎯 Perfect anchor match: '" + anchorBefore + "' → '" + anchorAfter + "'");
                        return imageInfo.getString("image_url");
                    }
                }
            }
            
            // Strategy 2: Check for single anchor match
            if (anchorBefore != null && !anchorBefore.trim().isEmpty() && 
                questionContent.contains(anchorBefore)) {
                System.out.println("  📍 Partial anchor match: '" + anchorBefore + "'");
                return imageInfo.getString("image_url");
            }
            
            // Strategy 3: Fallback to paragraph text matching
            String paragraphText = imageInfo.getString("paragraph_text");
            if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                // Check word overlap between question and paragraph
                String[] questionWords = questionContent.split("\\s+");
                String[] paraWords = paragraphText.split("\\s+");
                
                int matchCount = 0;
                for (String qWord : questionWords) {
                    for (String pWord : paraWords) {
                        if (qWord.length() > 2 && (qWord.equals(pWord) || qWord.contains(pWord) || pWord.contains(qWord))) {
                            matchCount++;
                        }
                    }
                }
                
                // If significant word overlap, consider it a match
                if (matchCount >= Math.min(3, questionWords.length / 4)) {
                    System.out.println("  📝 Word overlap match: " + matchCount + " matches");
                    return imageInfo.getString("image_url");
                }
            }
        }
        
        return null;
    }
    
    /**
     * 上下文匹配策略 - 基于文档位置和内容相似度
     */
    private String findImageByContext(String questionContent, JSONArray extractedImages, int questionIndex) {
        if (questionContent == null) return null;
        
        // Look for images whose nearby_text is most similar to question content
        JSONObject bestMatch = null;
        int bestScore = 0;
        
        for (Object imgObj : extractedImages) {
            JSONObject imageInfo = (JSONObject) imgObj;
            String nearbyText = imageInfo.getString("nearby_text");
            Integer docPosition = imageInfo.getInteger("document_position");
            
            if (nearbyText != null && docPosition != null) {
                int contextScore = calculateContextSimilarity(questionContent, nearbyText);
                
                // Boost score if document position is reasonable for this question
                int expectedPosition = questionIndex * 5; // Rough estimate
                int positionDiff = Math.abs(docPosition - expectedPosition);
                if (positionDiff < 10) {
                    contextScore += 2;
                }
                
                if (contextScore > bestScore) {
                    bestScore = contextScore;
                    bestMatch = imageInfo;
                }
            }
        }
        
        return bestMatch != null ? bestMatch.getString("image_url") : null;
    }
    
    /**
     * 智能位置分配 - 改进的位置匹配逻辑
     */
    private String getSmartPositionalImage(JSONArray extractedImages, int questionIndex) {
        if (extractedImages.isEmpty()) return null;
        
        // Sort images by document position
        JSONArray sortedImages = new JSONArray();
        for (Object imgObj : extractedImages) {
            sortedImages.add(imgObj);
        }
        
        sortedImages.sort((a, b) -> {
            JSONObject imgA = (JSONObject) a;
            JSONObject imgB = (JSONObject) b;
            Integer posA = imgA.getInteger("document_position");
            Integer posB = imgB.getInteger("document_position");
            if (posA == null) posA = 0;
            if (posB == null) posB = 0;
            return posA.compareTo(posB);
        });
        
        // Smart assignment - avoid simple 1:1 mapping
        int totalImages = sortedImages.size();
        int adjustedIndex = questionIndex;
        
        // If we have more questions than images, distribute images evenly
        if (questionIndex >= totalImages) {
            adjustedIndex = questionIndex % totalImages;
        }
        
        // Skip first image if it seems to be a title/header image
        if (adjustedIndex == 0 && totalImages > 3) {
            adjustedIndex = 1;
        }
        
        if (adjustedIndex < totalImages) {
            JSONObject selectedImage = sortedImages.getJSONObject(adjustedIndex);
            return selectedImage.getString("image_url");
        }
        
        return null;
    }
    
    /**
     * 计算内容相似度得分
     */
    private int calculateContextSimilarity(String content1, String content2) {
        if (content1 == null || content2 == null) return 0;
        
        String[] words1 = content1.toLowerCase().split("\\s+");
        String[] words2 = content2.toLowerCase().split("\\s+");
        
        int matches = 0;
        for (String word1 : words1) {
            if (word1.length() > 2) {
                for (String word2 : words2) {
                    if (word2.contains(word1) || word1.contains(word2)) {
                        matches++;
                        break;
                    }
                }
            }
        }
        
        return matches;
    }

    /**
     * 从题目内容中提取图片标记
     * 例如：从 "如图所示 [IMAGE_1] 求解..." 中提取 "IMAGE_1"
     */
    private String extractImageMarkerFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 使用正则表达式查找图片标记
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[IMAGE_\\d+\\]");
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
