package com.project1.AirTable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.project1.config.BotConfig;

public class AirtableClient {
    private static final String API_KEY = BotConfig.getAirtableToken(); // Thay bằng API key của bạn
    private static final String BASE_ID = BotConfig.getAirtableBaseId(); // Thay bằng Base ID của bạn
    private static final String TABLE_NAME = BotConfig.getScheduleTableName();
    private static final String API_URL = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;

    public List<Map<String, Object>> getSchedulesByGroupId(String groupId) {
        try {
            // Debug logs to verify API key and base ID
            if (API_KEY == null || API_KEY.isEmpty()) {
                System.err.println("Error: API_KEY is null or empty.");
                return new ArrayList<>();
            }
            if (BASE_ID == null || BASE_ID.isEmpty()) {
                System.err.println("Error: BASE_ID is null or empty.");
                return new ArrayList<>();
            }

            String urlString = API_URL + "?filterByFormula=groupId='" + groupId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization",API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching schedules: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return new ArrayList<>(); // Return an empty list instead of null
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");

            if (records == null) {
                System.err.println("No records found for groupId: " + groupId);
                conn.disconnect();
                return new ArrayList<>(); // Return an empty list if no records are found
            }

            List<Map<String, Object>> result = parseRecords(records);
            conn.disconnect();
            return result;
        } catch (IOException e) {
            System.err.println("Error fetching schedules: " + e.getMessage());
            return new ArrayList<>(); // Return an empty list in case of an exception
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private List<Map<String, Object>> parseRecords(JSONArray records) {
        List<Map<String, Object>> parsedRecords = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.getJSONObject(i);
            Map<String, Object> recordMap = record.toMap();
            parsedRecords.add(recordMap);
        }
        return parsedRecords;
    }

    public void saveConfirmation(String scheduleId, String userId, String userName, String groupId) {
        try {
            // Kiểm tra trước khi lưu
            if (isAlreadyConfirmed(scheduleId, userId)) {
                System.out.println("User " + userId + " has already confirmed schedule " + scheduleId + ". Skipping save.");
                return;
            }

            String confirmationTable = BotConfig.getConfirmationTableName();
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable;
            
            JSONObject fields = new JSONObject();
            fields.put("scheduleId", scheduleId);
            fields.put("userId", userId);
            fields.put("userName", userName);
            fields.put("groupId", groupId); // Thêm groupId vào fields

            JSONObject record = new JSONObject();
            record.put("fields", fields);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = record.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
                throw new IOException("Unexpected code " + responseCode + " - " + conn.getResponseMessage());
            }

            System.out.println("Confirmation saved successfully.");
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Error saving confirmation: " + e.getMessage());
        }
    }

    public boolean isAlreadyConfirmed(String scheduleId, String userId) {
        try {
            String confirmationTable = BotConfig.getConfirmationTableName();
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable +
                    "?filterByFormula=AND({ScheduleId}='" + scheduleId + "',{UserId}='" + userId + "')" ;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error checking confirmation: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return false;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.getJSONArray("records");

            boolean result = records.length() > 0;
            if (result) {
                System.out.println("User " + userId + " has already confirmed schedule " + scheduleId);
            }
            conn.disconnect();
            return result;
        } catch (IOException e) {
            System.err.println("Error checking confirmation: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getConfirmationsByUserId(String userId) {
        try {
            String confirmationTable = BotConfig.getConfirmationTableName();
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable + "?filterByFormula=UserId='" + userId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching confirmations: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return new ArrayList<>();
            }

            String responseBody = readResponse(conn);
            org.json.JSONObject json = new org.json.JSONObject(responseBody);
            org.json.JSONArray records = json.optJSONArray("records");

            List<Map<String, Object>> result = new ArrayList<>();
            if (records != null) {
                for (int i = 0; i < records.length(); i++) {
                    org.json.JSONObject record = records.getJSONObject(i);
                    Map<String, Object> recordMap = record.toMap();
                    result.add(recordMap);
                }
            }
            conn.disconnect();
            return result;
        } catch (IOException e) {
            System.err.println("Error fetching confirmations: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Lấy danh sách lịch học theo nhiều ScheduleId từ Airtable.
     */
    public List<Map<String, Object>> getSchedulesByIds(Set<String> scheduleIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (scheduleIds == null || scheduleIds.isEmpty()) return result;

        try {
            // Xây dựng filterByFormula dạng OR(ScheduleId='SCH1',ScheduleId='SCH2',...)
            StringBuilder filter = new StringBuilder("OR(");
            boolean first = true;
            for (String id : scheduleIds) {
                if (!first) filter.append(",");
                filter.append("ScheduleId='").append(id).append("'");
                first = false;
            }
            filter.append(")");

            String urlString = API_URL + "?filterByFormula=" + java.net.URLEncoder.encode(filter.toString(), "UTF-8");
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching schedules by ids: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return result;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");

            if (records != null) {
                result = parseRecords(records);
            }
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Error fetching schedules by ids: " + e.getMessage());
        }
        return result;
    }

    /**
     * Xóa xác nhận của một user cho một schedule trong bảng confirmation.
     */
    public void deleteConfirmation(String scheduleId, String userId) {
        try {
            String confirmationTable = BotConfig.getConfirmationTableName();
            // Lấy recordId của xác nhận cần xóa
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable +
                                    "?filterByFormula=AND({ScheduleId}='" + scheduleId + "',{UserId}='" + userId + "')";
                            URL url = new URL(urlString);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                            conn.setRequestMethod("GET");
                            conn.setRequestProperty("Authorization", API_KEY);
                            conn.setRequestProperty("Content-Type", "application/json");
                
                            int responseCode = conn.getResponseCode();
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                System.err.println("Error fetching confirmation record: HTTP " + responseCode + " - " + conn.getResponseMessage());
                                conn.disconnect();
                                return;
                            }
                
                            String responseBody = readResponse(conn);
                            JSONObject json = new JSONObject(responseBody);
                            JSONArray records = json.optJSONArray("records");
                
                            if (records != null && records.length() > 0) {
                                String recordId = records.getJSONObject(0).getString("id");
                                String deleteUrlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable + "/" + recordId;
                                URL deleteUrl = new URL(deleteUrlString);
                                HttpURLConnection deleteConn = (HttpURLConnection) deleteUrl.openConnection();
                
                                deleteConn.setRequestMethod("DELETE");
                                deleteConn.setRequestProperty("Authorization", API_KEY);
                
                                int deleteResponseCode = deleteConn.getResponseCode();
                                if (deleteResponseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                                    System.out.println("Confirmation deleted successfully.");
                                } else {
                                    System.err.println("Error deleting confirmation: HTTP " + deleteResponseCode + " - " + deleteConn.getResponseMessage());
                                }
                                deleteConn.disconnect();
                            } else {
                                System.err.println("No confirmation record found for ScheduleId: " + scheduleId + " and UserId: " + userId);
                            }
                            conn.disconnect();
                        } catch (IOException e) {
                            System.err.println("Error deleting confirmation: " + e.getMessage());
                        }
                    }

    /**
     * Xóa một schedule khỏi bảng schedule trên Airtable theo ScheduleId.
     */
    public void deleteSchedule(String scheduleId) {
        try {
            // Lấy recordId của schedule cần xóa
            String urlString = API_URL + "?filterByFormula={ScheduleId}='" + scheduleId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching schedule record for delete: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");

            if (records != null && records.length() > 0) {
                for (int i = 0; i < records.length(); i++) {
                    String recordId = records.getJSONObject(i).getString("id");
                    String deleteUrl = API_URL + "/" + recordId;
                    HttpURLConnection deleteConn = (HttpURLConnection) new URL(deleteUrl).openConnection();
                    deleteConn.setRequestMethod("DELETE");
                    deleteConn.setRequestProperty("Authorization", API_KEY);

                    int deleteResponse = deleteConn.getResponseCode();
                    if (deleteResponse != HttpURLConnection.HTTP_NO_CONTENT && deleteResponse != HttpURLConnection.HTTP_OK) {
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

    /**
     * Xóa tất cả xác nhận liên quan đến một scheduleId trong bảng confirmation.
     */
    public void deleteConfirmationsByScheduleId(String scheduleId) {
        try {
            String confirmationTable = BotConfig.getConfirmationTableName();
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable +
                    "?filterByFormula={ScheduleId}='" + scheduleId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Error fetching confirmations for delete: HTTP " + responseCode + " - " + conn.getResponseMessage());
                conn.disconnect();
                return;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");

            if (records != null && records.length() > 0) {
                for (int i = 0; i < records.length(); i++) {
                    String recordId = records.getJSONObject(i).getString("id");
                    String deleteUrl = "https://api.airtable.com/v0/" + BASE_ID + "/" + confirmationTable + "/" + recordId;
                    HttpURLConnection deleteConn = (HttpURLConnection) new URL(deleteUrl).openConnection();
                    deleteConn.setRequestMethod("DELETE");
                    deleteConn.setRequestProperty("Authorization", API_KEY);

                    int deleteResponse = deleteConn.getResponseCode();
                    if (deleteResponse != HttpURLConnection.HTTP_NO_CONTENT && deleteResponse != HttpURLConnection.HTTP_OK) {
                        System.err.println("Error deleting confirmation: HTTP " + deleteResponse + " - " + deleteConn.getResponseMessage());
                    } else {
                        System.out.println("Confirmation deleted successfully: " + recordId);
                    }
                    deleteConn.disconnect();
                }
            } else {
                System.err.println("No confirmation records found to delete for ScheduleId: " + scheduleId);
            }
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Error deleting confirmations by scheduleId: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem một schedule có tồn tại trên Airtable không (theo ScheduleId).
     */
    public boolean scheduleExists(String scheduleId) {
        try {
            String urlString = API_URL + "?filterByFormula={ScheduleId}='" + scheduleId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return false;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");
            conn.disconnect();
            return records != null && records.length() > 0;
        } catch (IOException e) {
            System.err.println("Error checking schedule exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy groupId của một schedule từ Airtable theo ScheduleId.
     */
    public String getGroupIdByScheduleId(String scheduleId) {
        try {
            String urlString = API_URL + "?filterByFormula={ScheduleId}='" + scheduleId + "'";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                return null;
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.optJSONArray("records");
            if (records != null && records.length() > 0) {
                JSONObject fields = records.getJSONObject(0).optJSONObject("fields");
                if (fields != null && fields.has("GroupId")) {
                    return fields.getString("GroupId");
                }
            }
            conn.disconnect();
        } catch (IOException e) {
            System.err.println("Error getting groupId by scheduleId: " + e.getMessage());
        }
        return null;
    }
}