package kz.fahrentech.unraidtelegrambot.permission;

import java.util.List;
import java.util.Locale;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import kz.fahrentech.unraidtelegrambot.repository.ContainerPermissionRepository;
import kz.fahrentech.unraidtelegrambot.repository.ManagedContainerRepository;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import kz.fahrentech.unraidtelegrambot.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ManagedContainerRepository managedContainerRepository;
    private final ContainerPermissionRepository permissionRepository;
    private final AppProperties properties;

    public List<ManagedContainer> listAllowedContainers(BotUser user) {
        if (user.role() == Role.ADMIN) {
            return managedContainerRepository.findAllEnabled().stream()
                    .filter(container -> !isDenylisted(container.containerName()))
                    .toList();
        }

        return permissionRepository.findAllowedContainersForUser(user.telegramUserId()).stream()
                .filter(container -> !isDenylisted(container.containerName()))
                .toList();
    }

    public PermissionDecision assertAllowed(BotUser user, String containerName, ContainerOperation operation) {
        if (user == null || !user.enabled()) {
            throw new UserVisibleException("Доступ запрещен: пользователь не найден или отключен.");
        }

        ManagedContainer container = managedContainerRepository.findEnabledByName(containerName)
                .orElseThrow(() -> new UserVisibleException(
                        "Контейнер `%s` не найден в списке разрешенных контейнеров.".formatted(containerName)));

        if (isDenylisted(container.containerName())) {
            throw new UserVisibleException(
                    "Контейнер `%s` находится в denylist и недоступен для управления через бота."
                            .formatted(container.containerName()));
        }

        if (user.role() == Role.ADMIN) {
            return new PermissionDecision(container, true);
        }

        ContainerPermission permission = permissionRepository
                .findByUserAndContainer(user.telegramUserId(), container.containerName())
                .orElseThrow(() -> new UserVisibleException(
                        "Нет явного разрешения на контейнер `%s`.".formatted(container.containerName())));

        if (!permission.allows(operation)) {
            throw new UserVisibleException(
                    "Недостаточно прав для операции `%s` с контейнером `%s`."
                            .formatted(operation.name().toLowerCase(Locale.ROOT), container.containerName()));
        }

        return new PermissionDecision(container, false);
    }

    private boolean isDenylisted(String containerName) {
        return properties.dockerDenylistNormalized()
                .contains(containerName.toLowerCase(Locale.ROOT));
    }
}
