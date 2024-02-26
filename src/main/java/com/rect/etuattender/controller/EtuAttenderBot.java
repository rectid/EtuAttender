package com.rect.etuattender.controller;


import com.rect.etuattender.config.BotConfig;
import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.UserService;
import com.rect.etuattender.states.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
public class EtuAttenderBot extends TelegramLongPollingBot {
    private final EtuAttenderBot etuAttenderBot = this;
    private final BotConfig botConfig;
    private final UserService userService;
    private final MainMenu mainMenu;
    private final EnterLk enterLk;
    private final EnterWithoutSave enterWithoutSave;
    public final EnterWithSave enterWithSave;
    private final LessonMenu lessonMenu;
    private final AdminPanel adminPanel;
    private final MassMessage massMessage;
    public final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();


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
        executor.execute(() -> {
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
                        case IN_MAIN_MENU -> handle(mainMenu.select(update, user), update);
                        case ENTERING_LK -> handle(enterLk.select(update, user, etuAttenderBot), update);
                        case ENTERING_WITH_SAVE -> handle(enterWithSave.select(update, user, etuAttenderBot), update);
                        case ENTERING_WITHOUT_SAVE ->
                                handle(enterWithoutSave.select(update, user, etuAttenderBot), update);
                        case IN_LESSONS_MENU -> handle(lessonMenu.select(update, user, etuAttenderBot), update);
                        case IN_ADMIN_PANEL -> handle(adminPanel.select(update, user, etuAttenderBot), update);
                        case SENDING_MASS_MESSAGE -> handle(massMessage.select(update, user, etuAttenderBot), update);
                        case IN_BAN ->
                                handle(new SendMessage(String.valueOf(user.getId()), "Вы заблокированы!"), update);
                    }

                } else {
                    signUp(update);
                }
            } else if (update.hasCallbackQuery()) {
                update.setMessage(update.getCallbackQuery().getMessage());
                onUpdateReceived(update);
            }
        });
    }

    public Future<?> onUpdateReceived(Update update, Runnable job) {
        return executor.submit(job);
    }

    public void signUp(Update update) {
        User user = new User();
        user.setId(update.getMessage().getChatId());
        if (user.getId() == botConfig.getOwner()) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }
        user.setState(UserState.IN_MAIN_MENU);
        if (update.getMessage().getFrom().getUserName() != null) {
            user.setNick(update.getMessage().getFrom().getUserName());
        } else {
            user.setNick(update.getMessage().getFrom().getId().toString());
        }
        ;
        userService.saveUser(user);
        onUpdateReceived(update);
    }

    public void handle(Object object, Update update) {
        if (object instanceof BotApiMethod) {
            action((BotApiMethod) object);
        }
        if (object instanceof UserState) {
            action((UserState) object, update);
        }
    }

    public void action(BotApiMethod botApiMethod) {
        try {
            execute(botApiMethod);
        } catch (TelegramApiException ignored) {}
    }

    public void action(UserState userState, Update update) {
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
