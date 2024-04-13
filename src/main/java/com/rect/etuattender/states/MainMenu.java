package com.rect.etuattender.states;

import com.rect.etuattender.model.User;
import com.rect.etuattender.model.UserState;
import com.rect.etuattender.service.ReplyKeyboardMarkupService;
import com.rect.etuattender.service.UserService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;

@Deprecated //DEPRECATED SINCE REWORK 13.04
@Component
public class MainMenu {

//    private Update update;
//    private User user;
//
//    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
//    private final UserService userService;
//
//    @Autowired
//    public MainMenu(ReplyKeyboardMarkupService replyKeyboardMarkupService, UserService userService) {
//        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
//        this.userService = userService;
//    }
//
//
//    public Object select(Update update, User user){
//        this.update=update;
//        this.user=user;
//        String command = update.getMessage().getText();
//
//        if (command.equals("Расписание") && user.getCookie()==null){
//            return error();
//        }
//
//        if(user.getCookieLifetime()==null && command.equals("Расписание")){
//            command = "/start";
//        }
//        if (command.equals("Расписание") && user.getCookieLifetime().isBefore(LocalDateTime.now())) {
//            return authExpired();
//        }
//
//        switch (command){
//            case "Ввести данные ЛК","Изменить данные лк": return UserState.ENTERING_LK;
//            case "Панель Админа": return UserState.IN_ADMIN_PANEL;
//            case "Информация": return getInfo();
//            case "Расписание": return UserState.IN_LESSONS_MENU;
//            case "Назад":
//            case "/start":
//                return inMainMenu();
//        }
//        if (user.getCookieLifetime()!=null){
//        if (user.getCookieLifetime().isBefore(LocalDateTime.now())){
//            return authExpired();
//        }}
//        return error();
//    }
//
//    private BotApiMethod getInfo(){
//        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()),
//                "\nТех поддержка: @rected" +
//                        "\n\nДанный бот предназначен для автоматический отметки на парах и просмотра расписания");
//        return message;
//    }
//
//     @SneakyThrows
//     public BotApiMethod inMainMenu() {
//        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update, user);
//        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Вы в главном меню. Если кнопки не появились - введите /start");
//        message.setReplyMarkup(replyKeyboardMarkup);
//        return message;
//    }
//
//    public BotApiMethod authExpired(){
//        ReplyKeyboardMarkup replyKeyboardMarkup = replyKeyboardMarkupService.get(update, user);
//        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Ваш токен регистрации в системе истек, необходимо ввести данные ЛК снова!");
//        message.setReplyMarkup(replyKeyboardMarkup);
//        return message;
//    }
//
//    public BotApiMethod error(){
//        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()),"Неизвестная команда. Введите /start");
//        return message;
//    }
}
