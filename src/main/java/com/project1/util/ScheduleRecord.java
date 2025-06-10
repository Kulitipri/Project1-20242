package com.project1.util;

import java.util.HashSet;
import java.util.Set;

public class ScheduleRecord {
    public final String id;               // Mã lịch học duy nhất, ví dụ: "SCH1234"
    public String subject;
    public String time;                   // Start time
    public String endTime;                // End time
    public String location;
    public Long groupId;
    public String chatTitle;             // Tên nhóm Telegram
    public final Set<Long> confirmedUsers = new HashSet<>(); // Danh sách user đã xác nhận
    public final Long creatorId;          // ID của người tạo lịch

    // Constructor chính với endTime và creatorId
    public ScheduleRecord(String id, String subject, String time, String endTime, String location, Long groupId, Long creatorId) {
        this.id = id;
        this.subject = subject;
        this.time = time;
        this.endTime = endTime;
        this.location = location;
        this.groupId = groupId;
        this.creatorId = creatorId;
    }

    // Constructor đầy đủ (có cả chatTitle) với endTime và creatorId
    public ScheduleRecord(String id, String subject, String time, String endTime, String location, Long groupId, String chatTitle, Long creatorId) {
        this(id, subject, time, endTime, location, groupId, creatorId);
        this.chatTitle = chatTitle;
    }

    // ==== Getter ====
    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getTime() { return time; }
    public String getEndTime() { return endTime; }
    public String getLocation() { return location; }
    public Long getGroupId() { return groupId; }
    public String getChatTitle() { return chatTitle; }
    public Long getCreatorId() { return creatorId; }

    // ==== Setter ====
    public void setSubject(String subject) { this.subject = subject; }
    public void setTime(String time) { this.time = time; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setLocation(String location) { this.location = location; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public void setChatTitle(String chatTitle) { this.chatTitle = chatTitle; }

    // ==== Confirm logic ====
    // Xoá logic xác nhận trên RAM
    // public boolean confirmUser(Long userId) {
    //     return confirmedUsers.add(userId); // Trả về true nếu là người mới
    // }
    // public boolean isConfirmed(Long userId) {
    //     return confirmedUsers.contains(userId);
    // }
    // public boolean addConfirmedUser(Long userId) {
    //     return confirmedUsers.add(userId);
    // }
}