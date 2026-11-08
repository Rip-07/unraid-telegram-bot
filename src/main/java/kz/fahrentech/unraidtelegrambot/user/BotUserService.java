package kz.fahrentech.unraidtelegrambot.user;

import java.util.Optional;
import kz.fahrentech.unraidtelegrambot.repository.BotUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BotUserService {

    private final BotUserRepository repository;

    public Optional<BotUser> findByTelegramUserId(long telegramUserId) {
        return repository.findByTelegramUserId(telegramUserId)
                .filter(BotUser::enabled);
    }

    public void updateUsernameIfPresent(long telegramUserId, String username) {
        repository.updateUsernameIfPresent(telegramUserId, username);
    }
}
