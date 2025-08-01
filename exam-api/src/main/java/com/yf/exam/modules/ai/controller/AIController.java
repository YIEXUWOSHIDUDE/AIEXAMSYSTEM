package com.yf.exam.modules.ai.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.ai.service.AIProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

/**
 * 统一AI控制器 - 替代原LLM模块
 * Unified AI Controller - Replaces LLM module
 */
@RestController
@RequestMapping("/api/ai")
public class AIController extends BaseController {

    @Autowired
    private AIProcessingService aiProcessingService;

    /**
     * 题目提取 - 替代 /api/llm/Extractquestions
     */
    @PostMapping("/extract-questions")
    public ApiRest<String> extractQuestions(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return super.failure("内容不能为空");
            }

            String result = aiProcessingService.extractQuestions(content);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("题目提取失败");
            }
        } catch (Exception e) {
            return super.failure("题目提取异常: " + e.getMessage());
        }
    }

    /**
     * 智能题目提取 - 自动检测文档结构并选择最佳提取方法
     */
    @PostMapping("/extract-questions-intelligent")
    public ApiRest<String> extractQuestionsIntelligent(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return super.failure("内容不能为空");
            }

            String result = aiProcessingService.extractQuestionsIntelligent(content);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("智能题目提取失败");
            }
        } catch (Exception e) {
            return super.failure("智能题目提取异常: " + e.getMessage());
        }
    }

    /**
     * 增强题目提取 - 支持图片和文字混合内容
     */
    @PostMapping("/extract-questions-with-images")
    public ApiRest<String> extractQuestionsWithImages(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            @SuppressWarnings("unchecked")
            List<String> images = (List<String>) request.get("images");
            
            // 内容和图片至少要有一个
            if ((content == null || content.trim().isEmpty()) && 
                (images == null || images.isEmpty())) {
                return super.failure("文字内容和图片至少要提供一个");
            }

            String result = aiProcessingService.extractQuestionsWithImages(content, images);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("多模态题目提取失败");
            }
        } catch (Exception e) {
            return super.failure("多模态题目提取异常: " + e.getMessage());
        }
    }

    /**
     * 题目选择 - 替代 /api/llm/selectquestions
     */
    @PostMapping("/select-questions")
    public ApiRest<String> selectQuestions(@RequestBody Map<String, Object> request) {
        try {
            String result = aiProcessingService.selectQuestions(request);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("题目选择失败");
            }
        } catch (Exception e) {
            return super.failure("题目选择异常: " + e.getMessage());
        }
    }

    /**
     * 轻量级题目选择 - 替代 /api/llm/selectlightweight
     */
    @PostMapping("/select-lightweight")
    public ApiRest<String> selectLightweight(@RequestBody Map<String, Object> request) {
        try {
            String result = aiProcessingService.selectLightweightQuestions(request);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("轻量级选择失败");
            }
        } catch (Exception e) {
            return super.failure("轻量级选择异常: " + e.getMessage());
        }
    }

    /**
     * 题干提取 - 替代 /api/llm/extractstem
     */
    @PostMapping("/extract-stem")
    public ApiRest<String> extractStem(@RequestBody Map<String, Object> request) {
        try {
            String questionContent = (String) request.get("questionContent");
            if (questionContent == null || questionContent.trim().isEmpty()) {
                return super.failure("题目内容不能为空");
            }

            String result = aiProcessingService.extractStem(questionContent);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("题干提取失败");
            }
        } catch (Exception e) {
            return super.failure("题干提取异常: " + e.getMessage());
        }
    }

    /**
     * 知识点识别 - 替代 /api/llm/identifyknowledge
     */
    @PostMapping("/identify-knowledge")
    public ApiRest<String> identifyKnowledge(@RequestBody Map<String, Object> request) {
        try {
            String questionContent = (String) request.get("questionContent");
            if (questionContent == null || questionContent.trim().isEmpty()) {
                return super.failure("题目内容不能为空");
            }

            String result = aiProcessingService.identifyKnowledge(questionContent);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("知识点识别失败");
            }
        } catch (Exception e) {
            return super.failure("知识点识别异常: " + e.getMessage());
        }
    }

    /**
     * 知识点识别（带约束） - 限制从指定学科年级选择
     */
    @PostMapping("/identify-knowledge-with-constraints")
    public ApiRest<String> identifyKnowledgeWithConstraints(@RequestBody Map<String, Object> request) {
        try {
            String questionContent = (String) request.get("questionContent");
            String subject = (String) request.get("subject");
            String grade = (String) request.get("grade");
            
            if (questionContent == null || questionContent.trim().isEmpty()) {
                return super.failure("题目内容不能为空");
            }
            
            if (subject == null || grade == null) {
                return super.failure("学科和年级不能为空");
            }

            String result = aiProcessingService.identifyKnowledgeWithConstraints(questionContent, subject, grade);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("约束知识点识别失败");
            }
        } catch (Exception e) {
            return super.failure("约束知识点识别异常: " + e.getMessage());
        }
    }


    /**
     * 简答题判分 - 替代 /api/llm/judge/short-answer
     */
    @PostMapping("/judge/short-answer")
    public ApiRest<String> judgeShortAnswer(@RequestBody Map<String, Object> request) {
        try {
            String result = aiProcessingService.judgeShortAnswer(request);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("简答题判分失败");
            }
        } catch (Exception e) {
            return super.failure("简答题判分异常: " + e.getMessage());
        }
    }

    /**
     * 整体测试判分 - 替代 /api/llm/judge/overall-test
     */
    @PostMapping("/judge/overall-test")
    public ApiRest<String> judgeOverallTest(@RequestBody Map<String, Object> request) {
        try {
            String result = aiProcessingService.judgeOverallTest(request);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("整体测试判分失败");
            }
        } catch (Exception e) {
            return super.failure("整体测试判分异常: " + e.getMessage());
        }
    }

    /**
     * 大纲识别 - 替代 /api/llm/identify-outline
     */
    @PostMapping("/identify-outline")
    public ApiRest<String> identifyOutline(@RequestBody Map<String, Object> request) {
        try {
            String result = aiProcessingService.identifyOutline(request);
            if (result != null) {
                return super.success(result);
            } else {
                return super.failure("大纲识别失败");
            }
        } catch (Exception e) {
            return super.failure("大纲识别异常: " + e.getMessage());
        }
    }
}