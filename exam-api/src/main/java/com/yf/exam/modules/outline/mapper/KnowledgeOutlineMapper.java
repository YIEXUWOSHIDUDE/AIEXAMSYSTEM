package com.yf.exam.modules.outline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识大纲Mapper - 简化版
 * Simple Knowledge Outline Mapper
 */
@Mapper
public interface KnowledgeOutlineMapper extends BaseMapper<KnowledgeOutline> {
    // MyBatis-Plus提供基本CRUD，无需额外方法
}