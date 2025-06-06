package com.project1.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.ConfirmationSaver;
import com.project1.util.ScheduleManager;

public class ConfirmHandler {

    private final AbsSender bot;
    private final ScheduleManager scheduleManager = ScheduleManager.getInstance();

    private final Map<String, Set<Long>> scheduleConfirmations = new HashMap<>();

    public ConfirmHandler(AbsSender bot) {
        this.bot = bot;
    }

    public void handleConfirm(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String userName = message.getFrom().getFirstName();
        String text = message.getText().trim();

        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            send(chatId, "❌ Please use `/confirm <schedule_id>`\nExample: /confirm SCH123456");
            return;
        }

        String scheduleId = parts[1];

        if (!scheduleManager.existsSchedule(scheduleId)) {
            send(chatId, "❌ Schedule `" + scheduleId + "` does not exist. Please check the schedule ID.");
            return;
        }

        scheduleConfirmations.putIfAbsent(scheduleId, new HashSet<>());

        if (scheduleConfirmations.get(scheduleId).contains(userId)) {
            send(chatId, "✅ You have already confirmed schedule `" + scheduleId + "`.");
            return;
        }

        scheduleConfirmations.get(scheduleId).add(userId);
        ConfirmationSaver.save(scheduleId, String.valueOf(userId), userName);
        send(chatId, "📌 " + userName + " has confirmed schedule `" + scheduleId + "`.");
    }

    public Set<Long> getConfirmedUsers(String scheduleId) {
        return scheduleConfirmations.getOrDefault(scheduleId, Collections.emptySet());
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