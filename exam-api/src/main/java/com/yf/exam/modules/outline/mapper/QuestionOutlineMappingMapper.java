package com.yf.exam.modules.outline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yf.exam.modules.outline.entity.QuestionOutlineMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目大纲映射Mapper - 简化版
 * Simple Question Outline Mapping Mapper
 */
@Mapper
public interface QuestionOutlineMappingMapper extends BaseMapper<QuestionOutlineMapping> {
    // MyBatis-Plus提供基本CRUD，无需额外方法
}