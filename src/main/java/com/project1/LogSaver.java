package com.project1;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogSaver {

    private static final String BASE_ID = "appfCAcN1QNkRSbWs";
    private static final String TABLE_NAME = "Logs";
    private static final String API_KEY = "Bearer patKBjQ8CmLopwrOY.62005630e32049f180f9bfaa528189dcc2639b9d7d4fc1b593b8ae581637dbaa";

    public void addRecord(String sender, String message, String time, String chatType, String chatTitle, String chatId) throws IOException {
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

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

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            throw new IOException("Airtable API returned error code: " + responseCode);
        }
    }

    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}
