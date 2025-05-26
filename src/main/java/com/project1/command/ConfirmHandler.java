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

public class ConfirmHandler {

    private final AbsSender bot;

    // ✅ Map lưu danh sách người dùng đã xác nhận cho từng buổi học
    // Key: scheduleId (ví dụ "SCH123456") → Set of userId
    private final Map<String, Set<Long>> scheduleConfirmations = new HashMap<>();

    public ConfirmHandler(AbsSender bot) {
        this.bot = bot;
    }

    // 📌 Hàm xử lý lệnh /confirm <scheduleId>
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

        scheduleConfirmations.putIfAbsent(scheduleId, new HashSet<>());

        if (scheduleConfirmations.get(scheduleId).contains(userId)) {
            send(chatId, "✅ You have already confirmed schedule `" + scheduleId + "`.");
        } else {
            scheduleConfirmations.get(scheduleId).add(userId);
            send(chatId, "📌 " + userName + " has confirmed schedule `" + scheduleId + "`.");
        }

        // TODO: Ghi xác nhận này vào Airtable nếu cần
    }

    // ✅ Lấy danh sách người đã xác nhận cho 1 lịch học
    public Set<Long> getConfirmedUsers(String scheduleId) {
        return scheduleConfirmations.getOrDefault(scheduleId, Collections.emptySet());
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true); // hỗ trợ in đậm và mã lịch
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }
}
