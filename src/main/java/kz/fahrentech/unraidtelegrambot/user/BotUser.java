package kz.fahrentech.unraidtelegrambot.user;

import java.time.OffsetDateTime;

public record BotUser(
        long telegramUserId,
        String username,
        Role role,
        boolean enabled,
        OffsetDateTime createdAt
) {
}
