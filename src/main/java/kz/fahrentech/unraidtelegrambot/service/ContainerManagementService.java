package kz.fahrentech.unraidtelegrambot.service;

import java.util.List;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.docker.DockerContainerStatus;
import kz.fahrentech.unraidtelegrambot.docker.DockerGateway;
import kz.fahrentech.unraidtelegrambot.docker.ExecResult;
import kz.fahrentech.unraidtelegrambot.permission.ContainerOperation;
import kz.fahrentech.unraidtelegrambot.permission.PermissionService;
import kz.fahrentech.unraidtelegrambot.security.ExecRequest;
import kz.fahrentech.unraidtelegrambot.security.ExecSecurityService;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContainerManagementService {

    private final PermissionService permissionService;
    private final ExecSecurityService execSecurityService;
    private final DockerGateway dockerGateway;
    private final AppProperties properties;

    public List<ManagedContainer> listAllowedContainers(BotUser user) {
        return permissionService.listAllowedContainers(user);
    }

    public DockerContainerStatus status(BotUser user, String containerName) {
        String managedName = permissionService.assertAllowed(user, containerName, ContainerOperation.STATUS)
                .managedContainer()
                .containerName();
        return dockerGateway.status(managedName);
    }

    public void start(BotUser user, String containerName) {
        String managedName = permissionService.assertAllowed(user, containerName, ContainerOperation.START)
                .managedContainer()
                .containerName();
        dockerGateway.start(managedName);
    }

    public void stop(BotUser user, String containerName) {
        String managedName = permissionService.assertAllowed(user, containerName, ContainerOperation.STOP)
                .managedContainer()
                .containerName();
        dockerGateway.stop(managedName);
    }

    public void restart(BotUser user, String containerName) {
        String managedName = permissionService.assertAllowed(user, containerName, ContainerOperation.RESTART)
                .managedContainer()
                .containerName();
        dockerGateway.restart(managedName);
    }

    public String logs(BotUser user, String containerName, int lines) {
        String managedName = permissionService.assertAllowed(user, containerName, ContainerOperation.LOGS)
                .managedContainer()
                .containerName();
        int safeLines = Math.max(1, Math.min(lines, properties.logsMaxLines()));
        return dockerGateway.logs(managedName, safeLines, properties.responseMaxChars());
    }

    public ExecResult exec(BotUser user, String containerName, String commandLine) {
        ExecRequest request = execSecurityService.validate(user, containerName, commandLine);
        return dockerGateway.exec(
                request.containerName(),
                request.command(),
                properties.execTimeoutSeconds(),
                properties.responseMaxChars()
        );
    }
}
