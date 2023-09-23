package com.rect.etuattender.service;

import com.rect.etuattender.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.*;

@Component
@Slf4j
public class CheckPoolService {

    private final UserService userService;
    private final EtuApiService etuApiService;

    public CheckPoolService(UserService userService, EtuApiService etuApiService) {
        this.userService = userService;
        this.etuApiService = etuApiService;
    }
//    public int getTime() {
//        ArrayList<User> users = userService.getAll();
//        Date minDate = users.stream().map(User::getClosestLessonDate).min(Date::compareTo).get();
//        for (User user :
//                users) {
//            Date closestLessonDateForAll = use
//        }
//        List<Lesson> lessons = etuApiService.getLessons(user);
//        this.lessons = lessons;
//        for (Lesson lesson :
//                lessons) {
//            if (lesson.start.after(new Date(1695369781429L))) {
//                user.setClosestLesson(String.valueOf(lesson.id));
//                user.setClosestLessonDate(lesson.start);
//                userService.saveUser(user);
//                break;
//            }
//        }
//    }

    public ArrayList<User> getUsers(){
        ArrayList<User> allUsers = userService.getAll();
        ArrayList<User> requiredUsers = new ArrayList<>();
        allUsers.stream().parallel().forEach(user -> {
            if (user.getStartOfClosestLesson().equals(new Date(System.currentTimeMillis()))) {
                requiredUsers.add(user);
            }
        });
        return requiredUsers;
    }

//    @EventListener({ContextRefreshedEvent.class})
    public void initChecking(){
        executeScheduledTask(getUsers());
    }


    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduleManager;
    private Runnable scheduledTask;

//    public void changeUsersI TODO

    public void executeScheduledTask(ArrayList<User> users) {
        executor = Executors.newSingleThreadScheduledExecutor();

        for (User user:
             users) {
            scheduledTask = () -> {
                etuApiService.check(user, user.getClosestLesson());

                //TODO добавить изменение ближайшей пары у юзера

                if (user==users.get(users.size()-1))
                changeScheduleTime(0);
            };
        }

        scheduleManager = executor.scheduleAtFixedRate(scheduledTask, 0, 0, TimeUnit.SECONDS);
    }

    public void changeScheduleTime(int timeSeconds) {
        if (scheduleManager != null) {
            scheduleManager.cancel(true);
        }
        scheduleManager = executor.scheduleAtFixedRate(scheduledTask, 0, timeSeconds, TimeUnit.SECONDS);
    }
}
