package kz.fahrentech.unraidtelegrambot.security;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import kz.fahrentech.unraidtelegrambot.permission.ContainerOperation;
import kz.fahrentech.unraidtelegrambot.permission.PermissionDecision;
import kz.fahrentech.unraidtelegrambot.permission.PermissionService;
import kz.fahrentech.unraidtelegrambot.repository.ExecCommandAllowlistRepository;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecSecurityService {

    private static final Set<String> SHELL_BINARIES = Set.of("sh", "bash", "ash", "dash", "zsh", "fish");

    private final PermissionService permissionService;
    private final ExecCommandAllowlistRepository allowlistRepository;
    private final CommandLineTokenizer tokenizer;
    private final AppProperties properties;

    public ExecRequest validate(BotUser user, String containerName, String commandLine) {
        if (!properties.execEnabled()) {
            throw new UserVisibleException("Команда /exec отключена глобально через APP_EXEC_ENABLED=false.");
        }

        PermissionDecision decision = permissionService.assertAllowed(user, containerName, ContainerOperation.EXEC);
        ManagedContainer container = decision.managedContainer();

        if (!container.execEnabled()) {
            throw new UserVisibleException(
                    "Exec для контейнера `%s` отключен в managed_containers.exec_enabled."
                            .formatted(container.containerName()));
        }

        List<String> command = tokenizer.tokenize(commandLine);
        if (command.isEmpty()) {
            throw new UserVisibleException("Команда exec не может быть пустой.");
        }

        if (!properties.execAllowShell() && isShellCommand(command)) {
            throw new UserVisibleException(
                    "Shell-команды запрещены. Для sh -c/bash -c нужна отдельная настройка APP_EXEC_ALLOW_SHELL=true.");
        }

        String normalizedCommand = String.join(" ", command);
        if (!allowlistRepository.isAllowed(container.containerName(), normalizedCommand)) {
            throw new UserVisibleException(
                    "Команда `%s` не входит в allowlist для контейнера `%s`."
                            .formatted(normalizedCommand, container.containerName()));
        }

        return new ExecRequest(container.containerName(), command, normalizedCommand);
    }

    private boolean isShellCommand(List<String> command) {
        String executable = command.getFirst();
        String fileName = Path.of(executable).getFileName().toString().toLowerCase(Locale.ROOT);
        return SHELL_BINARIES.contains(fileName);
    }
}
