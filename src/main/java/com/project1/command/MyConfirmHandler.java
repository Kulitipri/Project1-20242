package com.project1.command;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.AirtableClient;
import com.project1.util.TelegramApiUtil;

public class MyConfirmHandler {

    private final AbsSender bot;
    private final AirtableClient airtableClient = new AirtableClient();

    public MyConfirmHandler(AbsSender bot) {
        this.bot = bot;
    }

    public void handleMyConfirm(Message message) {
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String userName = message.getFrom().getFirstName();
        String chatType = message.getChat().getType();

        // Lấy danh sách xác nhận từ Airtable
        List<Map<String, Object>> confirmations = airtableClient.getConfirmationsByUserId(String.valueOf(userId));
        Set<String> confirmedScheduleIds = new HashSet<>();
        Set<String> groupIds = new HashSet<>();
        for (Map<String, Object> record : confirmations) {
            Map<String, Object> fields = (Map<String, Object>) record.get("fields");
            if (fields != null && fields.get("ScheduleId") != null) {
                confirmedScheduleIds.add(fields.get("ScheduleId").toString());
                if (fields.get("GroupId") != null) {
                    groupIds.add(fields.get("GroupId").toString());
                }
            }
        }

        StringBuilder response = new StringBuilder("📋 *Your Confirmed Schedules, " + userName + "*:\n\n");
        boolean hasConfirms = false;

        if (!confirmedScheduleIds.isEmpty()) {
            List<Map<String, Object>> allSchedules = airtableClient.getSchedulesByIds(confirmedScheduleIds);
            if ("private".equalsIgnoreCase(chatType)) {
                // Trong private chat: hiển thị theo từng group
                for (String groupId : groupIds) {
                    String groupName = TelegramApiUtil.getChatTitle(bot, Long.valueOf(groupId));
                    response.append("👥 *Group Name:* ").append(groupName != null ? groupName : "Unknown Group").append("\n");
                    for (Map<String, Object> record : allSchedules) {
                        Map<String, Object> fields = (Map<String, Object>) record.get("fields");
                        if (fields == null) continue;
                        if (groupId.equals(String.valueOf(fields.getOrDefault("GroupId", "")))) {
                            hasConfirms = true;
                            response.append("🔖 *Schedule ID:* ").append(fields.getOrDefault("ScheduleId", "N/A")).append("\n")
                                    .append("   📘 *Subject:* ").append(fields.getOrDefault("Subject", "N/A")).append("\n")
                                    .append("   🕒 *Start Time:* ").append(fields.getOrDefault("Time", "N/A")).append("\n")
                                    .append("   ⏰ *End Time:* ").append(fields.getOrDefault("EndTime", "N/A")).append("\n")
                                    .append("   🏫 *Location:* ").append(fields.getOrDefault("Location", "N/A")).append("\n\n");
                        }
                    }
                }
            } else {
                // Trong group chat: chỉ hiện lịch của group hiện tại, không cần header group name
                for (Map<String, Object> record : allSchedules) {
                    Map<String, Object> fields = (Map<String, Object>) record.get("fields");
                    if (fields == null) continue;
                    if (String.valueOf(chatId).equals(String.valueOf(fields.getOrDefault("GroupId", "")))) {
                        hasConfirms = true;
                        response.append("🔖 *Schedule ID:* ").append(fields.getOrDefault("ScheduleId", "N/A")).append("\n")
                                .append("   📘 *Subject:* ").append(fields.getOrDefault("Subject", "N/A")).append("\n")
                                .append("   🕒 *Start Time:* ").append(fields.getOrDefault("Time", "N/A")).append("\n")
                                .append("   ⏰ *End Time:* ").append(fields.getOrDefault("EndTime", "N/A")).append("\n")
                                .append("   🏫 *Location:* ").append(fields.getOrDefault("Location", "N/A")).append("\n\n");
                    }
                }
            }
        }

        if (!hasConfirms) {
            response.append("⚠️ You have not confirmed any schedules yet. Use `/confirm <schedule_id>` to join a schedule!");
        }

        send(chatId, response.toString());
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true);
            bot.execute(msg);
            System.out.println("DEBUG: MyConfirm sent to chatId=" + chatId + ": " + text);
        } catch (TelegramApiException e) {
            System.err.println("❌ Error sending MyConfirm to chatId=" + chatId + ": " + e.getMessage());
        }
    }
}