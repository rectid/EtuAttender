package com.rect.etuattender.handler;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.service.*;
import com.rect.etuattender.util.BotStrings;
import com.rect.etuattender.util.BotUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.rect.etuattender.model.User.State.*;
import static com.rect.etuattender.controller.Bot.executor;

@Component
@Slf4j
public class LessonMenuHandler {

    private final UserService userService;
    private final LessonService lessonService;
    private final Bot bot;
    private final EtuApiService etuApiService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;

    @Lazy
    public LessonMenuHandler(UserService userService, LessonService lessonService, Bot bot, EtuApiService etuApiService, InlineKeyboardMarkupService inlineKeyboardMarkupService, ReplyKeyboardMarkupService replyKeyboardMarkupService) {
        this.userService = userService;
        this.lessonService = lessonService;
        this.bot = bot;
        this.etuApiService = etuApiService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
    }

    public void handle(Update update, User user) {
        String command = BotUtils.getCommand(update).orElse("UNKNOWN");
        if (!BotUtils.checkCookies(user)) authExpired(update, user);
        List<Lesson> lessons = etuApiService.getLessons(user);

        switch (command) {
            case "UNKNOWN":
                error(update);
                break;
            case "AUTO_CHECK":
                changeAutoCheckStatus(update, user, lessons);
                break;
            case "Панель Админа":
                bot.routeHandling(update, user, IN_ADMIN_PANEL);
                break;
            case "Расписание":
                inLessonMenu(update, user, lessons, false);
                break;
            case "Назад", "/start":
                inLessonMenu(update, user, lessons, true);
            case "Изменить данные ЛК":
                bot.routeHandling(update, user, ENTERING_LK);
                break;
            case "Полное расписание":
                getFullSchedule(update, user);
                break;
            case "Информация":
                getInfo(update, user);
                break;
            default:
                changeLessonStatus(update, user, lessons);
        }
    }

    @SneakyThrows
    public void authExpired(Update update, User user) {
        user.setState(IN_MAIN_MENU);
        userService.saveUser(user);
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(replyKeyboardMarkupService.get(update, user))
                .text("Ваш токен регистрации в системе истек, необходимо ввести данные ЛК снова!")
                .build());
    }

    @SneakyThrows
    public void error(Update update) {
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .text("Неизвестная команда.")
                .build());
    }

    @SneakyThrows
    private void inLessonMenu(Update update, User user, List<Lesson> lessons, boolean isShowWelcome) {
        if (isShowWelcome) {
            bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(replyKeyboardMarkupService.get(update, user))
                .text("Добро пожаловать в расписание!").build());
        }
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(inlineKeyboardMarkupService.getLessonsButtons(lessons, user))
                .text("Ваше расписание на сегодня, выберите, на чем вас отметить:").build());
    }

    @SneakyThrows
    private void getFullSchedule(Update update, User user) {
        List<Lesson> lessonList = etuApiService.getLessons(user);
        String text;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ваше расписание на неделю: " + "\n(Отмечены: вы | староста | преподаватель)");
        DayOfWeek day = DayOfWeek.SUNDAY;
        for (Lesson lesson : lessonList) {
            if (lesson.getStartDate().getDayOfWeek() != day) {
                day = lesson.getStartDate().getDayOfWeek();
                stringBuilder.append("\n\n===").append(day.getDisplayName(TextStyle.FULL, Locale.of("ru")).toUpperCase()).append("===");
            }
            String cab = "?";
            if (lesson.getRoom() != null) {
                cab = lesson.getRoom();
            }
            if (lesson.isDistant()) {
                cab = "Дистанционно";
            }
            String teacher = "?";
            if (!lesson.getTeacher().equals("[]")) {
                teacher = StringUtils.substringBetween(lesson.getTeacher(), "name=", ",");
            }

            String selfReport = "❌";
            if (lesson.isSelfReported()) {
                selfReport = "✔";
            }

            String teacherReported = "❌";
            if (lesson.isTeacherReported()) {
                teacherReported = "✔";
            }

            String leaderReported = "❌";
            if (lesson.isGroupLeaderReported()) {
                leaderReported = "✔";
            }
            stringBuilder.append("\n\nПара: ")
                    .append(lesson.getShortTitle())
                    .append("\nКабинет: ")
                    .append(cab)
                    .append("\nПреподаватель: ")
                    .append(teacher).append("\nВремя: ")
                    .append(lesson.getStartDate().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append(" - ").append(lesson.getEndDate().format(DateTimeFormatter.ofPattern("HH:mm")))
                    .append("\nОтмечены: ").append(selfReport).append(" | ").append(leaderReported).append(" | ").append(teacherReported);
        }
        text = stringBuilder.toString();
        bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text(text).build());
    }

    @SneakyThrows
    private void getInfo(Update update, User user) {
        String auto = "❌";
        if (user.getLogin() != null) {
            auto = "✔";
        }
        bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text(BotStrings.getLessonMenuInfo(user.getCookieLifetime(), auto)).build());
    }

    @SneakyThrows
    private void changeAutoCheckStatus(Update update, User user, List<Lesson> lessons) {
        if (user.isAutoCheck()) {
            user.getLessons().clear();
            user.setAutoCheck(false);
            log.info(user.getId() + " turns off autocheck");
        } else {
            user.setLessons(lessons);
            user.setAutoCheck(true);
            executor.execute(() -> {
                userService.updateUserClosestLesson(user);
                Optional<Lesson> lesson = lessons.stream().filter(lesson1 -> lesson1.getStartDate().isAfter(LocalDateTime.now()) && lesson1.getEndDate().isBefore(LocalDateTime.now())).findFirst();
                lesson.ifPresent(value -> etuApiService.check(user, value));
            });
            log.info(user.getId() + " turns on autocheck");
        }
        userService.saveUser(user);
        bot.execute(EditMessageReplyMarkup.builder().chatId(BotUtils.getUserId(update)).messageId(BotUtils.getMessageId(update)).replyMarkup(inlineKeyboardMarkupService.editLessonButtons(update, user)).build());
    }

    @SneakyThrows
    private void changeLessonStatus(Update update, User user, List<Lesson> lessons) {
        Lesson etuApiLesson = lessons.stream().filter(lesson1 -> lesson1.getLessonId().equals(update.getCallbackQuery().getData())).findFirst().orElseThrow();
        Optional<Lesson> userLesson = user.getLessons().stream().filter(lesson1 -> lesson1.getLessonId().equals(etuApiLesson.getLessonId())).findFirst();
        if (userLesson.isPresent()) {
            user.getLessons().remove(userLesson.get());
            user.setAutoCheck(false);
            log.info(user.getId() + " removes lesson " + userLesson.get().getLessonId());
        } else {
            user.getLessons().add(etuApiLesson);
            executor.execute(() -> {
                userService.updateUserClosestLesson(user);
                if (etuApiLesson.getStartDate().isAfter(LocalDateTime.now()) && etuApiLesson.getEndDate().isBefore(LocalDateTime.now()))
                    etuApiService.check(user, etuApiLesson);
            });
            log.info(user.getId() + " adds lesson " + etuApiLesson.getLessonId());
        }
        userService.saveUser(user);
        bot.execute(EditMessageReplyMarkup.builder().chatId(BotUtils.getUserId(update)).messageId(BotUtils.getMessageId(update)).replyMarkup(inlineKeyboardMarkupService.editLessonButtons(update, user)).build());
    }
}
