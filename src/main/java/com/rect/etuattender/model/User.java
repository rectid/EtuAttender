package com.rect.etuattender.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
    private State state;

    @Column
    private String cookie;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime cookieLifetime;

    @Column
    private boolean autoCheck = false;

    @Column
    private String closestLesson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime startOfClosestLesson;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime endOfClosestLesson;

    @Column
    private int page = 0;

    @Column
    private String lastSearch = " ";

    @Column
    private String login;

    @Column
    private String password;

    @Column
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

    public enum State {
        IN_MAIN_MENU,
        ENTERING_LK,
        ENTERING_WITH_SAVE,
        ENTERING_WITHOUT_SAVE,
        IN_LESSONS_MENU,
        IN_ADMIN_PANEL
    }
}
