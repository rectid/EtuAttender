package com.rect.etuattender.controller;


import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.states.EnterLk;
import com.rect.etuattender.states.LessonMenu;
import com.rect.etuattender.states.MainMenu;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;

@Component
public class EtuAttenderBot extends TelegramLongPollingBot {

    private Update update;
    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenu mainMenu;
    private final EnterLk enterLk;
    private final LessonMenu lessonMenu;

    public EtuAttenderBot(BotConfig botConfig, UserService userService, MainMenu mainMenu, EnterLk enterLk, LessonMenu lessonMenu) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.mainMenu = mainMenu;
        this.enterLk = enterLk;
        this.lessonMenu = lessonMenu;
    }


    @Override
    public void onUpdateReceived(Update update) {
        this.update = update;
        new Thread(() -> {

            if (update.hasMessage() && update.getMessage().hasText()) {
                Optional<User> optionalUser = userService.getUser(update.getMessage().getChatId());
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    if (update.getMessage().hasText()) {
                        if (update.getMessage().getText().equals("/start")) {
                            user.setState(UserState.IN_MAIN_MENU);
                            userService.saveUser(user);
                        }
                    }

                    UserState userState = user.getState();
                    switch (userState) {
                        case IN_MAIN_MENU -> handle(mainMenu.select(update, user));
                        case ENTERING_LK -> handle(enterLk.select(update, user));
                        case IN_LESSONS_MENU -> handle(lessonMenu.select(update,user));

                    }

                } else {
                    signUp();
                }
            } else if (update.hasCallbackQuery()) {
                update.setMessage(update.getCallbackQuery().getMessage());
                onUpdateReceived(update);
            }
        }).start();
    }

    public void signUp() {
        User user = new User();
        user.setId(update.getMessage().getChatId());
        if (user.getId() == botConfig.getOwner()) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        user.setState(UserState.IN_MAIN_MENU);
        user.setNick(update.getMessage().getFrom().getUserName());
        userService.saveUser(user);
        onUpdateReceived(update);
    }

    public void handle(Object object){
        if (object instanceof BotApiMethod){
            action((BotApiMethod) object);
        }
        if (object instanceof UserState){
            action((UserState) object);
        }
    }

    @SneakyThrows
    public void action(BotApiMethod botApiMethod){
        execute(botApiMethod);
    }

    public void action(UserState userState){
        User user = userService.getUser(update.getMessage().getChatId()).get();
        user.setState(userState);
        userService.saveUser(user);
        onUpdateReceived(update);
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }
}
