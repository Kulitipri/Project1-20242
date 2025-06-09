package com.project1.AirTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Kiểm tra loại chat và lấy mô tả
        String chatType = getChatType(chatId);
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

    // Phương thức lấy loại chat
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

    // Phương thức lấy mô tả của chat
    private String getChatDescription(String chatId, String chatType) {
        try {
            Chat chat = bot.execute(new GetChat(chatId));
            return "supergroup".equals(chatType) && chat.getDescription() != null ? chat.getDescription() : "";
        } catch (TelegramApiException e) {
            System.err.println("Error fetching description for chatId " + chatId + ": " + e.getMessage());
            return "";
        }
    }

    // Phương thức lấy thông tin một nhóm cụ thể
    public Map<String, String> getGroupInfo(Long chatId) throws IOException {
        System.out.println("Attempting to fetch group info for chatId: " + chatId);
        String urlString = String.format("https://api.airtable.com/v0/%s/%s?filterByFormula=({GroupId}='%s')", 
            BASE_ID, TABLE_NAME, chatId);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY.trim());
        conn.setRequestProperty("Content-Type", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (InputStream is = conn.getInputStream()) {
                JsonNode response = objectMapper.readTree(is);
                JsonNode records = response.get("records");
                if (records != null && records.size() > 0) {
                    JsonNode record = records.get(0);
                    JsonNode fields = record.get("fields");

                    Map<String, String> groupInfo = new HashMap<>();
                    groupInfo.put("groupName", fields.has("GroupName") ? fields.get("GroupName").asText() : "Unknown Group");
                    groupInfo.put("inviteLink", fields.has("InviteLink") ? fields.get("InviteLink").asText() : "No invite link available");
                    groupInfo.put("description", fields.has("Description") ? fields.get("Description").asText() : "No description available");
                    System.out.println("Successfully fetched group info: " + groupInfo);
                    return groupInfo;
                } else {
                    System.out.println("No records found for chatId: " + chatId);
                }
            }
        } else {
            try (InputStream errorStream = conn.getErrorStream()) {
                String errorMessage = errorStream != null ? new String(errorStream.readAllBytes()) : "No error message";
                System.out.println("Failed to fetch group info. Response code: " + responseCode + ", Error: " + errorMessage);
                throw new IOException("Failed to retrieve group info. Response code: " + responseCode + "\n" + errorMessage);
            }
        }
        System.out.println("No group info available for chatId: " + chatId);
        return null;
    }

    // Phương thức mới để lấy tất cả thông tin nhóm
    public List<Map<String, String>> getAllGroupInfo() throws IOException {
        System.out.println("Attempting to fetch all group info");
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY.trim());
        conn.setRequestProperty("Content-Type", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            try (InputStream is = conn.getInputStream()) {
                JsonNode response = objectMapper.readTree(is);
                JsonNode records = response.get("records");
                List<Map<String, String>> allGroupInfo = new ArrayList<>();
                if (records != null && records.size() > 0) {
                    for (JsonNode record : records) {
                        JsonNode fields = record.get("fields");
                        Map<String, String> groupInfo = new HashMap<>();
                        groupInfo.put("groupId", fields.has("GroupId") ? fields.get("GroupId").asText() : "Unknown");
                        groupInfo.put("groupName", fields.has("GroupName") ? fields.get("GroupName").asText() : "Unknown Group");
                        groupInfo.put("inviteLink", fields.has("InviteLink") ? fields.get("InviteLink").asText() : "No invite link available");
                        groupInfo.put("description", fields.has("Description") ? fields.get("Description").asText() : "No description available");
                        allGroupInfo.add(groupInfo);
                    }
                    System.out.println("Successfully fetched all group info: " + allGroupInfo.size() + " records");
                    return allGroupInfo;
                } else {
                    System.out.println("No records found in Airtable");
                }
            }
        } else {
            try (InputStream errorStream = conn.getErrorStream()) {
                String errorMessage = errorStream != null ? new String(errorStream.readAllBytes()) : "No error message";
                System.out.println("Failed to fetch all group info. Response code: " + responseCode + ", Error: " + errorMessage);
                throw new IOException("Failed to retrieve all group info. Response code: " + responseCode + "\n" + errorMessage);
            }
        }
        System.out.println("No group info available");
        return new ArrayList<>();
    }

    private String[] getExistingRecord(String chatId) throws IOException {
        String urlString = "https://api.airtable.com/v0/" + BASE_ID + "/" + TABLE_NAME + "?filterByFormula=GroupId='" + chatId + "'";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", API_KEY.trim());

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

    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}