package com.yf.exam.modules.outline.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yf.exam.modules.outline.entity.QuestionOutlineMapping;

import java.util.List;

/**
 * 题目大纲映射服务接口 - 简化版
 * Simple Question Outline Mapping Service
 */
public interface QuestionOutlineMappingService extends IService<QuestionOutlineMapping> {

    /**
     * 根据题目ID获取映射
     */
    QuestionOutlineMapping getByQuestionId(String questionId);

    /**
     * 根据大纲ID获取所有映射
     */
    List<QuestionOutlineMapping> getByOutlineId(String outlineId);

    /**
     * 检查题目是否已有映射
     */
    boolean hasMapping(String questionId);

    /**
     * 删除题目的所有映射
     */
    boolean deleteByQuestionId(String questionId);

    /**
     * 创建或更新映射
     */
    boolean saveOrUpdateMapping(String questionId, String outlineId);
}