package com.rect.etuattender.handler;

import com.rect.etuattender.controller.Bot;
import com.rect.etuattender.model.User;
import com.rect.etuattender.service.CheckService;
import com.rect.etuattender.service.InlineKeyboardMarkupService;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.util.BotUtils;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;

import static com.rect.etuattender.model.User.State.IN_LESSONS_MENU;
import static com.rect.etuattender.model.User.State.IN_MAIN_MENU;

@Component
public class AdminPanelHandler {

    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
    private final InlineKeyboardMarkupService inlineKeyboardMarkupService;
    private final CheckService checkService;

    private final UserService userService;

    private final Bot bot;

    @Lazy
    public AdminPanelHandler(ReplyKeyboardMarkupService replyKeyboardMarkupService, InlineKeyboardMarkupService inlineKeyboardMarkupService, UserService userService, CheckService checkService, UserService userService1, Bot bot) {
        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
        this.inlineKeyboardMarkupService = inlineKeyboardMarkupService;
        this.checkService = checkService;
        this.userService = userService1;
        this.bot = bot;
    }

    @SneakyThrows
    public void handle(Update update, User user){
        String command = BotUtils.getCommand(update).orElse("UNKNOWN");
        switch (command) {
            case "CLOSE": BotUtils.deleteMessage(update, bot); break;
            case "Начать процесс отметки": startChecking(update); break;
            case "Панель Админа": inAdminPanel(update); break;
            case "Массовая рассылка": getMassMessageTarget(update); break;
            case "Обновить предметы в бд": updateLessons(update); break;
            case "Массовая рассылка для неактива": getInactiveMassMessageTarget(update); break;
            case "Назад": {
                if (BotUtils.checkCookies(user)) bot.routeHandling(update, user, IN_LESSONS_MENU);
                else bot.routeHandling(update, user, IN_MAIN_MENU);
                break;
            }
            case "/start": bot.routeHandling(update, user, IN_MAIN_MENU);
            default:
            {
                if (update.getMessage().getReplyToMessage()!=null){
                    if (update.getMessage().getReplyToMessage().getText().contains("all")){
                    sendToEveryone(update);}
                    else {
                        sendToInactive(update);
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void updateLessons(Update update){
        checkService.updateUsersLessons();
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .text("Панель администратора. Выберите функцию")
                .build());
    }


    @SneakyThrows
    public void inAdminPanel(Update update) {
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(replyKeyboardMarkupService.getAdminReplyButton())
                .text("Панель администратора. Выберите функцию")
                .build());
    }

    @SneakyThrows
    private void sendToEveryone(Update update){
        List<User> users = userService.getAll();
        for (User user : users) {
            bot.execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(update.getMessage().getText())
                    .build());
        }
    }

    @SneakyThrows
    private void sendToInactive(Update update){
        List<User> users = userService.getAll().stream().filter(user -> user.getStartOfClosestLesson()==null || user.getStartOfClosestLesson().isBefore(LocalDateTime.of(2024,4,1,0,0))).toList();
        for (User user : users) {
            bot.execute(SendMessage.builder()
                    .chatId(user.getId())
                    .text(update.getMessage().getText())
                    .build());
        }
    }

    @SneakyThrows
    private void getInactiveMassMessageTarget(Update update){
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(inlineKeyboardMarkupService.getCloseMessageButton())
                .text("Отправьте ответом сообщение для to inactive.")
                .build());
    }

    @SneakyThrows
    private void getMassMessageTarget(Update update){
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .replyMarkup(inlineKeyboardMarkupService.getCloseMessageButton())
                .text("Отправьте ответом сообщение для to all.")
                .build());
    }


    @SneakyThrows
    private void startChecking(Update update){
        checkService.initChecking();
        bot.execute(SendMessage.builder()
                .chatId(BotUtils.getUserId(update))
                .text("Проверка инициализирована!")
                .build());
    }
}
