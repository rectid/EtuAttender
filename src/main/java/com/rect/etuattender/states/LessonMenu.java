package com.rect.etuattender.states;

import com.rect.etuattender.model.lesson.Lesson;
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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.xml.crypto.Data;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LessonMenu {
    private Update update;
    private User user;
    private List<String> lessons = new ArrayList<>();
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

        List<Lesson>  lessons = etuApiService.getLessons(user);
        user.setClosestLesson(lessons.get(0).start);
        Date closestLesson = user.getClosestLesson();
        lessons.parallelStream().forEach(lesson ->
        {
            if (lesson.start.before(closestLesson)&&lesson.start.after(new Date(System.currentTimeMillis()))) {
                user.setClosestLesson(lesson.start);
            }
            this.lessons.add(String.valueOf(lesson.id));
        });

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
                    user.getAutoCheckLessons().clear();
                    user.setAutoCheck(false);
                } else {
//            user.getAutoCheckLessons().putAll(etuApiService.getLessons(user).stream().map(lesson -> String.valueOf(lesson.id)).toList());
            user.setAutoCheckLessons(lessons);
            user.setAutoCheck(true);
        }
        userService.saveUser(user);
         EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        return message;
    }



    private BotApiMethod changeLessonStatus(String data){
        if (user.getAutoCheckLessons().contains(data)) {
                user.getAutoCheckLessons().remove(data);
            } else {
                user.getAutoCheckLessons().add(data);}
         userService.saveUser(user);
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        return message;
    }

    private BotApiMethod<Message> inLessonMenu() {
        userService.saveUser(user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getLessonButtons(etuApiService.getLessons(user),user));
        return message;
    }

}
