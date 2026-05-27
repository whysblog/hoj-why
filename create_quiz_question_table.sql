/* 客观题（选择题）表，与编程题 problem 表完全独立 — 含单选/多选 */

DROP TABLE IF EXISTS `quiz_paper_item`;
DROP TABLE IF EXISTS `quiz_paper`;
DROP TABLE IF EXISTS `quiz_question`;

CREATE TABLE `quiz_question` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '题目标题',
  `description` mediumtext COMMENT '题干说明，可为 Markdown 或 HTML',
  `option_a` varchar(2000) NOT NULL COMMENT '选项A',
  `option_b` varchar(2000) NOT NULL COMMENT '选项B',
  `option_c` varchar(2000) NOT NULL COMMENT '选项C',
  `option_d` varchar(2000) NOT NULL COMMENT '选项D',
  `question_type` tinyint NOT NULL DEFAULT 0 COMMENT '0单选 1多选',
  `answer` varchar(16) NOT NULL COMMENT '正确答案：单选如A；多选升序如AB',
  `explanation` mediumtext COMMENT '答案解析，支持 Markdown',
  `difficulty` int NOT NULL DEFAULT '1' COMMENT '0简单 1中等 2困难',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0隐藏 1公开',
  `author` varchar(255) DEFAULT NULL COMMENT '出题人',
  `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_difficulty` (`difficulty`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客观选择题';

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
  `item_type` varchar(20) NOT NULL DEFAULT 'quiz' COMMENT 'quiz or problem',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '题目顺序，从小到大',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_item` (`paper_id`,`item_type`,`question_id`),
  KEY `idx_paper` (`paper_id`),
  CONSTRAINT `fk_paper_item_paper` FOREIGN KEY (`paper_id`) REFERENCES `quiz_paper` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='套卷题目关联';
