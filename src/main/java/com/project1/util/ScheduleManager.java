package com.project1.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScheduleManager {
    private final Map<String, ScheduleRecord> schedules = new ConcurrentHashMap<>(); // id -> record // đảm bảo đa luồng an toàn

    public String addSchedule(String subject, String time, String location, Long groupId) {
        String id = "SCH" + System.currentTimeMillis();
        ScheduleRecord record = new ScheduleRecord(id, subject, time, location, groupId);
        schedules.put(id, record);
        return id;
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
}
