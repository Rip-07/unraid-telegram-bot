package kz.fahrentech.unraidtelegrambot.docker;

public record DockerContainerStatus(
        String name,
        String id,
        String image,
        String status,
        boolean running,
        Long exitCode,
        String startedAt,
        String finishedAt
) {
}
