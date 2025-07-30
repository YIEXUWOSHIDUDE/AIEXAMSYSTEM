package com.yf.exam.modules.outline.service;

import com.yf.exam.core.api.ApiRest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识大纲上传服务接口
 * Outline Upload Service Interface
 */
public interface OutlineUploadService {
    
    /**
     * 处理知识大纲文档上传
     * @param file 上传的文件
     * @param subject 学科
     * @param grade 年级
     * @return 处理结果
     */
    ApiRest<?> handleOutlineUpload(MultipartFile file, String subject, String grade);
}