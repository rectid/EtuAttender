package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public Optional<User> getUser(long id){
        Optional<User> user = userRepository.findById(id);
        return user;
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public Optional<User> getUserByNick(String nick){
        Optional<User> user = userRepository.findUserByNick(nick);
        return user;
    }

    public List<User> getAll(){
        List<User> users =  userRepository.findAll();
        return users;
    }

    public void updateUserClosestLesson(User user, List<Lesson> lessons){
        user = getUser(user.getId()).get();
        for (Lesson lesson :
                lessons) {
            if (lesson.getStartDate().isAfter(LocalDateTime.now())){
                user.setClosestLesson(lesson.getLessonId());
                user.setStartOfClosestLesson(lesson.getStartDate());
                user.setEndOfClosestLesson(lesson.getEndDate());
                break;
            }
        }

        userRepository.save(user);
    }


    }
