package kz.fahrentech.unraidtelegrambot.telegram;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import kz.fahrentech.unraidtelegrambot.telegram.dto.TelegramUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "telegram", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TelegramLongPollingService implements SmartLifecycle {

    private final TelegramApiClient apiClient;
    private final TelegramUpdateHandler updateHandler;
    private final TelegramProperties properties;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new PollingThreadFactory());
    private volatile boolean running;
    private volatile long offset;

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor.submit(this::poll);
        log.info("Telegram long polling started");
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        log.info("Telegram long polling stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void poll() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                for (TelegramUpdate update : apiClient.getUpdates(offset, properties.longPollTimeoutSeconds())) {
                    offset = update.updateId() + 1;
                    updateHandler.handle(update);
                }
            } catch (Exception ex) {
                if (running) {
                    log.warn("Telegram polling error: {}", ex.getMessage());
                    sleepQuietly();
                }
            }
        }
    }

    private void sleepQuietly() {
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class PollingThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "telegram-long-polling");
            thread.setDaemon(false);
            return thread;
        }
    }
}
