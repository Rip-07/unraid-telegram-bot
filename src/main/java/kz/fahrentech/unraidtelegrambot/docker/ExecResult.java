package kz.fahrentech.unraidtelegrambot.docker;

public record ExecResult(
        int exitCode,
        String output,
        boolean timedOut,
        boolean truncated
) {
}
