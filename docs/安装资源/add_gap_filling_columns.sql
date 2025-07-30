-- Add gap filling columns to el_exam_repo table
-- This script adds support for fill-in-blank (gap filling) questions

-- Add gap_filling_count column
ALTER TABLE `el_exam_repo` 
ADD COLUMN `gap_filling_count` int NOT NULL DEFAULT '0' COMMENT '填空题数量' AFTER `saq_score`;

-- Add gap_filling_score column  
ALTER TABLE `el_exam_repo`
ADD COLUMN `gap_filling_score` int NOT NULL DEFAULT '0' COMMENT '填空题分数' AFTER `gap_filling_count`;

-- Update existing records to have default values (if any)
UPDATE `el_exam_repo` SET `gap_filling_count` = 0, `gap_filling_score` = 0 WHERE `gap_filling_count` IS NULL OR `gap_filling_score` IS NULL;

-- Verify the changes
-- SELECT * FROM `el_exam_repo` LIMIT 5;