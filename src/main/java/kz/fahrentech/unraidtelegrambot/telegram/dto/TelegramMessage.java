package kz.fahrentech.unraidtelegrambot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramMessage(
        @JsonProperty("message_id") long messageId,
        TelegramUser from,
        TelegramChat chat,
        String text
) {
}
