package com.rect.etuattender.service;

import com.rect.etuattender.model.lesson.Lesson;
import com.rect.etuattender.model.user.User;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class CheckPoolService {

    private final UserService userService;
    private final EtuApiService etuApiService;

    public CheckPoolService(UserService userService, EtuApiService etuApiService) {
        this.userService = userService;
        this.etuApiService = etuApiService;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void getTime(){
        ArrayList<User> users = userService.getAll();
        Date minDate = users.stream().map(User::getClosestLessonDate).min(Date::compareTo).get();
        for (User user:
             users) {
            Date closestLessonDateForAll = user
        }
        List<Lesson> lessons = etuApiService.getLessons(user);
        this.lessons = lessons;
        for (Lesson lesson:
             lessons) {
            if (lesson.start.after(new Date(1695369781429L))){
                user.setClosestLesson(String.valueOf(lesson.id));
                user.setClosestLessonDate(lesson.start);
                userService.saveUser(user);
                break;
            }
        }
    }


    public void timer(){
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.schedule(new Runnable() {
            @Override
            public void run() {

            }
        },10L, TimeUnit.SECONDS);
    }


}
