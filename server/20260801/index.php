<?php
require_once __DIR__ . '/lib/helpers.php';

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    respond_ok(['ok' => true]);
}

$config = load_config();
if (!$config) {
    respond_fail('服务未安装，请先打开 install.php', 503);
}

try {
    $pdo = db_connect($config);
} catch (Throwable $e) {
    respond_fail('数据库连接失败：' . $e->getMessage(), 500);
}

$route = $_GET['r'] ?? 'health';
$input = array_merge($_GET, json_input());

function map_member(array $row): array
{
    return [
        'id' => $row['id'],
        'familyId' => $row['family_id'],
        'openId' => $row['open_id'],
        'displayName' => $row['display_name'],
        'role' => $row['role'],
        'status' => $row['status'],
        'accessToken' => $row['access_token'],
    ];
}

function map_baby(array $row): array
{
    return [
        'id' => $row['id'],
        'familyId' => $row['family_id'],
        'nickname' => $row['nickname'],
        'birthday' => $row['birthday'],
        'birthHour' => $row['birth_hour'],
        'gender' => $row['gender'],
        'note' => $row['note'],
    ];
}

function map_record(array $row): array
{
    return [
        'id' => $row['id'],
        'familyId' => $row['family_id'],
        'babyId' => $row['baby_id'],
        'eventType' => $row['event_type'],
        'occurredAt' => (int)$row['occurred_at'],
        'dateKey' => $row['date_key'],
        'payload' => json_decode($row['payload_json'] ?: '{}', true) ?: new stdClass(),
        'imageFileIds' => json_decode($row['image_file_ids_json'] ?: '[]', true) ?: [],
        'createdByOpenId' => $row['created_by_open_id'],
        'createdByName' => $row['created_by_name'],
        'isDeleted' => (int)$row['is_deleted'] === 1,
        'createdAt' => $row['created_at'],
        'updatedAt' => $row['updated_at'],
    ];
}

function map_todo(array $row): array
{
    return [
        'id' => $row['id'],
        'familyId' => $row['family_id'],
        'babyId' => $row['baby_id'],
        'category' => $row['category'],
        'title' => $row['title'],
        'note' => $row['note'],
        'status' => $row['status'],
        'dueDateKey' => $row['due_date_key'],
        'createdByOpenId' => $row['created_by_open_id'],
        'createdByName' => $row['created_by_name'],
        'completedAt' => $row['completed_at'],
        'isDeleted' => (int)$row['is_deleted'] === 1,
    ];
}

function summarize_records(array $records): array
{
    $summary = [
        'formulaAmountTotal' => 0,
        'formulaFeedCount' => 0,
        'breastFeedCount' => 0,
        'poopCount' => 0,
        'careCount' => 0,
        'gasExerciseCount' => 0,
        'jaundiceCheckCount' => 0,
        'bathCount' => 0,
        'sleepStartCount' => 0,
        'sleepEndCount' => 0,
        'noteCount' => 0,
        'totalCount' => 0,
    ];
    foreach ($records as $record) {
        if (!empty($record['isDeleted'])) {
            continue;
        }
        $summary['totalCount']++;
        $type = $record['eventType'];
        $payload = is_array($record['payload']) ? $record['payload'] : [];
        if ($type === 'feeding_formula') {
            $summary['formulaFeedCount']++;
            $summary['formulaAmountTotal'] += (float)($payload['amountMl'] ?? 0);
        } elseif ($type === 'feeding_breast' || $type === 'feeding_warm_breast') {
            $summary['breastFeedCount']++;
        } elseif ($type === 'poop') {
            $summary['poopCount']++;
        } elseif ($type === 'care' || $type === 'butt_clean' || $type === 'pee_clean') {
            $summary['careCount']++;
        } elseif ($type === 'gas_exercise') {
            $summary['gasExerciseCount']++;
        } elseif ($type === 'jaundice_check') {
            $summary['jaundiceCheckCount']++;
        } elseif ($type === 'bath') {
            $summary['bathCount']++;
        } elseif ($type === 'sleep_start') {
            $summary['sleepStartCount']++;
        } elseif ($type === 'sleep_end') {
            $summary['sleepEndCount']++;
        } elseif ($type === 'note') {
            $summary['noteCount']++;
        }
    }
    return $summary;
}

switch ($route) {
    case 'health':
        respond_ok(['app' => $config['app_name'] ?? '宝宝记录', 'time' => date('c')]);
        break;

    case 'auth/login':
        $token = trim((string)($input['token'] ?? ''));
        $invite = strtoupper(trim((string)($input['inviteCode'] ?? '')));
        $displayName = trim((string)($input['displayName'] ?? ''));
        if ($token !== '') {
            $stmt = $pdo->prepare('SELECT * FROM family_members WHERE access_token = ? AND status = ? LIMIT 1');
            $stmt->execute([$token, 'active']);
            $member = $stmt->fetch();
            if (!$member) {
                respond_fail('口令无效');
            }
        } elseif ($invite !== '' && $displayName !== '') {
            $family = $pdo->prepare('SELECT * FROM families WHERE invite_code = ? AND status = ? LIMIT 1');
            $family->execute([$invite, 'active']);
            $familyRow = $family->fetch();
            if (!$familyRow) {
                respond_fail('邀请码无效');
            }
            $stmt = $pdo->prepare('SELECT * FROM family_members WHERE family_id = ? AND display_name = ? AND status = ? LIMIT 1');
            $stmt->execute([$familyRow['id'], $displayName, 'active']);
            $member = $stmt->fetch();
            if (!$member) {
                respond_fail('未找到该成员，请用口令登录');
            }
        } else {
            respond_fail('请提供 token，或邀请码+称呼');
        }
        respond_ok(map_member($member));
        break;

    case 'bootstrap/context':
        $member = require_member($pdo);
        $family = $pdo->prepare('SELECT * FROM families WHERE id = ? LIMIT 1');
        $family->execute([$member['family_id']]);
        $familyRow = $family->fetch();
        $baby = null;
        if ($familyRow && $familyRow['baby_id']) {
            $babyStmt = $pdo->prepare('SELECT * FROM babies WHERE id = ? LIMIT 1');
            $babyStmt->execute([$familyRow['baby_id']]);
            $babyRow = $babyStmt->fetch();
            if ($babyRow) {
                $baby = map_baby($babyRow);
            }
        }
        $members = $pdo->prepare('SELECT id, family_id, open_id, display_name, role, status FROM family_members WHERE family_id = ? AND status = ?');
        $members->execute([$member['family_id'], 'active']);
        respond_ok([
            'member' => map_member($member),
            'family' => $familyRow ? [
                'id' => $familyRow['id'],
                'name' => $familyRow['name'],
                'inviteCode' => $familyRow['invite_code'],
                'babyId' => $familyRow['baby_id'],
                'status' => $familyRow['status'],
            ] : null,
            'baby' => $baby,
            'members' => array_map(static function ($row) {
                return [
                    'id' => $row['id'],
                    'familyId' => $row['family_id'],
                    'openId' => $row['open_id'],
                    'displayName' => $row['display_name'],
                    'role' => $row['role'],
                    'status' => $row['status'],
                ];
            }, $members->fetchAll()),
        ]);
        break;

    case 'records/listByDate':
        $member = require_member($pdo);
        $dateKey = $input['dateKey'] ?? date('Y-m-d');
        $babyId = $input['babyId'] ?? '';
        $stmt = $pdo->prepare('SELECT * FROM records WHERE family_id = ? AND baby_id = ? AND date_key = ? AND is_deleted = 0 ORDER BY occurred_at DESC');
        $stmt->execute([$member['family_id'], $babyId, $dateKey]);
        respond_ok(array_map('map_record', $stmt->fetchAll()));
        break;

    case 'records/listRecent':
        $member = require_member($pdo);
        $babyId = $input['babyId'] ?? '';
        $limit = max(1, min(50, (int)($input['limit'] ?? 20)));
        $stmt = $pdo->prepare('SELECT * FROM records WHERE family_id = ? AND baby_id = ? AND is_deleted = 0 ORDER BY occurred_at DESC LIMIT ' . $limit);
        $stmt->execute([$member['family_id'], $babyId]);
        respond_ok(array_map('map_record', $stmt->fetchAll()));
        break;

    case 'records/create':
        $member = require_member($pdo);
        $babyId = trim((string)($input['babyId'] ?? ''));
        $eventType = trim((string)($input['eventType'] ?? ''));
        $dateKey = trim((string)($input['dateKey'] ?? date('Y-m-d')));
        $occurredAt = (int)($input['occurredAt'] ?? (int)round(microtime(true) * 1000));
        $payload = $input['payload'] ?? [];
        if ($babyId === '' || $eventType === '') {
            respond_fail('参数不完整');
        }
        $id = bin2hex(random_bytes(16));
        $now = gmdate('Y-m-d H:i:s');
        $stmt = $pdo->prepare('INSERT INTO records (id, family_id, baby_id, event_type, occurred_at, date_key, payload_json, image_file_ids_json, created_by_open_id, created_by_name, is_deleted, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,0,?,?)');
        $stmt->execute([
            $id,
            $member['family_id'],
            $babyId,
            $eventType,
            $occurredAt,
            $dateKey,
            json_encode($payload ?: new stdClass(), JSON_UNESCAPED_UNICODE),
            json_encode([], JSON_UNESCAPED_UNICODE),
            $member['open_id'],
            $member['display_name'],
            $now,
            $now,
        ]);
        $get = $pdo->prepare('SELECT * FROM records WHERE id = ?');
        $get->execute([$id]);
        respond_ok(map_record($get->fetch()));
        break;

    case 'records/delete':
        $member = require_member($pdo);
        $id = trim((string)($input['id'] ?? ''));
        $stmt = $pdo->prepare('UPDATE records SET is_deleted = 1, updated_at = ? WHERE id = ? AND family_id = ?');
        $stmt->execute([gmdate('Y-m-d H:i:s'), $id, $member['family_id']]);
        respond_ok(['id' => $id]);
        break;

    case 'stats/dashboard':
        $member = require_member($pdo);
        $babyId = $input['babyId'] ?? '';
        $dateKey = $input['dateKey'] ?? date('Y-m-d');
        $recentLimit = max(1, min(20, (int)($input['recentLimit'] ?? 5)));
        $todayStmt = $pdo->prepare('SELECT * FROM records WHERE family_id = ? AND baby_id = ? AND date_key = ? AND is_deleted = 0');
        $todayStmt->execute([$member['family_id'], $babyId, $dateKey]);
        $todayRecords = array_map('map_record', $todayStmt->fetchAll());

        $weekStart = date('Y-m-d', strtotime($dateKey . ' -6 days'));
        $weekStmt = $pdo->prepare('SELECT * FROM records WHERE family_id = ? AND baby_id = ? AND date_key >= ? AND date_key <= ? AND is_deleted = 0');
        $weekStmt->execute([$member['family_id'], $babyId, $weekStart, $dateKey]);
        $weekRecords = array_map('map_record', $weekStmt->fetchAll());

        $recentStmt = $pdo->prepare('SELECT * FROM records WHERE family_id = ? AND baby_id = ? AND is_deleted = 0 ORDER BY occurred_at DESC LIMIT ' . $recentLimit);
        $recentStmt->execute([$member['family_id'], $babyId]);
        respond_ok([
            'todaySummary' => summarize_records($todayRecords),
            'weekSummary' => summarize_records($weekRecords),
            'recentRecords' => array_map('map_record', $recentStmt->fetchAll()),
        ]);
        break;

    case 'todos/list':
        $member = require_member($pdo);
        $stmt = $pdo->prepare('SELECT * FROM todos WHERE family_id = ? AND is_deleted = 0 ORDER BY FIELD(status, "open", "done"), updated_at DESC');
        $stmt->execute([$member['family_id']]);
        respond_ok(array_map('map_todo', $stmt->fetchAll()));
        break;

    case 'todos/create':
        $member = require_member($pdo);
        $title = trim((string)($input['title'] ?? ''));
        if ($title === '') {
            respond_fail('标题不能为空');
        }
        $id = bin2hex(random_bytes(16));
        $now = gmdate('Y-m-d H:i:s');
        $stmt = $pdo->prepare('INSERT INTO todos (id, family_id, baby_id, category, title, note, status, due_date_key, created_by_open_id, created_by_name, is_deleted, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,0,?,?)');
        $stmt->execute([
            $id,
            $member['family_id'],
            $input['babyId'] ?? null,
            $input['category'] ?? 'baby',
            $title,
            $input['note'] ?? null,
            'open',
            $input['dueDateKey'] ?? null,
            $member['open_id'],
            $member['display_name'],
            $now,
            $now,
        ]);
        $get = $pdo->prepare('SELECT * FROM todos WHERE id = ?');
        $get->execute([$id]);
        respond_ok(map_todo($get->fetch()));
        break;

    case 'todos/toggle':
        $member = require_member($pdo);
        $id = trim((string)($input['id'] ?? ''));
        $get = $pdo->prepare('SELECT * FROM todos WHERE id = ? AND family_id = ? LIMIT 1');
        $get->execute([$id, $member['family_id']]);
        $todo = $get->fetch();
        if (!$todo) {
            respond_fail('待办不存在');
        }
        $next = $todo['status'] === 'done' ? 'open' : 'done';
        $completedAt = $next === 'done' ? gmdate('Y-m-d H:i:s') : null;
        $upd = $pdo->prepare('UPDATE todos SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?');
        $upd->execute([$next, $completedAt, gmdate('Y-m-d H:i:s'), $id]);
        $get->execute([$id, $member['family_id']]);
        respond_ok(map_todo($get->fetch()));
        break;

    default:
        respond_fail('未知接口: ' . $route, 404);
}
