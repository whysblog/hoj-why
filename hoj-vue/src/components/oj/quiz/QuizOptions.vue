<template>
  <div
    class="quiz-options"
    :role="multiple ? 'group' : 'radiogroup'"
    :aria-label="multiple ? '多选题选项' : '单选题选项'"
  >
    <button
      v-for="option in options"
      :key="option.key"
      type="button"
      class="quiz-option"
      :class="{
        selected: selected(option.key),
        correct: reviewed && correctAnswer.includes(option.key),
        wrong:
          reviewed &&
          selected(option.key) &&
          !correctAnswer.includes(option.key),
      }"
      :role="multiple ? 'checkbox' : 'radio'"
      :aria-checked="selected(option.key) ? 'true' : 'false'"
      :disabled="disabled || reviewed"
      @click="choose(option.key)"
    >
      <span class="quiz-option-letter">{{ option.key }}</span>
      <Markdown :content="option.text || ''" :isAvoidXss="true" />
      <span
        class="quiz-option-state"
        v-if="reviewed && correctAnswer.includes(option.key)"
        >正确选项</span
      >
      <span class="quiz-option-state" v-else-if="selected(option.key)"
        >已选择</span
      >
    </button>
  </div>
</template>
<script>
import Markdown from "@/components/oj/common/Markdown";
export default {
  name: "QuizOptions",
  components: { Markdown },
  props: {
    options: { type: Array, default: () => [] },
    value: { type: [String, Array], default: "" },
    multiple: Boolean,
    disabled: Boolean,
    reviewed: Boolean,
    correctAnswer: { type: String, default: "" },
  },
  methods: {
    selected(key) {
      return Array.isArray(this.value)
        ? this.value.includes(key)
        : String(this.value || "").includes(key);
    },
    choose(key) {
      if (this.disabled || this.reviewed) return;
      if (!this.multiple) {
        this.$emit("input", key);
        return;
      }
      const picked = Array.isArray(this.value) ? this.value.slice() : [];
      const index = picked.indexOf(key);
      if (index < 0) picked.push(key);
      else picked.splice(index, 1);
      this.$emit("input", picked.sort());
    },
  },
};
</script>
