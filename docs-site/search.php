<?php
/**
 * 服务端搜索页：读取 ?wd=（兼容 ?q=），在 search-index.json 中检索并渲染结果。
 * 索引由 search.html 内联的 VINDEX 抽取而来，两处共用同一份数据。
 */

$indexFile = __DIR__ . '/search-index.json';
$raw = isset($_GET['wd']) ? $_GET['wd'] : (isset($_GET['q']) ? $_GET['q'] : '');
$q = trim(strip_tags((string) $raw));
$q = mb_substr($q, 0, 64, 'UTF-8');

/** 截取关键词命中处的摘要片段（中文按字符计）。 */
function make_snippet(string $text, string $needle): string
{
    $t = trim((string) preg_replace('/\s+/u', ' ', $text));
    $len = mb_strlen($t, 'UTF-8');
    $pos = mb_strpos(mb_strtolower($t, 'UTF-8'), $needle, 0, 'UTF-8');
    if ($pos === false) {
        return mb_substr($t, 0, 120, 'UTF-8') . ($len > 120 ? '…' : '');
    }
    $start = max(0, $pos - 40);
    $snip = ($start > 0 ? '…' : '') . mb_substr($t, $start, 140, 'UTF-8');
    if ($start + 140 < $len) {
        $snip .= '…';
    }
    return $snip;
}

/** 把查询词包成 <mark> 高亮；入参均已转义，避免破坏 HTML。 */
function highlight(string $escaped, string $escapedQ): string
{
    if ($escapedQ === '') {
        return $escaped;
    }
    return str_replace($escapedQ, '<mark>' . $escapedQ . '</mark>', $escaped);
}

$results = [];
$total = 0;
if ($q !== '' && is_file($indexFile)) {
    $items = json_decode((string) file_get_contents($indexFile), true) ?: [];
    $needle = mb_strtolower($q, 'UTF-8');
    foreach ($items as $it) {
        $title = (string) ($it['title'] ?? '');
        $text = (string) ($it['text'] ?? '');
        $tl = mb_strtolower($title, 'UTF-8');
        $xl = mb_strtolower($text, 'UTF-8');
        $hay = $tl . ' ' . $xl;
        if ($hay === '' || mb_strpos($hay, $needle, 0, 'UTF-8') === false) {
            continue;
        }
        $total++;
        $results[] = [
            'score' => mb_substr_count($tl, $needle, 'UTF-8') * 10 + min(mb_substr_count($xl, $needle, 'UTF-8'), 30),
            'url' => (string) ($it['url'] ?? ''),
            'title' => $title,
            'snippet' => make_snippet($text, $needle),
        ];
    }
    usort($results, static fn (array $a, array $b): int => $b['score'] <=> $a['score']);
    $results = array_slice($results, 0, 30);
}

$esc = static fn (string $s): string => htmlspecialchars($s, ENT_QUOTES, 'UTF-8');
$escQ = $esc($q);
$titleTag = $q !== '' ? $escQ . ' - 搜索 - AiVerse' : '搜索 - AiVerse';
?>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title><?= $titleTag ?></title>
<style>
:root {
  --av-bg: #ffffff;
  --av-text: #3c3c43;
  --av-text-2: #67676c;
  --av-text-3: #929295;
  --av-border: #e2e2e3;
  --av-soft: #f6f6f7;
  --av-brand: #3451b2;
  --av-brand-2: #3a5ccc;
  --av-brand-soft: rgba(52, 81, 178, .12);
}
* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  background: var(--av-bg);
  color: var(--av-text);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Helvetica, Arial, sans-serif;
  line-height: 1.7;
}
header {
  border-bottom: 1px solid var(--av-border);
}
.nav {
  max-width: 1152px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 16px;
  color: var(--av-text);
  text-decoration: none;
}
.brand img { width: 24px; height: 24px; }
.back { margin-left: auto; font-size: 14px; color: var(--av-brand); text-decoration: none; }
.back:hover { color: var(--av-brand-2); }
main { max-width: 720px; margin: 0 auto; padding: 40px 24px 64px; }
h1 { font-size: 28px; line-height: 1.3; margin: 0 0 24px; font-weight: 700; }
.search-form { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input {
  flex: 1;
  height: 44px;
  padding: 0 14px;
  font-size: 15px;
  color: var(--av-text);
  background: var(--av-soft);
  border: 1px solid var(--av-border);
  border-radius: 8px;
  outline: none;
}
.search-input:focus { border-color: var(--av-brand); background: var(--av-bg); }
.search-btn {
  height: 44px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: var(--av-brand);
  border: 0;
  border-radius: 8px;
  cursor: pointer;
}
.search-btn:hover { background: var(--av-brand-2); }
.meta { font-size: 13px; color: var(--av-text-3); margin-bottom: 20px; }
.result { display: block; padding: 14px 16px; margin-bottom: 10px; border: 1px solid var(--av-border); border-radius: 10px; text-decoration: none; transition: border-color .2s, background .2s; }
.result:hover { border-color: var(--av-brand); background: var(--av-soft); }
.result-title { font-size: 16px; font-weight: 600; color: var(--av-brand); margin-bottom: 4px; }
.result-url { font-size: 12px; color: var(--av-text-3); margin-bottom: 6px; word-break: break-all; }
.result-snippet { font-size: 14px; color: var(--av-text-2); }
mark { background: var(--av-brand-soft); color: var(--av-brand); border-radius: 3px; padding: 0 2px; }
.empty { font-size: 15px; color: var(--av-text-2); padding: 24px 0; }
@media (max-width: 640px) { main { padding: 24px 16px 48px; } h1 { font-size: 24px; } }
</style>
</head>
<body>
<header>
  <div class="nav">
    <a class="brand" href="/"><img src="/logo.png" alt="AiVerse">AiVerse</a>
    <a class="back" href="/">返回文档</a>
  </div>
</header>
<main>
  <h1>搜索文档</h1>
  <form class="search-form" method="get" action="">
    <input class="search-input" type="search" name="wd" value="<?= $escQ ?>" placeholder="输入关键词，如：插件、终端、SSH" autofocus>
    <button class="search-btn" type="submit">搜索</button>
  </form>
  <?php if ($q === ''): ?>
    <p class="meta">输入关键词检索 AiVerse 文档，支持中文与英文。</p>
  <?php else: ?>
    <p class="meta">共找到 <?= count($results) ?> 条<?= $total > count($results) ? '（已截取前 30 条）' : '' ?>与「<?= $escQ ?>」相关的内容。</p>
    <?php if ($results === []): ?>
      <p class="empty">未找到与「<?= $escQ ?>」相关的内容，可尝试更换关键词。</p>
    <?php else: ?>
      <?php foreach ($results as $r): ?>
        <a class="result" href="<?= $esc($r['url']) ?>">
          <div class="result-title"><?= highlight($esc($r['title']), $escQ) ?></div>
          <div class="result-url"><?= $esc($r['url']) ?></div>
          <div class="result-snippet"><?= highlight($esc($r['snippet']), $escQ) ?></div>
        </a>
      <?php endforeach; ?>
    <?php endif; ?>
  <?php endif; ?>
</main>
</body>
</html>
