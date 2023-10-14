package com.rect.etuattender.states;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;

@Component
public class LessonMenu {
    private Update update;
    private User user;
    private List<Lesson> lessons = new ArrayList<>();
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;
    private final CheckService checkService;

    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;

    @Autowired
    public LessonMenu(InlineKeyboardMarkupService inlineKeyboardMarkupService, EtuApiService etuApiService, UserService userService, CheckService checkService, ReplyKeyboardMarkupService replyKeyboardMarkupService) {
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
        this.checkService = checkService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
    }


    public Object select(Update update, User user){
        this.update=update;
        this.user=user;
        this.lessons = etuApiService.getLessons(user);
        String command = update.getMessage().getText();
//        userService.updateUserClosestLesson(user,lessons);

        if (update.getCallbackQuery()!=null){
            String data = update.getCallbackQuery().getData();
            if (data.equals("AUTO_CHECK")){
                return changeAutoCheckStatus(data);
            } else {
                return changeLessonStatus(data);
            }
        }
        switch (command){
            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
            case "Назад":
            case "/start":
                return UserState.IN_MAIN_MENU;
        }


        return inLessonMenu();
    }

    private BotApiMethod changeAutoCheckStatus(String data){
        if (user.isAutoCheck()){
                    user.getLessons().clear();
                    user.setAutoCheck(false);
                } else {
            user.setLessons(lessons);
            user.setAutoCheck(true);
        }
         EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(user,data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        userService.saveUser(user);
        checkService.initChecking();
        return message;
    }



    private BotApiMethod changeLessonStatus(String data){
        Lesson listLesson = lessons.stream().filter(lesson1 -> lesson1.getLessonId().equals(data)).findFirst().get();
        Optional<Lesson> userLesson = user.getLessons().stream().filter(lesson1 -> lesson1.getLessonId().equals(listLesson.getLessonId())).findFirst();
        if (userLesson.isPresent()){
            user.getLessons().remove(userLesson.get());
        } else {
            user.getLessons().add(listLesson);
        }
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(user,data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        userService.saveUser(user);
        checkService.initChecking();
        return message;
    }

    private BotApiMethod inLessonMenu() {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getLessonButtons(lessons,user));
        return message;
    }

}
