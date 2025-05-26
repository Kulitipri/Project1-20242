package com.project1.bot;

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

import com.project1.AirTable.LogSaver;
import com.project1.AirTable.ScheduleSaver;
import com.project1.command.CommandHandler;
import com.project1.command.ConfirmHandler;
import com.project1.config.BotConfig;
import com.project1.util.InfoExtractor;
import com.project1.util.IsUserAdmin;

public class ChatLoggerBot extends TelegramLongPollingBot {

    private final CommandHandler commandHandler = new CommandHandler(this);
    private final LogSaver airtable = new LogSaver();
    private final Map<String, Map<String, List<String>>> pendingScheduleRequests = new ConcurrentHashMap<>();
    private final IsUserAdmin adminChecker = new IsUserAdmin(this);
    private final ConfirmHandler confirmHandler = new ConfirmHandler(this);

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

        Message message = update.getMessage();
        String text = message.getText().trim();
        String sender = message.getFrom().getFirstName();
        String username = message.getFrom().getUserName();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String chatTitle = message.getChat().getTitle();
        String chatTypeRaw = message.getChat().getType();
        String chatType = chatTypeRaw.equals("private") ? "PRIVATE" : "GROUP";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        // Key cho xác nhận lịch
        String key = userId + "_" + chatId;

        // In ra terminal
        System.out.println("──────────────────────────────────────────");
        System.out.println(timestamp);
        System.out.println("Chat ID: " + chatId + " (" + chatType + ")" + (chatTitle != null ? " (" + chatTitle + ")" : ""));
        System.out.println("Sender: " + sender + (username != null ? " (@" + username + ")" : ""));
        System.out.println("Message: " + text);

        // ✅ Handle confirmation nếu có lịch đang chờ xác nhận
        if (pendingScheduleRequests.containsKey(key)) {

            if (chatType.equals("GROUP")) {
                if (text.equalsIgnoreCase("y")) {

                if (!adminChecker.isAdmin(message)) {
                    pendingScheduleRequests.remove(key);
                    send(chatId, "❌ Only admins can confirm schedules.");
                    return; // Không xóa key → cho phép admin thực hiện lại
                }

                Map<String, List<String>> schedule = pendingScheduleRequests.remove(key);
                String scheduleId = "SCH" + System.currentTimeMillis(); // tạo mã duy nhất

                // Ghi vào Airtable
                ScheduleSaver.save(
                    String.join(", ", schedule.get("Subject")),
                    String.join(", ", schedule.get("Time")),
                    String.join(", ", schedule.get("Location")),
                    chatId.toString(),
                    chatTitle != null ? chatTitle : "Unknown Group",
                    scheduleId
                );

                send(chatId, "Schedule created successfully:\n\n"
                        + "📘 Subject: " + String.join(", ", schedule.get("Subject")) + "\n"
                        + "🕒 Time: " + String.join(", ", schedule.get("Time")) + "\n"
                        + "🏫 Location: " + String.join(", ", schedule.get("Location")) + "\n"
                        + "📍 Group ID: " + chatId + "\n\n"
                        + "Members can confirm with /confirm " + scheduleId);
                return;

            } else if (text.equalsIgnoreCase("n")) {
                pendingScheduleRequests.remove(key);
                send(chatId, "❌ Schedule request canceled.");
                return;
            }
        }
        return;
    }


        if (text.startsWith("/confirm")) {
        confirmHandler.handleConfirm(message);
        return;
    }


        // ✅ Xử lý lệnh người dùng
        commandHandler.handleCommand(message);

        // ✅ Ghi log vào Airtable
        try {
            airtable.addRecord(sender, text, timestamp, chatType, chatTitle != null ? chatTitle : "NULL", chatId.toString());
            System.out.println("✅ Log saved to Airtable.");
        } catch (IOException e) {
            System.err.println("❌ Airtable error: " + e.getMessage());
        }

        // ✅ Phân tích lịch học
        Map<String, List<String>> info = InfoExtractor.extractInfo(text);
        List<String> times = info.get("Time");
        List<String> subjects = info.get("Subject");
        List<String> locations = info.get("Location");

        if (!times.isEmpty() && !subjects.isEmpty() && !locations.isEmpty()) {
            pendingScheduleRequests.put(key, info);

            if (chatType.equals("PRIVATE")) {
                send(chatId, "❌ This feature is only available in group chats.");
                return;
            }

            String preview = String.format("Detected class schedule:\n\n"
                    + "📘 Subject: %s\n"
                    + "🕒 Time: %s\n"
                    + "🏫 Location: %s",
                    String.join(", ", subjects),
                    String.join(", ", times),
                    String.join(", ", locations));

            send(chatId, preview + "\n\nDo you want to add this class to your schedule? (y/n)");
            System.out.println("📌 Detected class schedule from message.");
        }
    }

    private void send(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChatLoggerBot());
            System.out.println("🤖 Bot is running properly...");
        } catch (TelegramApiException e) {
            System.err.println("❌ Telegram error: " + e.getMessage());
        }
    }
}
