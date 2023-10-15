package com.rect.etuattender.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Cascade;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity(name = "users")
@Getter
@Setter
public class User {

    @Id
    @Column
    @NotNull
    private Long id;

    @Column
    private String nick;


    @Column
    private String role;

    @Column
    @Enumerated(EnumType.STRING)
    private UserState state;

    @Column
    private String cookie;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime cookieLifetime;

    @Column
    private boolean autoCheck=false;

    @Column
    private String closestLesson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime startOfClosestLesson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime endOfClosestLesson;

    @Column
    private int page=0;

    @Column
    private String lastSearch = " ";

    @Column
    private String login;

    @Column
    private String password;

    @Column
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

}
