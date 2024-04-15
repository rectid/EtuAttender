package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class InlineKeyboardMarkupService {

    public InlineKeyboardMarkup getAuthButtons() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setCallbackData("SAVE");
        button.setText("Сохранять логин и пароль");
        rowInLine.add(button);
        rowsInLine.add(rowInLine);
        rowInLine = new ArrayList<>();
        button = new InlineKeyboardButton();
        button.setCallbackData("NOT_SAVE");
        button.setText("Не сохранять логин и пароль");
        rowInLine.add(button);
        rowsInLine.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getLessonsButtons(List<Lesson> lessons, User user) {
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
                String room = "?";
                if (lesson.getRoom() != null) {
                    room = lesson.getRoom();
                }
                button.setText(newDateString + " - " + lesson.getShortTitle() + " - каб " + room + " ✔");
            } else {
                String room = "?";
                if (lesson.getRoom() != null) {
                    room = lesson.getRoom();
                }
                button.setText(newDateString + " - " + lesson.getShortTitle() + " - каб " + room + " ❌");
            }

            button.setCallbackData(lesson.getLessonId());
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            rowInLine = new ArrayList<>();
        }
        var button = new InlineKeyboardButton();
        if (user.isAutoCheck()) {
            button.setText("Все пары ✔");
        } else {
            button.setText("Все пары ❌");
        }
        button.setCallbackData("AUTO_CHECK");
        rowInLine.add(button);
        rowsInLine.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup editLessonButtons(Update update, User user) {
        List<List<InlineKeyboardButton>> rowsInLine = update.getCallbackQuery().getMessage().getReplyMarkup().getKeyboard();
        for (List<InlineKeyboardButton> rowInLine :
                rowsInLine) {
            for (InlineKeyboardButton button :
                    rowInLine) {

                if (update.getCallbackQuery().getData().equals("AUTO_CHECK")) {
                    if (user.isAutoCheck()) {
                        button.setText(button.getText().replace("❌", "✔"));
                    } else button.setText(button.getText().replace("✔", "❌"));
                    continue;
                }

                if (button.getCallbackData().equals(update.getCallbackQuery().getData())) {

                    InlineKeyboardButton autoButton = rowsInLine.getLast().getFirst();
                    autoButton.setText("Все пары ❌");
                    user.setAutoCheck(false);
                    user.setLessons(user.getLessons().stream().filter(lesson -> lesson.getStartDate().getDayOfYear() == LocalDateTime.now().getDayOfYear()).toList());

                    if (button.getText().contains("✔")) {
                        button.setText(button.getText().replace("✔", "❌"));
                    } else button.setText(button.getText().replace("❌", "✔"));
                }

            }
        }
        return new InlineKeyboardMarkup(rowsInLine);
    }

    public InlineKeyboardMarkup getUsersButtons(Update update, List<User> users, int page) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        int offset = page * 8;
        int buttonCounter = 0;
        if (users.size() <= 8) {
            for (int i = offset; i < users.size(); i++) {
                User user = users.get(i);
                var button = new InlineKeyboardButton();
                button.setText(user.getNick());
                button.setCallbackData(String.valueOf(user.getNick()));
                rowInLine.add(button);
                rowsInLine.add(rowInLine);
                rowInLine = new ArrayList<>();
            }
            if (page != 0) {
                var button = new InlineKeyboardButton();
                button.setCallbackData("Back");
                button.setText("Назад");
                rowInLine.add(button);
                rowsInLine.add(rowInLine);
            }

        } else {
            for (int i = offset; i < users.size(); i++) {
                User user = users.get(i);
                var button = new InlineKeyboardButton();
                button.setText(user.getNick());
                button.setCallbackData(String.valueOf(user.getNick()));
                rowInLine.add(button);
                rowsInLine.add(rowInLine);
                rowInLine = new ArrayList<>();
                buttonCounter++;
                if (buttonCounter == 8) {
                    break;
                }
            }
            if (page != 0) {
                var button = new InlineKeyboardButton();
                button.setCallbackData("Back");
                button.setText("Назад");
                rowInLine.add(button);
            }
            if (users.size() - (offset + 8) > 0) {
                var button = new InlineKeyboardButton();
                button.setText("Далее");
                button.setCallbackData("Next");
                rowInLine.add(button);
            }
            rowsInLine.add(rowInLine);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup getAdminButtons(User user) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var button = new InlineKeyboardButton();
        button.setCallbackData("Ban");
        if (!user.getRole().equals("BANNED")) {
            button.setText("Выдать бан");
        } else button.setText("Разбанить");
        rowInLine.add(button);
        rowsInLine.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        return inlineKeyboardMarkup;
    }
}
