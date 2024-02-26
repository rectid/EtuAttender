package com.rect.etuattender.states;

import com.rect.etuattender.controller.EtuAttenderBot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;

@Component
public class EnterLk {
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final UserService userService;
    private final EtuApiService etuApiService;
    private EtuAttenderBot etuAttenderBot;


    @Autowired
    public EnterLk(ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, UserService userService, EtuApiService etuApiService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.userService = userService;
        this.etuApiService = etuApiService;
    }


    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
        this.etuAttenderBot = etuAttenderBot;
        String command = update.getMessage().getText();

        if (update.hasCallbackQuery()) {
            command = update.getCallbackQuery().getData();
            switch (command) {
                case "SAVE":
                    EditMessageText message = new EditMessageText();
                    message.setChatId(update.getMessage().getChatId());
                    message.setMessageId(update.getMessage().getMessageId());
                    message.setText("Выбрана аутентификация с сохранением данных");
                    etuAttenderBot.handle(message, update);
                return UserState.ENTERING_WITH_SAVE;
                case "NOT_SAVE":
                    message = new EditMessageText();
                    message.setChatId(update.getMessage().getChatId());
                    message.setMessageId(update.getMessage().getMessageId());
                    message.setText("Выбрана аутентификация без сохранением данных");
                    etuAttenderBot.handle(message, update);
                    return UserState.ENTERING_WITHOUT_SAVE;
            }
        }

        switch (command) {
            case "Ввести данные ЛК", "Изменить выбор","Изменить данные ЛК":
                return inEnterLk(update,user);
            case "Панель Админа":
                return UserState.IN_ADMIN_PANEL;
            case "Назад":
                if (user.getCookieLifetime()!=null){
                if (user.getCookieLifetime().isAfter(LocalDateTime.now())){
                    ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update,user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Меню расписания");
        message.setReplyMarkup(replyKeyboardMarkup);
        etuAttenderBot.handle(message, update);
                    return UserState.IN_LESSONS_MENU;
                } else return UserState.IN_MAIN_MENU;}else return UserState.IN_MAIN_MENU;
            case "/start":
                return UserState.IN_MAIN_MENU;
        }

        return error(update,user);
    }

    public BotApiMethod error(Update update, User user) {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неизвестная команда. Выберите вариант аутентификации");
        return message;
    }

    private Object auth(boolean save, Update update, User user) {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length != 2) {
            SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неправильный формат!");
            return message;
        }
        if (save) {
            user.setLogin(lk[0]);
            user.setPassword(lk[1]);
        }
        switch (etuApiService.auth(user, lk)) {
            case "ok":
                return UserState.IN_LESSONS_MENU;
            case "lk_error":
                SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неверные данные от ЛК");
                return message;
            case "server_error":
                SendMessage message1 = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ошибка сервера, попробуйте еще раз");
                return message1;
        }
        return null;
    }

    private BotApiMethod inEnterLk(Update update, User user) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.getBackButton();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Аутентификация с сохранением данных. \nПлюсы: удобный способ использования бота, однако ради этого я сохраняю ваш логин:пароль в базе данных, что позволяет мне автоматически обновлять токен доступа к ИС Посещаемость от ЛЭТИ." +
                "\nМинусы: относительно небезопасно хранить свои логин:пароль в недоступном напрямую для вас месте." +
                "\n\nАутентификация без сохранения данных. \nПлюсы: более безопасный способ использования бота, ведь в этом случае я не сохраняю ваши логин:пароль, только токен, который действует 6 дней " +
                "и может быть использован исключительно в сервисе ИС Посещаемость от ЛЭТИ. \nМинусы: срок действия, бот попросит пройти аутентификацию заново через 6 дней; при обновлении ИС Посещаемость аутентификация в боте слетает.");
        message.setReplyMarkup(replyKeyboardMarkup);
        etuAttenderBot.handle(message, update);
        message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Выберите вариант аутентификации:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getAuthButtons());
        return message;
    }

}
