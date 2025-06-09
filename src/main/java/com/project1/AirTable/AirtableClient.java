package com.project1.AirTable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void saveConfirmation(String scheduleId, String userId, String userName) {
        try {
            JSONObject fields = new JSONObject();
            fields.put("scheduleId", scheduleId);
            fields.put("userId", userId);
            fields.put("userName", userName);

            JSONObject record = new JSONObject();
            record.put("fields", fields);

            String urlString = API_URL;
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
            String urlString = API_URL + "?filterByFormula=AND(scheduleId='" + scheduleId + "',userId='" + userId + "')";
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected code " + responseCode);
            }

            String responseBody = readResponse(conn);
            JSONObject json = new JSONObject(responseBody);
            JSONArray records = json.getJSONArray("records");

            boolean result = records.length() > 0;
            conn.disconnect();
            return result;
        } catch (IOException e) {
            System.err.println("Error checking confirmation: " + e.getMessage());
            return false;
        }
    }
}