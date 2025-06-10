package com.project1.command;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;

class SetScheduleTest {
    @Mock private AbsSender bot;
    @Mock private Message message;
    @Mock private User user;
    
    private SetSchedule setSchedule;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setSchedule = new SetSchedule(bot, new HashMap<>());
        
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(123456L);
    }
    
    @Test
    void testStartInPrivateChat() {
        when(message.getChat().getType()).thenReturn("private");
        setSchedule.start(123456L, 789L, "private", message);
        assertTrue(SetSchedule.containsUserState("789_123456"));
    }
}
