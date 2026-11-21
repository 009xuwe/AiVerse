/* AiVerse 站点增强（全站所有 VitePress 页面生效）
 * 1. 移动菜单注入「返回首页」+「中英文切换」（菜单底部，全站统一）
 * 2. 文案随当前语言本地化（英文站显示 Home/Menu/简体中文）
 * 3. 语言切换指向「当前页对应语言版本」，不存在(404)则回退该语言首页
 * 4. 下载页版本通道/下载源切换淡入过渡
 * 5. 英文页侧边栏/顶部导航全量补全（对齐中文：6 组 guide + 4 组 advanced）
 */
(function () {
  /* ---- 语言检测 ---- */
  var path = location.pathname;
  var isEn = /^\/en(\/|$)/.test(path);
  var CUR = isEn ? 'en' : 'zh';
  var TARGET = isEn ? 'zh' : 'en';

  /* ---- 英文侧边栏 / 顶部导航全量配置（对齐中文） ----
   * VitePress 客户端 hydrate 用 __VP_SITE_DATA__.themeConfig.sidebar 渲染侧边栏。
   * 本脚本在 Vue 模块脚本（defer）之前同步执行，此时改写 __VP_SITE_DATA__
   * 可让所有英文页用完整数据重建侧边栏与顶部导航，无需逐页改 HTML。
   * 注：本对象需与 docs-site/scripts/en_sidebar_config.json 保持一致。 */
  var EN_NAV = [
    { text: 'Guide', link: '/en/guide/quick-start', activeMatch: '/en/guide/' },
    { text: 'Advanced', link: '/en/advanced/build-android-app', activeMatch: '/en/advanced/' },
    { text: 'Download', link: 'https://aivapp.cn/en/download' },
    { text: 'User Agreement', link: '/en/terms' },
    { text: 'Privacy', link: '/en/privacy' }
  ];
  var EN_SIDEBAR = {
    '/en/guide/': [
      { text: 'Getting Started', items: [
        { text: 'Quick Start', link: '/en/guide/quick-start' },
        { text: 'Feature Overview', link: '/en/guide/overview' },
        { text: 'Free Vendors', link: '/en/guide/free-providers' }
      ]},
      { text: 'Core Workflows', items: [
        { text: 'Chat Interface Tour', link: '/en/guide/chat' },
        { text: 'Roles', link: '/en/guide/roles' },
        { text: 'Four Modes (Build / Plan / Auto / Expert)', link: '/en/guide/modes' },
        { text: 'Session Goal', link: '/en/guide/goal' },
        { text: 'Checkpoints & Undo', link: '/en/guide/checkpoint' },
        { text: 'Terminal', link: '/en/guide/terminal' },
        { text: 'Built-in Browser', link: '/en/guide/browser' },
        { text: 'File Browsing & Code Editing', link: '/en/guide/files' },
        { text: 'Git Version Control', link: '/en/guide/git' },
        { text: 'Tablet & Large Screen', link: '/en/guide/tablet' }
      ]},
      { text: 'Models & Usage', items: [
        { text: 'AI Vendors & Models', link: '/en/guide/providers' },
        { text: 'Default & Dedicated Models', link: '/en/guide/default-models' },
        { text: 'Token Stats & Cost', link: '/en/guide/token-stats' }
      ]},
      { text: 'Runtime Environment', items: [
        { text: 'Container & Images', link: '/en/guide/container' },
        { text: 'Remote SSH Mode', link: '/en/guide/remote-ssh' },
        { text: 'Workspace Sync', link: '/en/guide/sync' },
        { text: 'Network Proxy', link: '/en/guide/proxy' }
      ]},
      { text: 'Extensions', items: [
        { text: 'MCP Servers', link: '/en/guide/mcp' },
        { text: 'Skills', link: '/en/guide/skills' },
        { text: 'Subagents', link: '/en/guide/subagent' },
        { text: 'Shizuku Tools', link: '/en/guide/shizuku' },
        { text: 'Custom Prompts', link: '/en/guide/custom-prompts' },
        { text: 'Memory & Project Rules', link: '/en/guide/memory' },
        { text: 'Plugin Install & Use', link: '/en/guide/scripts' }
      ]},
      { text: 'Settings & Maintenance', items: [
        { text: 'General Settings', link: '/en/guide/general' },
        { text: 'Appearance & Language', link: '/en/guide/appearance' },
        { text: 'Text-to-Speech', link: '/en/guide/tts' },
        { text: 'Tool Authorization', link: '/en/guide/permissions' },
        { text: 'System Permissions', link: '/en/guide/app-permissions' },
        { text: 'Background Run', link: '/en/guide/background-run' },
        { text: 'Logs & Troubleshooting', link: '/en/guide/logs' },
        { text: 'Common Error Messages', link: '/en/guide/error-codes' },
        { text: 'Storage', link: '/en/guide/storage' },
        { text: 'Backup & Restore', link: '/en/guide/backup' },
        { text: 'About & Updates', link: '/en/guide/about' },
        { text: 'User Agreement', link: '/en/terms' },
        { text: 'Privacy Policy', link: '/en/privacy' }
      ]}
    ],
    '/en/advanced/': [
      { text: 'Environment Setup', items: [
        { text: 'Build Android Apps in the Container', link: '/en/advanced/build-android-app' },
        { text: 'Build Flutter Apps in the Container', link: '/en/advanced/build-flutter-app' },
        { text: 'Build a Flutter Release APK in the Container', link: '/en/advanced/build-flutter-release' },
        { text: 'Install Playwright Browser Automation', link: '/en/advanced/playwright-mcp' }
      ]},
      { text: 'Device Debugging', items: [
        { text: 'Debug Other Devices over Wireless adb', link: '/en/advanced/adb-wireless-debug' }
      ]},
      { text: 'Models & Reasoning', items: [
        { text: 'Thinking Effort & Disabling Thinking', link: '/en/advanced/reasoning-modes' }
      ]},
      { text: 'Extension Development', items: [
        { text: 'Custom Dashboard Cards', link: '/en/advanced/dashboard-cards' },
        { text: 'Plugin Development Guide', link: '/en/advanced/plugin-dev' }
      ]}
    ]
  };

  /* 英文页存在缺正文页时，侧边栏链接指向对应中文页（避免 404）。
   * 键为英文路径，值为中文路径；生成对应英文页后从本表移除即自动指向英文页。 */
  var EN_MISSING_FALLBACK = {
    '/en/guide/chat': '/guide/chat',
    '/en/guide/modes': '/guide/modes',
    '/en/guide/goal': '/guide/goal',
    '/en/guide/checkpoint': '/guide/checkpoint',
    '/en/guide/terminal': '/guide/terminal',
    '/en/guide/browser': '/guide/browser',
    '/en/guide/files': '/guide/files',
    '/en/guide/git': '/guide/git',
    '/en/guide/tablet': '/guide/tablet',
    '/en/guide/default-models': '/guide/default-models',
    '/en/guide/token-stats': '/guide/token-stats',
    '/en/guide/container': '/guide/container',
    '/en/guide/remote-ssh': '/guide/remote-ssh',
    '/en/guide/sync': '/guide/sync',
    '/en/guide/proxy': '/guide/proxy',
    '/en/guide/mcp': '/guide/mcp',
    '/en/guide/skills': '/guide/skills',
    '/en/guide/subagent': '/guide/subagent',
    '/en/guide/shizuku': '/guide/shizuku',
    '/en/guide/custom-prompts': '/guide/custom-prompts',
    '/en/guide/memory': '/guide/memory',
    '/en/guide/general': '/guide/general',
    '/en/guide/appearance': '/guide/appearance',
    '/en/guide/permissions': '/guide/permissions',
    '/en/guide/app-permissions': '/guide/app-permissions',
    '/en/guide/background-run': '/guide/background-run',
    '/en/guide/logs': '/guide/logs',
    '/en/guide/error-codes': '/guide/error-codes',
    '/en/guide/storage': '/guide/storage',
    '/en/guide/backup': '/guide/backup',
    '/en/guide/about': '/guide/about',
    '/en/advanced/build-android-app': '/advanced/build-android-app',
    '/en/advanced/build-flutter-app': '/advanced/build-flutter-app',
    '/en/advanced/build-flutter-release': '/advanced/build-flutter-release',
    '/en/advanced/playwright-mcp': '/advanced/playwright-mcp',
    '/en/advanced/adb-wireless-debug': '/advanced/adb-wireless-debug',
    '/en/advanced/reasoning-modes': '/advanced/reasoning-modes',
    '/en/advanced/dashboard-cards': '/advanced/dashboard-cards',
    '/en/advanced/plugin-dev': '/advanced/plugin-dev'
  };

  var curPath = normalize(path).replace(/\/$/, '');

  function esc(s) {
    return ('' + s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }
  function linkFor(item) {
    var fb = EN_MISSING_FALLBACK[item.link];
    /* 缺英文正文的条目临时指向中文页；target=_blank 让 VitePress 路由放行为整页跳转（避免 SPA 404） */
    return fb ? { href: fb, external: true } : { href: item.link, external: false };
  }
  function isActive(link) {
    return normalize(link) === curPath;
  }

  /* 生成单个侧边栏条目（level-1 链接项），复刻 VitePress DOM 结构以沿用其样式 */
  function sidebarItemHtml(item) {
    var active = isActive(item.link);
    var L = linkFor(item);
    return '<div class="VPSidebarItem level-1 is-link' + (active ? ' is-active' : '') + '">' +
      '<div class="item"><div class="indicator"></div>' +
      '<a class="VPLink link link' + (L.external ? ' vp-external-link-icon' : '') + '" href="' + esc(L.href) + '"' +
      (L.external ? ' target="_top"' : '') + '>' +
      '<p class="text">' + esc(item.text) + '</p></a></div></div>';
  }

  /* 生成一个分组（level-0），激活分组加 has-active */
  function sidebarGroupHtml(group) {
    var anyActive = (group.items || []).some(function (it) { return isActive(it.link); });
    var inner = (group.items || []).map(sidebarItemHtml).join('');
    return '<div class="no-transition group"><section class="VPSidebarItem level-0' +
      (anyActive ? ' has-active' : '') + '">' +
      '<div class="item" role="button" tabindex="0"><div class="indicator"></div>' +
      '<h2 class="text">' + esc(group.text) + '</h2></div>' +
      '<div class="items">' + inner + '</div></section></div>';
  }

  /* 重建英文侧边栏：hydrate 后 Vue 已接管预渲染 DOM，直接重写 .VPSidebarNav 内容 */
  function rebuildEnSidebar() {
    if (!isEn) return;
    var nav = document.getElementById('VPSidebarNav');
    if (!nav) return;
    var base = curPath.indexOf('/en/advanced/') === 0 ? '/en/advanced/' : '/en/guide/';
    var groups = EN_SIDEBAR[base] || EN_SIDEBAR['/en/guide/'];
    nav.innerHTML = '<span class="visually-hidden" id="sidebar-aria-label"> Sidebar Navigation </span>' +
      groups.map(sidebarGroupHtml).join('');
  }

  /* 重建英文顶部导航菜单（补 Advanced 入口） */
  function rebuildEnNavMenu() {
    if (!isEn) return;
    var menu = document.querySelector('.VPNavBarMenu.menu');
    if (!menu) return;
    menu.innerHTML = '<span id="main-nav-aria-label" class="visually-hidden"> Main Navigation </span>' +
      EN_NAV.map(function (n) {
        var external = /^https?:/.test(n.link);
        var active = n.activeMatch && curPath.indexOf(n.activeMatch) === 0;
        return '<a class="VPLink link VPNavBarMenuLink' + (active ? ' active' : '') +
          (external ? ' vp-external-link-icon' : '') + '" href="' + esc(n.link) + '"' +
          (external ? ' target="_blank" rel="noreferrer"' : ' tabindex="0"') + '>' +
          '<span>' + esc(n.text) + '</span></a>';
      }).join('');
  }

  function normalize(p) {
    p = ('' + p).replace(/index\.html$/, '').replace(/\.html$/, '');
    if (p.length > 1 && p.charAt(p.length - 1) === '/') p = p.slice(0, -1);
    return p;
  }
  function langHome(lang) { return lang === 'en' ? '/en/' : '/'; }

  /* 计算目标语言对应页；已在目标语言返回 null */
  function computeTarget(lang) {
    if (lang === CUR) return null;
    var p = normalize(path), t;
    if (lang === 'en') {
      t = isEn ? p : (p === '/' ? '/en' : '/en' + p);
      if (!/^\/en/.test(t)) t = '/en' + (t.charAt(0) === '/' ? t : '/' + t);
    } else {
      t = isEn ? (p.replace(/^\/en/, '') || '/') : p;
    }
    t = normalize(t);
    return t === '' ? langHome(lang) : t;
  }

  /* 跳转目标语言页；不存在(404)回退该语言首页 */
  function goTarget(lang) {
    var t = computeTarget(lang);
    if (t === null) return;
    fetch(t, { method: 'GET', cache: 'no-store' })
      .then(function (r) { location.href = (r && r.ok) ? t : langHome(lang); })
      .catch(function () { location.href = langHome(lang); });
  }
  function goHome() { location.href = langHome(CUR); }

  /* ---- 返回首页（本地化，修「英文页返回首页跳中文站」） ---- */
  function addHome() {
    var navs = document.querySelectorAll('.v-mobile-nav');
    for (var i = 0; i < navs.length; i++) {
      var nav = navs[i];
      if (!nav || nav.querySelector('.v-mobile-home')) continue;
      var a = document.createElement('a');
      a.href = isEn ? '/en/' : '/';
      a.className = 'v-mobile-home';
      a.textContent = isEn ? '⌂ Home' : '⌂ 返回首页';
      nav.insertBefore(a, nav.firstChild);
    }
  }

  /* ---- 中英文切换（菜单底部） ---- */
  function addLangSwitch() {
    var navs = document.querySelectorAll('.v-mobile-nav');
    for (var i = 0; i < navs.length; i++) {
      var nav = navs[i];
      if (!nav || nav.querySelector('.v-mobile-lang')) continue;
      var a = document.createElement('a');
      a.href = '#';
      a.className = 'v-mobile-lang';
      a.setAttribute('role', 'button');
      a.setAttribute('aria-label', isEn ? 'Switch to Simplified Chinese' : '切换到 English');
      a.innerHTML = '<span class="v-lang-badge">文</span><span>' + (isEn ? '简体中文' : 'English') + '</span>';
      a.addEventListener('click', function (e) { e.preventDefault(); goTarget(TARGET); });
      nav.appendChild(a); /* 菜单底部 */
    }
  }

  /* ---- 移动菜单统一修复 ---- */
  function injectMenuCss() {
    if (document.getElementById('aiv-menu-fix')) return;
    var style = document.createElement('style');
    style.id = 'aiv-menu-fix';
    style.textContent = [
      '.v-mobile-head{position:sticky;top:-8px;z-index:2;display:flex;align-items:center;justify-content:space-between;min-height:48px;padding:0 4px 0 8px;margin:0 -4px 6px;background:var(--vp-c-bg,#fff);border-bottom:1px solid var(--vp-c-divider,#e2e2e3);font-size:13px;font-weight:650;letter-spacing:.04em;color:var(--vp-c-text-2,#777);}',
      '.v-mobile-close{position:static!important;display:grid;place-items:center;width:36px;height:36px;border:1px solid var(--vp-c-divider,#ddd)!important;border-radius:10px!important;background:var(--vp-c-bg-alt,#f6f6f7)!important;font-size:23px!important;line-height:1!important;color:var(--vp-c-text-1,#333)!important;cursor:pointer;top:auto!important;right:auto!important;}',
      '.v-mobile-nav a{display:flex;align-items:center;min-height:46px;padding:10px 12px;border-radius:9px;color:var(--vp-c-text-1,#333);text-decoration:none;font-size:15px;font-weight:550;}',
      '.v-mobile-nav a:hover{background:var(--vp-c-brand-soft,rgba(10,60,255,.08));}',
      '.v-mobile-nav .v-mobile-home{margin-bottom:5px;background:var(--vp-c-brand-soft,rgba(10,60,255,.08));color:var(--vp-c-brand-1,#49695d);font-weight:650;}',
      /* 中英文切换：菜单底部，分隔线 + 居中 + 品牌色 */
      '.v-mobile-nav .v-mobile-lang{margin-top:12px;border-top:1px solid var(--vp-c-divider,#e2e2e3);border-radius:0;padding:14px 12px 2px;justify-content:center;gap:8px;color:var(--vp-c-brand-1,#49695d);font-weight:650;}',
      '.v-mobile-nav .v-lang-badge{display:inline-grid;place-items:center;width:20px;height:20px;border-radius:5px;background:var(--vp-c-brand-1,#49695d);color:#fff;font-size:12px;font-weight:700;}',
      /* 侧边栏外链箭头不换行：VitePress 默认给 .vp-external-link-icon::after 用 position:absolute（相对 <a> 定位），
         会被 block 文本挤到下一行。改成 static + 内联，箭头跟文本同行；文本 nowrap 防折行。 */
      '.VPSidebar .VPLink .text{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}',
      '.VPSidebar a.vp-external-link-icon::after{position:static;display:inline-block;margin-left:3px;vertical-align:middle;white-space:nowrap;}'
    ].join('\n');
    document.head.appendChild(style);
  }

  /* 确保菜单存在（缺失时补建完整菜单） */
  /* 重建移动抽屉的导航列表：英文页用 EN_NAV（含 Advanced），中文页读桌面 nav。可重复调用。 */
  function fillMobileNav(menu) {
    var mnav = menu.querySelector('.v-mobile-nav');
    if (!mnav) return;
    mnav.innerHTML = '';
    if (isEn) {
      EN_NAV.forEach(function (n) {
        if (!n || !n.link) return;
        var p = document.createElement('a');
        p.href = n.link;
        if (/^https?:/.test(n.link)) { p.target = '_blank'; p.rel = 'noreferrer'; }
        p.textContent = n.text;
        mnav.appendChild(p);
      });
      return;
    }
    var added = false;
    document.querySelectorAll('.VPNavBarMenuLink[href]').forEach(function (a) {
      var h = a.getAttribute('href');
      if (!h || h === '#') return;
      var p = document.createElement('a');
      p.href = h;
      p.textContent = (a.textContent || '').trim();
      mnav.appendChild(p);
      added = true;
    });
    if (!added) {
      document.querySelectorAll('.VPNavBarMenu a[href]').forEach(function (a) {
        var h = a.getAttribute('href');
        if (!h || h === '#') return;
        var p = document.createElement('a');
        p.href = h;
        p.textContent = (a.textContent || '').trim();
        mnav.appendChild(p);
      });
    }
  }

  function ensureMenu() {
    var ham = document.querySelector('.VPNavBarHamburger');
    if (!ham) return null;
    var menu = document.querySelector('.v-mobile-menu');
    if (!menu) {
      menu = document.createElement('div');
      menu.className = 'v-mobile-menu';
      menu.innerHTML = '<div class="v-mobile-mask"></div><div class="v-mobile-panel">'
        + '<div class="v-mobile-head"><span>' + (isEn ? 'Menu' : '导航菜单') + '</span><button class="v-mobile-close" type="button" aria-label="' + (isEn ? 'Close menu' : '关闭菜单') + '">&times;</button></div>'
        + '<div class="v-mobile-nav"></div></div>';
      document.body.appendChild(menu);
      fillMobileNav(menu);
      function closeMenu() { menu.classList.remove('open'); ham.setAttribute('aria-expanded', 'false'); }
      ham.addEventListener('click', function (e) {
        e.stopPropagation();
        menu.classList.add('open');
        ham.setAttribute('aria-expanded', 'true');
      });
      menu.querySelector('.v-mobile-mask').addEventListener('click', closeMenu);
      menu.querySelector('.v-mobile-close').addEventListener('click', closeMenu);
      /* 菜单内点击任意链接后自动关闭 */
      menu.querySelector('.v-mobile-nav').addEventListener('click', function (e) {
        var a = e.target && e.target.closest ? e.target.closest('a[href]') : null;
        if (a) closeMenu();
      });
      /* 页面切换后也自动关闭 */
      window.addEventListener('vp:page-change', closeMenu);
    } else {
      /* 菜单壳已存在（SPA 残留），仍刷新导航列表保证与当前语言/条目一致 */
      fillMobileNav(menu);
    }
    return menu;
  }

  /* 已有菜单但缺头栏 → 注入头栏（双语） */
  function fixMenuHead() {
    var menu = document.querySelector('.v-mobile-menu');
    if (!menu) return;
    var panel = menu.querySelector('.v-mobile-panel');
    if (!panel) return;
    var head = menu.querySelector('.v-mobile-head');
    var nav = menu.querySelector('.v-mobile-nav');
    var close = menu.querySelector('.v-mobile-close');
    if (!head) {
      head = document.createElement('div');
      head.className = 'v-mobile-head';
      var span = document.createElement('span');
      span.textContent = isEn ? 'Menu' : '导航菜单';
      head.appendChild(span);
      if (close) {
        close.setAttribute('aria-label', isEn ? 'Close menu' : '关闭菜单');
        head.appendChild(close);
      } else {
        var c = document.createElement('button');
        c.type = 'button';
        c.className = 'v-mobile-close';
        c.setAttribute('aria-label', isEn ? 'Close menu' : '关闭菜单');
        c.innerHTML = '&times;';
        c.addEventListener('click', function () { menu.classList.remove('open'); });
        head.appendChild(c);
      }
      panel.insertBefore(head, nav || panel.firstChild);
    }
  }

  /* hydrate 后重建英文侧边栏/导航 DOM（改 __VP_SITE_DATA__ 对已接管 DOM 无效，故直接重建） */
  function rebuildEn() { rebuildEnSidebar(); rebuildEnNavMenu(); }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', rebuildEn);
  } else {
    rebuildEn();
  }

  /* 全量执行 */
  function run() {
    injectMenuCss();
    ensureMenu();
    fixMenuHead();
    addHome();
    addLangSwitch();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', run);
  } else {
    run();
  }

  /* 下载页：切换版本通道/下载源时内容淡入过渡 */
  function addSwitchFade() {
    document.querySelectorAll('.switch').forEach(function (s) {
      s.addEventListener('click', function (e) {
        if (!e.target.closest('.switch-btn')) return;
        ['.info-card', '.pkgs', '.ver-big', '.release-note'].forEach(function (sel) {
          var el = document.querySelector(sel);
          if (!el) return;
          el.classList.remove('aiv-fade');
          void el.offsetWidth;
          el.classList.add('aiv-fade');
        });
      });
    });
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', addSwitchFade);
  } else {
    addSwitchFade();
  }
})();


/* 搜索按钮统一交给 search.php（VitePress 本地搜索弹层不再弹出） */
(function () {
  function wirePhpSearch() {
    document.addEventListener('click', function (e) {
      var btn = e.target && e.target.closest ? e.target.closest('.VPNavBarSearch button, .DocSearch-Button') : null;
      if (!btn) return;
      e.preventDefault();
      e.stopPropagation();
      location.href = /^\/en(\/|$)/.test(location.pathname) ? '/search.php?lang=en' : '/search.php';
    }, true);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wirePhpSearch);
  else wirePhpSearch();
})();


/* 汉堡事件委托：英文页导航重建后监听仍生效，点击右上角展开居中菜单 */
(function () {
  function wireHamburger() {
    document.addEventListener('click', function (e) {
      var ham = e.target && e.target.closest ? e.target.closest('.VPNavBarHamburger') : null;
      if (!ham) return;
      var menu = document.querySelector('.v-mobile-menu');
      if (!menu) return;
      menu.classList.add('open');
      ham.setAttribute('aria-expanded', 'true');
    }, true);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', wireHamburger);
  else wireHamburger();
})();
