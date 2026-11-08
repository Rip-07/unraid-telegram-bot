package kz.fahrentech.unraidtelegrambot.repository;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ExecCommandAllowlistRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public boolean isAllowed(String containerName, String command) {
        Integer count = jdbc.queryForObject("""
                        SELECT count(*)
                        FROM exec_command_allowlist
                        WHERE container_name = :containerName
                          AND command = :command
                          AND enabled = TRUE
                        """,
                Map.of(
                        "containerName", containerName,
                        "command", command
                ),
                Integer.class);
        return count != null && count > 0;
    }
}
