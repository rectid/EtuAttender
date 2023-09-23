package com.rect.etuattender.dto.lesson;

import lombok.Data;

import java.util.Date;

@Data
public class LessonDto {
    private int id;
    private Date start;
    private Date end;
    private boolean isDistant;
    private String room;
    private LessonInfoDto lesson;
    private boolean selfReported;
    private Object groupLeaderReported;
    private boolean teacherReported;
    private boolean isGroupLeader;
    private Date checkInStart;
    private Date checkInDeadline;
}
