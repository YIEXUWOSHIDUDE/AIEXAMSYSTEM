-- Question Enhancement Database Migration Script
-- Adds support for question stem extraction and knowledge point identification
-- Execute this script to add new fields to support enhanced question processing

-- Add new fields to el_qu table for question enhancement
ALTER TABLE `el_qu` 
ADD COLUMN `question_stem` TEXT COMMENT '提取的题干内容' AFTER `content`,
ADD COLUMN `knowledge_points` TEXT COMMENT '知识点标签JSON数组' AFTER `question_stem`,
ADD COLUMN `extraction_status` TINYINT NOT NULL DEFAULT 0 COMMENT '提取状态:0=未处理,1=已提取,2=手动编辑' AFTER `knowledge_points`;

-- Create knowledge point dictionary table (optional for advanced features)
CREATE TABLE `el_knowledge_point` (
  `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ID',
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '知识点名称',
  `category` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '知识点分类',
  `description` TEXT COLLATE utf8mb4_general_ci COMMENT '知识点描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='知识点词典表';

-- Insert some sample knowledge points for testing
INSERT INTO `el_knowledge_point` (`id`, `name`, `category`, `description`) VALUES
('kp001', '加法运算', '数学基础', '整数和小数的基本加法运算'),
('kp002', '减法运算', '数学基础', '整数和小数的基本减法运算'),
('kp003', '乘法运算', '数学基础', '整数和小数的基本乘法运算'),
('kp004', '除法运算', '数学基础', '整数和小数的基本除法运算'),
('kp005', 'Java语法', '编程语言', 'Java编程语言的基本语法规则'),
('kp006', '面向对象', '编程概念', '面向对象编程的基本概念和原理'),
('kp007', '数据结构', '计算机科学', '常见数据结构的概念和应用'),
('kp008', '算法分析', '计算机科学', '算法复杂度分析和优化方法');

-- Update existing questions to have default values
UPDATE `el_qu` SET 
  `question_stem` = `content`,
  `knowledge_points` = '[]',
  `extraction_status` = 0 
WHERE `question_stem` IS NULL;

-- Create index for better query performance
CREATE INDEX `idx_extraction_status` ON `el_qu` (`extraction_status`);
CREATE INDEX `idx_knowledge_points` ON `el_qu` (`knowledge_points`(100));

-- Verify the changes
SELECT 
  id, 
  LEFT(content, 50) as content_preview,
  LEFT(question_stem, 50) as stem_preview,
  knowledge_points,
  extraction_status
FROM `el_qu` 
LIMIT 5;

-- Check table structure
DESCRIBE `el_qu`;
DESCRIBE `el_knowledge_point`;