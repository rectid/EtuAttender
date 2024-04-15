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
        if (user.getCookie() == null){
            return false;
        } else return !user.getCookieLifetime().isBefore(LocalDateTime.now());
    }

    public Long getUserId(Update update){
        if (update.hasMessage()){
            return update.getMessage().getFrom().getId();
        } else {
            return update.getCallbackQuery().getFrom().getId();
        }
    }

    public Integer getMessageId(Update update){
        if (update.hasMessage()){
            return update.getMessage().getMessageId();
        } else {
            return update.getCallbackQuery().getMessage().getMessageId();
        }
    }

}
