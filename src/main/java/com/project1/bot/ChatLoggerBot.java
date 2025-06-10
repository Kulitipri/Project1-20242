package com.project1.bot;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.polls.PollAnswer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.project1.AirTable.GroupSaver;
import com.project1.AirTable.LogSaver;
import com.project1.AirTable.ScheduleSaver;
import com.project1.command.CommandHandler;
import com.project1.command.ConfirmHandler;
import com.project1.config.BotConfig;
import com.project1.util.InfoExtractor;
import com.project1.util.IsUserAdmin;
import com.project1.util.ScheduleManager;

public class ChatLoggerBot extends TelegramLongPollingBot {

    private final LogSaver airtable = new LogSaver();
    private final Map<String, Map<String, List<String>>> pendingScheduleRequests = new ConcurrentHashMap<>();
    private final ScheduleManager scheduleManager = ScheduleManager.getInstance();
    private final ConfirmHandler confirmHandler = new ConfirmHandler(this);
    private final Map<Long, Timer> timers = new ConcurrentHashMap<>(); // Timer cho mỗi nhóm
    private final IsUserAdmin adminChecker = new IsUserAdmin(this);
    private final Map<String, Long> pollChatMap = new HashMap<>(); // Ánh xạ pollId với chatId
    private final CommandHandler commandHandler = new CommandHandler(this, pollChatMap);

    @Override
    public String getBotUsername() {
        return BotConfig.getTelegramBotname();
    }

    @Override
    public String getBotToken() {
        return BotConfig.getTelegramToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message != null && message.hasText()) {
                String text = message.getText().trim();
                String sender = message.getFrom().getFirstName();
                String username = message.getFrom().getUserName();
                Long chatId = message.getChatId();
                Long userId = message.getFrom().getId();
                String chatTitle = message.getChat().getTitle();
                String chatTypeRaw = message.getChat().getType();
                String chatType = chatTypeRaw.equals("private") ? "PRIVATE" : "GROUP";
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                String key = userId + "_" + chatId;

                System.out.println("──────────────────────────────────────────");
                System.out.println(timestamp);
                System.out.println("Chat ID: " + chatId + " (" + chatType + ")" + (chatTitle != null ? " (" + chatTitle + ")" : ""));
                System.out.println("Sender: " + sender + (username != null ? " (@" + username + ")" : ""));
                System.out.println("Message: " + text);

                // ✅ Handle confirmation nếu có lịch đang chờ xác nhận
                if (pendingScheduleRequests.containsKey(key)) {
                    if (chatType.equals("GROUP")) {
                        if (text.equalsIgnoreCase("y")) {
                            if (!adminChecker.isAdmin(message)) {
                                pendingScheduleRequests.remove(key);
                                send(chatId, "❌ Only admins can confirm schedules! ⛔");
                                return;
                            }

                            Map<String, List<String>> schedule = pendingScheduleRequests.remove(key);
                            String subject = String.join(", ", schedule.get("Subject"));
                            List<String> times = schedule.get("Time");
                            String time = times.get(0);
                            String endTime = times.size() > 1 ? times.get(1) : time;
                            String location = String.join(", ", schedule.get("Location"));

                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
                            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                            ZonedDateTime startTime;
                            ZonedDateTime endTimeZdt;
                            try {
                                startTime = ZonedDateTime.parse(time, formatter);
                                endTimeZdt = ZonedDateTime.parse(endTime, formatter);
                            } catch (Exception e) {
                                send(chatId, "❌ Error parsing time. Please ensure the format is 'dd/MM/yyyy HH:mm'.");
                                return;
                            }

                            if (startTime.isBefore(now)) {
                                send(chatId, "❌ Start time must be in the future! 🕒");
                                return;
                            }
                            if (endTimeZdt.isBefore(now)) {
                                send(chatId, "❌ End time must be in the future! ⏰");
                                return;
                            }
                            if (endTimeZdt.isBefore(startTime)) {
                                send(chatId, "❌ End time must be after start time! ⏳");
                                return;
                            }

                            String scheduleId = scheduleManager.addSchedule(subject, time, endTime, location, chatId, userId); // Thêm creatorId
                            ScheduleSaver.save(subject, time, endTime, location, chatId.toString(), chatTitle != null ? chatTitle : "Unknown Group", scheduleId);

                            // Thêm log để kiểm tra
                            System.out.println("DEBUG: Schedule created and notifications scheduled for scheduleId=" + scheduleId + ", chatId=" + chatId);

                            send(chatId, "✅ Schedule created successfully! 🎉\n" +
                                    "   📘 Subject: " + subject + "\n" +
                                    "   🕒 Start Time: " + time + "\n" +
                                    "   ⏰ End Time: " + endTime + "\n" +
                                    "   🏫 Location: " + location + "\n\n" +
                                    "👥 Members can confirm with /confirm " + scheduleId + "");

                            // Create a poll for members to vote
                            String pollQuestion = "Do you agree with the schedule?\n" +
                                    "📘 Subject: " + subject + "\n" +
                                    "🕒 Start Time: " + time + "\n" +
                                    "⏰ End Time: " + endTime + "\n" +
                                    "🏫 Location: " + location;
                            String pollId = createPoll(chatId, pollQuestion);
                            if (pollId != null) {
                                pollChatMap.put(pollId, chatId);
                                scheduleManager.mapPollToSchedule(pollId, scheduleId);
                                System.out.println("DEBUG: Poll created with pollId=" + pollId + ", chatId=" + chatId + ", scheduleId=" + scheduleId);
                            }
                            
                            // lên lịch thông báo
                            boolean skip15Min = startTime.isBefore(now.plusMinutes(15));
                            Timer groupTimer = timers.computeIfAbsent(chatId, k -> new Timer(true));
                            scheduleManager.scheduleNotifications(scheduleId, chatId, groupTimer, notificationMessage -> send(chatId, notificationMessage), skip15Min);
                            //send(chatId, "mts"); // debug đi vào huyền thoại

                            return;
                        } else if (text.equalsIgnoreCase("n")) {
                            pendingScheduleRequests.remove(key);
                            send(chatId, "❌ Schedule setup canceled! ");
                            return;
                        }
                    }
                    return;
                }

                // Xử lý lệnh /confirm
                if (text.startsWith("/confirm")) {
                    String[] parts = text.split("\\s+");
                    if (parts.length < 2) {
                        send(chatId, "❌ Please use `/confirm <schedule_id>`\nExample: /confirm SCH123456");
                        return;
                    }
                    String scheduleId = parts[1];
                    String userName = message.getFrom().getFirstName();
                    confirmHandler.handleConfirm(chatId, scheduleId, userId, userName);
                    return;
                }

                // ✅ Xử lý lệnh người dùng
                commandHandler.handleCommand(message);

                // ✅ Ghi log vào Airtable
                try {
                    airtable.addRecord(sender, userId.toString(), text, timestamp, chatType, chatTitle != null ? chatTitle : "NULL", chatId.toString());
                    System.out.println("Log saved to Airtable.");
                } catch (IOException e) {
                    System.err.println("Airtable error: " + e.getMessage());
                }

                // ✅ Lưu thông tin nhóm vào Airtable nếu là nhóm
                GroupSaver groupSaver = new GroupSaver(this);
                try {
                    if (chatType.equals("PRIVATE")) {
                        return;
                    }
                    groupSaver.saveGroup(chatId.toString(), chatTitle);
                } catch (IOException e) {
                    // Không cần thông báo lỗi
                }

                // ✅ Phân tích lịch học và yêu cầu xác nhận
                Map<String, List<String>> info = InfoExtractor.extractInfo(text);
                List<String> times = info.get("Time");
                List<String> subjects = info.get("Subject");
                List<String> locations = info.get("Location");

                if (!times.isEmpty() && !subjects.isEmpty() && !locations.isEmpty()) {
                    pendingScheduleRequests.put(key, info);

                    if (chatType.equals("PRIVATE")) {
                        send(chatId, "❌ This feature is only available in group chats! 🚫");
                        return;
                    }

                    String subject = String.join(", ", subjects);
                    String time = times.get(0);
                    String endTime = times.size() > 1 ? times.get(1) : time;
                    String location = String.join(", ", locations);

                    send(chatId, "📢 *Schedule detected!*\n" +
                            "   📘 Subject: " + subject + "\n" +
                            "   🕒 Start Time: " + time + "\n" +
                            "   ⏰ End Time: " + endTime + "\n" +
                            "   🏫 Location: " + location + "\n\n" +
                            "Reply with 'y' to confirm or 'n' to cancel.");
                }
            }
        } else if (update.hasPollAnswer()) {
            PollAnswer pollAnswer = update.getPollAnswer();
            String pollId = pollAnswer.getPollId();
            Long chatId = pollChatMap.get(pollId); // Lấy chatId từ ánh xạ

            if (chatId != null) {
                if (pollAnswer.getOptionIds().contains(0)) { // Vote "Yes"
                    send(chatId, "📢 " + pollAnswer.getUser().getFirstName() + " voted Yes for the poll! 👍");

                    // Kiểm tra type safety khi lấy scheduleId
                    Object scheduleIdObj = scheduleManager.getScheduleIdByPollId(pollId);
                    String scheduleId = null;
                    if (scheduleIdObj instanceof String) {
                        scheduleId = (String) scheduleIdObj;
                    }

                    if (scheduleId != null) {
                        Long userId = pollAnswer.getUser().getId();
                        String userName = pollAnswer.getUser().getFirstName();
                        confirmHandler.handleConfirm(chatId, scheduleId, userId, userName);
                    } else {
                        send(chatId, "❌ Unable to confirm schedule automatically. Invalid Schedule ID format.");
                    }
                } else if (pollAnswer.getOptionIds().contains(1)) { // Vote "No"
                    send(chatId, "📢 " + pollAnswer.getUser().getFirstName() + " voted No for the poll! 👎 (No confirmation will be made)");
                }
            }
        }
    }

    private String createPoll(Long chatId, String question) {
        SendPoll poll = new SendPoll();
        poll.setChatId(chatId.toString());
        poll.setQuestion(question);
        poll.setOptions(List.of("Yes", "No"));
        poll.setIsAnonymous(false);
        poll.setType("regular");

        try {
            return execute(poll).getPoll().getId(); // Retrieve pollId from the response
        } catch (TelegramApiException e) {
            System.err.println("❌ Error creating poll: " + e.getMessage());
            send(chatId, "❌ Failed to create poll. Please try again.");
            return null;
        }
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage(chatId.toString(), text);
            message.enableMarkdown(true);
            execute(message);
            System.out.println("DEBUG: Message sent to chatId=" + chatId + ": " + text);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message to chatId=" + chatId + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChatLoggerBot());
            System.out.println("Bot is running properly...");
        } catch (TelegramApiException e) {
            System.err.println("Telegram error: " + e.getMessage());
        }
    }
}