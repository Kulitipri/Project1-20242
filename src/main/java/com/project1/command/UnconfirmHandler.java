package com.project1.command;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.AirtableClient;

public class UnconfirmHandler {

    private final AbsSender bot;
    private final AirtableClient airtableClient = new AirtableClient();

    public UnconfirmHandler(AbsSender bot) {
        this.bot = bot;
    }

    public void handleUnconfirm(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String userName = message.getFrom().getFirstName();
        String text = message.getText().trim();

        // Kiểm tra cú pháp lệnh
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            send(chatId, "❌ Please use `/unconfirm <schedule_id>`\nExample: `/unconfirm SCH123456`");
            return;
        }

        String scheduleId = parts[1].trim();

        // Kiểm tra trên Airtable đã xác nhận chưa
        boolean alreadyConfirmed = airtableClient.isAlreadyConfirmed(scheduleId, String.valueOf(userId));
        if (!alreadyConfirmed) {
            send(chatId, "⚠️ You have not confirmed schedule `" + scheduleId + "` yet.");
            return;
        }

        // Xoá xác nhận của user này trên Airtable
        airtableClient.deleteConfirmation(scheduleId, String.valueOf(userId));

        send(chatId, "❌ " + userName + " has unconfirmed their participation in schedule `" + scheduleId + "`.");
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