package com.project1.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeValidator {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalDateTime NOW = LocalDateTime.now();

    /**
     * Kiểm tra xem chuỗi thời gian có đúng định dạng dd/MM/yyyy HH:mm và nằm trong tương lai hay không.
     * @param dateTime Chuỗi thời gian cần kiểm tra
     * @return true nếu hợp lệ, false nếu không
     */
    public static boolean isValidDateTime(String dateTime) {
        try {
            LocalDateTime parsed = LocalDateTime.parse(dateTime, DATE_TIME_FORMAT);
            return !parsed.isBefore(NOW); // Kiểm tra thời gian trong tương lai
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Chuyển đổi chuỗi thời gian thành LocalDateTime.
     * @param dateTime Chuỗi thời gian cần parse
     * @return LocalDateTime nếu parse thành công
     * @throws DateTimeParseException nếu định dạng không hợp lệ
     */
    public static LocalDateTime parseDateTime(String dateTime) throws DateTimeParseException {
        return LocalDateTime.parse(dateTime, DATE_TIME_FORMAT);
    }

    /**
     * Chuyển LocalDateTime thành chuỗi định dạng dd/MM/yyyy HH:mm.
     * @param dateTime LocalDateTime cần format
     * @return Chuỗi thời gian đã chuẩn hóa
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMAT);
    }

    /**
     * Lấy thời gian hiện tại dưới dạng chuỗi dd/MM/yyyy HH:mm.
     * @return Chuỗi thời gian hiện tại
     */
    public static String getCurrentDateTime() {
        return NOW.format(DATE_TIME_FORMAT);
    }
}