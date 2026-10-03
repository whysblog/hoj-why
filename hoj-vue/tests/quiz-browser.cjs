/* Browser regression with API fixtures; run after npm run build.
 * Requires Playwright + Chromium. QUIZ_DIST defaults to ../dist.
 */
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const http = require("http");
const fs = require("fs");
const path = require("path");
const assert = require("node:assert/strict");
const root = process.env.QUIZ_DIST || path.resolve(__dirname, "../dist");
const question = {
  id: 10,
  title: "C++ 表达式与运算顺序",
  description:
    "阅读下面的代码，选择正确的输出。\n```cpp\nint x = 3;\ncout << (x + 1);\n```",
  questionType: 0,
  difficulty: 1,
  author: "AKOJ",
  langCategory: "cpp",
  options: [
    { key: "A", text: "`3`" },
    { key: "B", text: "`4`" },
    { key: "C", text: "`5`" },
    { key: "D", text: "编译错误" },
  ],
};
const multi = {
  ...question,
  id: 11,
  title: "算法复杂度判断",
  questionType: 1,
  description: "哪些算法的时间复杂度可以为 $O(n \\log n)$？",
  options: [
    { key: "A", text: "归并排序" },
    { key: "B", text: "冒泡排序" },
    { key: "C", text: "堆排序" },
    { key: "D", text: "逐个枚举所有子集" },
  ],
};
const paper = {
  id: 20,
  title: "CSP 基础能力练习",
  description: "两道客观题，共 **60 分**。多选题完全选对得分。",
  items: [
    {
      itemType: "quiz",
      questionId: 10,
      score: 20,
      quizQuestion: question,
      title: question.title,
    },
    {
      itemType: "quiz",
      questionId: 11,
      score: 40,
      quizQuestion: multi,
      title: multi.title,
    },
  ],
};
const user = {
  uid: "alice",
  username: "alice",
  realname: "测试用户",
  phone: "123",
  roleList: ["root"],
  avatar: "",
  titleName: "",
};
const attempts = {};
let lastSubmit, lastSave;
const mime = {
  ".html": "text/html",
  ".js": "text/javascript",
  ".css": "text/css",
  ".json": "application/json",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".woff": "font/woff",
  ".woff2": "font/woff2",
};
const server = http.createServer((req, res) => {
  let name = path.join(root, decodeURIComponent(req.url.split("?")[0]));
  if (!fs.existsSync(name) || fs.statSync(name).isDirectory())
    name = path.join(root, "index.html");
  res.setHeader(
    "Content-Type",
    mime[path.extname(name)] || "application/octet-stream"
  );
  fs.createReadStream(name).pipe(res);
});
(async () => {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const origin = "http://127.0.0.1:" + server.address().port;
  const browser = await chromium.launch({ headless: true });
  try {
    const context = await browser.newContext({
      viewport: { width: 1440, height: 1050 },
    });
    await context.addInitScript((u) => {
      localStorage.setItem("token", "fixture");
      localStorage.setItem("userInfo", JSON.stringify(u));
    }, user);
    const page = await context.newPage();
    const errors = [];
    page.on("pageerror", (e) => errors.push(e.message));
    await page.route("**/api/**", async (route) => {
      const req = route.request(),
        url = new URL(req.url()),
        p = url.pathname;
      let data = {};
      if (p === "/api/get-user-auth-info") data = { roles: ["root"] };
      else if (p === "/api/get-website-config")
        data = {
          shortName: "AKOJ",
          projectName: "AKOJ",
          openPublicDiscussion: true,
        };
      else if (p === "/api/quiz/list")
        data = { records: [question, multi], total: 2 };
      else if (p === "/api/quiz/paper/list")
        data = {
          records: [{ id: 20, title: paper.title, author: "AKOJ" }],
          total: 1,
        };
      else if (p === "/api/quiz/10") data = question;
      else if (p === "/api/quiz/paper/20") data = paper;
      else if (p === "/api/quiz/10/submit") {
        const body = req.postDataJSON();
        lastSubmit = body;
        data = {
          attemptId: 91,
          correct: body.answer === "B",
          correctAnswer: "B",
          userAnswer: body.answer,
          score: body.answer === "B" ? 1 : 0,
          maxScore: 1,
          question,
          explanation: "`3 + 1` 的结果为 `4`。",
        };
        attempts[91] = {
          ...data,
          kind: "quiz",
          resourceId: 10,
          submittedAt: "2026-10-03 12:00",
        };
      } else if (p === "/api/quiz/paper/20/submit") {
        lastSubmit = req.postDataJSON();
        data = {
          attemptId: 92,
          paperTitle: paper.title,
          score: 20,
          maxScore: 60,
          itemResults: [
            {
              no: 1,
              itemType: "quiz",
              questionId: 10,
              questionType: 0,
              question,
              title: question.title,
              score: 20,
              maxScore: 20,
              userAnswer: lastSubmit.answers["10"],
              correctAnswer: "B",
              explanation: "3 + 1 = 4",
            },
            {
              no: 2,
              itemType: "quiz",
              questionId: 11,
              questionType: 1,
              question: multi,
              title: multi.title,
              score: 0,
              maxScore: 40,
              userAnswer: lastSubmit.answers["11"],
              correctAnswer: "AC",
              explanation: "归并排序与堆排序为 O(n log n)。",
            },
          ],
        };
        attempts[92] = {
          ...data,
          kind: "paper",
          resourceId: 20,
          submittedAt: "2026-10-03 12:00",
        };
      } else if (p === "/api/quiz/history")
        data = {
          records: [
            {
              id: 92,
              kind: "paper",
              title: paper.title,
              score: 20,
              maxScore: 60,
              correctCount: 1,
              questionCount: 2,
              gmtCreate: "2026-10-03 12:00",
            },
          ],
          total: 1,
        };
      else if (p.startsWith("/api/quiz/history/"))
        data = attempts[p.split("/").pop()] || {};
      else if (p === "/api/admin/quiz/list")
        data = {
          records: [
            {
              ...question,
              answer: "B",
              optionA: "3",
              optionB: "4",
              optionC: "5",
              optionD: "编译错误",
              status: 1,
            },
          ],
          total: 1,
        };
      else if (p === "/api/admin/quiz/10")
        data = {
          ...question,
          answer: "B",
          optionA: "3",
          optionB: "4",
          optionC: "5",
          optionD: "编译错误",
          explanation: "答案为 4",
          status: 1,
        };
      else if (p === "/api/admin/quiz/paper/list")
        data = {
          records: [{ id: 20, title: paper.title, status: 1 }],
          total: 1,
        };
      else if (p === "/api/admin/quiz/paper/20")
        data = {
          paper: { id: 20, title: paper.title, status: 1 },
          items: paper.items,
        };
      else if (p === "/api/admin/quiz/paper/save") {
        lastSave = req.postDataJSON();
        data = 20;
      }
      await route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({ status: 200, data }),
      });
    });
    await page.goto(origin + "/quiz");
    await page.getByText(question.title, { exact: true }).waitFor();
    await page.getByText(question.title, { exact: true }).click();
    await page.getByRole("radio").nth(1).click();
    await page.reload();
    await page.locator(".quiz-option.selected").waitFor();
    assert.equal(
      await page.locator(".quiz-option.selected").innerText(),
      "B\n4\n已选择"
    );
    await page.getByRole("button", { name: "提交答案", exact: true }).click();
    await page.getByText("回答正确", { exact: false }).waitFor();
    assert.equal(lastSubmit.answer, "B");
    await page.getByText("查看本次记录 →").click();
    await page.getByText("答案解析", { exact: true }).waitFor();
    await page.reload();
    await page.getByText("答案解析", { exact: true }).waitFor();
    await page.goto(origin + "/quiz/paper/20");
    await page.getByRole("radio").nth(1).click();
    await page.getByRole("checkbox").nth(0).click();
    await page.reload();
    await page.locator(".quiz-option.selected").nth(1).waitFor();
    assert.equal(await page.locator(".quiz-grid button.answered").count(), 2);
    await page.screenshot({
      path: "/tmp/quiz-paper-desktop.png",
      fullPage: true,
    });
    await page.getByRole("button", { name: "提交答卷", exact: true }).click();
    await page.getByText("答案解析", { exact: true }).first().waitFor();
    assert.equal(lastSubmit.answers["11"], "A");
    assert.equal(lastSubmit.answers["10"], "B");
    await page.getByText("只看未得满分").click();
    assert.equal(await page.locator(".quiz-question-block").count(), 1);
    await page.goto(origin + "/quiz/history");
    await page.getByText("查看解析 →").waitFor();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto(origin + "/quiz/paper/20");
    await page.getByText("答题卡", { exact: true }).waitFor();
    await page.screenshot({
      path: "/tmp/quiz-paper-mobile.png",
      fullPage: true,
    });
    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > window.innerWidth
    );
    assert.equal(overflow, false, "Mobile horizontal overflow");
    await page.setViewportSize({ width: 1440, height: 1050 });
    await page.goto(origin + "/admin/quiz");
    await page.getByRole("button", { name: "编辑", exact: true }).click();
    await page.getByRole("button", { name: "预览题目与解析" }).click();
    await page.getByText("解析预览", { exact: true }).waitFor();
    await page.screenshot({
      path: "/tmp/quiz-admin-desktop.png",
      fullPage: true,
    });
    await page.goto(origin + "/admin/quiz-paper");
    await page.getByRole("button", { name: "编辑", exact: true }).click();
    await page.getByRole("button", { name: "保存全部", exact: true }).click();
    await page.getByText("套卷信息、题目顺序与分值已全部保存").waitFor();
    assert.equal(lastSave.paper.id, 20);
    assert.equal(lastSave.items.length, 2);
    assert.equal(lastSave.items[1].score, 40);
    assert.deepEqual(errors, []);
    console.log(
      "PASS: catalog, single submit/review, draft reload, multi partial, answer card, history, mobile layout, admin preview and atomic save; no browser errors."
    );
  } finally {
    await browser.close();
    server.close();
  }
})().catch((e) => {
  console.error(e);
  server.close();
  process.exitCode = 1;
});
