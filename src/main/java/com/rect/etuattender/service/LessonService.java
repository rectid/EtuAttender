package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.repository.LessonRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LessonService {
    private final LessonRepository lessonRepository;

    public LessonService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public Optional<Lesson> getLesson(String lessonId){
        return lessonRepository.findById(Integer.parseInt(lessonId));
    }

    public void checkLesson(Lesson lesson) {
        lesson.setSelfReported(true);
        lessonRepository.save(lesson);
    }
}
