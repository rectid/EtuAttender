package com.rect.etuattender.controller;

import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.handler.MainMenuHandler;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.states.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Bot extends TelegramLongPollingBot {

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenuHandler mainMenuHandler;

    public Bot(BotConfig botConfig, UserService userService, MainMenuHandler mainMenuHandler) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.mainMenuHandler = mainMenuHandler;
    }

    @Override
    public void onUpdateReceived(Update update) {
        executor.submit(() -> {
            if ((update.hasMessage() && update.getMessage().hasText()) ||
                    (update.hasCallbackQuery() &&
                            (update.getCallbackQuery().getData()!=null && update.getCallbackQuery().getMessage()!=null))) {

                User user = userService.getUser(update.getMessage().getChatId())
                        .orElseGet(() -> mainMenuHandler.signUp(update));

                switch (user.getState()) {
                            case IN_MAIN_MENU -> mainMenuHandler.handle(update, user);
                        }
            }

        });
    }


    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    public long getBotOwner() {
        return botConfig.getOwner();
    }
}
