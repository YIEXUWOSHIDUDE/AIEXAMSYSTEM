import request from '@/utils/request'

// 知识大纲管理API

/**
 * 获取大纲列表
 */
export function getOutlineList(params) {
  return request({
    url: '/exam/api/outline/list',
    method: 'get',
    params
  })
}

/**
 * 根据学科和年级获取大纲
 */
export function getOutlinesBySubjectAndGrade(subject, grade) {
  return request({
    url: `/exam/api/outline/list/${subject}/${grade}`,
    method: 'get'
  })
}

/**
 * 获取所有学科
 */
export function getSubjects() {
  return request({
    url: '/exam/api/outline/subjects',
    method: 'get'
  })
}

/**
 * 根据学科获取年级
 */
export function getGrades(subject) {
  return request({
    url: `/exam/api/outline/grades/${subject}`,
    method: 'get'
  })
}

/**
 * 根据ID获取大纲详情
 */
export function getOutlineById(id) {
  return request({
    url: `/exam/api/outline/${id}`,
    method: 'get'
  })
}

/**
 * 创建大纲
 */
export function createOutline(data) {
  return request({
    url: '/exam/api/outline/create',
    method: 'post',
    data
  })
}

/**
 * 更新大纲
 */
export function updateOutline(data) {
  return request({
    url: '/exam/api/outline/update',
    method: 'post',
    data
  })
}

/**
 * 删除大纲
 */
export function deleteOutline(id) {
  return request({
    url: `/exam/api/outline/delete/${id}`,
    method: 'post'
  })
}

// AI识别相关API

/**
 * 单个题目AI识别
 */
export function identifySingleOutline(data) {
  return request({
    url: '/exam/api/outline-ai/identify-single',
    method: 'post',
    data
  })
}

/**
 * 批量题目AI识别
 */
export function identifyBatchOutlines(data) {
  return request({
    url: '/exam/api/outline-ai/identify-batch',
    method: 'post',
    data
  })
}

/**
 * 保存识别映射
 */
export function saveOutlineMapping(data) {
  return request({
    url: '/exam/api/outline-ai/save-mapping',
    method: 'post',
    data
  })
}

/**
 * 获取题目的大纲映射
 */
export function getOutlineMapping(questionId) {
  return request({
    url: `/exam/api/outline-ai/get-mapping/${questionId}`,
    method: 'get'
  })
}