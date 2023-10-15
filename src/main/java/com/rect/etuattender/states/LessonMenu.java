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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class LessonMenu {
    private Update update;
    private User user;
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
        this.update = update;
        this.user = user;
        if (user.getCookieLifetime()!=null) {
            if (user.getCookieLifetime().isBefore(LocalDateTime.now())) {
            if (user.getLogin() != null) {
                user.setState(UserState.ENTERING_WITH_SAVE);
                userService.saveUser(user);
                Update tempUpdate = new Update();
                Message message = new Message();
                message.setText(user.getLogin() + ":" + user.getPassword());
                tempUpdate.setMessage(message);
                Chat chat = new Chat();
                chat.setId(user.getId());
                tempUpdate.getMessage().setChat(chat);
                tempUpdate.setCallbackQuery(new CallbackQuery() {{
                    setData("REAUTH");
                }});
                etuAttenderBot.onUpdateReceived(tempUpdate);
            } else return UserState.IN_MAIN_MENU;
        }}

        this.lessons = etuApiService.getLessons(user);
        this.checkService = new CheckService(userService, etuApiService, etuAttenderBot);
        String command = update.getMessage().getText();


        if (update.getCallbackQuery() != null) {
            String data = update.getCallbackQuery().getData();
            switch (data) {
                case "AUTO_CHECK":
                    return changeAutoCheckStatus(data);
                case "REAUTH":
                    return inLessonMenu();
                default:
                    return changeLessonStatus(data);
            }
        }

        switch (command) {
            case "Панель Админа":
                return UserState.IN_ADMIN_PANEL;
            case "Расписание", "Назад":
                return inLessonMenu();
            case "Изменить данные ЛК":
                return UserState.ENTERING_LK;
            case "Полное расписание": return getFullSchedule();
            case "Информация":
                return getInfo();
            case "/start":
                return UserState.IN_MAIN_MENU;
        }

        checkService.initChecking();
        return inLessonMenu();
    }
    private BotApiMethod getFullSchedule() {
        List<Lesson> lessonList = etuApiService.getLessons(user);
        String text = "Ошибка!";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ваше расписание на неделю: ");
        for (Lesson lesson : lessonList) {
            String cab = "?";
            if (lesson.getRoom() != null) {
                cab = lesson.getRoom();
            }
            String teacher = "?";
            if (!lesson.getTeacher().equals("[]")) {
                teacher = StringUtils.substringBetween(lesson.getTeacher(),"name=",",");
            }
            String selfReport = "❌";

            if (lesson.isSelfReported()){
                selfReport = "✔";
            }
            stringBuilder.append("\n\nПара: " + lesson.getShortTitle() +
                    "\nКабинет: " + cab +
                    "\nПреподаватель: " + teacher +
                    "\nНачало: " + lesson.getStartDate().format(DateTimeFormatter.ofPattern("dd.MM HH:mm")) +
                    "\nКонец: " + lesson.getEndDate().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))+
                    "\nВы были отмечены: "+ selfReport);
        }
        text = stringBuilder.toString();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
        return message;
    }

    private BotApiMethod getInfo() {
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

    private BotApiMethod changeAutoCheckStatus(String data) {
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


    private BotApiMethod changeLessonStatus(String data) {
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

    private BotApiMethod inLessonMenu() {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваше расписание на сегодня:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getAuthButtons(lessons, user));
        return message;
    }

}
