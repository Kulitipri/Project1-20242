package com.project1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.project1.util.DateTimeValidator;

class DateTimeValidatorTest {
    @Test
    void testValidDateTime() {
        // Test thời gian trong tương lai
        String futureDate = "31/12/2024 23:59";
        assertTrue(DateTimeValidator.isValidDateTime(futureDate));
        
        // Test định dạng sai
        String invalidFormat = "2024-12-31 23:59";
        assertFalse(DateTimeValidator.isValidDateTime(invalidFormat));
    }

    @Test
    void testIsAfter() {
        String time1 = "01/01/2024 10:00";
        String time2 = "01/01/2024 11:00";
        assertTrue(DateTimeValidator.isAfter(time1, time2));
    }
}
