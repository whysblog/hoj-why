/* 套卷组合支持客观题与编程题混排 */

ALTER TABLE `quiz_paper_item`
  DROP FOREIGN KEY `fk_paper_item_question`;

ALTER TABLE `quiz_paper_item`
  ADD COLUMN `item_type` varchar(20) NOT NULL DEFAULT 'quiz' COMMENT 'quiz or problem' AFTER `question_id`;

ALTER TABLE `quiz_paper_item`
  DROP INDEX `uk_paper_question`,
  ADD UNIQUE KEY `uk_paper_item` (`paper_id`, `item_type`, `question_id`);
