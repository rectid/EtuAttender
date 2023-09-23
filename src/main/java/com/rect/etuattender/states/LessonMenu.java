package com.rect.etuattender.states;

import com.rect.etuattender.dto.lesson.LessonDto;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
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
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;

    private final ModelMapper modelMapper;

    @Autowired
    public LessonMenu(ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, EtuApiService etuApiService, UserService userService, ModelMapper modelMapper) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }


    public Object select(Update update, User user){
        this.update=update;
        this.user=user;
        String command = update.getMessage().getText();

        List<LessonDto> lessonDtos = etuApiService.getLessons(user);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> src.getLesson().getShortTitle(),Lesson::setShortTitle);
//        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> user,Lesson::setUser);
        List<Lesson> lessons = modelMapper.map(lessonDtos, new TypeToken<List<Lesson>>() {}.getType());
        this.lessons = lessons;
        for (Lesson lesson :
                lessons) {
            if (lesson.getStartDate().after(new Date(1695369781429L))){
                user.setClosestLesson(lesson.getId());
                user.getLessons().add(lesson);
                userService.saveUser(user);
                break;
            }
        }

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
        userService.saveUser(user);
         EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        return message;
    }



    private BotApiMethod changeLessonStatus(String data){
        user.getLessons().stream().parallel().forEach(lesson -> {
            if (Objects.equals(lesson.getId(),data)) {
                user.getLessons().remove(lesson);
            } else {
                user.getLessons().add(lesson);
            }
        });
         userService.saveUser(user);
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(data,update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        return message;
    }

    private BotApiMethod inLessonMenu() {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getLessonButtons(lessons,user));
        return message;
    }

}
