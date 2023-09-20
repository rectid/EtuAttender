package com.rect.etuattender.model.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

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
    private LocalDateTime localDateTime;
}
