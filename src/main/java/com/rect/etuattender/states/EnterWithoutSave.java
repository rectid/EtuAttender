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
public class EnterWithoutSave {
     private Update update;
    private User user;
    private EtuAttenderBot etuAttenderBot;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;

    public EnterWithoutSave(ReplyKeyboardMarkupService replyKeyboardMarkupService, EtuApiService etuApiService, UserService userService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
    }

    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot){
        this.etuAttenderBot = etuAttenderBot;
        this.update=update;
        this.user=user;
        String command = update.getMessage().getText();
        if (update.hasCallbackQuery()){
            command =update.getCallbackQuery().getData();
        }

        switch (command){
            case "NOT_SAVE": return inEnterWithoutSave();
            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
            case "Изменить выбор":
            case "/start":
                return UserState.ENTERING_LK;
        }

        return auth();
    }


    private Object auth() {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length!=2){
            SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неправильный формат!");
                return message;
        }
        user.setLogin(null);
        user.setPassword(null);
        userService.saveUser(user);
        switch (etuApiService.auth(user,lk)){
            case "ok":
                SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Добро пожаловать!");
                message.setReplyMarkup(replyKeyboardMarkupService.get(update,user));
                etuAttenderBot.handle(message);
                return UserState.IN_LESSONS_MENU;
            case "lk_error":
                message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неверные данные от ЛК");
                return message;
            case "server_error":
                message= new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ошибка сервера, попробуйте еще раз");
                return message;
        }
        return null;
    }

    private BotApiMethod inEnterWithoutSave() {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.getBackButtonToEnterLk();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите данные вашего ЛК ЛЭТИ в формате логин:пароль");
        message.setReplyMarkup(replyKeyboardMarkup);
        return message;
    }
}
