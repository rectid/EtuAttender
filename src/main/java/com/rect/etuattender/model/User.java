package com.rect.etuattender.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.modelmapper.ModelMapper;

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

    @Column
    private String closestLesson;

    @Column
    private Date startOfClosestLesson;

    @Column
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Lesson> lessons;

}
