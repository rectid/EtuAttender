package com.rect.etuattender.states;

import com.rect.etuattender.model.user.User;
import com.rect.etuattender.model.user.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class LessonMenu {
    private Update update;
    private User user;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;

    @Autowired
    public LessonMenu(ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, EtuApiService etuApiService, UserService userService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
    }


    public Object select(Update update, User user){
        this.update=update;
        this.user=user;
        String command = update.getMessage().getText();
        switch (command){
            case "Ввести данные ЛК": return user;
            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
            case "Назад":
            case "/start":
                return inLessonMenu();
        }


        return inLessonMenu();
    }

    private BotApiMethod<Message> inLessonMenu() {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getLessonButtons(etuApiService.getLessons(user),user));
        return message;
    }
}
