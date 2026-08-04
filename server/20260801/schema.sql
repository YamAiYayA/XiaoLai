CREATE TABLE IF NOT EXISTS families (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  invite_code VARCHAR(32) NOT NULL,
  owner_open_id VARCHAR(128) NOT NULL,
  baby_id VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_invite_code (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS babies (
  id VARCHAR(64) PRIMARY KEY,
  family_id VARCHAR(64) NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  birthday DATE NULL,
  birth_hour INT NULL,
  gender VARCHAR(16) NULL,
  note VARCHAR(255) NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  KEY idx_family (family_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS family_members (
  id VARCHAR(64) PRIMARY KEY,
  family_id VARCHAR(64) NOT NULL,
  open_id VARCHAR(128) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'member',
  status VARCHAR(32) NOT NULL DEFAULT 'active',
  access_token VARCHAR(64) NOT NULL,
  joined_at DATETIME NULL,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  UNIQUE KEY uk_token (access_token),
  UNIQUE KEY uk_family_open (family_id, open_id),
  KEY idx_open (open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS records (
  id VARCHAR(64) PRIMARY KEY,
  family_id VARCHAR(64) NOT NULL,
  baby_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  occurred_at BIGINT NOT NULL,
  date_key CHAR(10) NOT NULL,
  payload_json LONGTEXT NULL,
  image_file_ids_json LONGTEXT NULL,
  created_by_open_id VARCHAR(128) NULL,
  created_by_name VARCHAR(64) NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  KEY idx_timeline (family_id, baby_id, date_key, is_deleted),
  KEY idx_occurred (family_id, baby_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS todos (
  id VARCHAR(64) PRIMARY KEY,
  family_id VARCHAR(64) NOT NULL,
  baby_id VARCHAR(64) NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'baby',
  title VARCHAR(255) NOT NULL,
  note TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'open',
  due_date_key CHAR(10) NULL,
  created_by_open_id VARCHAR(128) NULL,
  created_by_name VARCHAR(64) NULL,
  completed_at DATETIME NULL,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NULL,
  updated_at DATETIME NULL,
  KEY idx_family_status (family_id, status, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
