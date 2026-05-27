USE `hoj`;

ALTER TABLE `quiz_paper_item`
  ADD COLUMN `score` int NOT NULL DEFAULT 100 COMMENT '本卷分值' AFTER `item_type`;
