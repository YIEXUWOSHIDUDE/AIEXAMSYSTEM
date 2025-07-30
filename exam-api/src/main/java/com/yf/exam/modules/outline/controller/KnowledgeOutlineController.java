package com.yf.exam.modules.outline.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.outline.entity.KnowledgeOutline;
import com.yf.exam.modules.outline.service.KnowledgeOutlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 知识大纲控制器 - 简化版
 * Simple Knowledge Outline Controller  
 */
@RestController
@RequestMapping("/exam/api/outline")
public class KnowledgeOutlineController extends BaseController {

    @Autowired
    private KnowledgeOutlineService knowledgeOutlineService;

    /**
     * 获取所有大纲列表
     */
    @GetMapping("/list")
    public ApiRest<List<KnowledgeOutline>> list() {
        List<KnowledgeOutline> list = knowledgeOutlineService.getAllActive();
        return super.success(list);
    }

    /**
     * 根据学科和年级获取大纲
     */
    @GetMapping("/list/{subject}/{grade}")
    public ApiRest<List<KnowledgeOutline>> listBySubjectAndGrade(
            @PathVariable String subject, 
            @PathVariable String grade) {
        List<KnowledgeOutline> list = knowledgeOutlineService.getBySubjectAndGrade(subject, grade);
        return super.success(list);
    }

    /**
     * 获取所有学科
     */
    @GetMapping("/subjects")
    public ApiRest<List<String>> getSubjects() {
        List<String> subjects = knowledgeOutlineService.getAllSubjects();
        return super.success(subjects);
    }

    /**
     * 根据学科获取年级
     */
    @GetMapping("/grades/{subject}")
    public ApiRest<List<String>> getGrades(@PathVariable String subject) {
        List<String> grades = knowledgeOutlineService.getGradesBySubject(subject);
        return super.success(grades);
    }

    /**
     * 根据ID获取大纲详情
     */
    @GetMapping("/{id}")
    public ApiRest<KnowledgeOutline> getById(@PathVariable String id) {
        KnowledgeOutline outline = knowledgeOutlineService.getById(id);
        if (outline == null) {
            return super.failure("大纲不存在");
        }
        return super.success(outline);
    }

    /**
     * 创建大纲
     */
    @PostMapping("/create")
    public ApiRest<String> create(@RequestBody KnowledgeOutline outline) {
        try {
            // 生成大纲编码
            String outlineCode = generateOutlineCode(outline.getSubject(), outline.getGrade());
            outline.setOutlineCode(outlineCode);
            outline.setIsActive(1);
            outline.setCreateTime(new Date());
            outline.setUpdateTime(new Date());

            boolean saved = knowledgeOutlineService.save(outline);
            if (saved) {
                return super.success("创建成功");
            } else {
                return super.failure("创建失败");
            }
        } catch (Exception e) {
            return super.failure("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新大纲
     */
    @PostMapping("/update")
    public ApiRest<String> update(@RequestBody KnowledgeOutline outline) {
        try {
            if (outline.getId() == null) {
                return super.failure("ID不能为空");
            }

            outline.setUpdateTime(new Date());
            boolean updated = knowledgeOutlineService.updateById(outline);
            if (updated) {
                return super.success("更新成功");
            } else {
                return super.failure("更新失败");
            }
        } catch (Exception e) {
            return super.failure("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除大纲
     */
    @PostMapping("/delete/{id}")
    public ApiRest<String> delete(@PathVariable String id) {
        try {
            boolean deleted = knowledgeOutlineService.removeById(id);
            if (deleted) {
                return super.success("删除成功");
            } else {
                return super.failure("删除失败");
            }
        } catch (Exception e) {
            return super.failure("删除失败: " + e.getMessage());
        }
    }

    /**
     * 生成大纲编码
     */
    private String generateOutlineCode(String subject, String grade) {
        String subjectCode = getSubjectCode(subject);
        long timestamp = System.currentTimeMillis();
        int random = (int)(Math.random() * 1000);
        return subjectCode + "_" + timestamp + "_" + random;
    }

    /**
     * 获取学科编码
     */
    private String getSubjectCode(String subject) {
        switch (subject) {
            case "数学": return "MATH";
            case "物理": return "PHYS";
            case "化学": return "CHEM";
            case "语文": return "CHIN";
            case "英语": return "ENG";
            case "生物": return "BIO";
            case "历史": return "HIST";
            case "地理": return "GEO";
            default: return "OTHER";
        }
    }
}