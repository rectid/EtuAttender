package com.rect.etuattender.controller;


import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.states.*;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EtuAttenderBot extends TelegramLongPollingBot {

    private Update update;
    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenu mainMenu;
    private final EnterLk enterLk;
    private final EnterWithoutSave enterWithoutSave;
    private final EnterWithSave enterWithSave;
    private final LessonMenu lessonMenu;
    private final AdminPanel adminPanel;
    private final MassMessage massMessage;
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();


    public EtuAttenderBot(BotConfig botConfig, UserService userService, MainMenu mainMenu, EnterLk enterLk, EnterWithoutSave enterWithoutSave, EnterWithSave enterWithSave, LessonMenu lessonMenu, AdminPanel adminPanel, MassMessage massMessage) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.userService = userService;
        this.mainMenu = mainMenu;
        this.enterLk = enterLk;
        this.enterWithoutSave = enterWithoutSave;
        this.enterWithSave = enterWithSave;
        this.lessonMenu = lessonMenu;
        this.adminPanel = adminPanel;
        this.massMessage = massMessage;
    }


    @Override
    public void onUpdateReceived(Update update) {
        this.update = update;
        executor.execute(()->{
            if (update.hasMessage() && update.getMessage().hasText()) {
                Optional<User> optionalUser = userService.getUser(update.getMessage().getChatId());
                if (optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    if (update.getMessage().getText().equals("/start")) {
                        user.setState(UserState.IN_MAIN_MENU);
                        userService.saveUser(user);
                    }


                    UserState userState = user.getState();
                    switch (userState) {
                        case IN_MAIN_MENU -> handle(mainMenu.select(update, user));
                        case ENTERING_LK -> handle(enterLk.select(update, user, this));
                        case ENTERING_WITH_SAVE -> handle(enterWithSave.select(update, user, this));
                        case ENTERING_WITHOUT_SAVE -> handle(enterWithoutSave.select(update, user, this));
                        case IN_LESSONS_MENU -> handle(lessonMenu.select(update, user, this));
                        case IN_ADMIN_PANEL -> handle(adminPanel.select(update, user, this));
                        case SENDING_MASS_MESSAGE -> handle(massMessage.select(update, user, this));
                        case IN_BAN -> handle(new SendMessage(String.valueOf(user.getId()), "Вы заблокированы!"));
                    }

                } else {
                    signUp();
                }
            } else if (update.hasCallbackQuery()) {
                update.setMessage(update.getCallbackQuery().getMessage());
                onUpdateReceived(update);
            }

        });
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
        if (update.getMessage().getFrom().getUserName()!=null){
        user.setNick(update.getMessage().getFrom().getUserName());}
        else {user.setNick(update.getMessage().getFrom().getId().toString());};
        userService.saveUser(user);
        onUpdateReceived(update);
    }

    public void handle(Object object) {
        if (object instanceof BotApiMethod) {
            action((BotApiMethod) object);
        }
        if (object instanceof UserState) {
            action((UserState) object);
        }
    }

    @SneakyThrows
    public void action(BotApiMethod botApiMethod) {
        execute(botApiMethod);
    }

    public void action(UserState userState) {
        User user = userService.getUser(update.getMessage().getChatId()).get();
        user.setState(userState);
        userService.saveUser(user);
        onUpdateReceived(update);
    }

    public ThreadPoolExecutor waitForCompletion() {
    return executor;
}

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }
}
