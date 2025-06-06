package com.project1.command;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.ScheduleSaver;
import com.project1.util.DateTimeValidator;
import com.project1.util.InfoExtractor;
import com.project1.util.IsUserAdmin;
import com.project1.util.ScheduleManager;
import com.project1.util.ScheduleRecord;
import com.project1.util.TelegramApiUtil;

public class SetSchedule {

    private static final Map<String, TempScheduleState> userStates = new ConcurrentHashMap<>();
    private final AbsSender bot;
    private final IsUserAdmin adminChecker;
    private final ScheduleManager scheduleManager = ScheduleManager.getInstance();

    public SetSchedule(AbsSender bot) {
        this.bot = bot;
        this.adminChecker = new IsUserAdmin(bot);
    }

    public void handle(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        String chatType = message.getChat().getType();
        User user = message.getFrom();

        String key = userId + "_" + chatId;
        TempScheduleState temp = userStates.get(key);
        if (temp == null) return;

        // Xử lý nhập Group ID ở private chat
        if ("private".equals(chatType) && temp.step == -2) {
            try {
                Long groupId = Long.valueOf(text);
                if (!adminChecker.check(groupId, user)) {
                    send(chatId, "⛔ You are not an admin of this group. Please enter a valid group ID.");
                    userStates.remove(key);
                    return;
                }
                temp.record.groupId = groupId;
                // Lấy tên nhóm từ Telegram API
                String chatTitle = TelegramApiUtil.getChatTitle(bot, groupId);
                temp.chatTitle = chatTitle != null ? chatTitle : "Unknown Group";
                temp.step = 0; // Chuyển sang bước nhập môn học
                send(chatId, "✅ Group verified. Now enter the *subject* of the class:");
            } catch (NumberFormatException e) {
                send(chatId, "❌ Invalid group ID. Please enter a numeric group ID.");
            }
            return;
        }

        // Xử lý xác nhận tự động phát hiện
        if (text.equalsIgnoreCase("yes") && temp.step == -1) {
            Map<String, List<String>> extractedInfo = InfoExtractor.extractInfo(message.getText());
            if (!extractedInfo.get("Subject").isEmpty()) temp.record.subject = extractedInfo.get("Subject").get(0);
            if (!extractedInfo.get("Time").isEmpty()) temp.record.time = extractedInfo.get("Time").get(0);
            if (!extractedInfo.get("Location").isEmpty()) temp.record.location = extractedInfo.get("Location").get(0);
            temp.step = 1; // Bắt đầu từ bước kiểm tra thời gian
            if (temp.record.time == null || !DateTimeValidator.isValidDateTime(temp.record.time)) {
                send(chatId, "🕒 Please enter the *time* of the class in 'dd/MM/yyyy HH:mm' format (e.g., 05/06/2025 14:30).");
            } else if (temp.record.subject == null) {
                send(chatId, "📘 Please enter the *subject* of the class:");
            } else {
                send(chatId, "🏫 Please enter the *location* of the class:");
            }
            return;
        }

        // Hủy giữa chừng
        if (text.equalsIgnoreCase("/cancel")) {
            send(chatId, "❌ Schedule creation canceled.");
            userStates.remove(key);
            return;
        }

        // Xử lý khi từ chối ("no")
        if (text.equalsIgnoreCase("no") && temp.step == -1) {
            temp.step = 0; // Bắt đầu từ bước nhập subject
            temp.record.subject = null;
            temp.record.time = null;
            temp.record.location = null;
            // Không reset groupId để giữ nguyên nếu đã nhập
            send(chatId, "📘 Please enter the *subject* of the class:");
            return;
        }

        // Logic từng bước
        switch (temp.step) {
            case 0: // Nhập subject
                temp.record.subject = text;
                temp.step = 1;
                send(chatId, "🕒 Please enter the *time* of the class in 'dd/MM/yyyy HH:mm' format (e.g., 05/06/2025 14:30).");
                break;

            case 1: // Nhập time
                if (!DateTimeValidator.isValidDateTime(text)) {
                    send(chatId, "❌ Invalid time format. Please enter in 'dd/MM/yyyy HH:mm' format (e.g., 05/06/2025 14:30). Time must be in the future.");
                    return;
                }
                temp.record.time = text;
                temp.step = 2;
                send(chatId, "🏫 Please enter the *location* of the class:");
                break;

            case 2: // Nhập location
                temp.record.location = text;

                String scheduleId = scheduleManager.addSchedule(
                    temp.record.subject,
                    temp.record.time,
                    temp.record.location,
                    temp.record.groupId
                );

                ScheduleSaver.save(
                    temp.record.subject,
                    temp.record.time,
                    temp.record.location,
                    String.valueOf(temp.record.groupId),
                    temp.chatTitle,
                    scheduleId
                );

                send(chatId, "✅ Schedule created successfully:\n\n"
                    + "📘 Subject: " + temp.record.subject + "\n"
                    + "🕒 Time: " + temp.record.time + "\n"
                    + "🏫 Location: " + temp.record.location + "\n"
                    + "📍 Group ID: " + temp.record.groupId + "\n\n"
                    + "Members can confirm with /confirm " + scheduleId);

                userStates.remove(key);
                break;

            default:
                send(chatId, "❌ Invalid step.");
                break;
        }
    }

    public void start(Long chatId, Long userId, String chatType, Message message) {
        String key = userId + "_" + chatId;
        Long groupId = "private".equals(chatType) ? null : chatId;

        if (groupId != null && !adminChecker.isAdmin(message)) {
            send(chatId, "⛔ Only group admins can create schedules.");
            return;
        }

        String groupTitle = message.getChat().getTitle(); // Sẽ là null trong private chat
        ScheduleRecord record = new ScheduleRecord(null, null, null, null, groupId);
        TempScheduleState temp = new TempScheduleState(record, -1, groupTitle); // -1 cho trạng thái tự động phát hiện

        if ("private".equals(chatType)) {
            temp.step = -2; // Trạng thái chờ ID nhóm
            send(chatId, "🔍 Please enter the *Group ID* where you want to create the schedule. You can find it in the group settings.");
        } else {
            temp.step = 0; // Trong nhóm, bắt đầu từ nhập môn học
            send(chatId, "📘 Please enter the *subject* of the class:");
        }

        userStates.put(key, temp);
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true);
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }

    private static class TempScheduleState {
        public final ScheduleRecord record;
        public int step;
        public String chatTitle;

        public TempScheduleState(ScheduleRecord record, int step, String chatTitle) {
            this.record = record;
            this.step = step;
            this.chatTitle = chatTitle;
        }
    }

    public static boolean containsUserState(String key) {
        return userStates.containsKey(key);
    }
}