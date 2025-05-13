package com.project1;

import java.util.List;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class IsUserAdmin {

    private final AbsSender bot;

    public IsUserAdmin(AbsSender bot) {
        this.bot = bot;
    }

    // Kiểm tra xem người dùng có phải là admin trong group không
    public boolean check(Long chatId, User user) {
        try {
            GetChatAdministrators getAdmins = new GetChatAdministrators();
            getAdmins.setChatId(chatId.toString());

            List<ChatMember> admins = bot.execute(getAdmins);

            for (ChatMember member : admins) {
                if (member.getUser().getId().equals(user.getId())) {
                    return true;
                }
            }
        } catch (TelegramApiException e) {
            System.err.println("Lỗi khi kiểm tra admin: " + e.getMessage());
        }
        return false;
    }

    // Shortcut: truyền trực tiếp từ Message
    public boolean isAdmin(Message message) {
        return check(message.getChatId(), message.getFrom());
    }
}
