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

    public static void save(String subject, String time, String location, String groupId) {
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
            String locationStr = String.join(", ", location);

            String json = String.format(
                "{ \"fields\": { " +
                "\"GroupId\": \"%s\", " +
                "\"Subject\": \"%s\", " +
                "\"Time\": \"%s\", " +
                "\"Location\": \"%s\" " +
                "} }",
                escapeJson(groupId),
                escapeJson(subjectStr),
                escapeJson(timeStr),
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
        return input.replace("\"", "\\\"");
    }
}
