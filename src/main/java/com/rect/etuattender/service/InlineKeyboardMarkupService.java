package com.rect.etuattender.service;

import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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
        for (Lesson lesson :
                lessons) {
            if (lesson.getStartDate().getDay()!=new Date(System.currentTimeMillis()).getDay()){
                continue;
            }
            var button = new InlineKeyboardButton();

            final String OLD_FORMAT = "YYYY-MM-DD hh:mm:ss";
            final String NEW_FORMAT = "hh:mm";
            SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
            Date date = lesson.getStartDate();
            sdf.applyPattern(NEW_FORMAT);
            String newDateString = sdf.format(date);

            if (user.getLessons().contains(lesson)){
            button.setText(newDateString + " - " + lesson.getShortTitle() + " - " + lesson.getRoom()+ " \u2714");}
            else { button.setText(newDateString + " - " + lesson.getShortTitle() + " - " + lesson.getRoom()+ " \u274C");}
            button.setCallbackData(lesson.getId());
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            rowInLine = new ArrayList<>();
        }
            var button = new InlineKeyboardButton();
            if (user.isAutoCheck()){
            button.setText("Авто-посещение \u2714");;}
            else {button.setText("Авто-посещение \u274C");}
            button.setCallbackData("AUTO_CHECK");
            rowInLine.add(button);
            rowsInLine.add(rowInLine);
            inlineKeyboardMarkup.setKeyboard(rowsInLine);
            return inlineKeyboardMarkup;
        }

    public InlineKeyboardMarkup editLessonButtons(String data, InlineKeyboardMarkup replyMarkup) {
        List<List<InlineKeyboardButton>> rowsInLine = replyMarkup.getKeyboard();
        for (List<InlineKeyboardButton> rowInLine:
             rowsInLine) {
            for (InlineKeyboardButton button:
                 rowInLine) {

                if (data.equals("AUTO_CHECK")){
                    if (rowInLine.get(rowInLine.size()-1).getText().contains("\u2714")){
                         button.setText(button.getText().replace("\u2714","\u274C"));
                    } else button.setText(button.getText().replace("\u274C","\u2714"));
                    if (button.getCallbackData().equals("AUTO_CHECK")){
                        break;
                    }
                }

                if (button.getCallbackData().equals(data)){
                    if (button.getText().contains("\u2714")){
                        button.setText(button.getText().replace("\u2714","\u274C"));
                    } else  button.setText(button.getText().replace("\u274C","\u2714"));


                }
            }
        }
        return replyMarkup;
    }
}
