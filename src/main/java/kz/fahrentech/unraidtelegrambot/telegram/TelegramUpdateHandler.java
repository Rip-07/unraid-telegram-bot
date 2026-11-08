package kz.fahrentech.unraidtelegrambot.telegram;

import java.util.List;
import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.container.ManagedContainer;
import kz.fahrentech.unraidtelegrambot.docker.DockerContainerStatus;
import kz.fahrentech.unraidtelegrambot.docker.ExecResult;
import kz.fahrentech.unraidtelegrambot.exception.DockerOperationException;
import kz.fahrentech.unraidtelegrambot.exception.UserVisibleException;
import kz.fahrentech.unraidtelegrambot.service.ContainerManagementService;
import kz.fahrentech.unraidtelegrambot.telegram.CommandParser.SplitValue;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramMessage;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUpdate;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUser;
import kz.fahrentech.unraidtelegrambot.user.BotUser;
import kz.fahrentech.unraidtelegrambot.user.BotUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramUpdateHandler {

    private final CommandParser commandParser;
    private final BotUserService botUserService;
    private final ContainerManagementService containerService;
    private final TelegramMessageSender sender;
    private final ResponseFormatter formatter;
    private final TelegramProperties telegramProperties;
    private final AppProperties appProperties;

    public void handle(TelegramUpdate update) {
        TelegramMessage message = update.message();
        if (message == null || message.text() == null || message.chat() == null || message.from() == null) {
            return;
        }

        Optional<BotCommand> parsed = commandParser.parse(message.text(), telegramProperties.botUsername());
        if (parsed.isEmpty()) {
            return;
        }

        long chatId = message.chat().id();
        TelegramUser telegramUser = message.from();
        long telegramUserId = telegramUser.id();
        botUserService.updateUsernameIfPresent(telegramUserId, usernameOf(telegramUser));

        try {
            dispatch(chatId, telegramUser, parsed.get());
        } catch (UserVisibleException | DockerOperationException ex) {
            sender.sendText(chatId, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to handle Telegram command {}", parsed.get().name(), ex);
            sender.sendText(chatId, "Не удалось выполнить команду. Подробности смотрите в логах приложения.");
        }
    }

    private void dispatch(long chatId, TelegramUser telegramUser, BotCommand command) {
        switch (command.name()) {
            case "start" -> handleStart(chatId, telegramUser);
            case "help" -> sender.sendText(chatId, helpText());
            case "containers" -> handleContainers(chatId, requireUser(telegramUser));
            case "status" -> handleStatus(chatId, requireUser(telegramUser), command);
            case "start_container" -> handleStartContainer(chatId, requireUser(telegramUser), command);
            case "stop_container" -> handleStopContainer(chatId, requireUser(telegramUser), command);
            case "restart_container" -> handleRestartContainer(chatId, requireUser(telegramUser), command);
            case "logs" -> handleLogs(chatId, requireUser(telegramUser), command);
            case "exec" -> handleExec(chatId, requireUser(telegramUser), command);
            default -> sender.sendText(chatId, "Неизвестная команда. Используйте /help.");
        }
    }

    private void handleStart(long chatId, TelegramUser telegramUser) {
        Optional<BotUser> user = botUserService.findByTelegramUserId(telegramUser.id());
        String access = user.isPresent()
                ? "Пользователь найден, роль: " + user.get().role()
                : "Пользователь пока не авторизован. Передайте этот ID администратору.";
        sender.sendText(chatId, """
                Docker Bot для Unraid.
                Ваш Telegram user ID: %d
                %s
                """.formatted(telegramUser.id(), access).strip());
    }

    private void handleContainers(long chatId, BotUser user) {
        List<ManagedContainer> containers = containerService.listAllowedContainers(user);
        if (containers.isEmpty()) {
            sender.sendText(chatId, "Для вас нет разрешенных контейнеров.");
            return;
        }

        StringBuilder response = new StringBuilder("Разрешенные контейнеры:\n");
        for (ManagedContainer container : containers) {
            response.append("- ").append(container.containerName());
            if (container.description() != null && !container.description().isBlank()) {
                response.append(" — ").append(container.description());
            }
            response.append('\n');
        }
        sender.sendText(chatId, formatter.limit(response.toString().stripTrailing()));
    }

    private void handleStatus(long chatId, BotUser user, BotCommand command) {
        String containerName = requireArg(command, "Использование: /status <container>");
        DockerContainerStatus status = containerService.status(user, containerName);
        sender.sendText(chatId, formatter.status(status));
    }

    private void handleStartContainer(long chatId, BotUser user, BotCommand command) {
        String containerName = requireArg(command, "Использование: /start_container <container>");
        containerService.start(user, containerName);
        sender.sendText(chatId, "Контейнер `%s` запущен.".formatted(containerName));
    }

    private void handleStopContainer(long chatId, BotUser user, BotCommand command) {
        String containerName = requireArg(command, "Использование: /stop_container <container>");
        containerService.stop(user, containerName);
        sender.sendText(chatId, "Контейнер `%s` остановлен.".formatted(containerName));
    }

    private void handleRestartContainer(long chatId, BotUser user, BotCommand command) {
        String containerName = requireArg(command, "Использование: /restart_container <container>");
        containerService.restart(user, containerName);
        sender.sendText(chatId, "Контейнер `%s` перезапущен.".formatted(containerName));
    }

    private void handleLogs(long chatId, BotUser user, BotCommand command) {
        if (command.arguments().size() < 2) {
            throw new UserVisibleException("Использование: /logs <container> <lines>");
        }
        String containerName = command.arguments().get(0);
        int requestedLines = parsePositiveInt(command.arguments().get(1), "Количество строк должно быть положительным числом.");
        int safeLines = Math.min(requestedLines, appProperties.logsMaxLines());
        String logs = containerService.logs(user, containerName, requestedLines);
        String response = formatter.logs(containerName, safeLines, logs);
        if (requestedLines > safeLines) {
            response = "Запрошено %d строк, лимит APP_LOGS_MAX_LINES=%d.\n\n%s"
                    .formatted(requestedLines, safeLines, response);
        }
        sender.sendText(chatId, formatter.limit(response));
    }

    private void handleExec(long chatId, BotUser user, BotCommand command) {
        SplitValue split = commandParser.splitFirstArgument(command.rawArguments());
        if (split.first().isBlank() || split.remainder().isBlank()) {
            throw new UserVisibleException("Использование: /exec <container> <command>");
        }

        ExecResult result = containerService.exec(user, split.first(), split.remainder());
        sender.sendText(chatId, formatter.exec(split.first(), split.remainder(), result));
    }

    private BotUser requireUser(TelegramUser telegramUser) {
        return botUserService.findByTelegramUserId(telegramUser.id())
                .orElseThrow(() -> new UserVisibleException(
                        "Доступ запрещен. Ваш Telegram user ID: %d. Передайте его администратору."
                                .formatted(telegramUser.id())));
    }

    private String requireArg(BotCommand command, String usage) {
        if (command.arguments().isEmpty() || command.arguments().getFirst().isBlank()) {
            throw new UserVisibleException(usage);
        }
        return command.arguments().getFirst();
    }

    private int parsePositiveInt(String value, String errorMessage) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new NumberFormatException("not positive");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new UserVisibleException(errorMessage);
        }
    }

    private String usernameOf(TelegramUser user) {
        if (user.username() != null && !user.username().isBlank()) {
            return user.username();
        }
        String fullName = ((user.firstName() == null ? "" : user.firstName())
                + " "
                + (user.lastName() == null ? "" : user.lastName())).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private String helpText() {
        return """
                Доступные команды:
                /start — приветствие и ваш Telegram user ID
                /help — список команд
                /containers — список разрешенных контейнеров
                /status <container> — статус контейнера
                /start_container <container> — запуск контейнера
                /stop_container <container> — остановка контейнера
                /restart_container <container> — перезапуск контейнера
                /logs <container> <lines> — последние N строк логов
                /exec <container> <command> — безопасный exec, если включен и разрешен
                """.strip();
    }
}
