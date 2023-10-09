package com.rect.etuattender.repository;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
}
