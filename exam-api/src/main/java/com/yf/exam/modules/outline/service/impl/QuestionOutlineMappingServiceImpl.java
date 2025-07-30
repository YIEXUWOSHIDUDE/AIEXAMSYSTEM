package com.yf.exam.modules.outline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.modules.outline.entity.QuestionOutlineMapping;
import com.yf.exam.modules.outline.mapper.QuestionOutlineMappingMapper;
import com.yf.exam.modules.outline.service.QuestionOutlineMappingService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 题目大纲映射服务实现 - 简化版
 * Simple Question Outline Mapping Service Implementation
 */
@Service
public class QuestionOutlineMappingServiceImpl extends ServiceImpl<QuestionOutlineMappingMapper, QuestionOutlineMapping>
        implements QuestionOutlineMappingService {

    @Override
    public QuestionOutlineMapping getByQuestionId(String questionId) {
        QueryWrapper<QuestionOutlineMapping> wrapper = new QueryWrapper<>();
        wrapper.eq("question_id", questionId);
        return this.getOne(wrapper);
    }

    @Override
    public List<QuestionOutlineMapping> getByOutlineId(String outlineId) {
        QueryWrapper<QuestionOutlineMapping> wrapper = new QueryWrapper<>();
        wrapper.eq("outline_id", outlineId);
        return this.list(wrapper);
    }

    @Override
    public boolean hasMapping(String questionId) {
        QueryWrapper<QuestionOutlineMapping> wrapper = new QueryWrapper<>();
        wrapper.eq("question_id", questionId);
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean deleteByQuestionId(String questionId) {
        QueryWrapper<QuestionOutlineMapping> wrapper = new QueryWrapper<>();
        wrapper.eq("question_id", questionId);
        return this.remove(wrapper);
    }

    @Override
    public boolean saveOrUpdateMapping(String questionId, String outlineId) {
        // 先删除现有映射
        deleteByQuestionId(questionId);
        
        // 创建新映射
        QuestionOutlineMapping mapping = new QuestionOutlineMapping(questionId, outlineId);
        mapping.setCreateTime(new Date());
        
        return this.save(mapping);
    }
}