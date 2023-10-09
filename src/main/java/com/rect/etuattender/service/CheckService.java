package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

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
                    checkExecutor.execute(() -> {
                    for (Lesson lesson:
                         user.getLessons()) {
                        if ((lesson.getStartDate().isBefore(LocalDateTime.now()) && lesson.getEndDate().isAfter(LocalDateTime.now())) || lesson.getStartDate().isEqual(LocalDateTime.now())){
                            if (!lesson.isSelfReported()) {
                                etuApiService.check(user, lesson.getLessonId());
                                lesson.setSelfReported(true);
                            }
                        }
                    }});
                    userService.updateUserClosestLesson(user,etuApiService.getLessons(user));

                }

                long nextLessonDate = userService.getAll().stream().parallel().mapToLong(user -> user.getStartOfClosestLesson().toEpochSecond(ZoneOffset.UTC)).min().getAsLong();
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
