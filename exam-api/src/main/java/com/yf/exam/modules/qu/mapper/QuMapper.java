package com.yf.exam.modules.qu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yf.exam.modules.qu.dto.QuDTO;
import com.yf.exam.modules.qu.dto.export.QuExportDTO;
import com.yf.exam.modules.qu.dto.request.QuQueryReqDTO;
import com.yf.exam.modules.qu.entity.Qu;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* <p>
* 问题题目Mapper
* </p>
*
* @author 聪明笨狗
* @since 2020-05-25 13:23
*/
public interface QuMapper extends BaseMapper<Qu> {



    /**
     * 随机抽取题库的数据
     * @param repoId
     * @param quType
     * @param level
     * @param excludes 要排除的ID列表
     * @param size
     * @return
     */
    List<Qu> listByRandom(@Param("repoId") String repoId,
                          @Param("quType") Integer quType,
                          @Param("excludes") List<String> excludes,
                          @Param("size") Integer size);

    /**
     * 获取指定题库和题型的所有题目（用于AI智能选择）
     * @param repoId
     * @param quType
     * @param excludes 要排除的ID列表
     * @return
     */
    List<Qu> listByType(@Param("repoId") String repoId,
                        @Param("quType") Integer quType,
                        @Param("excludes") List<String> excludes);

    /**
     * 获取指定题库、题型和知识点的题目（用于知识点筛选）
     * @param repoId
     * @param quType
     * @param excludes 要排除的ID列表
     * @param selectedKnowledgePoints 选定的知识点列表
     * @return
     */
    List<Qu> listByTypeAndKnowledgePoints(@Param("repoId") String repoId,
                                          @Param("quType") Integer quType,
                                          @Param("excludes") List<String> excludes,
                                          @Param("selectedKnowledgePoints") List<String> selectedKnowledgePoints);

    /**
     * 查找导出列表
     * @param query
     * @return
     */
    List<QuExportDTO> listForExport(@Param("query") QuQueryReqDTO query);

    /**
     * 分页查找
     * @param page
     * @param query
     * @return
     */
    IPage<QuDTO> paging(Page page, @Param("query") QuQueryReqDTO query);

    /**
     * 获取指定题库中所有题目的知识点
     * @param repoId 题库ID
     * @return 知识点JSON字符串列表
     */
    List<String> getKnowledgePointsByRepo(@Param("repoId") String repoId);

    /**
     * 获取指定题库和题型的知识点
     * @param repoId 题库ID
     * @param quType 题目类型
     * @return 知识点JSON字符串列表
     */
    List<String> getKnowledgePointsByRepoAndType(@Param("repoId") String repoId, @Param("quType") Integer quType);

}
