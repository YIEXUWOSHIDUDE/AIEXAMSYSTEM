package com.yf.exam.modules.outline.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识大纲实体 - 简化版
 * Simple Knowledge Outline Entity
 */
@TableName("knowledge_outline")
public class KnowledgeOutline implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    private String outlineCode;
    private String subject;
    private String grade;
    private String knowledgePoint;
    private Integer sortOrder;
    private Integer isActive;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // 无参构造函数
    public KnowledgeOutline() {}

    // 全参构造函数
    public KnowledgeOutline(String outlineCode, String subject, String grade, String knowledgePoint) {
        this.outlineCode = outlineCode;
        this.subject = subject;
        this.grade = grade;
        this.knowledgePoint = knowledgePoint;
        this.sortOrder = 0;
        this.isActive = 1;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOutlineCode() {
        return outlineCode;
    }

    public void setOutlineCode(String outlineCode) {
        this.outlineCode = outlineCode;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getKnowledgePoint() {
        return knowledgePoint;
    }

    public void setKnowledgePoint(String knowledgePoint) {
        this.knowledgePoint = knowledgePoint;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "KnowledgeOutline{" +
                "id='" + id + '\'' +
                ", outlineCode='" + outlineCode + '\'' +
                ", subject='" + subject + '\'' +
                ", grade='" + grade + '\'' +
                ", knowledgePoint='" + knowledgePoint + '\'' +
                '}';
    }
}