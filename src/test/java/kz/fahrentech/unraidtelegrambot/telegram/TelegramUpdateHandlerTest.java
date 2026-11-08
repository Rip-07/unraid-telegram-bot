package kz.fahrentech.unraidtelegrambot.telegram;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.service.ContainerManagementService;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramChat;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramMessage;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUpdate;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUser;
import kz.fahrentech.unraidtelegrambot.user.BotUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateHandlerTest {

    @Mock
    private BotUserService botUserService;

    @Mock
    private ContainerManagementService containerService;

    @Mock
    private TelegramMessageSender sender;

    @Test
    void startShowsTelegramUserIdForUnauthorizedUser() {
        when(botUserService.findByTelegramUserId(42L)).thenReturn(Optional.empty());
        TelegramUpdateHandler handler = new TelegramUpdateHandler(
                new CommandParser(),
                botUserService,
                containerService,
                sender,
                new ResponseFormatter(properties()),
                new TelegramProperties(true, "token", "my_bot", 30),
                properties()
        );

        handler.handle(update("/start"));

        verify(sender).sendText(eq(100L), contains("42"));
    }

    private TelegramUpdate update(String text) {
        return new TelegramUpdate(
                1L,
                new TelegramMessage(
                        10L,
                        new TelegramUser(42L, false, "Alice", null, "alice"),
                        new TelegramChat(100L, "private"),
                        text
                )
        );
    }

    private AppProperties properties() {
        return new AppProperties(
                null,
                new AppProperties.DockerProperties("unix:///var/run/docker.sock", List.of()),
                new AppProperties.LogsProperties(300),
                new AppProperties.ExecProperties(false, 10, false),
                new AppProperties.ResponseProperties(3500)
        );
    }
}
