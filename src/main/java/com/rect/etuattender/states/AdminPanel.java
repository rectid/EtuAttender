package com.rect.etuattender.states;


import com.rect.etuattender.controller.EtuAttenderBot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;

@Component
public class AdminPanel {
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final UserService userService;
    private EtuAttenderBot etuAttenderBot;

    @Autowired
    public AdminPanel(ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, UserService userService) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.userService = userService;
    }

    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
        this.etuAttenderBot=etuAttenderBot;
        String command = update.getMessage().getText();
        switch (command) {
            case "Панель Админа":
                return inAdminPanel(update);
            case "Массовая рассылка":
                return UserState.SENDING_MASS_MESSAGE;
            case "Назад":
            case "/start":
                return UserState.IN_MAIN_MENU;
        }

        if (update.hasCallbackQuery()) {
            command = update.getCallbackQuery().getData();
            User admin = userService.getUser(update.getMessage().getChatId()).get();
            switch (command) {
                case "Next":
                    admin.setPage(admin.getPage() + 1);
                    return changePage(update, admin);
                case "Back":
                    admin.setPage(admin.getPage() - 1);
                    return changePage(update, admin);
            }
            if (command.contains("Ban")){
                user = userService.getUserByNick(admin.getLastSearch()).get();
                if (!user.getRole().equals("BANNED")){
                user.setRole("BANNED"); user.setState(UserState.IN_BAN);} else {user.setRole("USER"); user.setState(UserState.IN_MAIN_MENU);}
                return banMessage(update, user);
            }
            update.getMessage().setText(update.getCallbackQuery().getData());
        }
        return inUserInfo(update);
    }

    public BotApiMethod<Message> inAdminPanel(Update update) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.getAdminReplyButton();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "С возвращением!");
        message.setReplyMarkup(replyKeyboardMarkup);
        etuAttenderBot.handle(message, update);
        message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите ник необходимого пользователя");
        message.setReplyMarkup(inlineKeyboardMarkupService.getUsersButtons(update,userService.getAll(),userService.getUser(update.getMessage().getChatId()).get().getPage()));
        User user = userService.getUser(update.getMessage().getChatId()).get();
        user.setLastSearch("");
        userService.saveUser(user);
        return message;
    }

    private BotApiMethod changePage(Update update, User admin) {
        userService.saveUser(admin);
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setText(update.getMessage().getText());
        message.setReplyMarkup(inlineKeyboardMarkupService.getUsersButtons(update,userService.getAll(), admin.getPage()));
        return message;
    }

    public BotApiMethod banMessage(Update update, User user){
        userService.saveUser(user);
        EditMessageText message = new EditMessageText();
        message.setChatId(update.getMessage().getChatId());
        message.setMessageId(update.getMessage().getMessageId());
        message.setText("Выбран пользователь: " + user.getNick() + " | " + user.getId()
                + "\nРоль: " + user.getRole());
        message.setReplyMarkup(inlineKeyboardMarkupService.getAdminButtons(user));
        return message;
    }

    public BotApiMethod inUserInfo(Update update) {
        User user;
        if (userService.getUserByNick(update.getMessage().getText()).isPresent()) {
            user = userService.getUserByNick(update.getMessage().getText()).get();
        } else {
            SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Данного пользователя не существует");
            return message;
        }
        User admin = userService.getUser(update.getMessage().getChatId()).get();
        admin.setLastSearch(user.getNick());
        userService.saveUser(admin);
        String status = "не активен";
        if (user.getCookie()!=null && user.getCookieLifetime().isAfter(LocalDateTime.now())){
            status = "активен";
        }
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Выбран пользователь: " + user.getNick() + " | " + user.getId()
                + "\nСтатус: " + status);
        message.setReplyMarkup(inlineKeyboardMarkupService.getAdminButtons(user));
        return message;
    }
}
