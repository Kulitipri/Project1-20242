package com.project1.command;

import java.util.HashMap;
import java.util.Map;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.util.IsUserAdmin;

public class SetSchedule {

    public static final Map<Long, ScheduleState> userStates = new HashMap<>();

    private final AbsSender bot;
    private final IsUserAdmin adminChecker;

    public SetSchedule(AbsSender bot) {
        this.bot = bot;
        this.adminChecker = new IsUserAdmin(bot);
    }

    public void handle(Message message) {
    Long userId = message.getFrom().getId();
    Long chatId = message.getChatId();
    String text = message.getText().trim();
    String chatType = message.getChat().getType(); // "private", "group", "supergroup"
    User user = message.getFrom();

    ScheduleState state = userStates.get(userId);
    if (state == null) return; // Không bắt đầu bằng /set_schedule thì bỏ qua

    // MỚI THÊM: Nếu user đang ở trạng thái private chat (chưa có targetGroup)
    // nhưng lại gửi tin nhắn trong group thì bỏ qua luôn
    if (chatType.equals("group") || chatType.equals("supergroup")) {
        if (!state.hasTargetGroup()) {
            // đang nhập group ID ở private chat, bỏ qua tin nhắn group này
            return;
        }
    }

    // STEP 1: Nhập group ID nếu trong private chat
    if (chatType.equals("private") && !state.hasTargetGroup()) {
        try {
            if (!adminChecker.check(Long.valueOf(text), user)) {
                send(chatId, "⛔ You are not an admin in this group. Cannot continue.");
                userStates.remove(userId);
                return;
            }
            state.setTargetGroup(text);
            send(chatId, "✅ Group verified. Now enter the *subject* of the class:");
        } catch (NumberFormatException e) {
            send(chatId, "❌ Invalid group ID format. Please enter a numeric group ID.");
        }
        return;
    }

    // STEP 2: Nếu chưa có subject → nhập subject
    if (!state.hasSubject()) {
        state.setSubject(text);
        send(chatId, "🕒 Please enter the *time* of the class:");
        return;
    }

    // STEP 3: Nếu chưa có time → nhập time
    if (!state.hasTime()) {
        state.setTime(text);
        send(chatId, "🏫 Please enter the *location* of the class:");
        return;
    }

    // STEP 4: Nếu chưa có location → nhập location
    if (!state.hasLocation()) {
        state.setLocation(text);

        // ✅ Xác nhận hoàn tất
        send(chatId, "✅ Schedule created successfully:\n\n"
                + "📘 Subject: " + state.subject + "\n"
                + "🕒 Time: " + state.time + "\n"
                + "🏫 Location: " + state.location + "\n"
                + "📍 Group ID: " + state.getTargetGroupId());

        // TODO: Ghi vào Airtable nếu cần

        userStates.remove(userId); // kết thúc
    }
}


    public void start(Long chatId, Long userId, String chatType, Message message) {
        ScheduleState state = new ScheduleState();

        // Nếu trong group → kiểm tra admin
        if (!chatType.equals("private")) {
            if (!adminChecker.isAdmin(message)) {
                send(chatId, "⛔ Only group admins can create schedules.");
                return;
            }
            state.setTargetGroup(chatId.toString());
            send(chatId, "📘 Please enter the *subject* of the class:");
        } else {
            send(chatId, "🆔 Please enter the *group ID* you want to schedule for:");
        }

        userStates.put(userId, state);
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

    // Schedule state
    public static class ScheduleState {
        private String subject;
        private String time;
        private String location;
        private String targetGroupId;

        public void setSubject(String s) { this.subject = s; }
        public void setTime(String t) { this.time = t; }
        public void setLocation(String l) { this.location = l; }
        public void setTargetGroup(String g) { this.targetGroupId = g; }

        public boolean hasSubject() { return subject != null; }
        public boolean hasTime() { return time != null; }
        public boolean hasLocation() { return location != null; }
        public boolean hasTargetGroup() { return targetGroupId != null; }

        public String getTargetGroupId() { return targetGroupId; }
    }
}
