package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.states.LessonMenu;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@Slf4j
public class CheckService {

    private final UserService userService;
    private final EtuApiService etuApiService;

    public CheckService(UserService userService, EtuApiService etuApiService) {
        this.userService = userService;
        this.etuApiService = etuApiService;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initChecking() {
        executeScheduledTask();
    }


    public void executeScheduledTask() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService checkExecutor = Executors.newSingleThreadScheduledExecutor();
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


                    if (user.getCookie()==null || user.getCookieLifetime().isBefore(LocalDateTime.now())){
                        user.setState(UserState.IN_MAIN_MENU);
                        userService.saveUser(user);
                        continue;
                    }
                    checkExecutor.execute(() -> {
                    for (Lesson lesson:
                         user.getLessons()) {
                        if ((lesson.getStartDate().isBefore(LocalDateTime.now()) && lesson.getEndDate().isAfter(LocalDateTime.now())) || lesson.getStartDate().isEqual(LocalDateTime.now())){
                            if (!lesson.isSelfReported()) {
                                if (etuApiService.check(user, lesson.getLessonId())) {
                                    lesson.setSelfReported(true);
                                }
                            }
                        }
                    }});
                    userService.updateUserClosestLesson(user,etuApiService.getLessons(user));

                }



                List<User> userList = userService.getAll().stream().filter(user -> user.getCookie()!=null && user.getCookieLifetime().isAfter(LocalDateTime.now()) && user.getStartOfClosestLesson()!=null).toList();
                long nextLessonDate = userList.stream().parallel().mapToLong(user -> user.getStartOfClosestLesson().toEpochSecond(ZoneOffset.UTC)).min().getAsLong();
                delay = nextLessonDate-LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)+10;
                if (delay<=0){
                    delay=3600;
                }
                log.info(LocalDateTime.ofEpochSecond(nextLessonDate,0,ZoneOffset.UTC) + " " + delay);
                scheduler.schedule(this, delay, TimeUnit.SECONDS);
                return null;
            }
        };
        scheduler.schedule(task, 1, TimeUnit.SECONDS);
    }
}
