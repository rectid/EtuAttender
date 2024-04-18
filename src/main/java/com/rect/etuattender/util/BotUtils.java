package com.rect.etuattender.util;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.User;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.rect.etuattender.model.User.State.IN_LESSONS_MENU;
import static com.rect.etuattender.model.User.State.IN_MAIN_MENU;

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

    @SneakyThrows
    public void deleteMessage(Update update, Bot bot){
        bot.execute(DeleteMessage.builder()
                    .chatId(BotUtils.getUserId(update))
                    .messageId(BotUtils.getMessageId(update))
                    .build());
    }

}
