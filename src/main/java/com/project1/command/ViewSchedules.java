package com.project1.command;

import java.util.Map;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.util.ScheduleManager;
import com.project1.util.ScheduleRecord;

public class ViewSchedules {

    private final AbsSender bot;
    private final ScheduleManager scheduleManager;

    public ViewSchedules(AbsSender bot) {
        this.bot = bot;
        this.scheduleManager = ScheduleManager.getInstance(); // Sử dụng singleton
    }

    public void handle(Message message) {
        Long chatId = message.getChatId();
        String chatType = message.getChat().getType();

        // Chỉ cho phép trong nhóm
        if (chatType.equals("private")) {
            send(chatId, "❌ This command is only available in group chats.");
            return;
        }

        // Lấy danh sách lịch học trong nhóm từ ScheduleManager
        Map<String, ScheduleRecord> groupSchedules = scheduleManager.getSchedulesByGroup(chatId);

        if (groupSchedules.isEmpty()) {
            send(chatId, "📅 No schedules found for this group.");
            return;
        }

        // Tạo thông báo danh sách lịch học
        StringBuilder response = new StringBuilder("📅 Schedules for this group:\n\n");
        for (Map.Entry<String, ScheduleRecord> entry : groupSchedules.entrySet()) {
            ScheduleRecord record = entry.getValue();
            response.append("📌 Schedule ID: `").append(record.getId()).append("`\n")
                    .append("📘 Subject: ").append(record.getSubject()).append("\n")
                    .append("🕒 Time: ").append(record.getTime()).append("\n")
                    .append("🏫 Location: ").append(record.getLocation()).append("\n\n");
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