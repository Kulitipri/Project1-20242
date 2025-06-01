package com.project1.AirTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project1.config.BotConfig;
import com.project1.util.InviteLinkFetcher;

public class GroupSaver {
    
    private static final String BASE_ID = BotConfig.getAirtableBaseId();
    private static final String TABLE_NAME = BotConfig.getListOfGroup();
    private static final String API_KEY = BotConfig.getAirtableToken();
    private final AbsSender bot;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroupSaver(AbsSender bot) {
        this.bot = bot;
    }

    public void saveGroup(String chatId, String chatTitle) throws IOException {
    // Kiểm tra loại chat
    String chatType = getChatType(chatId);
    if (!"group".equals(chatType) && !"supergroup".equals(chatType)) {
        return;
    }

    // Lấy mô tả (chỉ có trong supergroup)
    String description = getChatDescription(chatId, chatType);

    // Kiểm tra tồn tại và lấy link mời
    String[] existingRecord = getExistingRecord(chatId);
    String recordId = existingRecord[0];
    String existingInviteLink = existingRecord[1];
    String inviteLink = existingInviteLink;
    if (recordId == null) {
        InviteLinkFetcher fetcher = new InviteLinkFetcher(bot);
        inviteLink = fetcher.getChatInviteLink(chatId);
    }

    // Tạo JSON
    boolean isUpdate = recordId != null;
    String method = isUpdate ? "PATCH" : "POST";
    String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME + (isUpdate ? ("/" + recordId) : "");

    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

    // Thiết lập HTTP method
    if (method.equals("PATCH")) {
        // Workaround cho Java không hỗ trợ PATCH
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setRequestMethod("POST");
    } else {
        conn.setRequestMethod(method);
    }

    conn.setRequestProperty("Authorization", API_KEY.trim());
    conn.setRequestProperty("Content-Type", "application/json");
    conn.setDoOutput(true);

    // Build JSON payload
    String json = buildJson(chatId, chatTitle, inviteLink, description, isUpdate);
    System.out.println("Sending to Airtable (" + method + ")");

    try (OutputStream os = conn.getOutputStream()) {
        os.write(json.getBytes());
    }

    int responseCode = conn.getResponseCode();
    if (responseCode != 200 && responseCode != 201) {
        try (InputStream errorStream = conn.getErrorStream()) {
            String errorMessage = errorStream != null ? new String(errorStream.readAllBytes()) : "No error message";
            throw new IOException("Airtable API returned error code: " + responseCode + "\n" + errorMessage);
        }
    }
}


    private String getChatType(String chatId) {
        GetChat getChat = new GetChat();
        getChat.setChatId(chatId);
        try {
            return bot.execute(getChat).getType();
        } catch (TelegramApiException e) {
            System.err.println("Error checking chat type for chatId " + chatId + ": " + e.getMessage());
            return "unknown";
        }
    }

    // hàm lấy description của chat
    private String getChatDescription(String chatId, String chatType) {
        try {
            Chat chat = bot.execute(new GetChat(chatId));
            return "supergroup".equals(chatType) && chat.getDescription() != null ? chat.getDescription() : "";
        } catch (TelegramApiException e) {
            System.err.println("Error fetching description for chatId " + chatId + ": " + e.getMessage());
            return "";
        }
    }

    // hàm lấy record đã tồn tại trong airtable
    private String[] getExistingRecord(String chatId) throws IOException {
        
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME + "?filterByFormula=GroupId='" + chatId + "'";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY.trim());
        //System.out.println("Using API_KEY for GET: " + API_KEY.trim()); ko cần thiết nữa vì đã lưu được grp rồi

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            try (InputStream errorStream = conn.getErrorStream()) {
                String errorMessage = errorStream != null ? new String(errorStream.readAllBytes()) : "No error message";
                throw new IOException("Airtable API returned error code: " + responseCode + "\n" + errorMessage);
            }
        }

        try (InputStream inputStream = conn.getInputStream()) {
            JsonNode response = objectMapper.readTree(inputStream);
            JsonNode records = response.get("records");
            if (records != null && records.size() > 0) {
                JsonNode record = records.get(0);
                String recordId = record.get("id").asText();
                JsonNode inviteLinkNode = record.get("fields").get("InviteLink");
                return new String[] { recordId, inviteLinkNode != null ? inviteLinkNode.asText() : null };
            }
        }
        return new String[] { null, null };
    }

    // hàm tạo json gửi airtable
    private String buildJson(String chatId, String chatTitle, String inviteLink, String description, boolean isUpdate) {
        String fields = String.format(
            "\"GroupId\": \"%s\", \"GroupName\": \"%s\", \"Description\": \"%s\"",
            escapeJson(chatId), escapeJson(chatTitle), escapeJson(description)
        );
        if (!isUpdate && inviteLink != null && !inviteLink.startsWith("Error")) {
            fields += ", \"InviteLink\": \"" + escapeJson(inviteLink) + "\"";
        }
        return "{ \"fields\": { " + fields + " } }";
    }

    // hàm thoát các ký tự đặc biệt trong json
    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}