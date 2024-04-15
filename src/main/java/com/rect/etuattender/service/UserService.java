package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import com.rect.etuattender.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public Optional<User> getUser(long id){
        return userRepository.findById(id);
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public List<User> getAll(){
        return userRepository.findAll();
    }

    public void updateUserClosestLesson(User user){
        user = getUser(user.getId()).orElseThrow();
        for (Lesson lesson :
                user.getLessons()) {
            if (lesson.getStartDate().isAfter(LocalDateTime.now())){
                user.setClosestLesson(lesson.getLessonId());
                user.setStartOfClosestLesson(lesson.getStartDate());
                user.setEndOfClosestLesson(lesson.getEndDate());
                break;
            }
        }

        userRepository.save(user);
    }

    public User changeUserState(User user, User.State state){
        user.setState(state);
        userRepository.save(user);
        return user;
    }


    }
