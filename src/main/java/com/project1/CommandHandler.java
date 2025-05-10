package com.project1;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CommandHandler {
    private final ChatLoggerBot bot;

    public CommandHandler(ChatLoggerBot bot) {
        this.bot = bot;
    }

    public void handleCommand(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        User user = message.getFrom();
        Long userId = user.getId();

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Lệnh dành cho tất cả người dùng
        switch (text) {
            case "/start":
                send(chatId, "Hi! I'm the very very bot. How can I help you?");
                return;
            case "/help":
                send(chatId,"List of commands:\n/start - Start chatting\n/help - Show this help\n/about - Bot info\n/time - Show current time\n/stats - (admin only)");
                return;
            case "/about":
                send(chatId, "I'm the very very bot, created by Nguyen Thien Khai 20235595 and Tran Anh Tuan 20235628, SOICT, HUST");
                return;
            case "/time":
                send(chatId, "Today is " + date + "\nTime (GMT+7): " + time);
                return;
        }

        // Các lệnh chỉ dành cho admin
        switch (text) {
            case "/Test":
        String chatType = message.getChat().getType(); // "private", "group", "supergroup"
                if (chatType.equals("private")) {
                    send(chatId, "This command is only available in group chat.");
                    return;
                }

                if (isUserAdmin(chatId, userId)) {
                    send(chatId, "You are an admin. You can use this command to get statistics.");
                    // chèn xử lý lệnh ở đây
                } else {
                    send(chatId, "You are not an admin. Only admins can use this command.");
                }
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

    // Kiểm tra xem người dùng có phải admin không
    private boolean isUserAdmin(Long chatId, Long userId) {
        try {
            GetChatMember getMember = new GetChatMember();
            getMember.setChatId(chatId.toString());
            getMember.setUserId(userId);

            ChatMember member = bot.execute(getMember);
            String status = member.getStatus(); // "administrator", "creator", "member", etc.
            return status.equals("administrator") || status.equals("creator");
        } catch (TelegramApiException e) {
            System.err.println("Error checking admin: " + e.getMessage());
            return false;
        }
    }
}
