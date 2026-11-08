package kz.fahrentech.unraidtelegrambot.user;

import java.util.OptionalLong;
import kz.fahrentech.unraidtelegrambot.config.AppProperties;
import kz.fahrentech.unraidtelegrambot.repository.BotUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialAdminSeeder implements ApplicationListener<ApplicationReadyEvent> {

    private final AppProperties properties;
    private final BotUserRepository repository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repository.adminExists()) {
            log.info("Admin user already exists; APP_INITIAL_ADMIN_TELEGRAM_ID will not create a duplicate");
            return;
        }

        OptionalLong initialAdminId;
        try {
            initialAdminId = properties.initialAdminTelegramIdAsLong();
        } catch (NumberFormatException ex) {
            log.error("APP_INITIAL_ADMIN_TELEGRAM_ID must be a numeric Telegram user ID");
            return;
        }

        if (initialAdminId.isEmpty()) {
            log.warn("No admin exists yet. Set APP_INITIAL_ADMIN_TELEGRAM_ID for the first startup");
            return;
        }

        repository.upsertInitialAdmin(initialAdminId.getAsLong());
        log.info("Initial admin was created from APP_INITIAL_ADMIN_TELEGRAM_ID");
    }
}
