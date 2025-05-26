package com.project1.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.ScheduleSaver;
import com.project1.util.IsUserAdmin;
import com.project1.util.ScheduleManager;
import com.project1.util.ScheduleRecord;

public class SetSchedule {

    private static final Map<String, TempScheduleState> userStates = new ConcurrentHashMap<>();
    private final AbsSender bot;
    private final IsUserAdmin adminChecker;
    private final ScheduleManager scheduleManager = new ScheduleManager();

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
                send(chatId, "⛔ You are not an admin in this group.");
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

            send(chatId, "Schedule created successfully:\n\n"
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

        String groupTitle = message.getChat().getTitle(); // will be null in private, handled later
        ScheduleRecord record = new ScheduleRecord(null, null, null, null, groupId);
        TempScheduleState temp = new TempScheduleState(record, 0, groupTitle);
        userStates.put(key, temp);

        if ("private".equals(chatType)) {
            send(chatId, "🆔 Please enter the *group ID* you want to schedule for:");
        } else {
            send(chatId, "📘 Please enter the *subject* of the class:");
        }
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
