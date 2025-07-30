package com.yf.exam.modules.qu.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识点题库请求类
 * 
 * @author AI Assistant
 * @date 2025-07-25
 */
@Data
@ApiModel(value = "知识点题库请求类", description = "知识点题库请求类")
public class KnowledgePointRepoReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "题库ID", required = true)
    private String repoId;
}