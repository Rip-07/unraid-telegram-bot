package kz.fahrentech.unraidtelegrambot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import kz.fahrentech.unraidtelegrambot.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BotUserRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<BotUser> rowMapper = new BotUserRowMapper();

    public Optional<BotUser> findByTelegramUserId(long telegramUserId) {
        var users = jdbc.query("""
                        SELECT telegram_user_id, username, role, enabled, created_at
                        FROM bot_users
                        WHERE telegram_user_id = :telegramUserId
                        """,
                Map.of("telegramUserId", telegramUserId),
                rowMapper);
        return users.stream().findFirst();
    }

    public boolean adminExists() {
        Integer count = jdbc.queryForObject("""
                        SELECT count(*)
                        FROM bot_users
                        WHERE role = 'ADMIN'
                        """,
                Map.of(),
                Integer.class);
        return count != null && count > 0;
    }

    public void upsertInitialAdmin(long telegramUserId) {
        jdbc.update("""
                        INSERT INTO bot_users (telegram_user_id, username, role, enabled)
                        VALUES (:telegramUserId, :username, 'ADMIN', TRUE)
                        ON CONFLICT (telegram_user_id)
                        DO UPDATE SET role = 'ADMIN', enabled = TRUE
                        """,
                new MapSqlParameterSource()
                        .addValue("telegramUserId", telegramUserId)
                        .addValue("username", "initial-admin"));
    }

    public void updateUsernameIfPresent(long telegramUserId, String username) {
        jdbc.update("""
                        UPDATE bot_users
                        SET username = :username
                        WHERE telegram_user_id = :telegramUserId
                        """,
                new MapSqlParameterSource()
                        .addValue("telegramUserId", telegramUserId)
                        .addValue("username", username));
    }

    private static final class BotUserRowMapper implements RowMapper<BotUser> {
        @Override
        public BotUser mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BotUser(
                    rs.getLong("telegram_user_id"),
                    rs.getString("username"),
                    Role.valueOf(rs.getString("role")),
                    rs.getBoolean("enabled"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class)
            );
        }
    }
}
