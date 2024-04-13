package com.rect.etuattender.handler;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.util.BotUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;

import static com.rect.etuattender.model.User.State.*;

@Component
@Slf4j
public class MainMenuHandler {


    private final Bot bot;
    private final UserService userService;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;

    @Lazy
    public MainMenuHandler(Bot bot, UserService userService, ReplyKeyboardMarkupService replyKeyboardMarkupService) {
        this.bot = bot;
        this.userService = userService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
    }


    public void handle(Update update, User user) {
        String command = BotUtils.getCommand(update).orElse("unknown");

        switch (command) {
            case "unknown": error(update);
            case "Ввести данные ЛК", "Изменить данные лк": userService.changeUserState(user, ENTERING_LK);
            case "Панель Админа": userService.changeUserState(user, IN_ADMIN_PANEL);
            case "Информация": getInfo(update);
            case "Расписание": {
                if (BotUtils.checkCookies(user)) userService.changeUserState(user, IN_LESSONS_MENU);
                else authExpired(update,user);
            }
            case "Назад", "/start": inMainMenu(update, user);
        }
    }
    public User signUp(Update update) {
        User user = new User();
        user.setId(update.getMessage().getChatId());
        if (user.getId() == bot.getBotOwner()) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        user.setState(IN_MAIN_MENU);
        if (update.getMessage().getFrom().getUserName() != null) {
            user.setNick(update.getMessage().getFrom().getUserName());
        } else {
            user.setNick(update.getMessage().getFrom().getId().toString());
        }

        userService.saveUser(user);
        log.info(String.format("User %d signed up!", user.getId()));
        return user;
    }

    @SneakyThrows
     public void inMainMenu(Update update, User user) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update, user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Вы в главном меню. Если кнопки не появились - введите /start");
        message.setReplyMarkup(replyKeyboardMarkup);
        bot.execute(message);
    }
    @SneakyThrows
    public void error(Update update){
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()),"Неизвестная команда. Введите /start");
        bot.execute(message);
    }

    @SneakyThrows
    public void authExpired(Update update, User user){
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update, user);
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваш токен регистрации в системе истек, необходимо ввести данные ЛК снова!");
        message.setReplyMarkup(replyKeyboardMarkup);
        bot.execute(message);
    }

    @SneakyThrows
    private void getInfo(Update update){
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()),
                "\nТех поддержка: @rected" +
                        "\n\nДанный бот предназначен для автоматический отметки на парах и просмотра расписания");
        bot.execute(message);
    }

}
