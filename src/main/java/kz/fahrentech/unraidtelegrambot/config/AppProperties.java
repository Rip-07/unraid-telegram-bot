package kz.fahrentech.unraidtelegrambot.config;

import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String initialAdminTelegramId,
        DockerProperties docker,
        LogsProperties logs,
        ExecProperties exec,
        ResponseProperties response
) {

    public OptionalLong initialAdminTelegramIdAsLong() {
        if (initialAdminTelegramId == null || initialAdminTelegramId.isBlank()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(initialAdminTelegramId.trim()));
    }

    public Set<String> dockerDenylistNormalized() {
        if (docker == null || docker.denylist() == null) {
            return Set.of();
        }
        return docker.denylist().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public int logsMaxLines() {
        return logs == null ? 300 : logs.maxLines();
    }

    public int execTimeoutSeconds() {
        return exec == null ? 10 : exec.timeoutSeconds();
    }

    public int responseMaxChars() {
        return response == null ? 3500 : response.maxChars();
    }

    public boolean execEnabled() {
        return exec != null && exec.enabled();
    }

    public boolean execAllowShell() {
        return exec != null && exec.allowShell();
    }

    public record DockerProperties(String host, List<String> denylist) {
    }

    public record LogsProperties(int maxLines) {
    }

    public record ExecProperties(boolean enabled, int timeoutSeconds, boolean allowShell) {
    }

    public record ResponseProperties(int maxChars) {
    }
}
