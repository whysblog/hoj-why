USE `hoj`;

ALTER TABLE `quiz_question`
  ADD COLUMN `lang_category` varchar(16) NULL COMMENT '分类：cpp/python' AFTER `author`;

ALTER TABLE `quiz_paper`
  ADD COLUMN `lang_category` varchar(16) NULL COMMENT '分类：cpp/python' AFTER `author`;
