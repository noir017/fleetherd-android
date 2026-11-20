package io.github.noir017.fleetherd.web

/**
 * Lightweight ES5-compatible probe injected into same-origin fleetherd pages.
 *
 * 1. Exposes `window.FleetherdNative` as a clean public contract for future webui enhancements.
 * 2. Automatically tracks `<meta name="theme-color">`, drawers (`#app.sb-open` / `#app.sched-open`),
 *    and full-screen panels (`#settings-page`, `#task-page`, `#skills-win`, `#files-win`, `#git-win`,
 *    `#reader`, `#lightbox`) so the Android status bar and navigation bar adapt automatically
 *    without requiring server-side changes.
 * 3. Provides `window.__fleetherdConsumeBack()` for native Android back-gesture handling.
 */
object WebBridgeScript {
    val INJECT_JS: String = """
(function() {
  if (window.__fleetherdAndroidInstalled) return;
  window.__fleetherdAndroidInstalled = true;

  var raw = window.FleetherdAndroid;
  if (!raw) return;

  window.FleetherdNative = {
    setSystemBars: function(opts) {
      if (!opts) return;
      var s = typeof opts === 'string' ? opts : (opts.statusBar || opts.statusBarColor || '');
      var n = typeof opts === 'string' ? opts : (opts.navBar || opts.navBarColor || s);
      var l = (typeof opts === 'object' && typeof opts.lightStatusIcons === 'boolean')
        ? String(opts.lightStatusIcons)
        : '';
      if (s) raw.setSystemBars(s, n || s, l);
    },
    openServerManager: function() {
      raw.openServerManager();
    },
    getAppInfo: function() {
      try { return JSON.parse(raw.getAppInfo()); } catch (e) { return { platform: 'android' }; }
    }
  };

  function isOpaque(c) {
    return c && c !== 'transparent' && c !== 'rgba(0, 0, 0, 0)';
  }

  function bgOf(el) {
    if (!el) return '';
    try {
      var c = window.getComputedStyle(el).backgroundColor;
      return isOpaque(c) ? c : '';
    } catch (e) {
      return '';
    }
  }

  function isVisible(id) {
    var el = document.getElementById(id);
    return !!(el && !el.hidden && el.offsetParent !== null);
  }

  var lastKey = '';
  function syncSystemBars() {
    var topColor = '';
    var navColor = '';

    var overlayIds = ['lightbox', 'reader', 'files-win', 'git-win', 'settings-page', 'task-page', 'skills-win'];
    for (var i = 0; i < overlayIds.length; i++) {
      if (isVisible(overlayIds[i])) {
        var el = document.getElementById(overlayIds[i]);
        topColor = bgOf(el);
        navColor = topColor;
        break;
      }
    }

    var app = document.getElementById('app');
    if (!topColor && app) {
      if (app.classList.contains('sb-open')) {
        topColor = bgOf(document.getElementById('sidebar')) || '#171717';
        navColor = topColor;
      } else if (app.classList.contains('sched-open')) {
        topColor = bgOf(document.getElementById('schedules')) || '#171717';
        navColor = topColor;
      }
    }

    if (!topColor) {
      var meta = document.querySelector('meta[name="theme-color"]');
      var metaContent = meta && meta.getAttribute('content');
      topColor = bgOf(document.getElementById('chat-head'))
        || (metaContent && metaContent.trim())
        || bgOf(document.body)
        || '#131313';
    }

    if (!navColor) {
      navColor = bgOf(document.getElementById('bar')) || bgOf(document.body) || topColor;
    }

    var key = topColor + '|' + navColor;
    if (key !== lastKey) {
      lastKey = key;
      raw.setSystemBars(topColor, navColor, '');
    }
  }

  var timer = null;
  function scheduleSync() {
    if (timer) clearTimeout(timer);
    timer = setTimeout(syncSystemBars, 40);
  }

  try {
    var observer = new MutationObserver(scheduleSync);
    observer.observe(document.documentElement, {
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'hidden', 'style', 'content']
    });
  } catch (e) {}

  syncSystemBars();

  window.__fleetherdConsumeBack = function() {
    if (typeof window.__fleetherdBack === 'function') {
      try {
        if (window.__fleetherdBack()) return true;
      } catch (e) {}
    }

    var closableIds = [
      'lightbox', 'reader', 'files-win', 'git-win', 'settings-page',
      'task-page', 'skills-win', 'idle-dialog', 'hints', 'btw-box',
      'row-menu', 'model-pop'
    ];
    var hasOverlay = false;
    for (var i = 0; i < closableIds.length; i++) {
      var el = document.getElementById(closableIds[i]);
      if (el && !el.hidden) { hasOverlay = true; break; }
    }

    var app = document.getElementById('app');
    var hasDrawer = !!(app && (app.classList.contains('sb-open') || app.classList.contains('sched-open')));
    var hasMenu = !!document.querySelector('[aria-expanded="true"]');

    if (hasOverlay || hasDrawer || hasMenu) {
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
      scheduleSync();
      return true;
    }
    return false;
  };
})();
""".trimIndent()
}
