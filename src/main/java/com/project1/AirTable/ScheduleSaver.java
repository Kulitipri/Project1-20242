package com.project1.AirTable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.project1.config.BotConfig;

public class ScheduleSaver {

    private static final String BASE_ID = BotConfig.getAirtableBaseId();
    private static final String TABLE_NAME = BotConfig.getScheduleTableName();
    private static final String API_KEY = BotConfig.getAirtableToken();

    public static void save(String subject, String time, String endTime, String location, String groupId, String groupName, String scheduleId) {
        try {
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String subjectStr = String.join(", ", subject);
            String timeStr = String.join(", ", time);
            String endTimeStr = String.join(", ", endTime); // Xử lý endTime tương tự time
            String locationStr = String.join(", ", location);

            String json = String.format(
                "{ \"fields\": { " +
                "\"GroupId\": \"%s\", " +
                "\"GroupName\": \"%s\", " +
                "\"ScheduleId\": \"%s\", " +
                "\"Subject\": \"%s\", " +
                "\"Time\": \"%s\", " +
                "\"EndTime\": \"%s\", " + // Thêm EndTime
                "\"Location\": \"%s\" " +
                "} }",
                escapeJson(groupId),
                escapeJson(groupName),
                escapeJson(scheduleId),
                escapeJson(subjectStr),
                escapeJson(timeStr),
                escapeJson(endTimeStr), // Thêm endTimeStr
                escapeJson(locationStr)
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                System.err.println("Failed to save schedule. Response code: " + responseCode);
            } else {
                System.out.println("Schedule saved to Airtable.");
            }

        } catch (IOException e) {
            System.err.println("Error saving schedule: " + e.getMessage());
        }
    }

    private static String escapeJson(String input) {
        return input != null ? input.replace("\"", "\\\"") : "";
    }

    /**
     * Xóa một schedule khỏi bảng schedule trên Airtable theo ScheduleId.
     */
    public static void deleteByScheduleId(String scheduleId) {
        try {
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME +
                    "?filterByFormula={ScheduleId}='" + scheduleId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Error fetching schedule record for delete: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return;
            }

            String responseBody = new String(conn.getInputStream().readAllBytes());
            org.json.JSONObject json = new org.json.JSONObject(responseBody);
            org.json.JSONArray records = json.optJSONArray("records");

            if (records != null && records.length() > 0) {
                for (int i = 0; i < records.length(); i++) {
                    String recordId = records.getJSONObject(i).getString("id");
                    String deleteUrl = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME + "/" + recordId;
                    HttpURLConnection deleteConn = (HttpURLConnection) new URL(deleteUrl).openConnection();
                    deleteConn.setRequestMethod("DELETE");
                    deleteConn.setRequestProperty("Authorization", API_KEY);

                    int deleteResponse = deleteConn.getResponseCode();
                    if (deleteResponse != 200 && deleteResponse != 204) {
                        System.err.println("Error deleting schedule: HTTP " + deleteResponse + " - " + deleteConn.getResponseMessage());
                    } else {
                        System.out.println("Schedule deleted successfully: " + recordId);
                    }
                    deleteConn.disconnect();
                }
            } else {
                System.err.println("No schedule record found to delete for ScheduleId: " + scheduleId);
            }
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Error deleting schedule: " + e.getMessage());
        }
    }

    // Thêm hàm tạo scheduleId mới (nếu chưa có)
    public static String generateScheduleId() {
        return "SCH" + System.currentTimeMillis();
    }
}