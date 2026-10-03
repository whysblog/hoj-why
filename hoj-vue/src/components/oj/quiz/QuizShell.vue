<template>
  <main class="quiz-page">
    <header class="quiz-hero">
      <div class="quiz-eyebrow">AKOJ · KNOWLEDGE PRACTICE</div>
      <h1>{{ title }}</h1>
      <p class="quiz-lead">{{ subtitle }}</p>
      <nav class="quiz-nav" aria-label="客观题导航">
        <router-link to="/quiz" :class="{ active: active === 'quiz' }"
          >单题练习</router-link
        >
        <router-link to="/quiz/paper" :class="{ active: active === 'paper' }"
          >套卷练习</router-link
        >
        <router-link
          to="/quiz/history"
          :class="{ active: active === 'history' }"
          >作答记录</router-link
        >
      </nav>
    </header>
    <slot />
  </main>
</template>
<script>
import "@/styles/quiz.css";
export default {
  name: "QuizShell",
  props: { title: String, subtitle: String, active: String },
};
</script>
