package com.yf.exam.modules.outline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;
import com.yf.exam.modules.outline.mapper.KnowledgeOutlineMapper;
import com.yf.exam.modules.outline.service.KnowledgeOutlineService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识大纲服务实现 - 简化版
 * Simple Knowledge Outline Service Implementation
 */
@Service
public class KnowledgeOutlineServiceImpl extends ServiceImpl<KnowledgeOutlineMapper, KnowledgeOutline> 
        implements KnowledgeOutlineService {

    @Override
    public List<KnowledgeOutline> getBySubjectAndGrade(String subject, String grade) {
        QueryWrapper<KnowledgeOutline> wrapper = new QueryWrapper<>();
        wrapper.eq("subject", subject)
               .eq("grade", grade)
               .eq("is_active", 1)
               .orderByAsc("sort_order");
        return this.list(wrapper);
    }

    @Override
    public List<KnowledgeOutline> getAllActive() {
        QueryWrapper<KnowledgeOutline> wrapper = new QueryWrapper<>();
        wrapper.eq("is_active", 1)
               .orderByAsc("subject", "grade", "sort_order");
        return this.list(wrapper);
    }

    @Override
    public KnowledgeOutline getByKnowledgePoint(String subject, String grade, String knowledgePoint) {
        QueryWrapper<KnowledgeOutline> wrapper = new QueryWrapper<>();
        wrapper.eq("subject", subject)
               .eq("grade", grade)
               .eq("knowledge_point", knowledgePoint)
               .eq("is_active", 1);
        return this.getOne(wrapper);
    }

    @Override
    public List<String> getAllSubjects() {
        QueryWrapper<KnowledgeOutline> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT subject")
               .eq("is_active", 1)
               .orderByAsc("subject");
        return this.list(wrapper).stream()
                .map(KnowledgeOutline::getSubject)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getGradesBySubject(String subject) {
        QueryWrapper<KnowledgeOutline> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT grade")
               .eq("subject", subject)
               .eq("is_active", 1)
               .orderByAsc("grade");
        return this.list(wrapper).stream()
                .map(KnowledgeOutline::getGrade)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeOutline> getOutlinesBySubjectAndGrade(String subject, String grade) {
        return getBySubjectAndGrade(subject, grade);
    }

    @Override
    public List<KnowledgeOutline> getAllActiveOutlines() {
        return getAllActive();
    }
}