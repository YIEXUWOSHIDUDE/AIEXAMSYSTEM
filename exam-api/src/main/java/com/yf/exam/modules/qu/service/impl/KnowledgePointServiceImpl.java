package com.yf.exam.modules.qu.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.yf.exam.modules.qu.mapper.QuMapper;
import com.yf.exam.modules.qu.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识点服务实现类
 * 
 * @author AI Assistant
 * @date 2025-07-25
 */
@Service
public class KnowledgePointServiceImpl implements KnowledgePointService {

    @Autowired
    private QuMapper quMapper;

    @Override
    public List<String> getKnowledgePointsByRepo(String repoId) {
        return extractKnowledgePoints(quMapper.getKnowledgePointsByRepo(repoId));
    }

    @Override
    public List<String> getKnowledgePointsByRepoAndType(String repoId, Integer quType) {
        return extractKnowledgePoints(quMapper.getKnowledgePointsByRepoAndType(repoId, quType));
    }

    /**
     * 从知识点JSON字符串列表中提取所有唯一的知识点（仅取每个题目的第一个知识点）
     * @param knowledgePointsJsonList 知识点JSON字符串列表
     * @return 去重后的知识点列表
     */
    private List<String> extractKnowledgePoints(List<String> knowledgePointsJsonList) {
        Set<String> uniqueKnowledgePoints = new HashSet<>();
        
        for (String jsonStr : knowledgePointsJsonList) {
            if (StringUtils.hasText(jsonStr)) {
                try {
                    JSONArray jsonArray = JSONArray.parseArray(jsonStr);
                    // 只取第一个知识点
                    if (jsonArray.size() > 0) {
                        String firstKnowledgePoint = jsonArray.getString(0);
                        if (StringUtils.hasText(firstKnowledgePoint)) {
                            uniqueKnowledgePoints.add(firstKnowledgePoint.trim());
                        }
                    }
                } catch (Exception e) {
                    // 如果JSON解析失败，跳过这个记录
                    System.err.println("Failed to parse knowledge points JSON: " + jsonStr);
                }
            }
        }
        
        // 转换为List并排序
        return uniqueKnowledgePoints.stream()
                .sorted()
                .collect(Collectors.toList());
    }
}