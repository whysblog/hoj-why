// ============ 共享:幻灯片导航 ============
// 每个工具页面引入此脚本,自动启用左右翻页、键盘导航、进度条、圆点指示
(function() {
  let currentSlide = 0;
  let slides = [];
  let totalSlides = 0;

  function renderDots() {
    const dots = document.getElementById('slideDots');
    if (!dots) return;
    dots.innerHTML = '';
    for (let i = 0; i < totalSlides; i++) {
      const d = document.createElement('button');
      d.className = 'slide-dot' + (i === currentSlide ? ' active' : '');
      d.title = slides[i].dataset.title || ('第 ' + (i + 1) + ' 页');
      d.onclick = () => window.goToSlide(i);
      dots.appendChild(d);
    }
  }

  window.goToSlide = function(idx) {
    if (idx < 0 || idx >= totalSlides) return;
    slides[currentSlide].classList.remove('active');
    currentSlide = idx;
    slides[currentSlide].classList.add('active');
    const curNum = document.getElementById('curNum');
    if (curNum) curNum.textContent = idx + 1;
    const bar = document.getElementById('progressBar');
    if (bar) bar.style.width = ((idx + 1) / totalSlides * 100) + '%';
    const pb = document.getElementById('prevBtn');
    const nb = document.getElementById('nextBtn');
    if (pb) pb.disabled = idx === 0;
    if (nb) nb.disabled = idx === totalSlides - 1;
    renderDots();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    // 通知页面:幻灯片切换了 (用于触发懒加载的图表等)
    document.dispatchEvent(new CustomEvent('slidechange', { detail: { index: idx, title: slides[idx].dataset.title } }));
  };

  window.nextSlide = function() { window.goToSlide(currentSlide + 1); };
  window.prevSlide = function() { window.goToSlide(currentSlide - 1); };

  window.toggleFullscreen = function() {
    document.body.classList.toggle('fullscreen-mode');
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    } else {
      document.documentElement.requestFullscreen().catch(() => {});
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    slides = Array.from(document.querySelectorAll('.slide'));
    totalSlides = slides.length;
    const total = document.getElementById('totalNum');
    if (total) total.textContent = totalSlides;
    renderDots();
    const pb = document.getElementById('prevBtn');
    if (pb) pb.disabled = true;
  });

  document.addEventListener('keydown', (e) => {
    // 仅在 input/textarea 之外响应
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA' || e.target.tagName === 'SELECT') return;
    if (e.key === 'ArrowRight' || e.key === 'PageDown') { e.preventDefault(); window.nextSlide(); }
    if (e.key === 'ArrowLeft' || e.key === 'PageUp') { e.preventDefault(); window.prevSlide(); }
    if (e.key === 'Home') { e.preventDefault(); window.goToSlide(0); }
    if (e.key === 'End') { e.preventDefault(); window.goToSlide(totalSlides - 1); }
  });
})();
