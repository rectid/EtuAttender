package com.rect.etuattender.states;

import com.rect.etuattender.controller.EtuAttenderBot;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Component
public class LessonMenu {
    private List<Lesson> lessons = new ArrayList<>();
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final EtuApiService etuApiService;
    private final UserService userService;
    private CheckService checkService;

    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;

    @Autowired
    public LessonMenu(InlineKeyboardMarkupService inlineKeyboardMarkupService, EtuApiService etuApiService, UserService userService, ReplyKeyboardMarkupService replyKeyboardMarkupService) {
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.etuApiService = etuApiService;
        this.userService = userService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
    }


    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
        this.checkService = new CheckService(userService, etuApiService, etuAttenderBot);

        this.lessons = etuApiService.getLessons(user);
        String command = update.getMessage().getText();


        if (update.getCallbackQuery() != null) {
            String data = update.getCallbackQuery().getData();
            switch (data) {
                case "AUTO_CHECK":
                    return changeAutoCheckStatus(data, update, user);
                case "REAUTH":
                    userService.saveUser(user);
                    return inLessonMenu(update, user);
                default:
                    return changeLessonStatus(data, update, user);
            }
        }

        switch (command) {
            case "Панель Админа":
                return UserState.IN_ADMIN_PANEL;
            case "Расписание", "Назад":
                return inLessonMenu(update, user);
            case "Изменить данные ЛК":
                return UserState.ENTERING_LK;
            case "Полное расписание": return getFullSchedule(update, user);
            case "Информация":
                return getInfo(update, user);
            case "/start":
                return UserState.IN_MAIN_MENU;
        }

        checkService.initChecking();
        return inLessonMenu(update, user);
    }
    private BotApiMethod getFullSchedule(Update update, User user) {
        List<Lesson> lessonList = etuApiService.getLessons(user);
        String text = "Ошибка!";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ваше расписание на неделю: "+"\n(Отмечены: вы | староста | преподаватель)");
        LocalDate date = LocalDate.of(2024, Month.FEBRUARY,12);
        DayOfWeek day = DayOfWeek.SUNDAY;
        for (Lesson lesson : lessonList) {
            if (lesson.getStartDate().getDayOfWeek() != day){
                day = lesson.getStartDate().getDayOfWeek();
                stringBuilder.append("\n\n===" + day.getDisplayName(TextStyle.FULL, Locale.of("ru")).toUpperCase()+ "===");
            }
            String cab = "?";
            if (lesson.getRoom() != null) {
                cab = lesson.getRoom();
            }
            if (lesson.isDistant()){
                cab = "Дистанционно";
            }
            String teacher = "?";
            if (!lesson.getTeacher().equals("[]")) {
                teacher = StringUtils.substringBetween(lesson.getTeacher(),"name=",",");
            }

            String selfReport = "❌";
            if (lesson.isSelfReported()){
                selfReport = "✔";
            }

            String teacherReported = "❌";
            if (lesson.isTeacherReported()){
                teacherReported = "✔";
            }

            String leaderReported = "❌";
            if (lesson.isGroupLeaderReported()){
                leaderReported = "✔";
            }
            stringBuilder.append("\n\nПара: " + lesson.getShortTitle() +
                    "\nКабинет: " + cab +
                    "\nПреподаватель: " + teacher +
                    "\nВремя: " + lesson.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm")) + " - " + lesson.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm"))+
                    "\nОтмечены: "+ selfReport + " | " + leaderReported + " | " + teacherReported);
        }
        text = stringBuilder.toString();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
        return message;
    }

    private BotApiMethod getInfo(Update update, User user) {
        String auto = "❌";
        if (user.getLogin() != null) {
            auto = "✔";
        }
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Срок действия токена: " + user.getCookieLifetime() +
                "\nАвтоматическая аутентификация: " + auto +
                "\nТех поддержка: @rected"+
                "\n\nМеню расписание позволяет выбрать предметы, которые вы хотите отметить именно сегодня. " +
                "\nЛибо же, вы можете нажатием на кнопку Все пары включить отметку на все ваши пары на данной неделе. " +
                "\nРасписание обновляется каждый понедельник в 00:00 МСК" +
                "\nПриятного пользования!");
        return message;
    }

    private BotApiMethod changeAutoCheckStatus(String data, Update update, User user) {
        if (user.isAutoCheck()) {
            user.getLessons().clear();
            user.setAutoCheck(false);
        } else {
            user.setLessons(lessons);
            user.setAutoCheck(true);
        }
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(user, data, update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        userService.saveUser(user);
        checkService.initChecking();
        return message;
    }


    private BotApiMethod changeLessonStatus(String data, Update update, User user) {
        Lesson listLesson = lessons.stream().filter(lesson1 -> lesson1.getLessonId().equals(data)).findFirst().get();
        Optional<Lesson> userLesson = user.getLessons().stream().filter(lesson1 -> lesson1.getLessonId().equals(listLesson.getLessonId())).findFirst();
        if (userLesson.isPresent()) {
            user.getLessons().remove(userLesson.get());
        } else {
            user.getLessons().add(listLesson);
        }
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setReplyMarkup(inlineKeyboardMarkupService.editLessonButtons(user, data, update.getMessage().getReplyMarkup()));
        message.setText(update.getMessage().getText());
        userService.saveUser(user);
        checkService.initChecking();
        return message;
    }

    private BotApiMethod inLessonMenu(Update update, User user) {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня, выберите, на чем вас отметить:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getAuthButtons(lessons, user));
        return message;
    }

}
