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

    public static final Map<String, ScheduleState> userStates = new HashMap<>();

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
        String chatType = message.getChat().getType();
        User user = message.getFrom();

        String key = userId + "_" + chatId;
        ScheduleState state = userStates.get(key);
        if (state == null) return;

        if ((chatType.equals("group") || chatType.equals("supergroup")) && !state.hasTargetGroup()) {
            return;
        }

        if (chatType.equals("private") && !state.hasTargetGroup()) {
            try {
                Long groupId = Long.valueOf(text);
                if (!adminChecker.check(groupId, user)) {
                    send(chatId, "⛔ You are not an admin in this group.");
                    userStates.remove(key);
                    return;
                }
                state.setTargetGroup(groupId.toString());
                send(chatId, "✅ Group verified. Now enter the *subject* of the class:");
            } catch (NumberFormatException e) {
                send(chatId, "❌ Invalid group ID format. Please enter a numeric group ID.");
            }
            return;
        }

        if (!state.hasSubject()) {
            state.setSubject(text);
            send(chatId, "🕒 Please enter the *time* of the class:");
            return;
        }

        if (!state.hasTime()) {
            state.setTime(text);
            send(chatId, "🏫 Please enter the *location* of the class:");
            return;
        }

        if (!state.hasLocation()) {
            state.setLocation(text);
            send(chatId, "✅ Schedule created successfully:\n\n"
                    + "📘 Subject: " + state.subject + "\n"
                    + "🕒 Time: " + state.time + "\n"
                    + "🏫 Location: " + state.location + "\n"
                    + "📍 Group ID: " + state.getTargetGroupId());
            userStates.remove(key);
        }
    }

    public void start(Long chatId, Long userId, String chatType, Message message) {
        String key = userId + "_" + chatId;
        ScheduleState state = new ScheduleState();

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

        userStates.put(key, state);
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
