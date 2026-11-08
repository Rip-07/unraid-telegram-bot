package kz.fahrentech.unraidtelegrambot.docker;

import java.util.List;

public interface DockerGateway {

    DockerContainerStatus status(String containerName);

    void start(String containerName);

    void stop(String containerName);

    void restart(String containerName);

    String logs(String containerName, int lines, int maxChars);

    ExecResult exec(String containerName, List<String> command, int timeoutSeconds, int maxChars);
}
