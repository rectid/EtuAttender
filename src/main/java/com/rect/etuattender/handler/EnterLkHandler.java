package com.rect.etuattender.handler;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.EtuApiService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.util.BotUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;

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
                inEnterLk(update, user);
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
                .chatId(update.getMessage().getChatId())
                .messageId(update.getMessage().getMessageId())
                .text("Выбрана аутентификация без сохранения данных")
                .build();
        if (isSave) message.setText("Выбрана аутентификация с сохранением данных");
        bot.execute(message);
        if (isSave) bot.routeHandling(update, user, ENTERING_WITH_SAVE);
        else bot.routeHandling(update, user, ENTERING_WITHOUT_SAVE);
    }

    @SneakyThrows
    public void error(Update update) {
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неизвестная команда. Выберите вариант аутентификации");
        bot.execute(message);
    }

    @SneakyThrows
    private void auth(Update update, User user, boolean save) {
        String[] lk = update.getMessage().getText().split(":");
        if (lk.length != 2) {
            SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Неправильный формат!");
            bot.execute(message);
            return;
        }

        user.setLogin(lk[0]);
        user.setPassword(lk[1]);
        userService.saveUser(user);
        switch (etuApiService.auth(user, lk)) {
            case "ok":
                String text = "Добро пожаловать!";
                SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
                message.setReplyMarkup(replyKeyboardMarkupService.get(update, user));
                bot.execute(message);
                log.info(user.getId() + "|" + user.getNick() + " registered in lk!");
                bot.routeHandling(update, user, IN_LESSONS_MENU);
            case "lk_error":
                text = "Неверные данные от ЛК";
                message = new SendMessage(String.valueOf(update.getMessage().getChatId()), text);
                bot.execute(message);
            case "server_error":
                message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ошибка сервера, попробуйте еще раз");
                bot.execute(message);
        }
    }

    @SneakyThrows
    private void inEnterLk(Update update, User user) {
        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.getBackButton();
        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Аутентификация с сохранением данных. \nПлюсы: удобный способ использования бота, однако ради этого я сохраняю ваш логин:пароль в базе данных, что позволяет мне автоматически обновлять токен доступа к ИС Посещаемость от ЛЭТИ." +
                "\nМинусы: относительно небезопасно хранить свои логин:пароль в недоступном напрямую для вас месте." +
                "\n\nАутентификация без сохранения данных. \nПлюсы: более безопасный способ использования бота, ведь в этом случае я не сохраняю ваши логин:пароль, только токен, который действует 6 дней " +
                "и может быть использован исключительно в сервисе ИС Посещаемость от ЛЭТИ. \nМинусы: срок действия, бот попросит пройти аутентификацию заново через 6 дней; при обновлении ИС Посещаемость аутентификация в боте слетает.");
        message.setReplyMarkup(replyKeyboardMarkup);
        bot.execute(message);
        message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Выберите вариант аутентификации:");
        message.setReplyMarkup(inlineKeyboardMarkupService.getAuthButtons());
        bot.execute(message);
    }

}
