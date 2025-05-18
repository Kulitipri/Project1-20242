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
import com.project1.command.CommandHandler;
import com.project1.config.BotConfig;
import com.project1.util.InfoExtractor;
import com.project1.util.IsUserAdmin;

public class ChatLoggerBot extends TelegramLongPollingBot {

    private final CommandHandler commandHandler = new CommandHandler(this);
    private final LogSaver airtable = new LogSaver();
    private final Map<Long, Map<String, List<String>>> pendingScheduleRequests = new ConcurrentHashMap<>();
    private final IsUserAdmin adminChecker = new IsUserAdmin(this);

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

        String chatTypeRaw = message.getChat().getType(); // "private", "group", etc.
        String chatType = chatTypeRaw.equals("private") ? "PRIVATE" : "GROUP";
        String chatTitle = message.getChat().getTitle();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

        // Print to terminal
        System.out.println("──────────────────────────────────────────");
        System.out.println(timestamp);
        System.out.println("Chat ID: " + chatId + " (" + chatType + ")" + (chatTitle != null ? " (" + chatTitle + ")" : ""));
        System.out.println("Sender: " + sender + (username != null ? " (@" + username + ")" : ""));
        System.out.println("Message: " + text);

        

        // ✅ Handle confirmation after auto-detection
        if (pendingScheduleRequests.containsKey(userId)) {
            if (chatType.equals("PRIVATE")) {
                send(chatId, "This feature only works in group chats.");
                return;
            }

            if (text.equalsIgnoreCase("y")) {
                if (!adminChecker.isAdmin(message)) {
                    send(chatId, "Only admins can verify schedule requests in group chats.");
                    return;
                }

                Map<String, List<String>> schedule = pendingScheduleRequests.remove(userId);
                send(chatId, "✅ Schedule created successfully:" +
                        "\n📘 Subject: " + String.join(", ", schedule.get("Subject")) +
                        "\n🕒 Time: " + String.join(", ", schedule.get("Time")) +
                        "\n🏫 Location: " + String.join(", ", schedule.get("Location")) +
                        "\n📍 Group ID: " + chatId);
                return;

            } else if (text.equalsIgnoreCase("n")) {
                pendingScheduleRequests.remove(userId);
                send(chatId, "❌ Schedule request canceled.");
                return;
            }
        }

        // ✅ Command handler
        commandHandler.handleCommand(message);

        // ✅ Log to Airtable
        try {
            airtable.addRecord(sender, text, timestamp, chatType, chatTitle != null ? chatTitle : "NULL", chatId.toString());
            System.out.println("✅ Log saved to Airtable.");
        } catch (IOException e) {
            System.err.println("❌ Airtable error: " + e.getMessage());
        }

        // ✅ Extract schedule
        Map<String, List<String>> info = InfoExtractor.extractInfo(text);
        List<String> times = info.get("Time");
        List<String> subjects = info.get("Subject");
        List<String> locations = info.get("Location");

        if (!times.isEmpty() && !subjects.isEmpty() && !locations.isEmpty()) {
            pendingScheduleRequests.put(userId, info);

            String preview = String.format("📘 Detected class schedule:\n - Subject: %s\n - Time: %s\n - Location: %s",
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
