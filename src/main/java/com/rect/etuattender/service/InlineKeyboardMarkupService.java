package com.rect.etuattender.service;

import com.rect.etuattender.model.lesson.Lesson;
import com.rect.etuattender.model.user.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class InlineKeyboardMarkupService {

    public InlineKeyboardMarkup getLessonButtons(List<Lesson> lessons, User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        for (Lesson lesson:
             lessons) {
            var button = new InlineKeyboardButton();

            final String OLD_FORMAT = "YYYY-MM-DD hh:mm:ss";
            final String NEW_FORMAT = "hh:mm";
            SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
            Date date = null;
            try {
                date = sdf.parse(user.getTimestamp().toString());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            sdf.applyPattern(NEW_FORMAT);
            String newDateString = sdf.format(date);

            button.setText(newDateString + " - " + lesson.lesson.shortTitle + " - " + lesson.room);
            button.setCallbackData(String.valueOf(lesson.id));
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            rowInLine = new ArrayList<>();
        }
            var button = new InlineKeyboardButton();
            button.setText("Включить авто-посещение");
            button.setCallbackData("AUTO_CHANGE");
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            return inlineKeyboardMarkup;
        }
    }
