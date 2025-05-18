package com.project1.command;

// thư viện date time
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.util.IsUserAdmin;

public class CommandHandler {

    private final AbsSender bot;
    private final IsUserAdmin adminChecker; // Sửa lỗi thiếu class adminChecker
    private final SetSchedule setScheduleHandler;

    public CommandHandler(AbsSender bot) {
        this.bot = bot;
        this.adminChecker = new IsUserAdmin(bot); // Initialize adminChecker with bot
        this.setScheduleHandler = new SetSchedule(bot); // Initialize setScheduleHandler with bot
    }

    public void handleCommand(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String chatType = message.getChat().getType(); // "private", "group", "supergroup"
        Long userId = message.getFrom().getId();
        
        
        String stateKey = userId + "_" + chatId;// Tạo khóa duy nhất cho người dùng và nhóm
        if (SetSchedule.userStates.containsKey(stateKey)) {
            setScheduleHandler.handle(message);
            return;
        }

        // command handle
        switch (text) {
            
            // lệnh cho tất cả ng dùng
            case "/start":
                send(chatId, "Hi! I'm the very very bot. How can I help you?");
                return;
            case "/help":
                send(chatId, "List of commands:\n/start - Start chatting\n/help - Show this help\n/about - Bot info\n/time - Show current time\n/Test - (admin only) command is temporary developing");
                return;
            case "/about":
                send(chatId, "I'm the very very bot, created by Nguyen Thien Khai 20235595 and Tran Anh Tuan 20235628, SOICT, HUST");
                return;
            case "/time":
                send(chatId, "Today is " + date + "\nTime (GMT+7): " + time);
                return;
            
            // lệnh chỉ dành cho admin
            case "/Test":
                if (chatType.equals("private")) {
                    send(chatId, "This command is only available in group chat.");
                    return;
                }

                if (adminChecker.isAdmin(message)) {
                    send(chatId, "You are an admin. You can use this command to get statistics.");
                    // xử lý logic thống kê ở đây
                } else {
                    send(chatId, "You are not an admin. Only admins can use this command.");
                }
                return;
            case "/set_schedule":
                setScheduleHandler.start(chatId, userId, chatType, message); // ✅ bắt đầu lịch học
                return;
        }

        // Nếu là lệnh không hợp lệ
        if (text.startsWith("/")) {
            send(chatId, "Command not found. Type /help to see the list of commands.");
        }
    }

    // Gửi tin nhắn
    private void send(Long chatId, String text) {
        try {
            bot.execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}