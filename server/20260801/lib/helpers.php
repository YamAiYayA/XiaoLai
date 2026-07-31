<?php

function app_base_path(): string
{
    return dirname(__DIR__);
}

function load_config(): array
{
    $file = app_base_path() . '/config.php';
    if (!file_exists($file)) {
        return [];
    }
    $config = require $file;
    return is_array($config) ? $config : [];
}

function json_input(): array
{
    $raw = file_get_contents('php://input');
    if (!$raw) {
        return $_POST ?: [];
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

function respond_json($data, int $code = 200): void
{
    http_response_code($code);
    header('Content-Type: application/json; charset=utf-8');
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Access-Token');
    header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
    echo json_encode($data, JSON_UNESCAPED_UNICODE);
    exit;
}

function respond_ok($data = null): void
{
    respond_json(['success' => true, 'data' => $data]);
}

function respond_fail(string $message, int $code = 400): void
{
    respond_json(['success' => false, 'message' => $message], $code);
}

function parse_cloud_date($value): ?string
{
    if ($value === null || $value === '') {
        return null;
    }
    if (is_array($value) && isset($value['$date'])) {
        $value = $value['$date'];
    }
    if (is_numeric($value)) {
        $ts = (int)$value;
        if ($ts > 20000000000) {
            $ts = (int)floor($ts / 1000);
        }
        return gmdate('Y-m-d H:i:s', $ts);
    }
    $time = strtotime((string)$value);
    if ($time === false) {
        return null;
    }
    return gmdate('Y-m-d H:i:s', $time);
}

function read_ndjson(string $path): array
{
    if (!file_exists($path)) {
        return [];
    }
    $text = trim(file_get_contents($path));
    if ($text === '') {
        return [];
    }
    if ($text[0] === '[') {
        $data = json_decode($text, true);
        return is_array($data) ? $data : [];
    }
    $rows = [];
    foreach (preg_split("/\r\n|\n|\r/", $text) as $line) {
        $line = trim($line);
        if ($line === '') {
            continue;
        }
        $row = json_decode($line, true);
        if (is_array($row)) {
            $rows[] = $row;
        }
    }
    return $rows;
}

function db_connect(array $config): PDO
{
    $dsn = sprintf(
        'mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4',
        $config['db_host'] ?? '127.0.0.1',
        $config['db_port'] ?? '3306',
        $config['db_name'] ?? 'GuoZY'
    );
    $pdo = new PDO($dsn, $config['db_user'], $config['db_pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    ]);
    return $pdo;
}

function bearer_token(): string
{
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (stripos($header, 'Bearer ') === 0) {
        return trim(substr($header, 7));
    }
    if (!empty($_SERVER['HTTP_X_ACCESS_TOKEN'])) {
        return trim((string)$_SERVER['HTTP_X_ACCESS_TOKEN']);
    }
    if (!empty($_GET['token'])) {
        return trim((string)$_GET['token']);
    }
    return '';
}

function require_member(PDO $pdo): array
{
    $token = bearer_token();
    if ($token === '') {
        respond_fail('缺少登录凭证', 401);
    }
    $stmt = $pdo->prepare('SELECT * FROM family_members WHERE access_token = ? AND status = ? LIMIT 1');
    $stmt->execute([$token, 'active']);
    $member = $stmt->fetch();
    if (!$member) {
        respond_fail('登录已失效', 401);
    }
    return $member;
}
