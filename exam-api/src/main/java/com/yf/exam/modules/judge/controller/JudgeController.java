package com.yf.exam.modules.judge.controller;

import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.ai.service.AIProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * AI智能评判控制器
 * 提供实时的测试评判和反馈服务
 */
@RestController
@RequestMapping("/exam/api/judge")
public class JudgeController extends BaseController {

    @Autowired
    private AIProcessingService aiProcessingService;

    /**
     * 简答题AI评判
     */
    @PostMapping("/short-answer")
    public ApiRest<?> judgeShortAnswer(@RequestBody ShortAnswerJudgeDTO request) {
        try {
            System.out.println("🎯 Received short answer judging request:");
            System.out.println("  📝 Question: " + request.getQuestionContent());
            System.out.println("  👤 User Answer: " + request.getUserAnswer());
            System.out.println("  📊 Max Score: " + request.getMaxScore());

            // Validate input
            if (request.getQuestionContent() == null || request.getQuestionContent().trim().isEmpty()) {
                return super.failure("题目内容不能为空");
            }
            if (request.getMaxScore() <= 0) {
                return super.failure("分值必须大于0");
            }

            // Build request for LLM service
            Map<String, Object> llmRequest = new HashMap<>();
            llmRequest.put("questionContent", request.getQuestionContent());
            llmRequest.put("userAnswer", request.getUserAnswer() != null ? request.getUserAnswer() : "");
            llmRequest.put("standardAnswer", request.getStandardAnswer() != null ? request.getStandardAnswer() : "");
            llmRequest.put("knowledgePoint", request.getKnowledgePoint() != null ? request.getKnowledgePoint() : "基础知识");
            llmRequest.put("maxScore", request.getMaxScore()); // User-specified max score

            System.out.println("🚀 Calling AI judge service...");
            
            // Call AI judge service directly
            String response = aiProcessingService.judgeShortAnswer(llmRequest);

            if (response != null) {
                try {
                    // Parse response directly - simplified format
                    JSONObject judgeResult = JSONObject.parseObject(response.trim());
                    
                    // Ensure user-specified maxScore is preserved
                    judgeResult.put("maxScore", request.getMaxScore());
                    
                    System.out.println("✅ Short answer judging completed successfully");
                    System.out.println("📊 Result: score=" + judgeResult.get("score") + 
                                     ", grade=" + judgeResult.get("grade") + 
                                     ", comment=" + judgeResult.get("comment"));
                    
                    return super.success(judgeResult);
                } catch (Exception parseError) {
                    System.err.println("❌ Failed to parse LLM response: " + parseError.getMessage());
                    System.err.println("❌ Raw response: " + response);
                    return super.failure("评判结果解析失败: " + parseError.getMessage());
                }
            } else {
                System.err.println("❌ AI service returned null response");
                return super.failure("AI评判服务暂时不可用");
            }

        } catch (Exception e) {
            System.err.println("❌ Short answer judging error: " + e.getMessage());
            return super.failure("评判服务异常: " + e.getMessage());
        }
    }

    /**
     * 整体测试AI分析
     */
    @PostMapping("/overall-test")
    public ApiRest<?> judgeOverallTest(@RequestBody OverallTestJudgeDTO request) {
        try {
            System.out.println("🎯 Received overall test judging request:");
            System.out.println("  📊 Total questions: " + request.getAnswers().size());

            // Validate input
            if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
                return super.failure("测试答案不能为空");
            }

            // Build request for LLM service
            Map<String, Object> llmRequest = new HashMap<>();
            llmRequest.put("answers", request.getAnswers());

            System.out.println("🚀 Calling AI overall analysis service...");
            
            // Call AI judge service directly
            String response = aiProcessingService.judgeOverallTest(llmRequest);

            if (response != null) {
                try {
                    // Parse and return response
                    JSONObject analysisResult = JSONObject.parseObject(response);
                    System.out.println("✅ Overall test analysis completed successfully");
                    return super.success(analysisResult);
                } catch (Exception parseError) {
                    System.err.println("❌ Failed to parse LLM response: " + parseError.getMessage());
                    return super.failure("分析结果解析失败");
                }
            } else {
                System.err.println("❌ AI service returned null response");
                return super.failure("AI分析服务暂时不可用");
            }

        } catch (Exception e) {
            System.err.println("❌ Overall test judging error: " + e.getMessage());
            return super.failure("分析服务异常: " + e.getMessage());
        }
    }



    // DTO classes
    public static class ShortAnswerJudgeDTO {
        private String questionId;
        private String questionContent;
        private String userAnswer;
        private String standardAnswer;
        private String knowledgePoint;
        private double maxScore; // User-specified max score

        public ShortAnswerJudgeDTO() {}

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getQuestionContent() { return questionContent; }
        public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
        public String getStandardAnswer() { return standardAnswer; }
        public void setStandardAnswer(String standardAnswer) { this.standardAnswer = standardAnswer; }
        public String getKnowledgePoint() { return knowledgePoint; }
        public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
        public double getMaxScore() { return maxScore; }
        public void setMaxScore(double maxScore) { this.maxScore = maxScore; }
    }

    public static class OverallTestJudgeDTO {
        private List<TestAnswerItemDTO> answers;

        public OverallTestJudgeDTO() {}

        public List<TestAnswerItemDTO> getAnswers() { return answers; }
        public void setAnswers(List<TestAnswerItemDTO> answers) { this.answers = answers; }
    }

    public static class TestAnswerItemDTO {
        private String questionId;
        private String questionContent;
        private int questionType;
        private String userAnswer;
        private String correctAnswer;
        private String knowledgePoint;
        private double maxScore;

        public TestAnswerItemDTO() {}

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getQuestionContent() { return questionContent; }
        public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }
        public int getQuestionType() { return questionType; }
        public void setQuestionType(int questionType) { this.questionType = questionType; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
        public String getCorrectAnswer() { return correctAnswer; }
        public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
        public String getKnowledgePoint() { return knowledgePoint; }
        public void setKnowledgePoint(String knowledgePoint) { this.knowledgePoint = knowledgePoint; }
        public double getMaxScore() { return maxScore; }
        public void setMaxScore(double maxScore) { this.maxScore = maxScore; }
    }


}