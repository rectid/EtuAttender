package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class InlineKeyboardMarkupService {

    public InlineKeyboardMarkup getLessonButtons(List<Lesson> lessons, User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        for (Lesson lesson :
                lessons) {
            if (lesson.getStartDate().getDayOfYear() != LocalDate.now().getDayOfYear()) {
                continue;
            }
            var button = new InlineKeyboardButton();

            LocalDateTime date = lesson.getStartDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String newDateString = date.format(formatter);

            Optional<Lesson> userLesson = user.getLessons().stream().filter(lesson1 -> lesson1.getLessonId().equals(lesson.getLessonId())).findFirst();
            if (userLesson.isPresent()) {
                button.setText(newDateString + " - " + lesson.getShortTitle() + " - " + lesson.getRoom() + " ✔");
            } else {
                button.setText(newDateString + " - " + lesson.getShortTitle() + " - " + lesson.getRoom() + " ❌");
            }

            button.setCallbackData(lesson.getLessonId());
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            rowInLine = new ArrayList<>();
        }
        var button = new InlineKeyboardButton();
        if (user.isAutoCheck()) {
            button.setText("Авто-посещение ✔");
            ;
        } else {
            button.setText("Авто-посещение ❌");
        }
        button.setCallbackData("AUTO_CHECK");
        rowInLine.add(button);
        rowsInLine.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup editLessonButtons(String data, InlineKeyboardMarkup replyMarkup) {
        List<List<InlineKeyboardButton>> rowsInLine = replyMarkup.getKeyboard();
        for (List<InlineKeyboardButton> rowInLine :
                rowsInLine) {
            for (InlineKeyboardButton button :
                    rowInLine) {

                if (data.equals("AUTO_CHECK")) {
                    if (rowInLine.get(rowInLine.size() - 1).getText().contains("✔")) {
                        button.setText(button.getText().replace("✔", "❌"));
                    } else button.setText(button.getText().replace("❌", "✔"));
                    if (button.getCallbackData().equals("AUTO_CHECK")) {
                        break;
                    }
                }

                if (button.getCallbackData().equals(data)) {
                    if (button.getText().contains("\u2714")) {
                        button.setText(button.getText().replace("✔", "❌"));
                    } else button.setText(button.getText().replace("❌", "✔"));


                }
            }
        }
        return replyMarkup;
    }
}
