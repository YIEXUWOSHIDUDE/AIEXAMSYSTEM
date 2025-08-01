package com.yf.exam.modules.exam.dto.ext;

import com.yf.exam.modules.exam.dto.ExamRepoDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
* <p>
* 考试题库数据传输类
* </p>
*
* @author 聪明笨狗
* @since 2020-09-05 11:14
*/
@Data
@ApiModel(value="考试题库扩展响应类", description="考试题库扩展响应类")
public class ExamRepoExtDTO extends ExamRepoDTO {

    private static final long serialVersionUID = 1L;

    
    @ApiModelProperty(value = "单选题总量", required=true)
    private Integer totalRadio;
    
    @ApiModelProperty(value = "多选题总量", required=true)
    private Integer totalMulti;
    
    @ApiModelProperty(value = "判断题总量", required=true)
    private Integer totalJudge;
    
    @ApiModelProperty(value = "简答题数量", required=false)
    private Integer saqCount;
    
    @ApiModelProperty(value = "简答题分数", required=false)
    private Integer saqScore;
    
    @ApiModelProperty(value = "简答题总量", required=false)
    private Integer totalSaq;
    
    @ApiModelProperty(value = "填空题数量", required=false)
    private Integer gapFillingCount;
    
    @ApiModelProperty(value = "填空题分数", required=false)
    private Integer gapFillingScore;
    
    @ApiModelProperty(value = "填空题总量", required=false)
    private Integer totalGapFilling;
    
    @ApiModelProperty(value = "选定的知识点列表", required=false)
    private List<String> selectedKnowledgePoints;
    
    @ApiModelProperty(value = "是否启用知识点筛选", required=false)
    private Boolean knowledgePointFilterEnabled;
    
    @ApiModelProperty(value = "难度分布方案", required=false)
    private String difficultyScheme;
    
}
