package kz.fahrentech.unraidtelegrambot.security;

import java.util.ArrayList;
import java.util.List;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import org.springframework.stereotype.Component;

@Component
public class CommandLineTokenizer {

    public List<String> tokenize(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Character quote = null;
        boolean escaped = false;

        for (int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                escaped = true;
                continue;
            }

            if (quote != null) {
                if (ch == quote) {
                    quote = null;
                } else {
                    current.append(ch);
                }
                continue;
            }

            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }

            if (Character.isWhitespace(ch)) {
                appendToken(tokens, current);
            } else {
                current.append(ch);
            }
        }

        if (escaped) {
            current.append('\\');
        }

        if (quote != null) {
            throw new UserVisibleException("Команда содержит незакрытую кавычку.");
        }

        appendToken(tokens, current);
        return List.copyOf(tokens);
    }

    private void appendToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }
}
