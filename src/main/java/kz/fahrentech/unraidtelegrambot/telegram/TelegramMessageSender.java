package kz.fahrentech.unraidtelegrambot.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramMessageSender {

    private static final int TELEGRAM_MESSAGE_LIMIT = 4000;

    private final TelegramApiClient apiClient;

    public void sendText(long chatId, String text) {
        if (text == null || text.isBlank()) {
            apiClient.sendMessage(chatId, "Команда выполнена, ответ пустой.");
            return;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + TELEGRAM_MESSAGE_LIMIT, text.length());
            apiClient.sendMessage(chatId, text.substring(start, end));
            start = end;
        }
    }
}
