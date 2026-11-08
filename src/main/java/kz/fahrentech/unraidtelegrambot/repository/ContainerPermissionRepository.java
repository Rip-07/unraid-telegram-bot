package kz.fahrentech.unraidtelegrambot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.permission.ContainerPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContainerPermissionRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<ContainerPermission> permissionRowMapper = new ContainerPermissionRowMapper();
    private final RowMapper<ManagedContainer> containerRowMapper = new ManagedContainerRowMapper();

    public Optional<ContainerPermission> findByUserAndContainer(long telegramUserId, String containerName) {
        var permissions = jdbc.query("""
                        SELECT id, telegram_user_id, container_name,
                               can_start, can_stop, can_restart, can_logs, can_exec
                        FROM container_permissions
                        WHERE telegram_user_id = :telegramUserId
                          AND container_name = :containerName
                        """,
                Map.of(
                        "telegramUserId", telegramUserId,
                        "containerName", containerName
                ),
                permissionRowMapper);
        return permissions.stream().findFirst();
    }

    public List<ManagedContainer> findAllowedContainersForUser(long telegramUserId) {
        return jdbc.query("""
                        SELECT mc.container_name, mc.description, mc.enabled, mc.exec_enabled, mc.created_at
                        FROM managed_containers mc
                        JOIN container_permissions cp ON cp.container_name = mc.container_name
                        WHERE cp.telegram_user_id = :telegramUserId
                          AND mc.enabled = TRUE
                          AND (
                              cp.can_start = TRUE
                              OR cp.can_stop = TRUE
                              OR cp.can_restart = TRUE
                              OR cp.can_logs = TRUE
                              OR cp.can_exec = TRUE
                          )
                        ORDER BY mc.container_name
                        """,
                Map.of("telegramUserId", telegramUserId),
                containerRowMapper);
    }

    private static final class ContainerPermissionRowMapper implements RowMapper<ContainerPermission> {
        @Override
        public ContainerPermission mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ContainerPermission(
                    rs.getLong("id"),
                    rs.getLong("telegram_user_id"),
                    rs.getString("container_name"),
                    rs.getBoolean("can_start"),
                    rs.getBoolean("can_stop"),
                    rs.getBoolean("can_restart"),
                    rs.getBoolean("can_logs"),
                    rs.getBoolean("can_exec")
            );
        }
    }

    private static final class ManagedContainerRowMapper implements RowMapper<ManagedContainer> {
        @Override
        public ManagedContainer mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ManagedContainer(
                    rs.getString("container_name"),
                    rs.getString("description"),
                    rs.getBoolean("enabled"),
                    rs.getBoolean("exec_enabled"),
                    rs.getObject("created_at", java.time.OffsetDateTime.class)
            );
        }
    }
}
