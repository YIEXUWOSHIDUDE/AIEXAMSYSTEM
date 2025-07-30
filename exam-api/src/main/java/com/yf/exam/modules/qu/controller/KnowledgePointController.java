package com.yf.exam.modules.qu.controller;

import com.yf.exam.core.api.ApiRest;
import com.yf.exam.core.api.controller.BaseController;
import com.yf.exam.modules.qu.dto.request.KnowledgePointRepoReqDTO;
import com.yf.exam.modules.qu.dto.request.KnowledgePointRepoTypeReqDTO;
import com.yf.exam.modules.qu.service.KnowledgePointService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识点管理控制器
 * 
 * @author AI Assistant
 * @date 2025-07-25
 */
@Api(tags="知识点管理")
@RestController
@RequestMapping("/exam/api/knowledge-points")
public class KnowledgePointController extends BaseController {

    @Autowired
    private KnowledgePointService knowledgePointService;

    @ApiOperation(value = "获取题库的所有知识点")
    @RequestMapping(value = "/repo", method = {RequestMethod.POST})
    public ApiRest<List<String>> getKnowledgePointsByRepo(
            @ApiParam(value = "题库请求", required = true) @RequestBody KnowledgePointRepoReqDTO reqDTO) {
        List<String> knowledgePoints = knowledgePointService.getKnowledgePointsByRepo(reqDTO.getRepoId());
        return super.success(knowledgePoints);
    }

    @ApiOperation(value = "获取题库指定题型的知识点")
    @RequestMapping(value = "/repo-type", method = {RequestMethod.POST})
    public ApiRest<List<String>> getKnowledgePointsByRepoAndType(
            @ApiParam(value = "题库题型请求", required = true) @RequestBody KnowledgePointRepoTypeReqDTO reqDTO) {
        List<String> knowledgePoints = knowledgePointService.getKnowledgePointsByRepoAndType(reqDTO.getRepoId(), reqDTO.getQuType());
        return super.success(knowledgePoints);
    }
}