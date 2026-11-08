# Unraid Telegram Docker Bot

Telegram bot for managing Docker containers on an Unraid server: start/stop/restart, logs and a guarded `exec`, with roles, per-container permissions, a denylist and a command allowlist stored in PostgreSQL.

Telegram-бот на Java 21 и Spring Boot для управления Docker-контейнерами на сервере Unraid. Бот работает через Telegram Bot API long polling, хранит пользователей и права в PostgreSQL 18, применяет Flyway-миграции и управляет Docker через `/var/run/docker.sock` с помощью docker-java.

## Возможности

- `/start` — приветствие и Telegram user ID.
- `/help` — список команд.
- `/containers` — контейнеры, явно разрешенные пользователю.
- `/status <container>` — статус контейнера.
- `/start_container <container>` — запуск.
- `/stop_container <container>` — остановка.
- `/restart_container <container>` — перезапуск.
- `/logs <container> <lines>` — последние N строк логов.
- `/exec <container> <command>` — безопасный exec, по умолчанию выключен.

## Важное предупреждение по безопасности

Монтирование `/var/run/docker.sock` в контейнер приложения дает боту высокопривилегированный доступ к Docker Engine. На практике это близко к root-доступу к серверу: пользователь, который может управлять Docker, часто может получить доступ к файловой системе хоста, секретам контейнеров и другим сервисам.

Не добавляйте PostgreSQL в публичный интернет. Доступ к базе для локальной разработки делайте через VPN или другой приватный канал.

## Архитектура прав

Пользователи хранятся в `bot_users`:

- `telegram_user_id`
- `username`
- `role`: `ADMIN` или `USER`
- `enabled`
- `created_at`

Контейнер должен быть добавлен в `managed_containers`, иначе бот не будет им управлять. Для `USER` нужна запись в `container_permissions` с конкретными флагами:

- `can_start`
- `can_stop`
- `can_restart`
- `can_logs`
- `can_exec`

`ADMIN` получает доступ ко всем enabled-контейнерам из `managed_containers`, но denylist продолжает действовать. По умолчанию denylist: `postgres,gitea,gitea-runner,docker-telegram-bot`.

`/exec` дополнительно требует:

- `APP_EXEC_ENABLED=true`;
- у пользователя `can_exec=true`, если это `USER`;
- у контейнера `exec_enabled=true`;
- точное совпадение команды с `exec_command_allowlist.command`;
- shell-команды вроде `sh -c` запрещены, пока не включен `APP_EXEC_ALLOW_SHELL=true`;
- timeout и максимальный размер ответа ограничиваются настройками.

## Переменные окружения

Скопируйте пример:

```bash
cp .env.example .env
```

Основные переменные:

```env
TELEGRAM_BOT_TOKEN=replace-with-botfather-token
TELEGRAM_BOT_USERNAME=your_bot_username

SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/docker_bot_db
SPRING_DATASOURCE_USERNAME=docker_bot
SPRING_DATASOURCE_PASSWORD=replace-with-strong-password

APP_INITIAL_ADMIN_TELEGRAM_ID=123456789
APP_DOCKER_HOST=unix:///var/run/docker.sock
APP_DOCKER_DENYLIST=postgres,gitea,gitea-runner,docker-telegram-bot
APP_LOGS_MAX_LINES=300
APP_EXEC_ENABLED=false
APP_EXEC_TIMEOUT_SECONDS=10
APP_EXEC_ALLOW_SHELL=false
APP_RESPONSE_MAX_CHARS=3500

DOCKER_IMAGE=unraid-telegram-bot:latest
DOCKER_GID=281
```

`APP_INITIAL_ADMIN_TELEGRAM_ID` используется только если в `bot_users` еще нет admin. Если admin уже есть, дубликат не создается.

## Создание Telegram bot через BotFather

1. Откройте Telegram и напишите `@BotFather`.
2. Выполните `/newbot`.
3. Задайте имя и username бота.
4. Сохраните token в `.env` как `TELEGRAM_BOT_TOKEN`.
5. Username бота запишите в `TELEGRAM_BOT_USERNAME` без `@`.
6. Напишите своему боту `/start`, чтобы увидеть ваш Telegram user ID.

## PostgreSQL 18

PostgreSQL уже должен быть запущен отдельным контейнером на той же Docker network, что и бот. Для контейнерного запуска приложения используйте:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/docker_bot_db
```

Первичная настройка базы:

```sql
CREATE DATABASE docker_bot_db;

CREATE USER docker_bot WITH PASSWORD 'replace-with-strong-password';

GRANT CONNECT ON DATABASE docker_bot_db TO docker_bot;

\c docker_bot_db

GRANT USAGE, CREATE ON SCHEMA public TO docker_bot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO docker_bot;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO docker_bot;
```

Таблицы создаются Flyway при старте приложения.

## Локальный запуск из IntelliJ IDEA

Локальный запуск предполагает VPN-доступ к серверу, где работает PostgreSQL 18.

VM/options не нужны. В Run Configuration задайте environment variables:

```env
TELEGRAM_BOT_TOKEN=<token>
TELEGRAM_BOT_USERNAME=<username>
SPRING_DATASOURCE_URL=jdbc:postgresql://<vpn-ip-сервера>:5432/docker_bot_db
SPRING_DATASOURCE_USERNAME=docker_bot
SPRING_DATASOURCE_PASSWORD=<password>
APP_INITIAL_ADMIN_TELEGRAM_ID=<your-telegram-id>
APP_DOCKER_HOST=unix:///var/run/docker.sock
APP_EXEC_ENABLED=false
```

Если приложение запускается на Windows/macOS локально, `unix:///var/run/docker.sock` будет доступен только если вы отдельно пробросили Docker socket. Для разработки команд, БД и Telegram можно временно поставить `TELEGRAM_ENABLED=false` или запускать тесты без подключения к Docker.

### Подключение из IntelliJ IDEA к PostgreSQL через VPN

1. Убедитесь, что VPN подключен и сервер доступен по приватному IP.
2. В IntelliJ IDEA откройте `Database` -> `+` -> `Data Source` -> `PostgreSQL`.
3. Host: `<vpn-ip-сервера>`.
4. Port: `5432`.
5. Database: `docker_bot_db`.
6. User: `docker_bot`.
7. Password: пароль из `SPRING_DATASOURCE_PASSWORD`.
8. Проверьте `Test Connection`.

Не открывайте PostgreSQL наружу. Разрешайте доступ только через Docker network, VPN или firewall allowlist.

## Запуск на Unraid через Docker Compose

Создайте общую сеть, если ее еще нет:

```bash
docker network create backend
```

Если контейнер PostgreSQL называется `postgresql18`, подключите его к этой сети с alias `postgres`, потому что приложение в контейнере использует `jdbc:postgresql://postgres:5432/docker_bot_db`:

```bash
docker network connect --alias postgres backend postgresql18
```

Проверьте GID группы Docker socket:

```bash
stat -c '%g' /var/run/docker.sock
```

Запишите результат в `.env` как `DOCKER_GID`.

Сборка и запуск:

```bash
docker compose build
docker compose up -d
docker compose logs -f docker-telegram-bot
```

PostgreSQL не поднимается в этом compose-файле. Контейнер PostgreSQL должен быть подключен к сети `backend` и быть доступен по hostname `postgres`.

## SQL-примеры администрирования

Добавить пользователя:

```sql
INSERT INTO bot_users (telegram_user_id, username, role, enabled)
VALUES (123456789, 'alice', 'USER', TRUE)
ON CONFLICT (telegram_user_id)
DO UPDATE SET username = EXCLUDED.username, enabled = TRUE;
```

Сделать пользователя admin:

```sql
UPDATE bot_users
SET role = 'ADMIN', enabled = TRUE
WHERE telegram_user_id = 123456789;
```

Добавить контейнер в managed list:

```sql
INSERT INTO managed_containers (container_name, description, enabled, exec_enabled)
VALUES ('plex', 'Plex Media Server', TRUE, FALSE)
ON CONFLICT (container_name)
DO UPDATE SET description = EXCLUDED.description, enabled = TRUE;
```

Выдать права на конкретный контейнер:

```sql
INSERT INTO container_permissions (
    telegram_user_id,
    container_name,
    can_start,
    can_stop,
    can_restart,
    can_logs,
    can_exec
)
VALUES (123456789, 'plex', TRUE, TRUE, TRUE, TRUE, FALSE)
ON CONFLICT (telegram_user_id, container_name)
DO UPDATE SET
    can_start = EXCLUDED.can_start,
    can_stop = EXCLUDED.can_stop,
    can_restart = EXCLUDED.can_restart,
    can_logs = EXCLUDED.can_logs,
    can_exec = EXCLUDED.can_exec;
```

Запретить exec для контейнера:

```sql
UPDATE managed_containers
SET exec_enabled = FALSE
WHERE container_name = 'plex';

UPDATE container_permissions
SET can_exec = FALSE
WHERE container_name = 'plex';
```

Включить logs:

```sql
UPDATE container_permissions
SET can_logs = TRUE
WHERE telegram_user_id = 123456789
  AND container_name = 'plex';
```

Включить `/exec` безопасно:

```sql
UPDATE managed_containers
SET exec_enabled = TRUE
WHERE container_name = 'plex';

UPDATE container_permissions
SET can_exec = TRUE
WHERE telegram_user_id = 123456789
  AND container_name = 'plex';

INSERT INTO exec_command_allowlist (container_name, command, enabled)
VALUES ('plex', 'uptime', TRUE)
ON CONFLICT (container_name, command)
DO UPDATE SET enabled = TRUE;
```

Команды в allowlist сравниваются с нормализованной строкой после парсинга аргументов. Например `/exec plex echo "hello world"` проверяется как `echo hello world`.

## Команды разработки

Тесты:

```bash
./mvnw test
```

Локальный запуск:

```bash
./mvnw spring-boot:run
```

Сборка jar:

```bash
./mvnw package
```

Docker build:

```bash
docker build -t unraid-telegram-bot:latest .
```

Docker Compose deploy:

```bash
docker compose pull docker-telegram-bot
docker compose up -d docker-telegram-bot
docker compose logs -f docker-telegram-bot
```

Если образ собирается локально без registry:

```bash
docker compose build docker-telegram-bot
docker compose up -d docker-telegram-bot
```

## Лицензия

MIT
