package com.yf.exam.ability.upload.controller;

import com.yf.exam.ability.upload.service.impl.AIUploadService;
import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/exam/api/ai-upload")
public class AIUploadController extends BaseController {

    @Autowired
    private AIUploadService aiUploadService;

    /**
     * 上传试卷并自动拆题
     */
    @PostMapping("/upload")
    public ApiRest<?> uploadAndSplit(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "assignment_strategy", required = false, defaultValue = "smart") String assignmentStrategy) {
        return aiUploadService.handleUploadAndSplit(file, subject, grade, assignmentStrategy);
    }
}
