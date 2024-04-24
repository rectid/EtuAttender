package com.rect.etuattender.handler;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.util.BotStrings;
import com.rect.etuattender.util.BotUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.rect.etuattender.model.User.State.*;

@Component
@Slf4j
public class EnterLkHandler {

    private final Bot bot;
    private final UserService userService;
    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final EtuApiService etuApiService;

    @Lazy
    public EnterLkHandler(Bot bot, UserService userService, ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, EtuApiService etuApiService) {
        this.bot = bot;
        this.userService = userService;
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.etuApiService = etuApiService;
    }

    public void handle(Update update, User user) {
        String command = BotUtils.getCommand(update).orElse("UNKNOWN");

        switch (command) {
            case "SAVE":
                enterOptionChosen(update, user, true);
                break;
            case "NOT_SAVE":
                enterOptionChosen(update, user, false);
                break;
            case "Ввести данные ЛК", "Изменить выбор", "Изменить данные ЛК":
                inEnterLk(update);
                break;
            case "Назад": {
                if (BotUtils.checkCookies(user)) bot.routeHandling(update, user, IN_LESSONS_MENU);
                else bot.routeHandling(update, user, IN_MAIN_MENU);
                break;
            }
            case "/start":
                bot.routeHandling(update, user, IN_MAIN_MENU);
                break;
            case "UNKNOWN":
                error(update);
                break;
            default:
                auth(update, user, user.getState() == User.State.ENTERING_WITH_SAVE);
        }
    }


    @SneakyThrows
    private void enterOptionChosen(Update update, User user, boolean isSave) {
        EditMessageText message = EditMessageText.builder()
                .chatId(BotUtils.getUserId(update))
                .messageId(BotUtils.getMessageId(update))
                .text("Выбрана аутентификация без сохранения данных.\n\nВведите логин и пароль в формате логин:пароль")
                .build();
        if (isSave)
            message.setText("Выбрана аутентификация с сохранением данных.\n\nВведите логин и пароль в формате логин:пароль");
        bot.execute(message);
        if (isSave) bot.routeHandling(update, user, ENTERING_WITH_SAVE);
        else bot.routeHandling(update, user, ENTERING_WITHOUT_SAVE);
    }

    @SneakyThrows
    public void error(Update update) {
        bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text("Неизвестная команда. Выберите вариант аутентификации").build());
    }

    @SneakyThrows
    private void auth(Update update, User user, boolean isSave) {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length != 2) {
            bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text("Неправильный формат!").build());
            return;
        }
        if (isSave) {
            user.setLogin(lk[0]);
            user.setPassword(lk[1]);
            userService.saveUser(user);
        }
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .text("Регистрирую вас...")
                .build());
        switch (etuApiService.auth(user, lk)) {
            case "ok":
                bot.execute(SendMessage.builder()
                        .chatId(BotUtils.getUserId(update))
                        .text("Успешно. Добро пожаловать!")
                        .replyMarkup(replyKeyboardMarkupService.getLessonMenuButtons())
                        .build());
                bot.execute(SendMessage.builder()
                        .chatId(BotUtils.getUserId(update))
                        .text("Ваше расписание на сегодня, выберите, на чем вас отметить:")
                        .replyMarkup(inlineKeyboardMarkupService.getLessonsButtons(etuApiService.getLessons(user), user))
                        .build());
                log.info(user.getId() + "|" + user.getNick() + " registered in lk!");
                break;
            case "lk_error":
                bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text("Неверные данные от ЛК").build());
                break;
            case "server_error":
                bot.execute(SendMessage.builder().chatId(BotUtils.getUserId(update)).text("Ошибка сервера, попробуйте еще раз").build());
                break;
        }
    }

    @SneakyThrows
    private void inEnterLk(Update update) {
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(replyKeyboardMarkupService.getBackButton())
                .text(BotStrings.getEnterLkInfo())
                .build());
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(inlineKeyboardMarkupService.getAuthButtons())
                .text("Выберите вариант аутентификации:")
                .build());
    }

}
