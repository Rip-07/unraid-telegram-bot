package kz.fahrentech.unraidtelegrambot.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramSendMessageResponse(
        boolean ok,
        TelegramMessage result,
        String description
) {
}
