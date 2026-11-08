package kz.fahrentech.unraidtelegrambot.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import java.util.List;
import java.util.concurrent.TimeUnit;
import kz.fahrentech.unraidtelegrambot.exception.DockerOperationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DockerJavaGateway implements DockerGateway {

    private final DockerClient dockerClient;

    @Override
    public DockerContainerStatus status(String containerName) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerName).exec();
            InspectContainerResponse.ContainerState state = response.getState();
            String name = response.getName() == null ? containerName : response.getName().replaceFirst("^/", "");
            String image = response.getConfig() == null ? null : response.getConfig().getImage();
            return new DockerContainerStatus(
                    name,
                    response.getId(),
                    image,
                    state == null ? "unknown" : state.getStatus(),
                    state != null && Boolean.TRUE.equals(state.getRunning()),
                    state == null ? null : state.getExitCodeLong(),
                    state == null ? null : state.getStartedAt(),
                    state == null ? null : state.getFinishedAt()
            );
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API вернул ошибку при чтении статуса `%s`: %s"
                            .formatted(containerName, ex.getMessage()),
                    ex);
        }
    }

    @Override
    public void start(String containerName) {
        try {
            dockerClient.startContainerCmd(containerName).exec();
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API не смог запустить `%s`: %s".formatted(containerName, ex.getMessage()),
                    ex);
        }
    }

    @Override
    public void stop(String containerName) {
        try {
            dockerClient.stopContainerCmd(containerName).exec();
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API не смог остановить `%s`: %s".formatted(containerName, ex.getMessage()),
                    ex);
        }
    }

    @Override
    public void restart(String containerName) {
        try {
            dockerClient.restartContainerCmd(containerName).exec();
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API не смог перезапустить `%s`: %s".formatted(containerName, ex.getMessage()),
                    ex);
        }
    }

    @Override
    public String logs(String containerName, int lines, int maxChars) {
        BoundedOutput output = new BoundedOutput(maxChars);
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                output.append(frame.getPayload());
            }
        };

        try {
            dockerClient.logContainerCmd(containerName)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(false)
                    .withTail(lines)
                    .exec(callback)
                    .awaitCompletion(30, TimeUnit.SECONDS);
            if (output.truncated()) {
                return output.value() + "\n\n[Логи обрезаны по APP_RESPONSE_MAX_CHARS]";
            }
            return output.value();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DockerOperationException("Получение логов `%s` было прервано.".formatted(containerName), ex);
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API не смог получить логи `%s`: %s".formatted(containerName, ex.getMessage()),
                    ex);
        }
    }

    @Override
    public ExecResult exec(String containerName, List<String> command, int timeoutSeconds, int maxChars) {
        BoundedOutput output = new BoundedOutput(maxChars);
        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                output.append(frame.getPayload());
            }
        };

        try {
            ExecCreateCmdResponse createResponse = dockerClient.execCreateCmd(containerName)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withAttachStdin(false)
                    .withTty(false)
                    .withCmd(command.toArray(String[]::new))
                    .exec();

            boolean completed = dockerClient.execStartCmd(createResponse.getId())
                    .withDetach(false)
                    .withTty(false)
                    .exec(callback)
                    .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

            InspectExecResponse inspect = dockerClient.inspectExecCmd(createResponse.getId()).exec();
            Long exitCode = inspect.getExitCodeLong();
            return new ExecResult(exitCode == null ? -1 : exitCode.intValue(), output.value(), !completed,
                    output.truncated());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new DockerOperationException("Exec в `%s` был прерван.".formatted(containerName), ex);
        } catch (NotFoundException ex) {
            throw new DockerOperationException("Контейнер `%s` не найден в Docker.".formatted(containerName), ex);
        } catch (DockerException ex) {
            throw new DockerOperationException(
                    "Docker API не смог выполнить exec в `%s`: %s".formatted(containerName, ex.getMessage()),
                    ex);
        }
    }
}
