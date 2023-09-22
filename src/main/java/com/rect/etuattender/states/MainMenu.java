package com.rect.etuattender.states;

import com.rect.etuattender.model.user.User;
import com.rect.etuattender.model.user.UserState;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

@Component
public class MainMenu {

    private Update update;
    private User user;

    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final UserService userService;

    @Autowired
    public MainMenu(ReplyKeyboardMarkupService replyKeyboardMarkupService, UserService userService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.userService = userService;
    }


    public Object select(Update update, User user){
        this.update=update;
        this.user=user;
        String command = update.getMessage().getText();
        switch (command){
            case "Ввести данные ЛК": return UserState.ENTERING_LK;
            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
            case "Расписание": return UserState.IN_LESSONS_MENU;
            case "Назад":
            case "/start":
                return inMainMenu();
        }
        return error();
    }

     @SneakyThrows
     public BotApiMethod<Message> inMainMenu() {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update, user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Вы в главном меню. Если кнопки не появились - введите /start");
        message.setReplyMarkup(replyKeyboardMarkup);
        return message;
    }

    public BotApiMethod<Message> error(){
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()),"Неизвестная команда. Введите /start");
        return message;
    }
}
