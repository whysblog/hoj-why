const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const path = require('path');
const root = path.join(__dirname, '..');
const text = fs.readFileSync(path.join(root, 'src/components/oj/group/AddGroupPublicProblem.vue'), 'utf8');
const script = text.match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\s*$/gm, '').replace('export default', 'module.exports =');
let requests = [], resolveAdd, rejectAdd;
const api = {
  getProblemList: params => { requests.push(params); return Promise.resolve({data: {data: {total: 1, records: [{pid: 42, problemId: 'P1000'}]}}}); },
  addGroupProblemFromPublic: data => { requests.push(data); return new Promise((resolve, reject) => { resolveAdd = resolve; rejectAdd = reject; }); }
};
const context = {module: {exports: {}}, api, mMessage: {success() {}}, Pagination: {}};
vm.runInNewContext(script, context);
const component = context.module.exports;
const instance = Object.assign(component.data(), {groupId: '20', $i18n: {t: x => x}, $emit() {}});
for (const [name, fn] of Object.entries(component.methods)) instance[name] = fn.bind(instance);
(async () => {
  assert(text.includes('addPublicProblem(row.pid)'));
  instance.currentPage = 3;
  instance.limit = 30;
  instance.getPublicProblem();
  await new Promise(setImmediate);
  assert.strictEqual(requests[0].currentPage, 3);
  assert.strictEqual(requests[0].limit, 30);
  assert.strictEqual(instance.problemList[0].pid, 42);
  instance.addPublicProblem(instance.problemList[0].pid);
  instance.addPublicProblem(42);
  assert.strictEqual(requests.length, 2, 'double click must not create two copies');
  assert.strictEqual(requests[1].pid, 42);
  assert.strictEqual(requests[1].gid, '20');
  rejectAdd(new Error('denied'));
  await new Promise(setImmediate);
  assert.strictEqual(instance.adding, false);
  instance.addPublicProblem(42);
  resolveAdd({});
  await new Promise(setImmediate);
  assert.strictEqual(instance.adding, false);
  assert.strictEqual(instance.currentPage, 1);
  instance.currentPage = 4;
  instance.onPageSizeChange(50);
  assert.strictEqual(instance.currentPage, 1);
  // Evaluate the real API export with mocked transport, ensuring the method exists.
  const apiText = fs.readFileSync(path.join(root, 'src/common/api.js'), 'utf8');
  const objects = apiText.slice(apiText.indexOf('const ojApi'), apiText.indexOf('// 集中导出'));
  const apiContext = {ajax: (...args) => args};
  vm.runInNewContext(objects + '\nthis.exported = Object.assign(ojApi, adminApi)', apiContext);
  const payload = {pid: 42, gid: '20'};
  const call = apiContext.exported.addGroupProblemFromPublic(payload);
  assert.strictEqual(call[0], '/api/group/problem/add-from-public');
  assert.strictEqual(call[1], 'post');
  assert.strictEqual(call[2].data, payload);
  console.log('PASS: public problem ID, pagination, duplicate clicks, retry and API transport');
})().catch(error => { console.error(error); process.exitCode = 1; });
