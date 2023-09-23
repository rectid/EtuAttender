package com.rect.etuattender.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Entity(name = "autocheck_lessons")
@Data
public class Lesson {

    public Lesson(String id, Date startDate, Date endDate, String room, boolean selfReported, String shortTitle, User user){
        this.id = id;
        this.startDate = startDate;
        this.endDate = endDate;
        this.room = room;
        this.selfReported = selfReported;
        this.shortTitle = shortTitle;
        this.user = user;
    }

    public Lesson() {
    }

    @Id
    @Column
    private String id;

    @Column
    private Date startDate;

    @Column
    private Date endDate;

    @Column
    private String room;

    @Column
    private boolean selfReported;

    @Column
    private String shortTitle;

    @JoinColumn
    @ManyToOne(fetch = FetchType.EAGER)
    private User user;
}
