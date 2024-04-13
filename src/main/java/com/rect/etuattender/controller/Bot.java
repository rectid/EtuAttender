package com.rect.etuattender.controller;

import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.handler.EnterLkHandler;
import com.rect.etuattender.handler.MainMenuHandler;
import com.rect.etuattender.model.User;
import com.rect.etuattender.service.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Bot extends TelegramLongPollingBot {

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenuHandler mainMenuHandler;
    private final EnterLkHandler enterLkHandler;

    public Bot(BotConfig botConfig, UserService userService, MainMenuHandler mainMenuHandler, EnterLkHandler enterLkHandler) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.mainMenuHandler = mainMenuHandler;
        this.enterLkHandler = enterLkHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.submit(() -> {
            if (update.hasMessage()) {
                User user = userService.getUser(update.getMessage().getChatId())
                        .orElseGet(() -> mainMenuHandler.signUp(update));

                routeHandling(update, user, user.getState());
            }
        });
    }

    public void routeHandling(Update update, User user, User.State state) {
        if (user.getState() != state) user = userService.changeUserState(user, state);
        switch (state) {
            case IN_MAIN_MENU -> mainMenuHandler.handle(update, user);
            case ENTERING_LK -> enterLkHandler.handle(update, user);
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
