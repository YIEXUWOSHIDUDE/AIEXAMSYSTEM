package com.yf.exam.modules.qu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

/**
* <p>
* 候选答案实体类
* </p>
*
* @author 聪明笨狗
* @since 2020-05-25 13:23
*/
@Data
@TableName("el_qu_answer")
public class QuAnswer extends Model<QuAnswer> {

    private static final long serialVersionUID = 1L;

    /**
     * 答案ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 问题ID
     */
    @TableField("qu_id")
    private String quId;

    /**
     * 是否正确
     */
    @TableField("is_right")
    private Boolean isRight;

    /**
     * 选项图片
     */
    private String image;
    
    /**
     * 多图片支持 - JSON数组格式存储多张图片URL
     */
    @TableField("image_refs")
    private String imageRefs;

    /**
     * 答案内容
     */
    private String content;


    /**
     * 答案分析
     */
    private String analysis;
    
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
