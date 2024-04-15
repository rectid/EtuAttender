package com.rect.etuattender.controller;

import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.handler.EnterLkHandler;
import com.rect.etuattender.handler.LessonMenuHandler;
import com.rect.etuattender.handler.MainMenuHandler;
import com.rect.etuattender.model.User;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.util.BotUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Bot extends TelegramLongPollingBot {

    public static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenuHandler mainMenuHandler;
    private final EnterLkHandler enterLkHandler;
    private final LessonMenuHandler lessonMenuHandler;

    public Bot(BotConfig botConfig, UserService userService, MainMenuHandler mainMenuHandler, EnterLkHandler enterLkHandler, LessonMenuHandler lessonMenuHandler) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.mainMenuHandler = mainMenuHandler;
        this.enterLkHandler = enterLkHandler;
        this.lessonMenuHandler = lessonMenuHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.submit(() -> {
            if (update.hasMessage() || update.hasCallbackQuery()) {
                User user = userService.getUser(BotUtils.getUserId(update))
                        .orElseGet(() -> mainMenuHandler.signUp(update));
                routeHandling(update, user, user.getState());
            }
        });
    }

    public void routeHandling(Update update, User user, User.State state) {
        if (user.getState() != state) user = userService.changeUserState(user, state);
        switch (state) {
            case IN_MAIN_MENU -> mainMenuHandler.handle(update, user);
            case ENTERING_LK, ENTERING_WITH_SAVE, ENTERING_WITHOUT_SAVE -> enterLkHandler.handle(update, user);
            case IN_LESSONS_MENU -> lessonMenuHandler.handle(update, user);
        }
    }


    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    public long getBotOwner() {
        return botConfig.getOwner();
    }
}
