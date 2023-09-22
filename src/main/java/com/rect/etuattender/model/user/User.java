package com.rect.etuattender.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

@Entity(name = "users")
@Data
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

    @Column
    private LocalDateTime cookieLifetime;

    @Column
    private boolean autoCheck=false;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column
    private List<String> autoCheckLessons;

    @Column
    private Date closestLesson;

}
