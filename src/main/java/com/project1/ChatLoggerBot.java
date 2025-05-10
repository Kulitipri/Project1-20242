package com.project1;

//thư viện datetime
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class ChatLoggerBot extends TelegramLongPollingBot {

    // Thay token và username của bot
    private static final String BOT_TOKEN = "7605592923:AAHSP_XIE00dpeq9ZOSOB20aYsHIU8xL8Ck";
    private static final String BOT_USERNAME = "theveryverybot";

    private final CommandHandler commandHandler = new CommandHandler(this);// khởi tạo CommandHandler để xử lý các lệnh từ người dùng
    
    
    private final LogSaver airtable = new LogSaver(); // Khởi tạo LogSaver để ghi log vào Airtable

    // override các method của TelegramLongPollingBot
    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }


    // method để xử lý các tin nhắn đến
    // method sẽ được gọi khi bot nhận được tin nhắn
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();// lấy tin nhắn từ update
            commandHandler.handleCommand(message);// xử lý lệnh từ người dùng
            
            String sender = message.getFrom().getFirstName();// lấy tên người gửi(theo tên trong tài khoản telegram)
            String username = message.getFrom().getUserName();// lấy tên người gửi(theo tên trong tài khoản telegram)
            
            String text = message.getText();// lấy nội dung tin nhắn
            Long chatId = message.getChatId();// lấy ID của chat
           
            String chatTypeRaw = message.getChat().getType(); // "private", "group", "supergroup"
            String chatType = chatTypeRaw.equals("private") ? "PRIVATE" : "GROUP";// kiểm tra loại chat (nhóm hay cá nhân)
            String chatTitle = message.getChat().getTitle(); // Lấy tên group nếu là group chat

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));// format thời gian

            // in tin nhắn vào terminal log
            System.out.println("──────────────────────────────────────────");
            System.out.println(timestamp);
            if (chatType.equals("GROUP")) {// để log chat phân biệt giữa group và nhắn riêng
                System.out.println("Chat ID: " + chatId + " (" + chatType + ")" + "(" + chatTitle + ")");
            } else {
                System.out.println("Chat ID: " + chatId + " (" + chatType + ")");
            }
            System.out.println("Sender: " + sender + (username != null ? " (@" + username + ")" : ""));
            System.out.println("Message: " + text);

            // ghi vào airtable
            try {
                airtable.addRecord(sender, text, timestamp, chatType, chatTitle != null ? chatTitle : "NULL", chatId != null ? chatId.toString() : "NULL");
                System.out.println("Log saved to Airtable successfully.");
            } catch (IOException e) {
                System.err.println("Unditected error: " + e.getMessage());
        } 
    }
}
    // method main để khởi động bot
    // method main sẽ được gọi khi chạy chương trình
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChatLoggerBot());
            System.out.println("Bot is running properly...");
        } catch (TelegramApiException e) {
            System.err.println("Error starting bot: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
