package com.project1.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ScheduleManager {
    private static final ScheduleManager INSTANCE = new ScheduleManager();
    private final Map<String, ScheduleRecord> schedules = new ConcurrentHashMap<>(); // id -> record, đảm bảo đa luồng an toàn
    private final AtomicInteger counter = new AtomicInteger(0); // Counter nguyên tử để tạo ID duy nhất

    // Constructor private để ngăn tạo instance mới
    private ScheduleManager() {
        // Khởi tạo Timer để dọn dẹp định kỳ (mỗi 1 giờ)
        Timer cleanupTimer = new Timer(true);
        cleanupTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                cleanupPastSchedules();
            }
        }, 0, 60 * 60 * 1000); // Chạy mỗi giờ
    }

    // Phương thức lấy instance singleton
    public static ScheduleManager getInstance() {
        return INSTANCE;
    }

    public String addSchedule(String subject, String time, String endTime, String location, Long groupId) {
        String id = generateShortId();
        ScheduleRecord record = new ScheduleRecord(id, subject, time, endTime, location, groupId);
        schedules.put(id, record);
        return id;
    }

    private String generateShortId() {
        int sequence = counter.getAndIncrement(); // Lấy và tăng counter
        int random = (int) (Math.random() * 100); // Số ngẫu nhiên từ 0-99
        return String.format("SCH%05d%02d", sequence % 100000, random);
    }

    public boolean confirm(String scheduleId, Long userId) {
        ScheduleRecord record = schedules.get(scheduleId);
        if (record == null) return false;
        return record.confirmUser(userId);
    }

    public ScheduleRecord get(String id) {
        return schedules.get(id);
    }

    public boolean remove(String id) {
        return schedules.remove(id) != null;
    }

    public Map<String, ScheduleRecord> getSchedulesByGroup(Long groupId) {
        cleanupPastSchedules(); // Dọn dẹp trước khi trả về danh sách
        Map<String, ScheduleRecord> result = new HashMap<>();
        for (Map.Entry<String, ScheduleRecord> entry : schedules.entrySet()) {
            if (entry.getValue().getGroupId().equals(groupId)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, ScheduleRecord> getAll() {
        cleanupPastSchedules(); // Dọn dẹp trước khi trả về danh sách
        return new HashMap<>(schedules); // Trả về bản sao để tránh sửa đổi trực tiếp
    }

    public boolean existsSchedule(String scheduleId) {
        return schedules.containsKey(scheduleId);
    }

    // Phương thức tùy chọn để cập nhật lịch
    public boolean updateSchedule(String scheduleId, String subject, String time, String endTime, String location, Long groupId) {
        ScheduleRecord record = schedules.get(scheduleId);
        if (record == null) return false;
        record.setSubject(subject);
        record.setTime(time);
        record.setEndTime(endTime);
        record.setLocation(location);
        record.setGroupId(groupId);
        return true;
    }

    // Phương thức để lên lịch thông báo cho một sự kiện với tùy chọn bỏ qua thông báo 15'
    public void scheduleNotifications(String scheduleId, Long chatId, Timer timer, Consumer<String> sendCallback, boolean skip15MinReminder) {
        ScheduleRecord record = schedules.get(scheduleId);
        if (record == null) {
            System.out.println("DEBUG: Schedule record not found for scheduleId=" + scheduleId);
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime startTime = ZonedDateTime.parse(record.getTime(), formatter);
        ZonedDateTime endTime = ZonedDateTime.parse(record.getEndTime(), formatter);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        long startMillis = startTime.toInstant().toEpochMilli();
        long endMillis = endTime.toInstant().toEpochMilli();
        long nowMillis = now.toInstant().toEpochMilli();

        System.out.println("DEBUG: Scheduling for scheduleId=" + scheduleId + ", chatId=" + chatId + ", now=" + now);
        System.out.println("DEBUG: startTime=" + startTime + ", endTime=" + endTime + ", nowMillis=" + nowMillis);

        // Thông báo 15 phút trước (chỉ nếu không bỏ qua và thời gian còn lại)
        long fifteenMinBefore = startMillis - (15 * 60 * 1000);
        if (!skip15MinReminder && fifteenMinBefore > nowMillis) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("DEBUG: Sending 15min reminder for scheduleId=" + scheduleId + ", chatId=" + chatId);
                    sendCallback.accept("⏰ Reminder: " + record.getSubject() + " starts in 15 minutes! 🕒 " +
                            "Time: " + record.getTime() + ", Location: " + record.getLocation());
                }
            }, new java.util.Date(fifteenMinBefore));
        }

        // Thông báo khi bắt đầu (gửi ngay nếu đang diễn ra, hoặc lên lịch)
        if (startMillis <= nowMillis && nowMillis <= endMillis) {
            System.out.println("DEBUG: Sending start notification immediately for scheduleId=" + scheduleId + ", chatId=" + chatId);
            sendCallback.accept("🎉 Event Started: " + record.getSubject() + " has begun! 🕒 " +
                    "Time: " + record.getTime() + ", Location: " + record.getLocation());
        } else if (startMillis > nowMillis) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("DEBUG: Sending start notification for scheduleId=" + scheduleId + ", chatId=" + chatId);
                    sendCallback.accept("🎉 Event Started: " + record.getSubject() + " has begun! 🕒 " +
                            "Time: " + record.getTime() + ", Location: " + record.getLocation());
                }
            }, new java.util.Date(startMillis));
        }

        // Thông báo khi kết thúc
        if (endMillis > nowMillis) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    System.out.println("DEBUG: Sending end notification for scheduleId=" + scheduleId + ", chatId=" + chatId);
                    sendCallback.accept("🏁 Event Ended: " + record.getSubject() + " has ended. ⏰ " +
                            "Time: " + record.getEndTime() + ", Location: " + record.getLocation());
                }
            }, new java.util.Date(endMillis));
        } else if (nowMillis > endMillis) {
            System.out.println("DEBUG: Sending end notification immediately for scheduleId=" + scheduleId + ", chatId=" + chatId);
            sendCallback.accept("🏁 Event Ended: " + record.getSubject() + " has ended. ⏰ " +
                    "Time: " + record.getEndTime() + ", Location: " + record.getLocation());
        }
    }

    // Phương thức để lên lịch thông báo cho tất cả sự kiện trong nhóm
    public void scheduleAllNotifications(Long chatId, Timer timer, Consumer<String> sendCallback) {
        Map<String, ScheduleRecord> groupSchedules = getSchedulesByGroup(chatId);
        for (Map.Entry<String, ScheduleRecord> entry : groupSchedules.entrySet()) {
            scheduleNotifications(entry.getKey(), chatId, timer, sendCallback, false); // Mặc định không bỏ qua 15'
        }
    }

    // Phương thức dọn dẹp lịch đã kết thúc
    private void cleanupPastSchedules() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        schedules.entrySet().removeIf(entry -> {
            ScheduleRecord record = entry.getValue();
            try {
                ZonedDateTime endTime = ZonedDateTime.parse(record.getEndTime(), formatter);
                return endTime.isBefore(now);
            } catch (Exception e) {
                System.err.println("Error parsing endTime for schedule " + record.getId() + ": " + e.getMessage());
                return false; // Giữ lại nếu không parse được
            }
        });
    }
}