package com.project1.command;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.project1.AirTable.GroupSaver;

public class GroupInfoHandler {

    private final AbsSender bot;
    private final GroupSaver groupSaver;

    public GroupInfoHandler(AbsSender bot) {
        this.bot = bot;
        this.groupSaver = new GroupSaver(bot);
    }

    public void handle(Message message) {
        Long chatId = message.getChatId();
        String chatType = message.getChat().getType();

        // Chỉ cho phép trong chat private
        if (!chatType.equals("private")) {
            send(chatId, "❌ This command is only available in private chats with the bot.");
            return;
        }

        // Lấy tất cả thông tin nhóm từ Airtable
        try {
            List<Map<String, String>> allGroupInfo = groupSaver.getAllGroupInfo();

            if (allGroupInfo.isEmpty()) {
                send(chatId, "❌ No group information available. Please ensure groups are registered in the bot's database.");
                return;
            }

            // Tạo thông báo danh sách nhóm
            StringBuilder response = new StringBuilder("📢 All Group Information:\n\n");
            for (Map<String, String> groupInfo : allGroupInfo) {
                String groupName = escapeMarkdown(groupInfo.getOrDefault("groupName", "Unknown Group"));
                String rawInviteLink = groupInfo.getOrDefault("inviteLink", "No invite link available"); // Phiên bản chưa thoát
                String description = escapeMarkdown(groupInfo.getOrDefault("description", "No description available"));

                // Chỉ thoát groupName và description, không thoát inviteLink trong URL
                response.append("🏠 Group Name: ").append(groupName).append("\n")
                        .append("🔗 Invite Link: [").append(rawInviteLink).append("](").append(rawInviteLink).append(")\n") // Sử dụng rawInviteLink cho URL
                        .append("📝 Description: ").append(description).append("\n\n");
            }

            // Debug: In chuỗi trước khi gửi để kiểm tra
            send(chatId, response.toString());
        } catch (IOException e) {
            send(chatId, "❌ Error retrieving group information. Please try again later. (Error: " + e.getMessage() + ")");
            System.err.println("Error retrieving all group info: " + e.getMessage());
        }
    }

    // Phương thức thoát ký tự Markdown
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        // Chỉ thoát nếu không phải URL (kiểm tra cơ bản)
        if (isUrl(text)) return text; // Không thoát URL
        return text.replace("_", "\\_")
                   .replace("*", "\\*")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("~", "\\~")
                   .replace("`", "\\`")
                   .replace(">", "\\>")
                   .replace("#", "\\#")
                   .replace("+", "\\+")
                   .replace("-", "\\-")
                   .replace("=", "\\=")
                   .replace("|", "\\|")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace(".", "\\.")
                   .replace("!", "\\!");
    }

    // Phương thức kiểm tra xem text có phải là URL không (kiểm tra cơ bản)
    private boolean isUrl(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.matches("^(https?://).+"); // Kiểm tra xem có bắt đầu bằng http:// hoặc https:// không
    }

    private void send(Long chatId, String text) {
        try {
            SendMessage msg = new SendMessage(chatId.toString(), text);
            msg.enableMarkdown(true); // Bắt buộc để hỗ trợ liên kết Markdown
            bot.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }
}