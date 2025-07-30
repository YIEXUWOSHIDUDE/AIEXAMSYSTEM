package com.yf.exam.modules.outline.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yf.exam.core.api.ApiRest;
import com.yf.exam.modules.ai.service.AIProcessingService;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;
import com.yf.exam.modules.outline.service.KnowledgeOutlineService;
import com.yf.exam.modules.outline.service.OutlineUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识大纲上传服务实现
 * Outline Upload Service Implementation
 */
@Service
public class OutlineUploadServiceImpl implements OutlineUploadService {

    private static final Logger logger = LoggerFactory.getLogger(OutlineUploadServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AIProcessingService aiProcessingService;

    @Autowired
    private KnowledgeOutlineService knowledgeOutlineService;

    @Override
    public ApiRest<?> handleOutlineUpload(MultipartFile file, String subject, String grade) {
        try {
            logger.info("🎯 开始处理知识大纲上传: 文件={}, 学科={}, 年级={}", 
                file.getOriginalFilename(), subject, grade);

            // 1. 先调用Python服务提取文本内容
            String textContent = extractTextFromFile(file);
            if (textContent == null || textContent.trim().isEmpty()) {
                return createFailureResponse("文件解析失败，无法提取文本内容");
            }

            // 2. 调用AI服务解析知识大纲结构
            String outlineStructure = parseOutlineStructure(textContent, subject, grade);
            if (outlineStructure == null || outlineStructure.trim().isEmpty()) {
                return createFailureResponse("AI解析大纲结构失败");
            }

            // 3. 解析AI返回的JSON数据并保存到数据库
            int importedCount = saveOutlinesToDatabase(outlineStructure, subject, grade);

            // 4. 返回成功结果
            Map<String, Object> result = new HashMap<>();
            result.put("importedCount", importedCount);
            result.put("subject", subject);
            result.put("grade", grade);

            ApiRest<Map<String, Object>> apiRest = new ApiRest<>();
            apiRest.setCode(0);
            apiRest.setMsg("成功导入 " + importedCount + " 个知识点");
            apiRest.setData(result);
            
            logger.info("✅ 知识大纲上传完成: 导入{}个知识点", importedCount);
            return apiRest;

        } catch (Exception e) {
            logger.error("❌ 知识大纲上传失败", e);
            return createFailureResponse("上传处理异常: " + e.getMessage());
        }
    }

    /**
     * 调用Python服务提取文件文本内容
     */
    private String extractTextFromFile(MultipartFile file) {
        try {
            String pythonUrl = "http://localhost:8003/api/extract_text_content";
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // 转换MultipartFile为Resource
            Resource fileResource = new MultipartFileResource(file);
            body.add("file", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject responseObj = JSONObject.parseObject(response.getBody());
                return responseObj.getString("textContent");
            } else {
                logger.error("Python服务调用失败: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("调用Python服务提取文本失败", e);
            return null;
        }
    }

    /**
     * 调用AI服务解析知识大纲结构
     */
    private String parseOutlineStructure(String textContent, String subject, String grade) {
        try {
            String prompt = buildOutlineParsingPrompt(textContent, subject, grade);
            return aiProcessingService.extractOutlineStructure(prompt);
        } catch (Exception e) {
            logger.error("AI解析大纲结构失败", e);
            return null;
        }
    }

    /**
     * 构建大纲解析提示词
     */
    private String buildOutlineParsingPrompt(String textContent, String subject, String grade) {
        return "请从以下文档内容中提取知识大纲结构，按照JSON格式返回：\n\n" +
               "学科：" + subject + "\n" +
               "年级：" + grade + "\n\n" +
               "文档内容：\n" + textContent + "\n\n" +
               "要求：\n" +
               "1. 识别文档中的知识点层次结构\n" +
               "2. 按照以下JSON格式返回：\n" +
               "[\n" +
               "  {\n" +
               "    \"knowledgePoint\": \"知识点名称\",\n" +
               "    \"outlineCode\": \"自动生成编码\",\n" +
               "    \"sortOrder\": 1\n" +
               "  }\n" +
               "]\n" +
               "3. 只返回JSON数组，不要其他内容\n" +
               "4. 确保每个知识点都有独特的名称和合理的排序";
    }

    /**
     * 保存大纲数据到数据库
     */
    private int saveOutlinesToDatabase(String outlineJson, String subject, String grade) {
        try {
            JSONArray outlines = JSONArray.parseArray(outlineJson);
            int importedCount = 0;

            for (Object item : outlines) {
                if (item instanceof JSONObject) {
                    JSONObject outlineObj = (JSONObject) item;
                    
                    KnowledgeOutline outline = new KnowledgeOutline();
                    outline.setSubject(subject);
                    outline.setGrade(grade);
                    outline.setKnowledgePoint(outlineObj.getString("knowledgePoint"));
                    
                    // 生成大纲编码
                    String outlineCode = generateOutlineCode(subject, grade, importedCount + 1);
                    outline.setOutlineCode(outlineCode);
                    
                    outline.setSortOrder(outlineObj.getInteger("sortOrder"));
                    outline.setIsActive(1);
                    outline.setCreateTime(new Date());
                    outline.setUpdateTime(new Date());

                    // 检查是否已存在相同的知识点
                    KnowledgeOutline existing = knowledgeOutlineService.getByKnowledgePoint(
                        subject, grade, outline.getKnowledgePoint());
                    
                    if (existing == null) {
                        if (knowledgeOutlineService.save(outline)) {
                            importedCount++;
                            logger.info("📝 导入知识点: {}", outline.getKnowledgePoint());
                        }
                    } else {
                        logger.info("⏭️ 跳过重复知识点: {}", outline.getKnowledgePoint());
                    }
                }
            }

            return importedCount;
        } catch (Exception e) {
            logger.error("保存大纲数据失败", e);
            return 0;
        }
    }

    /**
     * 生成大纲编码
     */
    private String generateOutlineCode(String subject, String grade, int sequence) {
        String subjectCode = getSubjectCode(subject);
        String gradeCode = getGradeCode(grade);
        return String.format("%s_%s_%03d", subjectCode, gradeCode, sequence);
    }

    private String getSubjectCode(String subject) {
        switch (subject) {
            case "数学": return "MATH";
            case "物理": return "PHY";
            case "化学": return "CHEM";
            case "语文": return "LAN";
            case "英语": return "ENG";
            case "生物": return "BIO";
            case "历史": return "HIS";
            case "地理": return "GEO";
            default: return "OTH";
        }
    }

    private String getGradeCode(String grade) {
        switch (grade) {
            case "七年级": return "G7";
            case "八年级": return "G8";
            case "九年级": return "G9";
            case "高一": return "H1";
            case "高二": return "H2";
            case "高三": return "H3";
            default: return "GX";
        }
    }

    private ApiRest<Object> createFailureResponse(String message) {
        ApiRest<Object> apiRest = new ApiRest<>();
        apiRest.setCode(1);
        apiRest.setMsg(message);
        apiRest.setData(null);
        return apiRest;
    }

    /**
     * MultipartFile到Resource的适配器
     */
    private static class MultipartFileResource implements Resource {
        private final MultipartFile multipartFile;

        public MultipartFileResource(MultipartFile multipartFile) {
            this.multipartFile = multipartFile;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public boolean isReadable() {
            return true;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public java.net.URL getURL() throws java.io.IOException {
            throw new java.io.FileNotFoundException("MultipartFile resource cannot be resolved to URL");
        }

        @Override
        public java.net.URI getURI() throws java.io.IOException {
            throw new java.io.FileNotFoundException("MultipartFile resource cannot be resolved to URI");
        }

        @Override
        public java.io.File getFile() throws java.io.IOException {
            throw new java.io.FileNotFoundException("MultipartFile resource cannot be resolved to absolute file path");
        }

        @Override
        public long contentLength() throws java.io.IOException {
            return multipartFile.getSize();
        }

        @Override
        public long lastModified() throws java.io.IOException {
            return System.currentTimeMillis();
        }

        @Override
        public Resource createRelative(String relativePath) throws java.io.IOException {
            throw new java.io.IOException("Cannot create a relative resource for a MultipartFile resource");
        }

        @Override
        public String getFilename() {
            return multipartFile.getOriginalFilename();
        }

        @Override
        public String getDescription() {
            return "MultipartFile resource [" + multipartFile.getOriginalFilename() + "]";
        }

        @Override
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return multipartFile.getInputStream();
        }
    }
}