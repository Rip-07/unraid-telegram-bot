package kz.fahrentech.unraidtelegrambot.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import kz.fahrentech.unraidtelegrambot.repository.ContainerPermissionRepository;
import kz.fahrentech.unraidtelegrambot.repository.ManagedContainerRepository;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import kz.fahrentech.unraidtelegrambot.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private ManagedContainerRepository managedContainerRepository;

    @Mock
    private ContainerPermissionRepository permissionRepository;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(managedContainerRepository, permissionRepository, properties(List.of("postgres")));
    }

    @Test
    void deniesAdminForDenylistedContainer() {
        BotUser admin = user(1L, Role.ADMIN);
        when(managedContainerRepository.findEnabledByName("postgres"))
                .thenReturn(Optional.of(container("postgres")));

        assertThatThrownBy(() -> service.assertAllowed(admin, "postgres", ContainerOperation.STATUS))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("denylist");
    }

    @Test
    void deniesUserWithoutExplicitPermission() {
        BotUser user = user(2L, Role.USER);
        when(managedContainerRepository.findEnabledByName("plex"))
                .thenReturn(Optional.of(container("plex")));
        when(permissionRepository.findByUserAndContainer(2L, "plex"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertAllowed(user, "plex", ContainerOperation.LOGS))
                .isInstanceOf(UserVisibleException.class)
                .hasMessageContaining("Нет явного разрешения");
    }

    @Test
    void allowsUserWithGranularPermission() {
        BotUser user = user(2L, Role.USER);
        when(managedContainerRepository.findEnabledByName("plex"))
                .thenReturn(Optional.of(container("plex")));
        when(permissionRepository.findByUserAndContainer(2L, "plex"))
                .thenReturn(Optional.of(new ContainerPermission(10L, 2L, "plex", false, false, false, true, false)));

        PermissionDecision decision = service.assertAllowed(user, "plex", ContainerOperation.LOGS);

        assertThat(decision.managedContainer().containerName()).isEqualTo("plex");
        assertThat(decision.adminAccess()).isFalse();
    }

    private BotUser user(long id, Role role) {
        return new BotUser(id, "user" + id, role, true, OffsetDateTime.now());
    }

    private ManagedContainer container(String name) {
        return new ManagedContainer(name, "test", true, false, OffsetDateTime.now());
    }

    private AppProperties properties(List<String> denylist) {
        return new AppProperties(
                null,
                new AppProperties.DockerProperties("unix:///var/run/docker.sock", denylist),
                new AppProperties.LogsProperties(300),
                new AppProperties.ExecProperties(false, 10, false),
                new AppProperties.ResponseProperties(3500)
        );
    }
}
