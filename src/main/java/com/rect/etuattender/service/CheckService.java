package com.rect.etuattender.service;

import com.rect.etuattender.controller.EtuAttenderBot;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.*;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
@Deprecated
public class CheckService {

    private final UserService userService;
    private final EtuApiService etuApiService;
    private final EtuAttenderBot etuAttenderBot;
    private final Object monitor = new Object();


    @Autowired
    public CheckService(UserService userService, EtuApiService etuApiService, EtuAttenderBot etuAttenderBot) {
        this.userService = userService;
        this.etuApiService = etuApiService;
        this.etuAttenderBot = etuAttenderBot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initChecking() {
        executeScheduledTask();
    }


    public void executeScheduledTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ThreadPoolExecutor checkExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        List<User> users = new ArrayList<>();
        Callable<Void> task = new Callable<>() {
            @SneakyThrows
            public Void call() {
                log.info(LocalDateTime.now().toString());
                long delay = 0;
                Callable<List<User>> c = userService::getAll;
                users.clear();
                users.addAll(c.call());
                for (User user :
                        users) {

                    if (user.getCookieLifetime()!=null){
                    if (user.getCookieLifetime().isBefore(LocalDateTime.now())) {
                        if (user.getCookie().equals("expired")) {
                            continue;
                        }

                        if (user.getLogin() != null) {
                            user.setState(UserState.ENTERING_WITH_SAVE);
                            userService.saveUser(user);
                            Update update = new Update();
                            Message message = new Message();
                            message.setText(user.getLogin() + ":" + user.getPassword());
                            update.setMessage(message);
                            Chat chat = new Chat();
                            chat.setId(user.getId());
                            update.getMessage().setChat(chat);
                            update.setCallbackQuery(new CallbackQuery() {{
                                setData("REAUTH");
                            }});
                            Runnable job = etuAttenderBot.createJob();
                            Future<?> future=  etuAttenderBot.onUpdateReceived(update, job);
                            while (!future.isDone()){
                                Thread.sleep(100);
                            }
                        } else {

                            user.setState(UserState.IN_MAIN_MENU);
                            user.setCookie("expired");
                            userService.saveUser(user);
                            Update update = new Update();
                            Message message = new Message();
                            message.setText("Расписание");
                            update.setMessage(message);
                            Chat chat = new Chat();
                            chat.setId(user.getId());
                            update.getMessage().setChat(chat);
                            Runnable job = etuAttenderBot.createJob();
                            Future<?> future=  etuAttenderBot.onUpdateReceived(update, job);
                            while (!future.isDone()){
                                Thread.sleep(100);
                            }
                            continue;
                        }
                    User tempUser = userService.getUser(user.getId()).get();
                    if (tempUser.getCookieLifetime().equals(user.getCookieLifetime())){
                        user.setState(UserState.IN_MAIN_MENU);
                            user.setCookie("expired");
                            userService.saveUser(user);
                            Update update = new Update();
                            Message message = new Message();
                            message.setText("Расписание");
                            update.setMessage(message);
                            Chat chat = new Chat();
                            chat.setId(user.getId());
                            update.getMessage().setChat(chat);
                            Runnable job = etuAttenderBot.createJob();
                            Future<?> future=  etuAttenderBot.onUpdateReceived(update, job);
                            while (!future.isDone()){
                                Thread.sleep(100);
                            }
                            continue;
                    }
                    }
                    } else continue;
                    user = userService.getUser(user.getId()).get();
                    if (LocalTime.now().isBefore(LocalTime.of(2,50))&&LocalTime.now().isAfter(LocalTime.of(1,1))&& LocalDate.now().getDayOfWeek()==DayOfWeek.TUESDAY){
                        if (user.isAutoCheck()){
                            user.setLessons(etuApiService.getLessons(user));
                        } else {
                            user.setLessons(new ArrayList<>());
                        }
                            userService.saveUser(user);
                    }

                    User finalUser = user;
                    checkExecutor.execute(() -> {
                        for (Lesson lesson :
                                finalUser.getLessons()) {
                            if ((lesson.getStartDate().isBefore(LocalDateTime.now()) && lesson.getEndDate().isAfter(LocalDateTime.now())) || lesson.getStartDate().isEqual(LocalDateTime.now())) {
                                if (!lesson.isSelfReported()) {
                                        boolean succes = etuApiService.check(finalUser, lesson.getLessonId());
                                        if (succes) {
                                            lesson.setSelfReported(true);
                                        }
                                }
                            }
                        }
                    });
                    userService.updateUserClosestLesson(user, etuApiService.getLessons(user));

                }


                List<User> userList = userService.getAll().stream().parallel().filter(user -> user.getCookie() != null && user.getCookieLifetime().isAfter(LocalDateTime.now())).toList();
                long nextLessonDate = 0;
                long nextAuthExpireDate = 0;
                if (!userList.isEmpty()) {
                    nextLessonDate = userList.stream().parallel().filter(user -> user.getStartOfClosestLesson() != null).mapToLong(user -> user.getStartOfClosestLesson().toEpochSecond(ZoneOffset.UTC)).min().orElse(0);
                    nextAuthExpireDate = userList.stream().parallel().mapToLong(user -> user.getCookieLifetime().toEpochSecond(ZoneOffset.UTC)).min().orElse(0);
                }
                delay = nextLessonDate - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + 10;
                long delayAuth = nextAuthExpireDate - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) + 10;
                if (LocalTime.now().isBefore(LocalTime.of(3,0))&& LocalDate.now().getDayOfWeek()==DayOfWeek.MONDAY){
                    delay = LocalTime.of(2,2).toEpochSecond(LocalDate.now(),ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                }
                if (delay <= 0) {
                    delay = 3600;
                }
                if (delayAuth < delay && delayAuth > 0) {
                    delay = delayAuth;
                }
                log.info(LocalDateTime.ofEpochSecond(nextLessonDate, 0, ZoneOffset.UTC) + " " + delay);
                scheduler.schedule(this, delay, TimeUnit.SECONDS);
                return null;
            }
        };

        scheduler.schedule(task, 1, TimeUnit.SECONDS);
    }
}
