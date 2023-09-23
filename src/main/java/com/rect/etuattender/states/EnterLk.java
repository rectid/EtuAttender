package com.rect.etuattender.states;

import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Component
public class EnterLk {

    private Update update;
    private User user;

    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final UserService userService;
    private final EtuApiService etuApiService;

    @Autowired
    public EnterLk(ReplyKeyboardMarkupService replyKeyboardMarkupService, UserService userService, EtuApiService etuApiService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.userService = userService;
        this.etuApiService = etuApiService;
    }


    public Object select(Update update, User user){
        this.update=update;
        this.user=user;
        String command = update.getMessage().getText();
        switch (command){
            case "Ввести данные ЛК": return inEnterLk();
            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
            case "Назад":
            case "/start":
                return UserState.IN_MAIN_MENU;
        }

        return auth();
    }

    private Object auth() {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length!=2){
            return UserState.ENTERING_LK;
        }
        switch (etuApiService.auth(user,lk)){
            case "ok":
                return UserState.IN_LESSONS_MENU;
            case "lk_error":
                SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неверные данные от лк");
                return message;
            case "server_error":
                SendMessage message1 = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ошибка сервера, попробуйте еще раз");
                return message1;
        }
        return null;
    }

    private BotApiMethod inEnterLk() {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update,user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите данные вашего лк в формате логин:пароль");
        message.setReplyMarkup(replyKeyboardMarkup);
        return message;
    }

}
