package com.rect.etuattender.states;

@Deprecated
public class MassMessage {
//    private EtuAttenderBot etuAttenderBot;
//    private final UserService userService;
//    private final ReplyKeyboardMarkupService replyKeyboardMarkupService;
//    private final EtuApiService etuApiService;
//
//    public MassMessage(UserService userService, ReplyKeyboardMarkupService replyKeyboardMarkupService, EtuApiService etuApiService) {
//        this.userService = userService;
//        this.replyKeyboardMarkupService = replyKeyboardMarkupService;
//        this.etuApiService = etuApiService;
//    }
//
//    public Object select(Update update, User user, EtuAttenderBot etuAttenderBot) {
//        this.etuAttenderBot = etuAttenderBot;
//        String command = update.getMessage().getText();
//
//        switch (command) {
//            case "Массовая рассылка":
//                return inMessageToUsers(update, user);
//            case "Назад":
//            case "/start":
//                return UserState.IN_ADMIN_PANEL;
//        }
//
//        return send(command, update, user);
//    }
//
//    private UserState send(String text, Update update, User user) {
//        List<User> userList = userService.getAll();
//        for (User tempUser:
//             userList) {
//            SendMessage message = new SendMessage(String.valueOf(tempUser.getId()), text);
//            etuAttenderBot.handle(message, update);
//        }
//        update.getMessage().setText("Панель Админа");
//        return UserState.IN_ADMIN_PANEL;
//    }
//
//    private BotApiMethod inMessageToUsers(Update update, User user) {
//        SendMessage message = new SendMessage(String.valueOf(update.getMessage().getChatId()), "Введите сообщение для всех пользователей");
//        message.setReplyMarkup(replyKeyboardMarkupService.getBackButton());
//        return message;
//    }


}
