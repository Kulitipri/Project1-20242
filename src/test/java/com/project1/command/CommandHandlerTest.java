package com.project1.command;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

public class CommandHandlerTest {
    @Mock private AbsSender bot;
    @Mock private Message message;
    private CommandHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CommandHandler(bot, new HashMap<>());
    }

    @Test
    void testHandleCommand() {
        // Add test cases
    }
}
