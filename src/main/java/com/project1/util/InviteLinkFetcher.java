package com.project1.util;

import org.telegram.telegrambots.meta.api.methods.groupadministration.ExportChatInviteLink;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class InviteLinkFetcher {

    private final AbsSender bot;

    public InviteLinkFetcher(AbsSender bot) {
        this.bot = bot;
    }

    // Fetches the invite link for a given chat ID
    public String getChatInviteLink(String chatId) {
        ExportChatInviteLink exportLink = new ExportChatInviteLink();
        exportLink.setChatId(chatId);

        try {
            return bot.execute(exportLink); // returns the invite link as a String
        } catch (TelegramApiException e) {
            System.err.println("Error occurred while fetching invite link: " + e.getMessage());
            return "Error getting invite link: " + e.getMessage();
        }
    }
}
