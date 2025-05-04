package com.project1;

// thư viện date time của java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CommandHandler {
    private final ChatLoggerBot bot;//bot sẽ gọi đến class này để xử lý các lệnh từ người dùng

    public CommandHandler(ChatLoggerBot bot) {
        this.bot = bot;// khởi tạo bot
    }

    // method xử lý các lệnh từ người dùng
    public void handleCommand(Message message) {
        String text = message.getText();
        Long chatId = message.getChatId();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));


        // Xử lý các lệnh cơ bản
        switch (text) {
            case "/start":
                send(chatId, "Hi! I'm the very very bot. How can I help you?");
                break;
            case "/help":
                send(chatId, "List of command:\n/start - Start chatting with bot\n/help - To see list of command\n/about - Introduction about the bot\n/time - Current time");
                break;
            case "/about":
                send(chatId, "I'm the very very bot, created by Nguyen Thien Khai 20235595, SOICT, HUST");
                break;
            case "/time":
                send(chatId, "Today is " + date + "\nTime (GMT +7): " + time);
                break;
            default:
                if (text.startsWith("/")) {
                    send(chatId, "Invalid command. Type /help for a list of commands.");
                }
        }
    }

    // gửi tin nhắn đến user
    private void send(Long chatId, String text) {
        try {
            bot.execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
        }
    }
}
