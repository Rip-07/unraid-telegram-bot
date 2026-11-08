CREATE TABLE bot_users (
    telegram_user_id BIGINT PRIMARY KEY,
    username TEXT,
    role TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bot_users_role CHECK (role IN ('ADMIN', 'USER'))
);

CREATE TABLE managed_containers (
    container_name TEXT PRIMARY KEY,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    exec_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_managed_containers_name_not_blank CHECK (btrim(container_name) <> '')
);

CREATE TABLE container_permissions (
    id BIGSERIAL PRIMARY KEY,
    telegram_user_id BIGINT NOT NULL REFERENCES bot_users(telegram_user_id) ON DELETE CASCADE,
    container_name TEXT NOT NULL REFERENCES managed_containers(container_name) ON DELETE CASCADE,
    can_start BOOLEAN NOT NULL DEFAULT FALSE,
    can_stop BOOLEAN NOT NULL DEFAULT FALSE,
    can_restart BOOLEAN NOT NULL DEFAULT FALSE,
    can_logs BOOLEAN NOT NULL DEFAULT FALSE,
    can_exec BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_container_permissions_user_container UNIQUE (telegram_user_id, container_name)
);

CREATE TABLE exec_command_allowlist (
    id BIGSERIAL PRIMARY KEY,
    container_name TEXT NOT NULL REFERENCES managed_containers(container_name) ON DELETE CASCADE,
    command TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_exec_command_allowlist_command_not_blank CHECK (btrim(command) <> ''),
    CONSTRAINT uq_exec_command_allowlist_container_command UNIQUE (container_name, command)
);

CREATE INDEX idx_bot_users_enabled ON bot_users(enabled);
CREATE INDEX idx_managed_containers_enabled ON managed_containers(enabled);
CREATE INDEX idx_container_permissions_user ON container_permissions(telegram_user_id);
CREATE INDEX idx_container_permissions_container ON container_permissions(container_name);
CREATE INDEX idx_exec_command_allowlist_container_enabled
    ON exec_command_allowlist(container_name, enabled);
