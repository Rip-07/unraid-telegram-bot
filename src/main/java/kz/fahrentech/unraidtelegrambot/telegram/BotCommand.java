package kz.fahrentech.unraidtelegrambot.telegram;

import java.util.List;

public record BotCommand(
        String name,
        List<String> arguments,
        String rawArguments
) {
}
