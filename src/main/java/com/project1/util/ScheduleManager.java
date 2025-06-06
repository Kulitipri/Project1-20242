package com.project1.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleManager {
    private static final ScheduleManager INSTANCE = new ScheduleManager();
    private final Map<String, ScheduleRecord> schedules = new ConcurrentHashMap<>(); // id -> record, đảm bảo đa luồng an toàn
    private final AtomicInteger counter = new AtomicInteger(0); // Counter nguyên tử để tạo ID duy nhất

    // Constructor private để ngăn tạo instance mới
    private ScheduleManager() {}

    // Phương thức lấy instance singleton
    public static ScheduleManager getInstance() {
        return INSTANCE;
    }

    public String addSchedule(String subject, String time, String location, Long groupId) {
        String id = generateShortId();
        ScheduleRecord record = new ScheduleRecord(id, subject, time, location, groupId);
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
        Map<String, ScheduleRecord> result = new HashMap<>();
        for (Map.Entry<String, ScheduleRecord> entry : schedules.entrySet()) {
            if (entry.getValue().getGroupId().equals(groupId)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public Map<String, ScheduleRecord> getAll() {
        return schedules;
    }

    public boolean existsSchedule(String scheduleId) {
        return schedules.containsKey(scheduleId);
    }
}