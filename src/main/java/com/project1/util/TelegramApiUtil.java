package com.project1.util;

import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramApiUtil {

    public static String getChatTitle(AbsSender bot, Long chatId) {
        try {
            GetChat getChat = new GetChat();
            getChat.setChatId(chatId.toString());
            Chat chat = bot.execute(getChat);
            return chat.getTitle();
        } catch (TelegramApiException e) {
            System.err.println("Error fetching chat title: " + e.getMessage());
            return null;
        }
    }
}