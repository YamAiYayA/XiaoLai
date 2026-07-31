<?php
/**
 * One-time installer: create tables + import WeChat cloud export JSON.
 * Open on phone after uploading this folder:
 * https://api.guoziai.com/20260801/install.php
 */
require_once __DIR__ . '/lib/helpers.php';

$lockFile = __DIR__ . '/install.lock';
$dataDir = __DIR__ . '/data';
$schemaFile = __DIR__ . '/schema.sql';

function find_data_file(string $dir, string $keyword): string
{
    foreach (glob($dir . '/*.json') as $file) {
        if (stripos(basename($file), $keyword) !== false) {
            return $file;
        }
    }
    return '';
}

function random_token(): string
{
    return bin2hex(random_bytes(24));
}

$defaults = [
    'db_host' => '127.0.0.1',
    'db_port' => '3306',
    'db_name' => 'GuoZY',
    'db_user' => 'GuoZY',
    'db_pass' => '',
];

$existing = load_config();
$config = array_merge($defaults, $existing);
$message = '';
$links = [];
$done = file_exists($lockFile);

if ($_SERVER['REQUEST_METHOD'] === 'POST' && !$done) {
    $config['db_host'] = trim($_POST['db_host'] ?? '127.0.0.1');
    $config['db_port'] = trim($_POST['db_port'] ?? '3306');
    $config['db_name'] = trim($_POST['db_name'] ?? 'GuoZY');
    $config['db_user'] = trim($_POST['db_user'] ?? 'GuoZY');
    $config['db_pass'] = (string)($_POST['db_pass'] ?? '');

    try {
        $pdo = db_connect($config);
        $schema = file_get_contents($schemaFile);
        $pdo->exec($schema);

        $families = read_ndjson(find_data_file($dataDir, 'BB-families'));
        $babies = read_ndjson(find_data_file($dataDir, 'BB-babies'));
        $members = read_ndjson(find_data_file($dataDir, 'BB-familyMembers'));
        $records = read_ndjson(find_data_file($dataDir, 'BB-records'));
        $todos = read_ndjson(find_data_file($dataDir, 'BB-todos'));

        $pdo->exec('SET FOREIGN_KEY_CHECKS=0');
        $pdo->exec('TRUNCATE TABLE todos');
        $pdo->exec('TRUNCATE TABLE records');
        $pdo->exec('TRUNCATE TABLE family_members');
        $pdo->exec('TRUNCATE TABLE babies');
        $pdo->exec('TRUNCATE TABLE families');

        $familyStmt = $pdo->prepare('INSERT INTO families (id, name, invite_code, owner_open_id, baby_id, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)');
        foreach ($families as $row) {
            $familyStmt->execute([
                $row['_id'],
                $row['name'] ?? '',
                $row['inviteCode'] ?? '',
                $row['ownerOpenId'] ?? '',
                $row['babyId'] ?? null,
                $row['status'] ?? 'active',
                parse_cloud_date($row['createdAt'] ?? null),
                parse_cloud_date($row['updatedAt'] ?? null),
            ]);
        }

        $babyStmt = $pdo->prepare('INSERT INTO babies (id, family_id, nickname, birthday, birth_hour, gender, note, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)');
        foreach ($babies as $row) {
            $babyStmt->execute([
                $row['_id'],
                $row['familyId'] ?? '',
                $row['nickname'] ?? '',
                $row['birthday'] ?? null,
                isset($row['birthHour']) ? (int)$row['birthHour'] : null,
                $row['gender'] ?? null,
                $row['note'] ?? null,
                parse_cloud_date($row['createdAt'] ?? null),
                parse_cloud_date($row['updatedAt'] ?? null),
            ]);
        }

        $memberStmt = $pdo->prepare('INSERT INTO family_members (id, family_id, open_id, display_name, role, status, access_token, joined_at, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)');
        foreach ($members as $row) {
            $token = random_token();
            $memberStmt->execute([
                $row['_id'],
                $row['familyId'] ?? '',
                $row['openId'] ?? '',
                $row['displayName'] ?? '',
                $row['role'] ?? 'member',
                $row['status'] ?? 'active',
                $token,
                parse_cloud_date($row['joinedAt'] ?? null),
                parse_cloud_date($row['createdAt'] ?? null),
                parse_cloud_date($row['updatedAt'] ?? null),
            ]);
            $links[] = [
                'name' => $row['displayName'] ?? '成员',
                'role' => $row['role'] ?? 'member',
                'token' => $token,
            ];
        }

        $recordStmt = $pdo->prepare('INSERT INTO records (id, family_id, baby_id, event_type, occurred_at, date_key, payload_json, image_file_ids_json, created_by_open_id, created_by_name, is_deleted, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)');
        foreach ($records as $row) {
            $occurred = $row['occurredAt'] ?? 0;
            if (is_array($occurred) && isset($occurred['$date'])) {
                $occurred = (int)(strtotime($occurred['$date']) * 1000);
            }
            $recordStmt->execute([
                $row['_id'],
                $row['familyId'] ?? '',
                $row['babyId'] ?? '',
                $row['eventType'] ?? '',
                (int)$occurred,
                $row['dateKey'] ?? '',
                json_encode($row['payload'] ?? new stdClass(), JSON_UNESCAPED_UNICODE),
                json_encode($row['imageFileIds'] ?? [], JSON_UNESCAPED_UNICODE),
                $row['createdByOpenId'] ?? null,
                $row['createdByName'] ?? null,
                !empty($row['isDeleted']) ? 1 : 0,
                parse_cloud_date($row['createdAt'] ?? null),
                parse_cloud_date($row['updatedAt'] ?? null),
            ]);
        }

        $todoStmt = $pdo->prepare('INSERT INTO todos (id, family_id, baby_id, category, title, note, status, due_date_key, created_by_open_id, created_by_name, completed_at, is_deleted, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)');
        foreach ($todos as $row) {
            $todoStmt->execute([
                $row['_id'],
                $row['familyId'] ?? '',
                $row['babyId'] ?? null,
                $row['category'] ?? 'baby',
                $row['title'] ?? '',
                $row['note'] ?? null,
                $row['status'] ?? 'open',
                $row['dueDateKey'] ?? null,
                $row['createdByOpenId'] ?? null,
                $row['createdByName'] ?? null,
                parse_cloud_date($row['completedAt'] ?? null),
                !empty($row['isDeleted']) ? 1 : 0,
                parse_cloud_date($row['createdAt'] ?? null),
                parse_cloud_date($row['updatedAt'] ?? null),
            ]);
        }

        $configExport = "<?php\nreturn " . var_export([
            'db_host' => $config['db_host'],
            'db_port' => $config['db_port'],
            'db_name' => $config['db_name'],
            'db_user' => $config['db_user'],
            'db_pass' => $config['db_pass'],
            'app_name' => '宝宝记录',
        ], true) . ";\n";
        file_put_contents(__DIR__ . '/config.php', $configExport);
        file_put_contents($lockFile, date('c') . "\nimported_records=" . count($records) . "\n");
        $done = true;
        $message = '安装成功：家庭 ' . count($families) . '，宝宝 ' . count($babies) . '，成员 ' . count($members) . '，记录 ' . count($records) . '，待办 ' . count($todos);
    } catch (Throwable $e) {
        $message = '安装失败：' . $e->getMessage();
    }
} elseif ($done) {
    $message = '已经安装过了。如需重装，先删除 install.lock 再打开本页。';
    try {
        $pdo = db_connect(load_config());
        $rows = $pdo->query('SELECT display_name, role, access_token FROM family_members WHERE status="active" ORDER BY role DESC, display_name ASC')->fetchAll();
        foreach ($rows as $row) {
            $links[] = [
                'name' => $row['display_name'],
                'role' => $row['role'],
                'token' => $row['access_token'],
            ];
        }
    } catch (Throwable $e) {
        // ignore
    }
}
?>
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>宝宝记录 · 安装导入</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background:#FFF7F9; color:#3D2C29; margin:0; padding:20px; }
    .card { background:#fff; border-radius:16px; padding:18px; margin:0 auto 16px; max-width:640px; box-shadow:0 8px 24px rgba(0,0,0,.06); }
    h1 { margin:0 0 8px; font-size:22px; }
    label { display:block; margin:10px 0 4px; font-size:14px; }
    input { width:100%; padding:10px 12px; border:1px solid #E7D5DA; border-radius:10px; box-sizing:border-box; }
    button { margin-top:14px; width:100%; padding:12px; border:0; border-radius:12px; background:#FF6B81; color:#fff; font-size:16px; }
    .msg { white-space:pre-wrap; background:#FFF1F4; padding:12px; border-radius:12px; }
    .token { word-break:break-all; font-family:ui-monospace, monospace; font-size:12px; background:#F7F7F7; padding:8px; border-radius:8px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>宝宝记录 · 安装导入</h1>
    <p>会把小程序导出的数据写入 MySQL 库 <b>GuoZY</b>，并生成爸爸/妈妈登录口令。</p>
    <?php if ($message): ?><div class="msg"><?= htmlspecialchars($message, ENT_QUOTES, 'UTF-8') ?></div><?php endif; ?>
  </div>

  <?php if (!$done): ?>
  <div class="card">
    <form method="post">
      <label>数据库主机</label>
      <input name="db_host" value="<?= htmlspecialchars($config['db_host'], ENT_QUOTES, 'UTF-8') ?>" />
      <label>端口</label>
      <input name="db_port" value="<?= htmlspecialchars($config['db_port'], ENT_QUOTES, 'UTF-8') ?>" />
      <label>数据库名</label>
      <input name="db_name" value="<?= htmlspecialchars($config['db_name'], ENT_QUOTES, 'UTF-8') ?>" />
      <label>用户名</label>
      <input name="db_user" value="<?= htmlspecialchars($config['db_user'], ENT_QUOTES, 'UTF-8') ?>" />
      <label>密码</label>
      <input name="db_pass" type="password" value="<?= htmlspecialchars($config['db_pass'], ENT_QUOTES, 'UTF-8') ?>" />
      <button type="submit">开始建表并导入数据</button>
    </form>
  </div>
  <?php endif; ?>

  <?php if ($links): ?>
  <div class="card">
    <h1>家庭成员登录口令</h1>
    <p>在 App 里粘贴对应 token 即可进入（媳妇用「妈妈」）。</p>
    <?php foreach ($links as $link): ?>
      <p><b><?= htmlspecialchars($link['name'], ENT_QUOTES, 'UTF-8') ?></b>（<?= htmlspecialchars($link['role'], ENT_QUOTES, 'UTF-8') ?>）</p>
      <div class="token"><?= htmlspecialchars($link['token'], ENT_QUOTES, 'UTF-8') ?></div>
    <?php endforeach; ?>
  </div>
  <?php endif; ?>
</body>
</html>
