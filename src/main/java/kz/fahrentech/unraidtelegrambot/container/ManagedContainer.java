package kz.fahrentech.unraidtelegrambot.container;

import java.time.OffsetDateTime;

public record ManagedContainer(
        String containerName,
        String description,
        boolean enabled,
        boolean execEnabled,
        OffsetDateTime createdAt
) {
}
