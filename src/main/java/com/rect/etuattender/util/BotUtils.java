package com.rect.etuattender.util;

import com.rect.etuattender.model.User;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@UtilityClass
public class BotUtils {

    public Optional<String> getCommand(Update update){
        if (update.hasMessage()){
            return Optional.ofNullable(update.getMessage().getText());
        } else if (update.hasCallbackQuery()) {
            return Optional.ofNullable(update.getCallbackQuery().getData());
        }
        return Optional.empty();
    }

    public boolean checkCookies(User user){
        if (user.getCookie().isEmpty()){
            return false;
        } else if (user.getCookieLifetime().isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
