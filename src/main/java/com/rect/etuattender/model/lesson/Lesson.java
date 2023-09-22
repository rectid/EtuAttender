package com.rect.etuattender.model.lesson;

import java.sql.Date;
import java.time.LocalDateTime;

public class Lesson {
    public int id;
    public Date start;
    public Date end;
    public boolean isDistant;
    public String room;
    public LessonInfo lesson;
    public boolean selfReported;
    public Object groupLeaderReported;
    public boolean teacherReported;
    public boolean isGroupLeader;
    public Date checkInStart;
    public Date checkInDeadline;
}
