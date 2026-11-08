package kz.fahrentech.unraidtelegrambot.permission;

import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;

public record PermissionDecision(
        ManagedContainer managedContainer,
        boolean adminAccess
) {
}
