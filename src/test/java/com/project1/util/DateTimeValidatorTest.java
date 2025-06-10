package com.project1.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DateTimeValidatorTest {
    
    @Test
    void testValidDateTime() {
        String futureDate = LocalDateTime.now().plusDays(1)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        assertTrue(DateTimeValidator.isValidDateTime(futureDate));
    }
    
    @Test
    void testInvalidDateTime() {
        assertFalse(DateTimeValidator.isValidDateTime("invalid-date"));
    }
    
    @Test
    void testIsAfter() {
        String time1 = "01/01/2024 10:00";
        String time2 = "01/01/2024 11:00"; 
        assertTrue(DateTimeValidator.isAfter(time1, time2));
    }
}
