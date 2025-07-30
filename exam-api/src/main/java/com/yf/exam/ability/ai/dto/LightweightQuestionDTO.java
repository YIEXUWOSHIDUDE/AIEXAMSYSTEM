package com.yf.exam.ability.ai.dto;

import lombok.Data;

/**
 * 轻量级题目选择DTO
 * 只包含ID、题干和知识点，用于高效的AI选题处理
 * 
 * @author AI Assistant
 * @date 2025-07-25
 */
@Data
public class LightweightQuestionDTO {
    
    /**
     * 题目ID
     */
    private String id;
    
    /**
     * 题目题干（简化版）
     */
    private String questionStem;
    
    /**
     * 知识点列表（JSON字符串）
     */
    private String knowledgePoints;
    
    /**
     * 题目难度等级
     */
    private Integer level;
    
    public LightweightQuestionDTO(String id, String questionStem, String knowledgePoints, Integer level) {
        this.id = id;
        this.questionStem = questionStem != null ? questionStem : "";
        this.knowledgePoints = knowledgePoints != null ? knowledgePoints : "[]";
        this.level = level != null ? level : 1;
    }
}