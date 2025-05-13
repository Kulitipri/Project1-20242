package com.project1;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class ChatLoggerBot extends TelegramLongPollingBot {

    // Khai báo các biến cần thiết
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final LogSaver airtable = new LogSaver();
    private final Map<Long, Map<String, List<String>>> pendingScheduleRequests = new ConcurrentHashMap<>();
    private final IsUserAdmin adminChecker = new IsUserAdmin(this);

    // Các method override của telegrampollingbot
    @Override
    public String getBotUsername() {
        return BotConfig.getTelegramBotname();
    }

    @Override
    public String getBotToken() {
        return BotConfig.getTelegramToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        // cụm lấy thông tin của người dùng và tin nhắn
        Message message = update.getMessage();
        String text = message.getText().trim();
        String sender = message.getFrom().getFirstName();
        String username = message.getFrom().getUserName();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        // cụm lấy thông tin của chat, thời gian và tên grp
        String chatTypeRaw = message.getChat().getType(); // "private", "group", "supergroup"
        String chatType = chatTypeRaw.equals("private") ? "PRIVATE" : "GROUP";
        String chatTitle = message.getChat().getTitle();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        // in thông in vào terminal
        System.out.println("──────────────────────────────────────────");
        System.out.println(timestamp);
        System.out.println("Chat ID: " + chatId + " (" + chatType + ")" + (chatTitle != null ? " (" + chatTitle + ")" : ""));
        System.out.println("Sender: " + sender + (username != null ? " (@" + username + ")" : ""));
        System.out.println("Message: " + text);


        commandHandler.handleCommand(message);// xử lý lệnh

        // ghi log vào airtable
        try {
            airtable.addRecord(sender, text, timestamp, chatType, chatTitle != null ? chatTitle : "NULL", chatId.toString());
            System.out.println("Log saved to Airtable.");
        } catch (IOException e) {
            System.err.println("Airtable error: " + e.getMessage());
        }

        // Xử lý xác nhận "yes" trong lịch học
        if (pendingScheduleRequests.containsKey(userId)) {
            if (text.equalsIgnoreCase("y")) {
                if (chatType.equals("GROUP") && adminChecker.isAdmin(message)) {
                    send(chatId, "Only admins can verify schedule requests in group chats.");
                    return;
                }

                Map<String, List<String>> schedule = pendingScheduleRequests.remove(userId);
                send(chatId, "Class schedule confirmed:\n" +
                        " - Subject: " + String.join(", ", schedule.get("Subject")) +
                        "\n - Time: " + String.join(", ", schedule.get("Time")) +
                        "\n - Location: " + String.join(", ", schedule.get("Location")));
                // TODO: Ghi vào bảng Schedules nếu cần
            } else if (text.equalsIgnoreCase("n")) {
                pendingScheduleRequests.remove(userId);
                send(chatId, "Schedule request canceled.");
            }
            return;
        }

        // Phân tích lịch học
        Map<String, List<String>> info = InfoExtractor.extractInfo(text);
        List<String> times = info.get("Time");
        List<String> subjects = info.get("Subject");
        List<String> locations = info.get("Location");

        if (!times.isEmpty() && !subjects.isEmpty() && !locations.isEmpty()) {
            pendingScheduleRequests.put(userId, info);

            String preview = String.format(" - Subject: %s\n - Time: %s\n - Location: %s",
                    String.join(", ", subjects),
                    String.join(", ", times),
                    String.join(", ", locations));

            send(chatId, "I have detected a class schedule:\n" + preview +
                    "\n\nDo you want to add class to schedule? (y/n)");

            System.out.println("Detected class schedule.");
        }
    }

    // gửi tin nhắn
    private void send(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            System.err.println("Sending message errors: " + e.getMessage());
        }
    }

    // main method khởi động bot
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChatLoggerBot());
            System.out.println("Bot is running properly...");
        } catch (TelegramApiException e) {
            System.err.println("Telegram error: " + e.getMessage());
        }
    }
}
