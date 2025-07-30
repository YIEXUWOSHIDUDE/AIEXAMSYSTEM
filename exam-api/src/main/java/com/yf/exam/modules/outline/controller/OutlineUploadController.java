package com.yf.exam.modules.outline.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.outline.service.OutlineUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识大纲上传控制器
 * Outline Upload Controller
 */
@RestController
@RequestMapping("/exam/api/outline")
public class OutlineUploadController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OutlineUploadController.class);

    @Autowired
    private OutlineUploadService outlineUploadService;

    /**
     * 上传知识大纲文档 (Word/PDF)
     */
    @PostMapping("/upload")
    public ApiRest<?> uploadOutline(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject,
            @RequestParam("grade") String grade) {
        
        try {
            if (file.isEmpty()) {
                return super.failure("请选择要上传的文件");
            }

            // 验证文件类型
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".docx") && !fileName.toLowerCase().endsWith(".pdf"))) {
                return super.failure("只支持上传 .docx 或 .pdf 格式的文件");
            }

            // 验证文件大小 (10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return super.failure("文件大小不能超过10MB");
            }

            // 验证参数
            if (subject == null || subject.trim().isEmpty()) {
                return super.failure("请选择学科");
            }
            if (grade == null || grade.trim().isEmpty()) {
                return super.failure("请选择年级");
            }

            // 处理文件上传
            return outlineUploadService.handleOutlineUpload(file, subject, grade);
            
        } catch (Exception e) {
            logger.error("大纲文档上传失败", e);
            return super.failure("上传失败: " + e.getMessage());
        }
    }
}