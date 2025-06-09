package com.project1.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.AirtableClient;
import com.project1.util.IsUserAdmin;

public class DeleteScheduleHandler {

    private final AbsSender bot;
    private final AirtableClient airtableClient = new AirtableClient();

    public DeleteScheduleHandler(AbsSender bot) {
        this.bot = bot;
        this.adminChecker = new IsUserAdmin(bot); // Initialize adminChecker after bot is set
    }

    private final IsUserAdmin adminChecker;

    public void handleDeleteSchedule(Message message) {
        Long chatId = message.getChatId();
        String userName = message.getFrom().getFirstName();
        String text = message.getText().trim();

        // Kiểm tra xem người dùng có phải admin không
        if (!adminChecker.isAdmin(message)) {
            send(chatId, "⛔ Only group administrators can delete schedules.");
            return;
        }

        // Kiểm tra cú pháp lệnh
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            send(chatId, "❌ Please use `/delete_schedule <schedule_id>`\nExample: `/delete_schedule SCH123456`");
            return;
        }

        String scheduleId = parts[1].trim();

        // Kiểm tra xem lịch có tồn tại trên Airtable không
        boolean exists = airtableClient.scheduleExists(scheduleId);
        if (!exists) {
            send(chatId, "❌ Schedule `" + scheduleId + "` does not exist or has already been deleted.");
            return;
        }

        // Xoá schedule khỏi Airtable và tất cả xác nhận liên quan
        airtableClient.deleteSchedule(scheduleId);
        airtableClient.deleteConfirmationsByScheduleId(scheduleId);

        send(chatId, "🗑️ Schedule `" + scheduleId + "` has been deleted by admin " + userName + ".");
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true);
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("❌ Error sending message to chatId=" + chatId + ": " + e.getMessage());
        }
    }
}
