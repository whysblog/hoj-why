const fs = require("fs");
const path = require("path");
const vm = require("vm");
const assert = require("node:assert/strict");
const babel = require("@babel/core");
const compiler = require("vue-template-compiler");
const root = path.resolve(__dirname, "../src");
const entries = new Map();
const localStorage = {
  getItem: (k) => entries.get(k) || null,
  setItem: (k, v) => entries.set(k, v),
  removeItem: (k) => entries.delete(k),
};
let api = {};
function evaluate(file) {
  let source = fs.readFileSync(path.join(root, file), "utf8");
  if (file.endsWith(".vue")) {
    const parsed = compiler.parseComponent(source);
    source = parsed.script.content;
    const check = compiler.compile(parsed.template.content);
    assert.deepEqual(check.errors, [], file + " template errors");
  }
  const code = babel.transformSync(source, {
    babelrc: false,
    configFile: false,
    plugins: [require.resolve("@babel/plugin-transform-modules-commonjs")],
  }).code;
  const module = { exports: {} };
  vm.runInNewContext(code, {
    module,
    exports: module.exports,
    localStorage,
    Set,
    JSON,
    console,
    require(name) {
      if (name === "@/common/api") return api;
      if (name === "vuex") return { mapGetters: () => ({}) };
      if (name === "@/common/quiz") return evaluate("common/quiz.js");
      return {};
    },
  });
  return module.exports;
}
function component(file, extra = {}) {
  const options = evaluate(file).default;
  const ctx = {
    ...(options.data ? options.data() : {}),
    $route: { params: { quizId: "10", paperId: "20" }, query: {} },
    isAuthenticated: true,
    userInfo: { uid: "alice" },
    $set: (o, k, v) => {
      o[k] = v;
    },
    $store: { commit() {} },
    $confirm: async () => true,
    $router: { push() {} },
    ...extra,
  };
  Object.entries(options.methods || {}).forEach(
    ([k, v]) => (ctx[k] = v.bind(ctx))
  );
  Object.entries(options.computed || {}).forEach(([k, v]) =>
    Object.defineProperty(ctx, k, {
      get: () => v.call(ctx),
      configurable: true,
    })
  );
  return { ctx, options };
}
const question = {
  id: 10,
  title: "Q",
  questionType: 1,
  options: [
    { key: "A", text: "One" },
    { key: "B", text: "Two" },
  ],
};
(async () => {
  const draft = evaluate("common/quiz.js");
  assert.equal(draft.answerText(["C", "A", "A", "E"]), "AC");
  assert.equal(draft.answerText("garbage"), "");
  const key = draft.draftKey("quiz", 10, "alice");
  draft.saveDraft(key, "v1", { picked: ["A"] });
  assert.deepEqual(JSON.parse(JSON.stringify(draft.readDraft(key, "v1"))), {
    picked: ["A"],
  });
  assert.deepEqual(JSON.parse(JSON.stringify(draft.readDraft(key, "v2"))), {});
  assert.notEqual(key, draft.draftKey("quiz", 10, "bob"));
  const choice = component("components/oj/quiz/QuizOptions.vue", {
    options: question.options,
    value: ["B"],
    multiple: true,
    disabled: false,
    reviewed: false,
    $emit: (event, value) => {
      assert.equal(event, "input");
      assert.equal(value.join(""), "AB");
    },
  }).ctx;
  choice.choose("A");
  let emitted = false;
  choice.reviewed = true;
  choice.$emit = () => {
    emitted = true;
  };
  choice.choose("B");
  assert.equal(emitted, false);
  let called = 0;
  api.getQuizDetail = async () => ({ data: { data: question } });
  api.submitQuizAnswer = async (id, answer) => {
    called++;
    assert.equal(answer, "A");
    return { data: { data: { attemptId: 99, correct: false, score: 0 } } };
  };
  const { ctx: single, options: singleOptions } = component(
    "views/oj/quiz/QuizDetail.vue"
  );
  await single.fetch();
  single.picked = ["A"];
  singleOptions.watch.picked.handler.call(single);
  await single.fetch();
  assert.equal(single.picked.join(""), "A");
  await single.submit();
  assert.equal(single.result.correct, false);
  assert.equal(entries.has(single.key), false);
  assert.equal(called, 1);
  single.submitting = true;
  await single.submit();
  assert.equal(called, 1);
  let resolveOld;
  api.getQuizDetail = () =>
    new Promise((resolve) => {
      resolveOld = resolve;
    });
  const old = single.fetch();
  api.getQuizDetail = async () => ({
    data: { data: { ...question, id: 11, title: "new" } },
  });
  await single.fetch();
  resolveOld({ data: { data: question } });
  await old;
  assert.equal(
    single.detail.id,
    11,
    "Stale request must not overwrite new question"
  );
  const { ctx: paper, options: paperOptions } = component(
    "views/oj/quiz/QuizPaperDetail.vue"
  );
  api.getQuizPaperDetail = async () => ({
    data: {
      data: {
        id: 20,
        items: [
          {
            itemType: "quiz",
            questionId: 10,
            score: 35,
            quizQuestion: question,
          },
          { itemType: "problem", questionId: 30, score: 65, problemId: "P30" },
        ],
      },
    },
  });
  await paper.fetch();
  paper.selections[10] = ["A"];
  paper.onProblemStatus({
    pid: 30,
    submitId: 50,
    pending: false,
    status: 0,
    score: 999,
  });
  assert.equal(paper.answered, 2);
  assert.equal(paper.maxScore, 100);
  api.submitQuizPaper = async (id, body) => {
    assert.equal(body.answers["10"], "A");
    assert.equal(body.problemSnapshots["30"].submitId, 50);
    assert.equal(
      body.problemSnapshots["30"].score,
      undefined,
      "Never submit browser score"
    );
    return { data: { data: { attemptId: 101 } } };
  };
  let navigated;
  paper.$router.push = (p) => {
    navigated = p;
  };
  await paper.submit();
  assert.equal(navigated, "/quiz/history/101");
  paper.onProblemStatus({ pid: 30, pending: true });
  assert.equal(paper.pending, true);
  navigated = null;
  await paper.submit();
  assert.equal(navigated, null, "Pending grading blocks submission");
  paper.problemStatusMap = {};
  let confirmResolve;
  paper.$confirm = () =>
    new Promise((resolve) => {
      confirmResolve = resolve;
    });
  const submitting = paper.submit();
  assert.equal(
    paper.submitting,
    true,
    "Repeated clicks are blocked during confirmation"
  );
  await paper.submit();
  confirmResolve(false);
  api.submitQuizPaper = async () => ({ data: { data: { attemptId: 102 } } });
  await submitting;
  const { ctx: review } = component("views/oj/quiz/QuizAttemptDetail.vue");
  review.result = {
    kind: "paper",
    itemResults: [
      { score: 35, maxScore: 35 },
      { score: 0, maxScore: 65 },
    ],
  };
  review.wrongOnly = true;
  assert.equal(review.rows.length, 1);
  let payload;
  api.admin_saveQuizPaper = async (body) => {
    payload = body;
  };
  const { ctx: admin } = component("views/admin/quiz/AdminQuizPaper.vue", {
    $message: { success() {}, warning() {} },
    load() {},
  });
  admin.load = () => {};
  admin.paperForm = { id: 20, title: "Paper", status: 1 };
  admin.orderedItems = [{ itemType: "quiz", questionId: 10, score: 35 }];
  await admin.savePaper();
  assert.equal(payload.paper.id, 20);
  assert.equal(payload.items[0].score, 35);
  assert.equal(admin.visible, false);
  console.log(
    "PASS: options, draft isolation/invalidation/reload, submission guards, stale requests, paper payload/score, pending grading, review filter, atomic admin save; Vue templates compile."
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
