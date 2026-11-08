package kz.fahrentech.unraidtelegrambot.telegram;

import java.util.List;
import java.util.Map;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramSendMessageResponse;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUpdate;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUpdatesResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramApiClient {

    private final RestClient restClient;

    public TelegramApiClient(RestClient.Builder restClientBuilder, TelegramProperties properties) {
        Assert.hasText(properties.botToken(), "TELEGRAM_BOT_TOKEN must not be empty");
        this.restClient = restClientBuilder
                .baseUrl("https://api.telegram.org/bot" + properties.botToken())
                .build();
    }

    public List<TelegramUpdate> getUpdates(long offset, int timeoutSeconds) {
        TelegramUpdatesResponse response = restClient.post()
                .uri("/getUpdates")
                .body(Map.of(
                        "offset", offset,
                        "timeout", timeoutSeconds,
                        "allowed_updates", List.of("message")
                ))
                .retrieve()
                .body(TelegramUpdatesResponse.class);

        if (response == null || !response.ok()) {
            String description = response == null ? "empty Telegram response" : response.description();
            throw new IllegalStateException("Telegram getUpdates failed: " + description);
        }
        return response.result() == null ? List.of() : response.result();
    }

    public void sendMessage(long chatId, String text) {
        TelegramSendMessageResponse response = restClient.post()
                .uri("/sendMessage")
                .body(Map.of(
                        "chat_id", chatId,
                        "text", text,
                        "disable_web_page_preview", true
                ))
                .retrieve()
                .body(TelegramSendMessageResponse.class);

        if (response == null || !response.ok()) {
            String description = response == null ? "empty Telegram response" : response.description();
            throw new IllegalStateException("Telegram sendMessage failed: " + description);
        }
    }
}
