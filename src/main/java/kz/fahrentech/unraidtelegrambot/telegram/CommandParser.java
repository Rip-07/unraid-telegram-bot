package kz.fahrentech.unraidtelegrambot.telegram;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CommandParser {

    public Optional<BotCommand> parse(String text, String botUsername) {
        if (text == null) {
            return Optional.empty();
        }

        String trimmed = text.trim();
        if (!trimmed.startsWith("/")) {
            return Optional.empty();
        }

        int firstWhitespace = firstWhitespaceIndex(trimmed);
        String commandToken = firstWhitespace < 0 ? trimmed : trimmed.substring(0, firstWhitespace);
        String rawArguments = firstWhitespace < 0 ? "" : trimmed.substring(firstWhitespace).trim();

        String commandName = commandToken.substring(1);
        int suffixIndex = commandName.indexOf('@');
        if (suffixIndex >= 0) {
            String addressedUsername = commandName.substring(suffixIndex + 1);
            commandName = commandName.substring(0, suffixIndex);
            if (botUsername != null
                    && !botUsername.isBlank()
                    && !addressedUsername.equalsIgnoreCase(botUsername)) {
                return Optional.empty();
            }
        }

        if (commandName.isBlank()) {
            return Optional.empty();
        }

        List<String> args = rawArguments.isBlank()
                ? List.of()
                : Arrays.stream(rawArguments.split("\\s+")).toList();
        return Optional.of(new BotCommand(commandName.toLowerCase(Locale.ROOT), args, rawArguments));
    }

    public SplitValue splitFirstArgument(String rawArguments) {
        if (rawArguments == null || rawArguments.isBlank()) {
            return new SplitValue("", "");
        }
        String trimmed = rawArguments.trim();
        int firstWhitespace = firstWhitespaceIndex(trimmed);
        if (firstWhitespace < 0) {
            return new SplitValue(trimmed, "");
        }
        return new SplitValue(trimmed.substring(0, firstWhitespace), trimmed.substring(firstWhitespace).trim());
    }

    private int firstWhitespaceIndex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public record SplitValue(String first, String remainder) {
    }
}
