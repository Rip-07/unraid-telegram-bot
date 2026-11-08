package kz.fahrentech.unraidtelegrambot.security;

import java.util.List;

public record ExecRequest(
        String containerName,
        List<String> command,
        String normalizedCommand
) {
}
