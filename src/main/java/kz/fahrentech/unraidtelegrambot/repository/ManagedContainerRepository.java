package kz.fahrentech.unraidtelegrambot.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ManagedContainerRepository {

    private final NamedParameterJdbcTemplate jdbc;
    private final RowMapper<ManagedContainer> rowMapper = new ManagedContainerRowMapper();

    public Optional<ManagedContainer> findEnabledByName(String containerName) {
        var containers = jdbc.query("""
                        SELECT container_name, description, enabled, exec_enabled, created_at
                        FROM managed_containers
                        WHERE container_name = :containerName
                          AND enabled = TRUE
                        """,
                Map.of("containerName", containerName),
                rowMapper);
        return containers.stream().findFirst();
    }

    public List<ManagedContainer> findAllEnabled() {
        return jdbc.query("""
                        SELECT container_name, description, enabled, exec_enabled, created_at
                        FROM managed_containers
                        WHERE enabled = TRUE
                        ORDER BY container_name
                        """,
                rowMapper);
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
