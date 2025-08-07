package com.yf.exam.modules.qu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
* <p>
* 问题题目实体类
* </p>
*
* @author 聪明笨狗
* @since 2020-05-25 13:23
*/
@Data
@TableName("el_qu")
public class Qu extends Model<Qu> {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 题目类型
     */
    @TableField("qu_type")
    private Integer quType;

    /**
     * 1普通,2较难
     */
    private Integer level;

    /**
     * 题目图片
     */
    private String image;
    
    /**
     * 多图片支持 - JSON数组格式存储多张图片URL
     */
    @TableField("image_refs")
    private String imageRefs;

    /**
     * 题目内容
     */
    private String content;

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

    /**
     * 题目备注
     */
    private String remark;

    /**
     * 整题解析
     */
    private String analysis;
    
    /**
     * 提取的题干内容
     */
    @TableField("question_stem")
    private String questionStem;
    
    /**
     * 知识点标签JSON数组
     */
    @TableField("knowledge_points")
    private String knowledgePoints;
    
    /**
     * 提取状态：0=未处理，1=已提取，2=手动编辑
     */
    @TableField("extraction_status")
    private Integer extractionStatus;
    
    /**
     * 学科
     */
    @TableField("subject")
    private String subject;
    
    /**
     * 年级
     */
    @TableField("grade")
    private String grade;
    
    // 便捷方法：获取解析后的图片列表
    @TableField(exist = false)
    private java.util.List<String> imageList;
    
    /**
     * 获取图片列表（解析JSON）
     */
    public java.util.List<String> getImageList() {
        if (imageList != null) {
            return imageList;
        }
        
        imageList = new java.util.ArrayList<>();
        
        // 优先使用imageRefs
        if (imageRefs != null && !imageRefs.trim().isEmpty()) {
            try {
                com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(imageRefs);
                for (Object obj : array) {
                    if (obj != null) {
                        imageList.add(obj.toString());
                    }
                }
            } catch (Exception e) {
                // JSON解析失败，降级使用单图片
                if (image != null && !image.trim().isEmpty()) {
                    imageList.add(image);
                }
            }
        } else if (image != null && !image.trim().isEmpty()) {
            // 使用传统单图片字段
            imageList.add(image);
        }
        
        return imageList;
    }
    
    /**
     * 设置图片列表（转换为JSON）
     */
    public void setImageList(java.util.List<String> imageList) {
        this.imageList = imageList;
        
        if (imageList == null || imageList.isEmpty()) {
            this.imageRefs = null;
            this.image = null;
        } else {
            // 转换为JSON数组
            this.imageRefs = com.alibaba.fastjson2.JSONArray.toJSONString(imageList);
            // 为了兼容性，设置第一张图片为主图片
            this.image = imageList.get(0);
        }
    }
    
}
