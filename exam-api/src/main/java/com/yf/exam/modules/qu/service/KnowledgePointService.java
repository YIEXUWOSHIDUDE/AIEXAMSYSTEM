package com.yf.exam.modules.qu.service;

import java.util.List;

/**
 * 知识点服务接口
 * 
 * @author AI Assistant
 * @date 2025-07-25
 */
public interface KnowledgePointService {

    /**
     * 获取指定题库中所有题目的知识点列表
     * @param repoId 题库ID
     * @return 知识点列表
     */
    List<String> getKnowledgePointsByRepo(String repoId);

    /**
     * 获取指定题库和题型的知识点列表
     * @param repoId 题库ID
     * @param quType 题目类型
     * @return 知识点列表
     */
    List<String> getKnowledgePointsByRepoAndType(String repoId, Integer quType);
}