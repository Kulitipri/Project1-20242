package com.project1.AirTable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.project1.config.BotConfig;

public class ConfirmationSaver {

    private static final String BASE_ID = BotConfig.getAirtableBaseId();
    private static final String TABLE_NAME = BotConfig.getConfirmationTableName(); // Giả định tên bảng xác nhận
    private static final String API_KEY = BotConfig.getAirtableToken();

    public static void save(String scheduleId, String userId, String userName) {
        try {
            String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json = String.format(
                "{ \"fields\": { " +
                "\"ScheduleId\": \"%s\", " +
                "\"UserId\": \"%s\", " +
                "\"UserName\": \"%s\" " +
                "} }",
                escapeJson(scheduleId),
                escapeJson(userId),
                escapeJson(userName)
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                System.err.println("Failed to save confirmation. Response code: " + responseCode);
            } else {
                System.out.println("Confirmation saved to Airtable.");
            }

        } catch (IOException e) {
            System.err.println("Error saving confirmation: " + e.getMessage());
        }
    }

    private static String escapeJson(String input) {
        return input != null ? input.replace("\"", "\\\"") : "";
    }
}