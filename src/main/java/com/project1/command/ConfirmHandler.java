package com.project1.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.AirtableClient;
import com.project1.AirTable.ConfirmationSaver;

public class ConfirmHandler {

    private final AbsSender bot;
    private final AirtableClient airtableClient = new AirtableClient();

    public ConfirmHandler(AbsSender bot) {
        this.bot = bot;
    }

    public void handleConfirm(Long chatId, String scheduleId, Long userId, String userName) {
        // Kiểm tra schedule tồn tại trên Airtable
        boolean scheduleExists = airtableClient.scheduleExists(scheduleId);
        if (!scheduleExists) {
            send(chatId, "❌ Schedule `" + scheduleId + "` does not exist. Please check the schedule ID.");
            return;
        }

        // Kiểm tra trên Airtable đã xác nhận chưa (chặn trùng)
        if (airtableClient.isAlreadyConfirmed(scheduleId, String.valueOf(userId))) {
            send(chatId, "✅ You have already confirmed schedule `" + scheduleId + "`.");
            return;
        }

        // Lấy groupId từ Airtable (nếu cần)
        String groupId = airtableClient.getGroupIdByScheduleId(scheduleId);

        // Lưu xác nhận lên Airtable
        ConfirmationSaver.save(scheduleId, String.valueOf(userId), userName, groupId != null ? groupId : "");

        send(chatId, "📌 " + userName + " has confirmed schedule `" + scheduleId + "`.");
    }

    public void handleUnconfirm(Long chatId, String scheduleId, Long userId, String userName) {
        boolean scheduleExists = airtableClient.scheduleExists(scheduleId);
        if (!scheduleExists) {
            send(chatId, "❌ Schedule `" + scheduleId + "` does not exist. Please check the schedule ID.");
            return;
        }

        boolean alreadyConfirmed = airtableClient.isAlreadyConfirmed(scheduleId, String.valueOf(userId));
        if (!alreadyConfirmed) {
            send(chatId, "⚠️ You have not confirmed schedule `" + scheduleId + "` yet.");
            return;
        }

        airtableClient.deleteConfirmation(scheduleId, String.valueOf(userId));

        send(chatId, "❌ " + userName + " has unconfirmed their participation in schedule `" + scheduleId + "`.");
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true);
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }
}