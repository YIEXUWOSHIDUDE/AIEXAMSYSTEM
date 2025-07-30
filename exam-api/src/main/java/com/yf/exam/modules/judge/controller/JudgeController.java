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
                    // Parse and validate response
                    JSONObject judgeResult = JSONObject.parseObject(response);
                    
                    // Ensure user-specified maxScore is preserved
                    judgeResult.put("maxScore", request.getMaxScore());
                    
                    System.out.println("✅ Short answer judging completed successfully");
                    return super.success(judgeResult);
                } catch (Exception parseError) {
                    System.err.println("❌ Failed to parse LLM response: " + parseError.getMessage());
                    return super.failure("评判结果解析失败");
                }
            } else {
                System.err.println("❌ LLM service returned error: " + response.getStatusCode());
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
                System.err.println("❌ LLM service returned error: " + response.getStatusCode());
                return super.failure("AI分析服务暂时不可用");
            }

        } catch (Exception e) {
            System.err.println("❌ Overall test judging error: " + e.getMessage());
            return super.failure("分析服务异常: " + e.getMessage());
        }
    }

    /**
     * 批量简答题评判（可选功能）
     */
    @PostMapping("/batch-short-answer")
    public ApiRest<?> judgeBatchShortAnswer(@RequestBody BatchShortAnswerJudgeDTO request) {
        try {
            System.out.println("🎯 Received batch short answer judging request:");
            System.out.println("  📊 Questions count: " + request.getQuestions().size());

            // Validate input
            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                return super.failure("简答题列表不能为空");
            }

            // Process each question individually to maintain accuracy
            Map<String, Object> batchResults = new HashMap<>();
            int successCount = 0;
            int totalCount = request.getQuestions().size();

            for (int i = 0; i < request.getQuestions().size(); i++) {
                ShortAnswerJudgeDTO question = request.getQuestions().get(i);
                String questionKey = question.getQuestionId() != null ? question.getQuestionId() : "q" + (i + 1);
                
                try {
                    // Call individual judging
                    ApiRest<?> judgeResult = judgeShortAnswer(question);
                    if (judgeResult.getCode() == 0) {
                        batchResults.put(questionKey, judgeResult.getData());
                        successCount++;
                    } else {
                        // Create error result for this question
                        Map<String, Object> errorResult = new HashMap<>();
                        errorResult.put("score", 0.0);
                        errorResult.put("maxScore", question.getMaxScore());
                        errorResult.put("feedback", "该题评判失败: " + judgeResult.getMsg());
                        errorResult.put("keyPointsCovered", new ArrayList<String>());
                        errorResult.put("keyPointsMissed", new ArrayList<String>());
                        List<String> suggestions = new ArrayList<>();
                        suggestions.add("请联系老师人工评判");
                        errorResult.put("suggestions", suggestions);
                        batchResults.put(questionKey, errorResult);
                    }
                } catch (Exception e) {
                    System.err.println("❌ Failed to judge question " + questionKey + ": " + e.getMessage());
                    // Create fallback error result
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("score", 0.0);
                    errorResult.put("maxScore", question.getMaxScore());
                    errorResult.put("feedback", "评判异常: " + e.getMessage());
                    errorResult.put("keyPointsCovered", new ArrayList<String>());
                    errorResult.put("keyPointsMissed", new ArrayList<String>());
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("请检查网络连接后重试");
                    errorResult.put("suggestions", suggestions);
                    batchResults.put(questionKey, errorResult);
                }
            }

            Map<String, Object> finalResult = new HashMap<>();
            finalResult.put("totalCount", totalCount);
            finalResult.put("successCount", successCount);
            finalResult.put("failureCount", totalCount - successCount);
            finalResult.put("results", batchResults);

            System.out.println("✅ Batch judging completed: " + successCount + "/" + totalCount);
            return super.success(finalResult);

        } catch (Exception e) {
            System.err.println("❌ Batch judging error: " + e.getMessage());
            return super.failure("批量评判服务异常: " + e.getMessage());
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

    public static class BatchShortAnswerJudgeDTO {
        private List<ShortAnswerJudgeDTO> questions;

        public BatchShortAnswerJudgeDTO() {}

        public List<ShortAnswerJudgeDTO> getQuestions() { return questions; }
        public void setQuestions(List<ShortAnswerJudgeDTO> questions) { this.questions = questions; }
    }
}