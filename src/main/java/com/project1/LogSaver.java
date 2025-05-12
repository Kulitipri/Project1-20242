package com.project1;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogSaver {
    
    // khai báo final các biến của airtable api
    private static final String BASE_ID = BotConfig.getAirtableBaseId();
    private static final String TABLE_NAME = BotConfig.getAirtableTableName();
    private static final String API_KEY = BotConfig.getAirtableToken();

    // method thêm log vào airtable
    public void addRecord(String sender, String message, String time, String chatType, String chatTitle, String chatId) throws IOException {
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
                "\"Sender\": \"%s\", " +
                "\"Message\": \"%s\", " +
                "\"Time\": \"%s\", " +
                "\"ChatType\": \"%s\", " +
                "\"ChatTitle\": \"%s\", " +
                "\"ChatId\": \"%s\" " +
            "} }",
            escapeJson(sender),
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
 
    // phương thức này dùng để escape các ký tự đặc biệt trong json
    // ví dụ như dấu nháy kép (") thành \"
    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}
