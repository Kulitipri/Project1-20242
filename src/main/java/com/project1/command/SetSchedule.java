package com.project1.command;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.ScheduleSaver;
import com.project1.util.InfoExtractor;
import com.project1.util.IsUserAdmin;
import com.project1.util.ScheduleManager;
import com.project1.util.ScheduleRecord;

public class SetSchedule {

    private static final Map<String, TempScheduleState> userStates = new ConcurrentHashMap<>();
    private final AbsSender bot;
    private final IsUserAdmin adminChecker;
    private final ScheduleManager scheduleManager = new ScheduleManager();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    static {
        DATE_FORMAT.setLenient(false); // Không cho phép định dạng linh hoạt
    }

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

        // Nếu đang nhập Group ID ở private chat
        if ("private".equals(chatType) && temp.record.groupId == null) {
            try {
                Long groupId = Long.valueOf(text);
                if (!adminChecker.check(groupId, user)) {
                    send(chatId, "⛔ You are not an admin of this group. Please enter a valid group ID.");
                    userStates.remove(key);
                    return;
                }
                temp.record.groupId = groupId;
                send(chatId, "✅ Group verified. Now enter the *subject* of the class:");
            } catch (NumberFormatException e) {
                send(chatId, "❌ Invalid group ID. Please enter a numeric group ID.");
            }
            return;
        }

        // Xử lý xác nhận tự động phát hiện
        if (text.equalsIgnoreCase("yes") && temp.step == 0) {
            Map<String, List<String>> extractedInfo = InfoExtractor.extractInfo(message.getText());
            if (!extractedInfo.get("Subject").isEmpty()) temp.record.subject = extractedInfo.get("Subject").get(0);
            if (!extractedInfo.get("Time").isEmpty()) temp.record.time = extractedInfo.get("Time").get(0);
            if (!extractedInfo.get("Location").isEmpty()) temp.record.location = extractedInfo.get("Location").get(0);
            temp.step = 2; // Bỏ qua nếu đã có thời gian và môn học
            if (temp.record.time == null || !isValidDateTime(temp.record.time)) {
                send(chatId, "🕒 Please enter the *time* of the class in 'dd/MM/yyyy HH:mm' format (e.g., 01/06/2025 14:30).");
                temp.step = 1;
            } else if (temp.record.subject == null) {
                send(chatId, "📘 Please enter the *subject* of the class:");
                temp.step = 0;
            } else {
                send(chatId, "🏫 Please enter the *location* of the class (or confirm with any text if already set):");
            }
            return;
        }

        // Hủy giữa chừng
        if (text.equalsIgnoreCase("/cancel")) {
            send(chatId, "❌ Schedule creation canceled.");
            userStates.remove(key);
            return;
        }

        // Logic từng bước
        switch (temp.step) {
            case 0:
                temp.record.subject = text;
                temp.step++;
                send(chatId, "🕒 Please enter the *time* of the class:");
                break;
            case 1:
                if (!isValidDateTime(text)) {
                    send(chatId, "❌ Invalid time format. Please enter in 'dd/MM/yyyy HH:mm' format (e.g., 01/06/2025 14:30).");
                    return;
                }
                temp.record.time = text;
                temp.step++;
                send(chatId, "🏫 Please enter the *location* of the class:");
                break;
            case 2:
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
                    temp.chatTitle != null ? temp.chatTitle : "N/A",
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

        String groupTitle = message.getChat().getTitle(); // Sẽ là null trong private chat, được xử lý sau
        ScheduleRecord record = new ScheduleRecord(null, null, null, null, groupId);
        TempScheduleState temp = new TempScheduleState(record, 0, groupTitle);
        userStates.put(key, temp);

        // Tự động phát hiện thông tin từ tin nhắn
        String text = message.getText() != null ? message.getText().trim() : "";
        Map<String, List<String>> extractedInfo = InfoExtractor.extractInfo(text);

        if (!extractedInfo.get("Subject").isEmpty() || !extractedInfo.get("Time").isEmpty() || !extractedInfo.get("Location").isEmpty()) {
            StringBuilder prompt = new StringBuilder("📋 Detected schedule info:\n");
            if (!extractedInfo.get("Subject").isEmpty()) prompt.append("Subject: ").append(extractedInfo.get("Subject").get(0)).append("\n");
            if (!extractedInfo.get("Time").isEmpty()) prompt.append("Time: ").append(extractedInfo.get("Time").get(0)).append("\n");
            if (!extractedInfo.get("Location").isEmpty()) prompt.append("Location: ").append(extractedInfo.get("Location").get(0)).append("\n");
            prompt.append("\nDo you want to add this schedule? Reply with 'yes' or 'no'.");
            send(chatId, prompt.toString());
        } else {
            if ("private".equals(chatType)) {
                send(chatId, "🆔 Please enter the *group ID* you want to schedule for:");
            } else {
                send(chatId, "📘 Please enter the *subject* of the class:");
            }
        }
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

    private boolean isValidDateTime(String dateTime) {
        try {
            DATE_FORMAT.parse(dateTime);
            return true;
        } catch (ParseException e) {
            return false;
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