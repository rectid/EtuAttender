package com.rect.etuattender.states;

import com.rect.etuattender.controller.EtuAttenderBot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Component
public class EnterWithSave {
    private Update update;
    private User user;
    private EtuAttenderBot etuAttenderBot;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;

    public EnterWithSave(ReplyKeyboardMarkupService replyKeyboardMarkupService, EtuApiService etuApiService, UserService userService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
    }

    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
        this.etuAttenderBot = etuAttenderBot;
        this.update = update;
        this.user = user;
        String command = update.getMessage().getText();
        if (update.hasCallbackQuery()) {
            if (update.getCallbackQuery().getData().equals("REAUTH")){
                return auth(true);
            }
            command = update.getCallbackQuery().getData();
        }

        switch (command) {
            case "SAVE":
                return inEnterWithSave();
            case "Панель Админа":
                return UserState.IN_ADMIN_PANEL;
            case "Изменить выбор":
            case "/start":
                return UserState.ENTERING_LK;
        }

        return auth(false);
    }


    private Object auth(boolean reauth) {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length != 2) {
            SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неправильный формат!");
            return message;
        }

        user.setLogin(lk[0]);
        user.setPassword(lk[1]);
        userService.saveUser(user);
        switch (etuApiService.auth(user, lk)) {
            case "ok":
                String text = "Добро пожаловать!";
                if (reauth){
                    text = "Смена токена выполнена успешно!";
                }
                SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
                message.setReplyMarkup(replyKeyboardMarkupService.get(update, user));
                etuAttenderBot.handle(message);
                return UserState.IN_LESSONS_MENU;
            case "lk_error":
                text = "Неверные данные от ЛК";
                if (reauth){
                    text = "Смена токена не выполнена, проверьте данные ЛК!";
                }
                message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
                return message;
            case "server_error":
                message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ошибка сервера, попробуйте еще раз");
                return message;
        }
        return null;
    }

    private BotApiMethod inEnterWithSave() {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.getBackButtonToEnterLk();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите данные вашего ЛК ЛЭТИ в формате логин:пароль");
        message.setReplyMarkup(replyKeyboardMarkup);
        return message;
    }
}
