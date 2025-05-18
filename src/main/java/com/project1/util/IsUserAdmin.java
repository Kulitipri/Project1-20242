package com.project1.util;

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

    public boolean check(Long chatId, User user) {
        
        if (chatId == null || user == null) return false;
        
        try {
            GetChatAdministrators getAdmins = new GetChatAdministrators();
            getAdmins.setChatId(chatId.toString());

            List<ChatMember> admins = bot.execute(getAdmins);

            for (ChatMember member : admins) {
                if (member.getUser().getId().equals(user.getId())) {
                    return true;
                }
            }

            return false;

        } catch (TelegramApiException e) {
            System.err.println("⚠️ Failed to check admin rights: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in IsUserAdmin: " + e.getMessage());
        }

        return false;
    }

    public boolean isAdmin(Message message) {
        if (message == null || message.getFrom() == null) return false;
        return check(message.getChatId(), message.getFrom());
    }
}
