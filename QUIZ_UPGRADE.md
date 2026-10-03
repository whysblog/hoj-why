# 客观题完整功能升级

## 部署

1. 备份数据库。已有客观题模块的实例先执行根目录 `upgrade_quiz_complete.sql`，它只新增 `quiz_attempt`，可重复执行，不删除原有题目或套卷。
2. 首次安装才使用 `create_quiz_question_table.sql`（包含 DROP，不用于已有数据的升级）。已有实例还应具备此前版本的 `question_type`、`explanation`、`lang_category` 及套卷关联表的 `item_type`、`score` 字段。
3. 构建并部署后端和 `hoj-vue`，保持前后端版本一致。前端 `npm run build`，将生成的 dist 部署到 Web 服务。
4. 验收管理员创建/编辑/隐藏题目、预览 Markdown、套卷增删排序与分值保存，以及学生单题/套卷作答、刷新草稿、交卷、历史复盘。

## 作答与计分

- 题型为单选和多选，选项 A–D。答案接受大小写、空格和逗号，规范为去重升序的选项；其它字符被拒绝，避免误判为正确答案。
- 单题正确得 1 分；套卷按各题配置分值（0–1000）计分。多选完全选对得分，漏选、错选和未答得 0 分。
- 套卷保留混合编程题。前端仅提交 `submitId`，服务端核验当前用户、题目及普通公开题库提交范围，再读取真实评测成绩。AC 得本题满分；OI 部分成绩按原题满分比例换算并限制在本卷分值范围内。未提交为 0 分，评测中不能交卷。
- 客观题草稿只保存在当前浏览器，按用户及题目内容隔离。编程题代码继续使用现有编辑器；草稿不保存已评测成绩，刷新后需重新提交编程题评测。
- 每次成功作答保存题目/选项/解析及成绩快照。记录仅本人可访问；后续题目编辑或删除不改变已有复盘内容。升级前的 sessionStorage 结果仍可通过旧结果链接读取，但不会自动迁移为历史记录。

## 管理端与接口

- `POST /api/admin/quiz/paper/save` 接受 `{paper, items}`，在同一事务中保存基本信息、题目顺序和分值；验证失败保留原卷。公开套卷不得为空，全部题目必须可用且公开，重复题目被拒绝。
- 旧套卷信息/题目保存接口保留并采用相同校验。旧创建接口默认创建隐藏草稿；不能创建空的公开套卷。
- 被套卷引用的客观题不能直接删除，应先移除关联；隐藏题目后，相关公开套卷将提示不可用，避免静默漏题。
- `GET /api/quiz/history` 支持分页、kind（quiz/paper）、resourceId；`GET /api/quiz/history/{id}` 返回当前用户的历史快照。

## 验证

- 后端测试：`mvn -pl DataBackup -am test -Dtest=QuizWorkflowTest,QuizHistoryTest -Dsurefire.failIfNoSpecifiedTests=false -DskipTests=false`。
- Java 17 环境下，原项目旧 Lombok 需在本地验证时增加 `-Dlombok.version=1.18.30`；未改动项目依赖版本。Node 新版本运行旧 Vue CLI 构建时可能需要 `NODE_OPTIONS=--openssl-legacy-provider`。
- 前端逻辑回归：在 `hoj-vue` 执行 `npm run test:quiz`，覆盖选项交互、草稿隔离/恢复、旧请求保护、重复提交、待评测拦截、复盘筛选和套卷整体保存。
- 可选浏览器回归：安装 Playwright/Chromium 后先构建，再执行 `npm run test:quiz:browser`。测试使用模拟 API，检查桌面/手机布局和操作流程。本次环境浏览器下载受限，尚未运行这一项。
- 这些测试不依赖真实 MySQL/Nacos/评测服务器。上线前仍需在配置齐全的实例执行上述业务验收。
