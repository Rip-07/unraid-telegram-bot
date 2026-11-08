package kz.fahrentech.unraidtelegrambot.docker;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import kz.fahrentech.unraidtelegrambot.exception.DockerOperationException;
import org.junit.jupiter.api.Test;

class DockerJavaGatewayTest {

    @Test
    void wrapsDockerClientNotFoundException() {
        DockerClient dockerClient = mock(DockerClient.class);
        InspectContainerCmd inspectContainerCmd = mock(InspectContainerCmd.class);
        when(dockerClient.inspectContainerCmd("missing")).thenReturn(inspectContainerCmd);
        when(inspectContainerCmd.exec()).thenThrow(new NotFoundException("missing"));

        DockerJavaGateway gateway = new DockerJavaGateway(dockerClient);

        assertThatThrownBy(() -> gateway.status("missing"))
                .isInstanceOf(DockerOperationException.class)
                .hasMessageContaining("не найден");
    }
}
