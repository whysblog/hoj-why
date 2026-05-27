USE `hoj`;

ALTER TABLE `quiz_question`
  ADD COLUMN `explanation` mediumtext NULL COMMENT '答案解析，支持 Markdown' AFTER `answer`;
