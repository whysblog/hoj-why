-- 客观题作答历史。升级前请备份数据库；此脚本不删除原题、套卷或成绩。
CREATE TABLE IF NOT EXISTS `quiz_attempt` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `uid` varchar(32) NOT NULL,
  `kind` varchar(16) NOT NULL COMMENT 'quiz / paper',
  `resource_id` bigint unsigned NOT NULL,
  `title` varchar(255) NOT NULL,
  `score` int NOT NULL,
  `max_score` int NOT NULL,
  `correct_count` int NOT NULL,
  `question_count` int NOT NULL,
  `result_json` mediumtext NOT NULL COMMENT '提交时的题面、答案、解析及评分快照',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_quiz_attempt_user` (`uid`, `id`),
  KEY `idx_quiz_attempt_resource` (`uid`, `kind`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
