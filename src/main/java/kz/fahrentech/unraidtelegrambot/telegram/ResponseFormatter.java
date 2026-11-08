package kz.fahrentech.unraidtelegrambot.telegram;

import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.docker.DockerContainerStatus;
import kz.fahrentech.unraidtelegrambot.docker.ExecResult;
import org.springframework.stereotype.Component;

@Component
public class ResponseFormatter {

    private static final String TRUNCATED_SUFFIX = "\n\n[Ответ обрезан по APP_RESPONSE_MAX_CHARS]";

    private final AppProperties properties;

    public ResponseFormatter(AppProperties properties) {
        this.properties = properties;
    }

    public String limit(String text) {
        int maxChars = Math.max(256, properties.responseMaxChars());
        if (text == null || text.length() <= maxChars) {
            return text;
        }
        int end = Math.max(0, maxChars - TRUNCATED_SUFFIX.length());
        return text.substring(0, end) + TRUNCATED_SUFFIX;
    }

    public String status(DockerContainerStatus status) {
        return limit("""
                Контейнер: %s
                Image: %s
                Status: %s
                Running: %s
                Exit code: %s
                Started at: %s
                Finished at: %s
                """.formatted(
                status.name(),
                valueOrDash(status.image()),
                valueOrDash(status.status()),
                status.running() ? "yes" : "no",
                status.exitCode() == null ? "-" : status.exitCode(),
                valueOrDash(status.startedAt()),
                valueOrDash(status.finishedAt())
        ).strip());
    }

    public String logs(String containerName, int lines, String logs) {
        String body = logs == null || logs.isBlank() ? "[логи пустые]" : logs.stripTrailing();
        return limit("Логи `%s`, последние %d строк:\n%s".formatted(containerName, lines, body));
    }

    public String exec(String containerName, String command, ExecResult result) {
        StringBuilder response = new StringBuilder();
        response.append("Exec `%s` в `%s`\n".formatted(command, containerName));
        response.append("Exit code: ").append(result.exitCode()).append('\n');
        if (result.timedOut()) {
            response.append("Внимание: выполнение превысило timeout и ожидание результата было остановлено.\n");
        }
        if (result.truncated()) {
            response.append("Внимание: вывод был обрезан по APP_RESPONSE_MAX_CHARS.\n");
        }
        response.append('\n');
        response.append(result.output() == null || result.output().isBlank() ? "[вывод пустой]" : result.output());
        return limit(response.toString());
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
