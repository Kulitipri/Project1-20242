package com.project1.command;

import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.AirtableClient;

public class ViewSchedules {

    private final AbsSender bot;
    private final AirtableClient airtableClient = new AirtableClient();

    public ViewSchedules(AbsSender bot) {
        this.bot = bot;
    }

    public void handle(Message message) {
        Long chatId = message.getChatId();
        String chatType = message.getChat().getType();

        // Chỉ cho phép trong nhóm
        if (chatType.equals("private")) {
            send(chatId, "❌ This command is only available in group chats.");
            return;
        }

        // Lấy danh sách lịch học của nhóm từ Airtable (chỉ lấy những lịch còn tồn tại trên Airtable)
        List<Map<String, Object>> schedules = airtableClient.getSchedulesByGroupId(chatId.toString());

        if (schedules == null || schedules.isEmpty()) {
            send(chatId, "📅 No schedules found for this group.");
            return;
        }

        // Tạo thông báo danh sách lịch học
        StringBuilder response = new StringBuilder("📅 Schedules for this group:\n\n");
        for (Map<String, Object> record : schedules) {
            Map<String, Object> fields = (Map<String, Object>) record.get("fields");
            if (fields == null) continue;
            response.append("📌 Schedule ID: `").append(fields.getOrDefault("ScheduleId", "N/A")).append("`\n")
                    .append("📘 Subject: ").append(fields.getOrDefault("Subject", "N/A")).append("\n")
                    .append("🕒 Start Time: ").append(fields.getOrDefault("Time", "N/A")).append("\n")
                    .append("⏰ End Time: ").append(fields.getOrDefault("EndTime", "Not specified")).append("\n")
                    .append("🏫 Location: ").append(fields.getOrDefault("Location", "N/A")).append("\n\n");
        }

        send(chatId, response.toString());
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