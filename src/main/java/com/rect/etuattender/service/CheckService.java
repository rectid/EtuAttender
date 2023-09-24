package com.rect.etuattender.service;

import com.rect.etuattender.model.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
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

    public ArrayList<User> getRequiredUsers() {
        Callable<List<User>> c = () -> userService.getAll();

        List<User> allUsers = null;
        try {
            allUsers = c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ArrayList<User> requiredUsers = new ArrayList<>();
        allUsers.stream().parallel().forEach(user -> {
            userService.updateUserClosestLesson(user, etuApiService.getLessons(user));
            if ((user.getStartOfClosestLesson().isBefore(LocalDateTime.now()) && user.getEndOfClosestLesson().isAfter(LocalDateTime.now())) || user.getStartOfClosestLesson().isEqual(LocalDateTime.now())) {
                if (user.getLessons().stream().anyMatch(lesson -> lesson.getLessonId().equals(user.getClosestLesson()))) {
                    requiredUsers.add(user);
                }
            }
        });
        return requiredUsers;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void initChecking() {
        executeScheduledTask();
    }


    public void executeScheduledTask() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ArrayList<User> users = new ArrayList<>();
        Callable<Void> task = new Callable<>() {
            @SneakyThrows
            public Void call() {
                long delay = 0;

                users.clear();
                users.addAll(getRequiredUsers());
                for (User user :
                        users) {
                    etuApiService.check(user, user.getClosestLesson());
                }

                System.out.println(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
                long nextLessonDate = userService.getAll().stream().parallel().mapToLong(user -> user.getStartOfClosestLesson().toEpochSecond(ZoneOffset.UTC)).min().getAsLong();
                delay = nextLessonDate-LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)+10;
                log.info(nextLessonDate + " " + delay);
                if (delay<=0){
                    long temp = LocalDateTime.now().getHour()*3600+LocalDateTime.now().getMinute()*60;
                    long mondayTime = LocalDateTime.now().with(DayOfWeek.MONDAY).toEpochSecond(ZoneOffset.UTC)-temp;
                    delay=LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)-mondayTime+10;
                }
                executor.schedule(this, delay, TimeUnit.SECONDS);
                return null;
            }
        };
        executor.schedule(task, 1, TimeUnit.SECONDS);
    }
}
