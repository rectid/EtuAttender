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

import java.util.List;

@Component
public class MassMessage {
        private Update update;
    private User user;
    private EtuAttenderBot etuAttenderBot;
    private final UserService userService;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final EtuApiService etuApiService;

    public MassMessage(UserService userService, ReplyKeyboardMarkupService replyKeyboardMarkupService, EtuApiService etuApiService) {
        this.userService = userService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.etuApiService = etuApiService;
    }

    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
        this.etuAttenderBot = etuAttenderBot;
        this.update = update;
        this.user = user;
        String command = update.getMessage().getText();

        switch (command) {
            case "Массовая рассылка":
                return inMessageToUsers();
            case "Назад":
            case "/start":
                return UserState.IN_ADMIN_PANEL;
        }

        return send(command);
    }

    private UserState send(String text) {
        List<User> userList = userService.getAll();
        for (User tempUser:
             userList) {
            SendMessage message = new SendMessage(String.valueOf(tempUser.getId()), text);
            etuAttenderBot.handle(message);
        }
        update.getMessage().setText("Панель Админа");
        return UserState.IN_ADMIN_PANEL;
    }

    private BotApiMethod inMessageToUsers() {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите сообщение для всех пользователей");
        message.setReplyMarkup(replyKeyboardMarkupService.getBackButton());
        return message;
    }


}
