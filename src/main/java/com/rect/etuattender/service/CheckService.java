package com.rect.etuattender.service;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static com.rect.etuattender.controller.Bot.executor;
import static com.rect.etuattender.model.User.State.IN_MAIN_MENU;

@Component
@Slf4j
public class CheckService {

    private final UserService userService;
    private final EtuApiService etuApiService;
    private final Bot bot;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;

    @Lazy
    public CheckService(UserService userService, EtuApiService etuApiService, Bot bot, ReplyKeyboardMarkupService replyKeyboardMarkupService) {
        this.userService = userService;
        this.etuApiService = etuApiService;
        this.bot = bot;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initChecking() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        executeScheduledTask();
    }

    public void updateUsersLessons() {
        List<User> users = userService.getAll();
        for (var user : users) {
            if (user.isAutoCheck()) {
                user.setLessons(etuApiService.getLessons(user));
            }
            userService.saveUser(user);
        }
    }

    @SneakyThrows
    public boolean checkUserCookieStatus(User user) {
        if (user.getCookie() == null || user.getCookieLifetime() == null) return false;
        if (user.getCookieLifetime().isBefore(LocalDateTime.now())) {
            if (user.getCookie().equals("EXPIRED")) {
                return false;
            }
            if (user.getLogin() != null) {
                bot.execute(SendMessage.builder()
                .chatId(user.getId())
                .text("Ваш токен регистрации в системе истек, начинаю замену")
                .build());
                Update update = new Update();
                Message message = new Message();
                message.setFrom(new org.telegram.telegrambots.meta.api.objects.User() {{
                    setId(user.getId());
                }});
                message.setText(user.getLogin() + ":" + user.getPassword());
                update.setMessage(message);
                Future<?> task = executor.submit(() -> bot.routeHandling(update, user, User.State.ENTERING_WITH_SAVE));
                task.get();
                return true;
            } else {
                user.setCookie("EXPIRED");
                user.setState(IN_MAIN_MENU);
                userService.saveUser(user);
                bot.execute(SendMessage.builder()
                .chatId(user.getId())
                .replyMarkup(replyKeyboardMarkupService.getMainMenuButtons(user))
                .text("Ваш токен регистрации в системе истек, необходимо ввести данные ЛК снова!")
                .build());
                return false;
            }
        }
        return true;
    }

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<Void> scheduledFuture;

    private void executeScheduledTask() {
        Callable<Void> task = new Callable<>() {

            @SneakyThrows
            public Void call() {
                log.info(LocalDateTime.now().toString());
                long delay;
                Callable<List<User>> c = userService::getAll;
                List<User> users = new ArrayList<>(c.call());
                if (LocalTime.now().isBefore(LocalTime.of(1, 1)) && LocalTime.now().isAfter(LocalTime.of(0, 1)) && LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    updateUsersLessons();
                }
                for (User user :
                        users) {
                    if (!checkUserCookieStatus(user)) continue;
                    executor.execute(() -> {
                        for (Lesson lesson :
                                user.getLessons()) {
                            if ((lesson.getStartDate().isBefore(LocalDateTime.now()) && lesson.getEndDate().isAfter(LocalDateTime.now())) || lesson.getStartDate().isEqual(LocalDateTime.now())) {
                                if (!lesson.isSelfReported()) {
                                    etuApiService.check(user, lesson);
                                }
                            }
                        }
                    });
                    userService.updateUserClosestLesson(user);
                }


                List<User> userList = userService.getAll().stream().parallel().filter(user -> user.getCookie() != null && user.getCookieLifetime().isAfter(LocalDateTime.now())).toList();
                long nextLessonDate = 0;
                long nextAuthExpireDate = 0;
                if (!userList.isEmpty()) {
                    nextLessonDate = userList.stream().parallel().filter(user -> user.getStartOfClosestLesson() != null && user.getStartOfClosestLesson().isAfter(LocalDateTime.now())).mapToLong(user -> user.getStartOfClosestLesson().toEpochSecond(ZoneOffset.UTC)).min().orElse(0);
                    nextAuthExpireDate = userList.stream().parallel().mapToLong(user -> user.getCookieLifetime().toEpochSecond(ZoneOffset.UTC)).min().orElse(0);
                }
                delay = nextLessonDate - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + 10;
                long delayAuth = nextAuthExpireDate - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + 10;
                if (LocalTime.now().isBefore(LocalTime.of(1, 0)) && LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    delay = LocalTime.of(1, 0).toEpochSecond(LocalDate.now(), ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                }
                if (delay <= 0) {
                    delay = 3600;
                }
                if (delayAuth < delay && delayAuth > 0) {
                    delay = delayAuth;
                }
                log.info(LocalDateTime.now().plusSeconds(delay) + " " + delay);
                scheduler.schedule(this, delay, TimeUnit.SECONDS);
                return null;
            }
        };

        scheduledFuture = scheduler.schedule(task, 1, TimeUnit.SECONDS);
    }
}
