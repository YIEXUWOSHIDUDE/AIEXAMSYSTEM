package com.yf.exam.modules.outline.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.ai.service.AIProcessingService;
import com.yf.exam.modules.outline.service.QuestionOutlineMappingService;
import com.yf.exam.modules.qu.entity.Qu;
import com.yf.exam.modules.qu.service.QuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大纲AI识别控制器 - 简化版
 * Simple Outline AI Controller
 */
@RestController
@RequestMapping("/exam/api/outline-ai")
public class OutlineAIController extends BaseController {

    @Autowired
    private AIProcessingService aiProcessingService;

    @Autowired
    private QuService quService;

    @Autowired
    private QuestionOutlineMappingService mappingService;

    /**
     * 单个题目AI识别大纲
     */
    @PostMapping("/identify-single")
    public ApiRest<?> identifySingle(@RequestBody Map<String, Object> request) {
        try {
            String questionId = (String) request.get("questionId");
            String subject = (String) request.get("subject");
            String grade = (String) request.get("grade");

            if (questionId == null) {
                return super.failure("题目ID不能为空");
            }

            // 获取题目信息
            Qu question = quService.getById(questionId);
            if (question == null) {
                return super.failure("题目不存在");
            }

            // 调用AI识别
            Map<String, Object> aiResult = callAIForIdentification(question, subject, grade);
            
            if (aiResult != null) {
                return super.success(aiResult);
            } else {
                return super.failure("AI识别失败");
            }

        } catch (Exception e) {
            return super.failure("识别失败: " + e.getMessage());
        }
    }

    /**
     * 批量题目AI识别大纲
     */
    @PostMapping("/identify-batch")
    public ApiRest<?> identifyBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> questionIds = (List<String>) request.get("questionIds");
            String subject = (String) request.get("subject");
            String grade = (String) request.get("grade");

            if (questionIds == null || questionIds.isEmpty()) {
                return super.failure("题目ID列表不能为空");
            }

            Map<String, Object> batchResult = new HashMap<>();
            int successCount = 0;
            int failCount = 0;

            for (String questionId : questionIds) {
                try {
                    Qu question = quService.getById(questionId);
                    if (question != null) {
                        Map<String, Object> aiResult = callAIForIdentification(question, subject, grade);
                        if (aiResult != null) {
                            batchResult.put(questionId, aiResult);
                            successCount++;
                        } else {
                            failCount++;
                        }
                    } else {
                        failCount++;
                    }
                    
                    // 避免请求过快
                    Thread.sleep(200);
                    
                } catch (Exception e) {
                    failCount++;
                    System.err.println("处理题目 " + questionId + " 失败: " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("results", batchResult);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCount", questionIds.size());

            return super.success(result);

        } catch (Exception e) {
            return super.failure("批量识别失败: " + e.getMessage());
        }
    }

    /**
     * 保存识别结果到数据库
     */
    @PostMapping("/save-mapping")
    public ApiRest<String> saveMapping(@RequestBody Map<String, Object> request) {
        try {
            String questionId = (String) request.get("questionId");
            String outlineId = (String) request.get("outlineId");

            if (questionId == null || outlineId == null) {
                return super.failure("题目ID和大纲ID不能为空");
            }

            boolean saved = mappingService.saveOrUpdateMapping(questionId, outlineId);
            if (saved) {
                return super.success("保存成功");
            } else {
                return super.failure("保存失败");
            }

        } catch (Exception e) {
            return super.failure("保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取题目的大纲映射
     */
    @GetMapping("/get-mapping/{questionId}")
    public ApiRest<?> getMapping(@PathVariable String questionId) {
        try {
            Object mapping = mappingService.getByQuestionId(questionId);
            return super.success(mapping);
        } catch (Exception e) {
            return super.failure("获取映射失败: " + e.getMessage());
        }
    }

    /**
     * 调用AI进行大纲识别 (直接服务调用)
     */
    private Map<String, Object> callAIForIdentification(Qu question, String subject, String grade) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("questionContent", question.getContent());
            requestBody.put("questionType", question.getQuType());
            requestBody.put("subject", subject);
            requestBody.put("grade", grade);

            String result = aiProcessingService.identifyOutline(requestBody);
            if (result != null) {
                // 简单解析JSON结果为Map (实际项目中应使用更robust的JSON解析)
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("result", result);
                return resultMap;
            } else {
                System.err.println("AI大纲识别失败");
                return null;
            }

        } catch (Exception e) {
            System.err.println("调用AI服务失败: " + e.getMessage());
            return null;
        }
    }
}