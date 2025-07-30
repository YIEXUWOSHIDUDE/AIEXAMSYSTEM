import { post } from '@/utils/request'

/**
 * 获取题库的所有知识点
 * @param {string} repoId 题库ID
 * @returns {Promise} 知识点列表
 */
export function getKnowledgePointsByRepo(repoId) {
  return post('/exam/api/knowledge-points/repo', {
    repoId: repoId
  })
}

/**
 * 获取题库指定题型的知识点
 * @param {string} repoId 题库ID
 * @param {number} quType 题目类型
 * @returns {Promise} 知识点列表
 */
export function getKnowledgePointsByRepoAndType(repoId, quType) {
  return post('/exam/api/knowledge-points/repo-type', {
    repoId: repoId,
    quType: quType
  })
}