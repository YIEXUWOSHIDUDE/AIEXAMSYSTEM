-- Migration: Add Multi-Image Support
-- Description: Add image_refs columns to support multiple images per question/answer

-- Add image_refs column to questions table
ALTER TABLE el_qu 
ADD COLUMN image_refs TEXT COMMENT '多图片支持 - JSON数组格式存储多张图片URL';

-- Add image_refs column to answers table
ALTER TABLE el_qu_answer 
ADD COLUMN image_refs TEXT COMMENT '多图片支持 - JSON数组格式存储多张图片URL';

-- Add indexes for better performance
CREATE INDEX idx_el_qu_image_refs ON el_qu (image_refs(100));
CREATE INDEX idx_el_qu_answer_image_refs ON el_qu_answer (image_refs(100));

-- Add comments for documentation
ALTER TABLE el_qu MODIFY COLUMN image VARCHAR(500) COMMENT '题目图片（兼容字段，优先使用image_refs）';
ALTER TABLE el_qu_answer MODIFY COLUMN image VARCHAR(500) COMMENT '选项图片（兼容字段，优先使用image_refs）';