package com.yf.exam.modules.outline.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;

import java.util.List;

/**
 * 知识大纲服务接口 - 简化版
 * Simple Knowledge Outline Service
 */
public interface KnowledgeOutlineService extends IService<KnowledgeOutline> {

    /**
     * 根据学科和年级获取大纲列表
     */
    List<KnowledgeOutline> getBySubjectAndGrade(String subject, String grade);

    /**
     * 获取所有启用的大纲
     */
    List<KnowledgeOutline> getAllActive();

    /**
     * 根据知识点名称查找大纲
     */
    KnowledgeOutline getByKnowledgePoint(String subject, String grade, String knowledgePoint);

    /**
     * 获取所有学科列表
     */
    List<String> getAllSubjects();

    /**
     * 根据学科获取年级列表
     */
    List<String> getGradesBySubject(String subject);

    /**
     * 根据学科和年级获取大纲列表 (别名方法)
     */
    List<KnowledgeOutline> getOutlinesBySubjectAndGrade(String subject, String grade);

    /**
     * 获取所有启用的大纲 (别名方法)
     */
    List<KnowledgeOutline> getAllActiveOutlines();
}