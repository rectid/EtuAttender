package com.rect.etuattender.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.boot.autoconfigure.graphql.ConditionalOnGraphQlSchema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity(name = "lessons")
@Getter
@Setter
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private String lessonId;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime startDate;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime endDate;

    @Column
    private String room;

    @Column
    private boolean selfReported = false;



    @Column
    private String shortTitle;

    @Column
    private String teacher;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
