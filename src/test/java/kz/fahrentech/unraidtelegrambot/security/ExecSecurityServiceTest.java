package kz.fahrentech.unraidtelegrambot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import kz.fahrentech.unraidtelegrambot.permission.ContainerOperation;
import kz.fahrentech.unraidtelegrambot.permission.PermissionDecision;
import kz.fahrentech.unraidtelegrambot.permission.PermissionService;
import kz.fahrentech.unraidtelegrambot.repository.ExecCommandAllowlistRepository;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import kz.fahrentech.unraidtelegrambot.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExecSecurityServiceTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private ExecCommandAllowlistRepository allowlistRepository;

    private final CommandLineTokenizer tokenizer = new CommandLineTokenizer();
    private final BotUser user = new BotUser(1L, "alice", Role.USER, true, OffsetDateTime.now());

    @Test
    void deniesWhenExecIsGloballyDisabled() {
        ExecSecurityService service = service(false, false);

        assertThatThrownBy(() -> service.validate(user, "plex", "uptime"))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("APP_EXEC_ENABLED=false");
        verifyNoInteractions(permissionService, allowlistRepository);
    }

    @Test
    void deniesShellCommandWhenShellIsDisabled() {
        ExecSecurityService service = service(true, false);
        when(permissionService.assertAllowed(user, "plex", ContainerOperation.EXEC))
                .thenReturn(new PermissionDecision(container("plex", true), false));

        assertThatThrownBy(() -> service.validate(user, "plex", "sh -c uptime"))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("Shell-команды запрещены");
    }

    @Test
    void requiresExactAllowlistEntry() {
        ExecSecurityService service = service(true, false);
        when(permissionService.assertAllowed(user, "plex", ContainerOperation.EXEC))
                .thenReturn(new PermissionDecision(container("plex", true), false));
        when(allowlistRepository.isAllowed("plex", "echo hello world")).thenReturn(true);

        ExecRequest request = service.validate(user, "plex", "echo \"hello world\"");

        assertThat(request.command()).containsExactly("echo", "hello world");
        assertThat(request.normalizedCommand()).isEqualTo("echo hello world");
    }

    @Test
    void deniesWhenContainerExecIsDisabled() {
        ExecSecurityService service = service(true, false);
        when(permissionService.assertAllowed(user, "plex", ContainerOperation.EXEC))
                .thenReturn(new PermissionDecision(container("plex", false), false));

        assertThatThrownBy(() -> service.validate(user, "plex", "uptime"))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("exec_enabled");
    }

    private ExecSecurityService service(boolean execEnabled, boolean allowShell) {
        return new ExecSecurityService(permissionService, allowlistRepository, tokenizer,
                new AppProperties(
                        null,
                        new AppProperties.DockerProperties("unix:///var/run/docker.sock", List.of()),
                        new AppProperties.LogsProperties(300),
                        new AppProperties.ExecProperties(execEnabled, 10, allowShell),
                        new AppProperties.ResponseProperties(3500)
                ));
    }

    private ManagedContainer container(String name, boolean execEnabled) {
        return new ManagedContainer(name, "test", true, execEnabled, OffsetDateTime.now());
    }
}
