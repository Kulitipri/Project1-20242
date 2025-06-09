package com.project1;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.project1.AirTable.AirtableClient;

class AirTableClientTest {
    private final AirtableClient client = new AirtableClient();

    @Test
    void testIsAlreadyConfirmed() {
        assertFalse(client.isAlreadyConfirmed("TEST_SCHEDULE_ID", "TEST_USER_ID"));
    }

    @Test
    void testScheduleExists() {
        assertFalse(client.scheduleExists("INVALID_SCHEDULE_ID"));
    }
}
