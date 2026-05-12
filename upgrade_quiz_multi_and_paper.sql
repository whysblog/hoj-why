USE `hoj`;

/* 客观题：多选 + 答案字段扩展 */
ALTER TABLE `quiz_question`
  ADD COLUMN `question_type` tinyint NOT NULL DEFAULT 0 COMMENT '0单选 1多选' AFTER `option_d`,
  MODIFY COLUMN `answer` varchar(16) NOT NULL COMMENT '正确答案：单选如A；多选为升序字母如AB';

/* 套卷 */
DROP TABLE IF EXISTS `quiz_paper_item`;
DROP TABLE IF EXISTS `quiz_paper`;

CREATE TABLE `quiz_paper` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '套卷标题',
  `description` mediumtext COMMENT '套卷说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0隐藏 1公开',
  `author` varchar(255) DEFAULT NULL,
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_paper_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客观题套卷';

CREATE TABLE `quiz_paper_item` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `paper_id` bigint(20) unsigned NOT NULL,
  `question_id` bigint(20) unsigned NOT NULL,
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '题目顺序，从小到大',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`,`question_id`),
  KEY `idx_paper` (`paper_id`),
  CONSTRAINT `fk_paper_item_paper` FOREIGN KEY (`paper_id`) REFERENCES `quiz_paper` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_paper_item_question` FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套卷题目关联';
