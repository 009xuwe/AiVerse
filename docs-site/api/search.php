<?php
/**
 * AI 抓取用文档搜索接口。
 * GET /api/search.php?wd=关键词[&limit=N]
 * 返回结构化 JSON：{ query, count, total, results: [{url,title,snippet}] }
 * 与搜索页共用同一份 search-index.json。
 */

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

$indexFile = dirname(__DIR__) . '/search-index.json';

$raw = isset($_GET['wd']) ? $_GET['wd'] : (isset($_GET['q']) ? $_GET['q'] : '');
$q = trim(strip_tags((string) $raw));
$q = mb_substr($q, 0, 64, 'UTF-8');
$limit = isset($_GET['limit']) ? max(1, min(30, (int) $_GET['limit'])) : 10;

function api_snippet(string $text, string $needle): string
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
            'url' => 'https://www.aivapp.cn' . (string) ($it['url'] ?? ''),
            'title' => $title,
            'snippet' => api_snippet($text, $needle),
        ];
    }
    usort($results, static fn (array $a, array $b): int => $b['score'] <=> $a['score']);
    $results = array_slice($results, 0, $limit);
    foreach ($results as &$r) {
        unset($r['score']);
    }
    unset($r);
}

echo json_encode(
    ['query' => $q, 'count' => count($results), 'total' => $total, 'results' => $results],
    JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
);
