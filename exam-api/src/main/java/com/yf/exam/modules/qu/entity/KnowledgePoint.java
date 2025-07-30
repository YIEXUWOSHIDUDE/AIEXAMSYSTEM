package com.yf.exam.modules.qu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;

/**
 * <p>
 * 知识点词典实体类
 * </p>
 *
 * @author AI Assistant
 * @since 2025-07-24
 */
@Data
@TableName("el_knowledge_point")
public class KnowledgePoint extends Model<KnowledgePoint> {

    private static final long serialVersionUID = 1L;

    /**
     * 知识点ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 知识点名称
     */
    private String name;

    /**
     * 知识点分类
     */
    private String category;

    /**
     * 知识点描述
     */
    private String description;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private Date updateTime;
}