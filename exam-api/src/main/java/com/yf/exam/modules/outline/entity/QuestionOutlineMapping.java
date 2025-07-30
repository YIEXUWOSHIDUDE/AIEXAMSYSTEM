package com.yf.exam.modules.outline.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目大纲映射实体 - 简化版
 * Simple Question Outline Mapping Entity
 */
@TableName("question_outline_mapping")
public class QuestionOutlineMapping implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String questionId;
    private String outlineId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // 无参构造函数
    public QuestionOutlineMapping() {}

    // 构造函数
    public QuestionOutlineMapping(String questionId, String outlineId) {
        this.questionId = questionId;
        this.outlineId = outlineId;
        this.createTime = new Date();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getOutlineId() {
        return outlineId;
    }

    public void setOutlineId(String outlineId) {
        this.outlineId = outlineId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "QuestionOutlineMapping{" +
                "id='" + id + '\'' +
                ", questionId='" + questionId + '\'' +
                ", outlineId='" + outlineId + '\'' +
                '}';
    }
}