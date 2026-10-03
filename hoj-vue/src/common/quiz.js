// Drafts are isolated by user and resource, and discarded when the question contents change.
export const answerText = (value) =>
  Array.isArray(value)
    ? [...new Set(value)]
        .filter((x) => /^[A-D]$/.test(x))
        .sort()
        .join("")
    : /^[A-D]$/.test(value || "")
    ? value
    : "";
export const draftKey = (kind, id, user) =>
  `hoj_quiz_draft_v2_${user || "guest"}_${kind}_${id}`;
export function readDraft(key, signature) {
  try {
    const draft = JSON.parse(localStorage.getItem(key));
    return draft && draft.signature === signature ? draft.answers || {} : {};
  } catch (e) {
    return {};
  }
}
export function saveDraft(key, signature, answers) {
  try {
    localStorage.setItem(key, JSON.stringify({ signature, answers }));
    return true;
  } catch (e) {
    return false;
  }
}
export function clearDraft(key) {
  try {
    localStorage.removeItem(key);
  } catch (e) {
    /* browser storage may be unavailable */
  }
}
export const questionSignature = (questions) =>
  JSON.stringify(
    questions.map((q) => [
      q.id,
      q.title,
      q.description,
      q.questionType,
      q.options,
    ])
  );
