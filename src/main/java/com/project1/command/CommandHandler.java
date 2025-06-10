package com.project1.command;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.util.IsUserAdmin;

public class CommandHandler {

    private final AbsSender bot;
    private final IsUserAdmin adminChecker;
    private final SetSchedule setScheduleHandler;
    private final ConfirmHandler confirmHandler;


    public CommandHandler(AbsSender bot, Map<String, Long> pollChatMap) {
        this.bot = bot;
        this.adminChecker = new IsUserAdmin(bot);
        this.setScheduleHandler = new SetSchedule(bot, pollChatMap);
        this.confirmHandler = new ConfirmHandler(bot);
        this.viewSchedules = new ViewSchedules(bot); 
        this.groupInfoHandler = new GroupInfoHandler(bot);
        this.unconfirmHandler = new UnconfirmHandler(bot);
        this.myConfirmHandler = new MyConfirmHandler(bot);
        this.deleteScheduleHandler = new DeleteScheduleHandler(bot);
    }

    private final ViewSchedules viewSchedules; 
    private final GroupInfoHandler groupInfoHandler; 
    private final UnconfirmHandler unconfirmHandler;
    private final MyConfirmHandler myConfirmHandler;
    private final DeleteScheduleHandler deleteScheduleHandler;

    public void handleCommand(Message message) {
    String text = message.getText().trim();
    Long chatId = message.getChatId();
    Long userId = message.getFrom().getId();
    String chatType = message.getChat().getType(); // "private", "group", etc.

    String key = userId + "_" + chatId;

    // ✅ 1. Nếu user đang ở trong quá trình nhập schedule → gọi handle tiếp
    if (SetSchedule.containsUserState(key)) {
        setScheduleHandler.handle(message);  // QUAN TRỌNG: xử lý chuỗi tiếp theo như "toán", "12h", ...
        return;
    }

    // ✅ 2. Nếu là command thì xử lý
    if (text.equalsIgnoreCase("/start")) {
        send(chatId, "Hi! I'm the very very bot. How can I help you?");
        return;
    }

    if (text.equalsIgnoreCase("/help")) {
        send(chatId,
            "*Available Commands:*\n\n" +
            "🤖 *Basic Commands:*\n" +
            "*/start* - Start the bot\n" +
            "*/help* - Show this help message\n" +
            "*/about* - Show bot information\n" +
            "*/time* - Show current time\n\n" +

            "📅 *Schedule Commands:*\n" +
            "*/set_schedule* - Create a new schedule\n" + 
            "*/view_schedules* - View all schedules in group\n" +
            "*/delete_schedule* <id> - Delete a schedule (admin only)\n\n" +
            
            "✅ *Confirmation Commands:*\n" +
            "*/confirm* <id> - Confirm attendance for a schedule\n" +
            "*/unconfirm* <id> - Cancel your confirmation\n" +
            "*/myconfirm* - View your confirmed schedules\n\n" +
            
            "👥 *Group Commands:*\n" +
            "*/group_info* - View information about all groups\n" +
            "*/cancel* - Cancel ongoing schedule creation\n\n" +

            "💡 *Additional Features:*\n" +
            "• Automatic schedule detection from messages\n" +
            "• Poll voting for schedule agreement\n" +
            "• Automatic notifications before classes\n\n" +
            
            "Note: Some commands are restricted to group admins only."
        );
        return;
    }

    if (text.equalsIgnoreCase("/about")) {
        send(chatId, "I'm the very very bot, created by Nguyen Thien Khai 20235595 and Tran Anh Tuan 20235628, SOICT, HUST");
        return;
    }

    if (text.equalsIgnoreCase("/time")) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        send(chatId, "Today is " + date + "\nTime (GMT+7): " + time);
        return;
    }

    if (text.equalsIgnoreCase("/Test")) {
        if (chatType.equals("private")) {
            send(chatId, "This command is only available in group chat.");
            return;
        }

        if (adminChecker.isAdmin(message)) {
            send(chatId, "You are an admin. You can use this command to get statistics.");
        } else {
            send(chatId, "You are not an admin. Only admins can use this command.");
        }
        return;
    }

    if (text.startsWith("/set_schedule")) {
        if (SetSchedule.containsUserState(key)) {
            setScheduleHandler.handle(message);
        } else {
            setScheduleHandler.start(chatId, userId, chatType, message);
        }
        return;
    }

    if (text.equalsIgnoreCase("/cancel")) {
        send(chatId, "⚠️ You have to be in the process of setting a schedule to use this command.");
        return;
    }

    if (text.startsWith("/confirm")) {
        String userName = message.getFrom().getUserName();
        String confirmText = message.getText();
        confirmHandler.handleConfirm(chatId, confirmText, userId, userName);
        return;
    }

    if (text.startsWith("/unconfirm")) {
        unconfirmHandler.handleUnconfirm(message);
        return;
    }

    if (text.startsWith("/view_schedules")) {
        viewSchedules.handle(message);
        return;
    }

    if (text.startsWith("/group_info")) {
        groupInfoHandler.handle(message);
        return;
    }

    if (text.startsWith("/myconfirm")) {
        myConfirmHandler.handleMyConfirm(message);
        return;
    }

    if (text.startsWith("/delete_schedule")) {
        deleteScheduleHandler.handleDeleteSchedule(message);
        return;
    }

    // ❌ Nếu không khớp bất kỳ command nào
    if (text.startsWith("/")) {
        send(chatId, "Command not found. Type */help* to see the list of commands.");
    }
}


    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true);
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}
