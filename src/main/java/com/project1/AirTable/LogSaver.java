package com.project1.AirTable;

// thư viện https và url
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.project1.config.BotConfig;

public class LogSaver {
    
    // khai báo final các biến của airtable api
    private static final String BASE_ID = BotConfig.getAirtableBaseId();
    private static final String TABLE_NAME = BotConfig.getLogTableName();
    private static final String API_KEY = BotConfig.getAirtableToken();

    // method thêm log vào airtable
    public void addRecord(String sender,String userId ,String message, String time, String chatType, String chatTitle, String chatId) throws IOException {
        // Kiểm tra và xóa bản ghi trước khi thêm mới
        checkAndDeleteRecords();

        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // thiết lập các thuộc tính của connection
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // tạo json pack để gửi lên airtable
        // sử dụng String.format để định dạng json
        String json = String.format(
            "{ \"fields\": { " +
                "\"UserName\": \"%s\", " +
                "\"UserId\": \"%s\", " +
                "\"Message\": \"%s\", " +
                "\"Time\": \"%s\", " +
                "\"ChatType\": \"%s\", " +
                "\"GroupName\": \"%s\", " +
                "\"GroupId\": \"%s\" " +
            "} }",
            escapeJson(sender),
            escapeJson(userId),
            escapeJson(message),
            escapeJson(time),
            escapeJson(chatType),
            escapeJson(chatTitle),
            escapeJson(chatId)
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }
        
        // kiểm tra mã phản hồi lỗi từ airtable
        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            throw new IOException("Airtable API returned error code: " + responseCode);
        }
    }

    // Phương thức kiểm tra và xóa bản ghi khi vượt 500
    private void checkAndDeleteRecords() throws IOException {
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Thiết lập request để lấy danh sách bản ghi
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");

        // Đọc phản hồi từ Airtable
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // Phân tích JSON thủ công (giả định phản hồi chứa "records")
            String jsonResponse = response.toString();
            int recordCount = countRecords(jsonResponse); // Đếm số bản ghi

            if (recordCount >= 500) {
                deleteOldestRecords(200); // Xóa 200 bản ghi cũ nhất
            }
        } else {
            throw new IOException("Failed to fetch records. Error code: " + responseCode);
        }
    }

    // Đếm số bản ghi từ JSON phản hồi
    private int countRecords(String jsonResponse) {
        // Giả định JSON có cấu trúc {"records": [{...}, {...}, ...]}
        int count = 0;
        int index = jsonResponse.indexOf("\"records\":");
        if (index != -1) {
            index = jsonResponse.indexOf("[", index);
            if (index != -1) {
                int bracketCount = 1;
                int i = index + 1;
                while (i < jsonResponse.length() && bracketCount > 0) {
                    if (jsonResponse.charAt(i) == '{') bracketCount++;
                    else if (jsonResponse.charAt(i) == '}') bracketCount--;
                    i++;
                }
                String recordsPart = jsonResponse.substring(index + 1, i - 1);
                count = recordsPart.split("\\{").length - 1; // Đếm số object trong mảng
            }
        }
        return count;
    }

    // Xóa 500 bản ghi cũ nhất dựa trên trường Time
    private void deleteOldestRecords(int numberToDelete) throws IOException {
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Lấy tất cả bản ghi để sắp xếp theo Time
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // Lấy danh sách ID của các bản ghi (giả định đơn giản, cần thư viện JSON thực tế)
            List<String> recordIds = extractRecordIds(response.toString());
            if (recordIds.size() > numberToDelete) {
                // Sắp xếp và xóa 500 bản ghi cũ nhất (giả định Time là trường để sắp xếp)
                for (int i = 0; i < numberToDelete && i < recordIds.size(); i++) {
                    deleteRecord(recordIds.get(i));
                }
            }
        }
    }

    // Lấy danh sách ID từ JSON (cần thư viện JSON thực tế, đây là cách thủ công đơn giản)
    private List<String> extractRecordIds(String jsonResponse) {
        List<String> ids = new ArrayList<>();
        int index = jsonResponse.indexOf("\"id\":");
        while (index != -1) {
            int start = jsonResponse.indexOf("\"", index + 5);
            int end = jsonResponse.indexOf("\"", start + 1);
            if (start != -1 && end != -1) {
                ids.add(jsonResponse.substring(start + 1, end));
                index = jsonResponse.indexOf("\"id\":", end);
            } else {
                break;
            }
        }
        return ids;
    }

    // Xóa một bản ghi cụ thể
    private void deleteRecord(String recordId) throws IOException {
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME + "/" + recordId;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", API_KEY);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to delete record " + recordId + ". Error code: " + responseCode);
        }
    }

    // phương thức này dùng để escape các ký tự đặc biệt trong json
    // ví dụ như dấu nháy kép (") thành \"
    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}