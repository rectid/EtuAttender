package com.rect.etuattender.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReplyKeyboardMarkupService {

    public ReplyKeyboardMarkup get(Update update) {
        String command = update.getMessage().getText();
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow;
        if ("Ввести данные ЛК".equals(command) || "Панель Админа".equals(command)) {
            keyboardRow = new KeyboardRow();
            keyboardRow.add("Назад");
            keyboardRows.add(keyboardRow);
            replyKeyboardMarkup.setKeyboard(keyboardRows);
            return replyKeyboardMarkup;
        }
        keyboardRow = new KeyboardRow();
        keyboardRow.add("Ввести данные ЛК");
        keyboardRows.add(keyboardRow);

        if (update.getMessage().getChatId()==595667050){
            keyboardRow = new KeyboardRow();
            keyboardRow.add("Панель Админа");
            keyboardRows.add(keyboardRow);
        }
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        return replyKeyboardMarkup;
    }
}
