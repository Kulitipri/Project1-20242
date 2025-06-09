package com.project1.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoExtractor {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Ánh xạ để chuẩn hóa tên môn học
    private static final Map<String, String> SUBJECT_MAPPING = new HashMap<>();
    static {
        SUBJECT_MAPPING.put("xstk", "Xác suất thống kê");
        SUBJECT_MAPPING.put("xác suất thống kê", "Xác suất thống kê");
        SUBJECT_MAPPING.put("xác suất", "Xác suất thống kê");
        SUBJECT_MAPPING.put("vldc", "Vật lý đại cương");
        SUBJECT_MAPPING.put("vlđc", "Vật lý đại cương");
        SUBJECT_MAPPING.put("vật lý đại cương", "Vật lý đại cương");
        SUBJECT_MAPPING.put("vật lý", "Vật lý đại cương");
        SUBJECT_MAPPING.put("triết", "Triết học");
        SUBJECT_MAPPING.put("triết học", "Triết học");
        SUBJECT_MAPPING.put("kinh tế chính trị", "Kinh tế chính trị");
        SUBJECT_MAPPING.put("ktct", "Kinh tế chính trị");
        SUBJECT_MAPPING.put("chủ nghĩa xã hội khoa học", "Chủ nghĩa xã hội khoa học");
        SUBJECT_MAPPING.put("cnxh", "Chủ nghĩa xã hội khoa học");
        SUBJECT_MAPPING.put("tư tưởng hồ chí minh", "Tư tưởng Hồ Chí Minh");
        SUBJECT_MAPPING.put("tthcm", "Tư tưởng Hồ Chí Minh");
        SUBJECT_MAPPING.put("tu tuong ho chi minh", "Tư tưởng Hồ Chí Minh");
        SUBJECT_MAPPING.put("lịch sử đảng", "Lịch sử Đảng");
        SUBJECT_MAPPING.put("lsđ", "Lịch sử Đảng");
        SUBJECT_MAPPING.put("pháp luật đại cương", "Pháp luật đại cương");
        SUBJECT_MAPPING.put("pldc", "Pháp luật đại cương");
        SUBJECT_MAPPING.put("giáo dục thể chất", "Giáo dục thể chất");
        SUBJECT_MAPPING.put("gdtc", "Giáo dục thể chất");
        SUBJECT_MAPPING.put("giải tích", "Giải tích");
        SUBJECT_MAPPING.put("gt", "Giải tích");
        SUBJECT_MAPPING.put("cơ sở dữ liệu", "Cơ sở dữ liệu");
        SUBJECT_MAPPING.put("csdl", "Cơ sở dữ liệu");
        SUBJECT_MAPPING.put("dtb", "Đại số tuyến tính");
        SUBJECT_MAPPING.put("đại số tuyến tính", "Đại số tuyến tính");
        SUBJECT_MAPPING.put("mạng máy tính", "Mạng máy tính");
        SUBJECT_MAPPING.put("mmc", "Mạng máy tính");
        SUBJECT_MAPPING.put("tdtt", "Thể dục thể thao");
        SUBJECT_MAPPING.put("thể dục thể thao", "Thể dục thể thao");
        SUBJECT_MAPPING.put("qp", "Quốc phòng");
        SUBJECT_MAPPING.put("quốc phòng", "Quốc phòng");
        SUBJECT_MAPPING.put("toán rời rạc", "Toán rời rạc");
        SUBJECT_MAPPING.put("trr", "Toán rời rạc");
        SUBJECT_MAPPING.put("lập trình cơ bản", "Lập trình cơ bản");
        SUBJECT_MAPPING.put("ltcb", "Lập trình cơ bản");
        SUBJECT_MAPPING.put("lập trình nâng cao", "Lập trình nâng cao");
        SUBJECT_MAPPING.put("ltnc", "Lập trình nâng cao");
        SUBJECT_MAPPING.put("cấu trúc dữ liệu", "Cấu trúc dữ liệu");
        SUBJECT_MAPPING.put("ctdl", "Cấu trúc dữ liệu");
        SUBJECT_MAPPING.put("hệ điều hành", "Hệ điều hành");
        SUBJECT_MAPPING.put("hđh", "Hệ điều hành");
        SUBJECT_MAPPING.put("tiếng anh", "Tiếng Anh");
        SUBJECT_MAPPING.put("ta", "Tiếng Anh");
    }

    // TIME_PATTERN
    private static final String TIME_PATTERN = "\\b((\\d{1,2})h(\\d{2})?\\s*(?:-|[đđ]ến|t tới)\\s*(\\d{1,2})h(\\d{2})?\\s*(?:sáng|chiều|tối)?)|" +
            "(từ\\s+(\\d{1,2})h(\\d{2})?\\s+(tới|[đđ]ến)\\s+(\\d{1,2})h(\\d{2})?\\s*(?:sáng|chiều|tối)?)|" +
            "(\\d{1,2}h\\d{2}|\\d{1,2}:\\d{2})(\\s?(am|pm|sáng|chiều|tối))?" +
            "|(\\d{1,2}\\s?(giờ)?\\s?(\\d{2})?\\s?(phút)?(\\s?(am|pm|sáng|chiều|tối))?)\\b|" +
            "\\b(today|tomorrow|ngày mai|nay|hôm nay|sáng mai|chiều mai|tối mai|ngày kia|ngày kìa|chiều nay|tối nay)\\b";

    private static final String LOCATION_PATTERN = "\\b((room|phòng|P)\\s?\\d{1,4}|" +
            "(floor|tầng)\\s?\\d{1,2}|" +
            "((class|lớp|tiết|ca)\\s?[A-Z]?\\d{1,2})|" +
            "((building|tòa nhà|toà nhà|tòa|toà)?\\s?[A-Z]{1}\\d{0,2}\\s?\\d{1,4}))\\b";

    private static final String SUBJECT_PATTERN = "\\b(xstk|Xác suất thống kê|xác suất thống kê|xác suất|" +
            "vldc|vlđc|Vật lý đại cương|vật lý đại cương|vật lý|Triết học|triết|" +
            "Kinh tế chính trị|kinh tế chính trị|ktct|Chủ nghĩa xã hội khoa học|chủ nghĩa xã hội khoa học|cnxh|" +
            "Tư tưởng Hồ Chí Minh|tư tưởng hồ chí minh|tu tuong ho chi minh|tthcm|" +
            "Lịch sử Đảng|lịch sử đảng|lsđ|Pháp luật đại cương|pháp luật đại cương|pldc|" +
            "Giáo dục thể chất|giáo dục thể chất|gdtc|Giải tích|giải tích|gt|" +
            "Cơ sở dữ liệu|cơ sở dữ liệu|csdl|Đại số tuyến tính|đại số tuyến tính|dtb|" +
            "Mạng máy tính|mạng máy tính|mmc|Thể dục thể thao|thể dục thể thao|tdtt|" +
            "Quốc phòng|quốc phòng|qp|Toán rời rạc|toán rời rạc|trl|" +
            "Lập trình cơ bản|lập trình cơ bản|ltcb|Lập trình nâng cao|lập trình nâng cao|ltnc|" +
            "Cấu trúc dữ liệu|cấu trúc dữ liệu|ctdl|Hệ điều hành|hệ điều hành|hđh|" +
            "Tiếng Anh|tiếng anh|ta)\\b";

    // Trích xuất thông tin và chuẩn hóa
    public static Map<String, List<String>> extractInfo(String text) {
        Map<String, List<String>> result = new HashMap<>();

        List<String> rawTimes = findMatches(TIME_PATTERN, text);
        List<String> formattedTimes = convertTimes(rawTimes, text);
        result.put("Time", formattedTimes);
        result.put("Location", findMatches(LOCATION_PATTERN, text));
        result.put("Subject", normalizeSubjects(findMatches(SUBJECT_PATTERN, text)));

        return result;
    }

    // Tìm các chuỗi khớp với mẫu regex
    private static List<String> findMatches(String pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        return matches;
    }

    // Chuẩn hóa tên môn học
    private static List<String> normalizeSubjects(List<String> subjects) {
        List<String> normalized = new ArrayList<>();
        for (String subject : subjects) {
            String lowerCaseSubject = subject.toLowerCase();
            String normalizedSubject = SUBJECT_MAPPING.getOrDefault(lowerCaseSubject, subject);
            normalized.add(normalizedSubject);
        }
        return normalized;
    }

    // Chuyển đổi biểu thức thời gian thành định dạng dd/MM/yyyy HH:mm
    private static List<String> convertTimes(List<String> times, String fullText) {
        List<String> formattedTimes = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        ZonedDateTime baseDate = now.toLocalDate().atStartOfDay(VIETNAM_ZONE);

        // Điều chỉnh baseDate dựa trên ngữ cảnh
        if (fullText.toLowerCase().contains("tomorrow") || fullText.toLowerCase().contains("ngày mai") || fullText.toLowerCase().contains("mai")) {
            baseDate = baseDate.plusDays(1);
        } else if (fullText.toLowerCase().contains("today") || fullText.toLowerCase().contains("hôm nay") || fullText.toLowerCase().contains("nay")) {
            baseDate = baseDate.withYear(now.getYear()).withMonth(now.getMonthValue()).withDayOfMonth(now.getDayOfMonth());
        }

        // Xác định ngữ cảnh từ toàn bộ văn bản
        boolean isSang = fullText.toLowerCase().contains("sáng") && !fullText.toLowerCase().contains("chiều") && !fullText.toLowerCase().contains("tối");
        boolean isChieu = fullText.toLowerCase().contains("chiều") && !fullText.toLowerCase().contains("sáng") && !fullText.toLowerCase().contains("tối");
        boolean isToi = fullText.toLowerCase().contains("tối") && !fullText.toLowerCase().contains("sáng") && !fullText.toLowerCase().contains("chiều");

        for (String timeStr : times) {
            timeStr = timeStr.toLowerCase().trim();
            System.out.println("Processing time string: " + timeStr); // Debug
            Matcher matcher = Pattern.compile("(\\d{1,2})h(\\d{2})?\\s*(?:-|[đđ]ến|t tới)\\s*(\\d{1,2})h(\\d{2})?").matcher(timeStr);
            if (matcher.find()) {
                String startHourStr = matcher.group(1);
                String startMinuteStr = matcher.group(2);
                String endHourStr = matcher.group(3);
                String endMinuteStr = matcher.group(4);
                System.out.println("Groups: startHour=" + startHourStr + ", startMinute=" + startMinuteStr + ", endHour=" + endHourStr + ", endMinute=" + endMinuteStr); // Debug

                if (startHourStr == null || endHourStr == null) continue;

                int startHour = parseHourOrDefault(startHourStr, 0);
                int startMinute = startMinuteStr != null ? parseMinuteOrDefault(startMinuteStr, 0) : 0;
                int endHour = parseHourOrDefault(endHourStr, 0);
                int endMinute = endMinuteStr != null ? parseMinuteOrDefault(endMinuteStr, 0) : 0;

                // Điều chỉnh giờ dựa trên ngữ cảnh
                if (isSang && startHour >= 0 && startHour <= 11 && endHour >= 0 && endHour <= 11) {
                    // Giữ nguyên cho buổi sáng
                } else if (isChieu && startHour >= 12 && startHour <= 17 && endHour >= 12 && endHour <= 17) {
                    // Giữ nguyên cho buổi chiều
                } else if (isToi && startHour >= 18 && startHour <= 23 && endHour >= 18 && endHour <= 23) {
                    // Giữ nguyên cho buổi tối
                } else if (isSang && (startHour < 0 || startHour > 11 || endHour < 0 || endHour > 11)) {
                    startHour += 12; endHour += 12; // Điều chỉnh nếu không phải sáng
                } else if (isChieu && (startHour < 12 || startHour > 17 || endHour < 12 || endHour > 17)) {
                    startHour += 12; endHour += 12; // Điều chỉnh nếu không phải chiều
                } else if (isToi && (startHour < 18 || startHour > 23 || endHour < 18 || endHour > 23)) {
                    startHour += 12; endHour += 12; // Điều chỉnh nếu không phải tối
                }

                ZonedDateTime startTime = baseDate.withHour(startHour).withMinute(startMinute).withZoneSameInstant(VIETNAM_ZONE);
                ZonedDateTime endTime = baseDate.withHour(endHour).withMinute(endMinute).withZoneSameInstant(VIETNAM_ZONE);

                // Thêm cả startTime và endTime mà không lọc thời gian tại đây
                formattedTimes.add(startTime.format(OUTPUT_FORMAT));
                if (!endTime.isBefore(startTime)) {
                    formattedTimes.add(endTime.format(OUTPUT_FORMAT));
                }
            } else {
                Matcher singleMatcher = Pattern.compile("(\\d{1,2})h(\\d{2})?").matcher(timeStr);
                if (singleMatcher.find()) {
                    String hourStr = singleMatcher.group(1);
                    String minuteStr = singleMatcher.group(2);
                    if (hourStr == null) continue;

                    int hour = parseHourOrDefault(hourStr, 0);
                    int minute = minuteStr != null ? parseMinuteOrDefault(minuteStr, 0) : 0;
                    formattedTimes.add(baseDate.withHour(hour).withMinute(minute).withZoneSameInstant(VIETNAM_ZONE).format(OUTPUT_FORMAT));

                    if (isSang && hour >= 0 && hour <= 11) {
                        // Giữ nguyên
                    } else if (isChieu && hour >= 12 && hour <= 17) {
                        // Giữ nguyên
                    } else if (isToi && hour >= 18 && hour <= 23) {
                        // Giữ nguyên
                    } else if (isSang && (hour < 0 || hour > 11)) {
                        hour += 12;
                    } else if (isChieu && (hour < 12 || hour > 17)) {
                        hour += 12;
                    } else if (isToi && (hour < 18 || hour > 23)) {
                        hour += 12;
                    }

                    ZonedDateTime singleTime = baseDate.withHour(hour).withMinute(minute).withZoneSameInstant(VIETNAM_ZONE);
                    formattedTimes.add(singleTime.format(OUTPUT_FORMAT));
                }
            }
        }

        if (formattedTimes.isEmpty()) {
            ZonedDateTime defaultTime = now.toLocalDate().plusDays(1).atStartOfDay(VIETNAM_ZONE).withHour(8);
            formattedTimes.add(defaultTime.format(OUTPUT_FORMAT));
        }

        return formattedTimes;
    }

    private static int parseHourOrDefault(String hourStr, int defaultValue) {
        try {
            int hour = Integer.parseInt(hourStr);
            return Math.max(0, Math.min(23, hour));
        } catch (NumberFormatException e) {
            System.err.println("Invalid hour string: " + hourStr);
            return defaultValue;
        }
    }

    private static int parseMinuteOrDefault(String minuteStr, int defaultValue) {
        try {
            int minute = Integer.parseInt(minuteStr);
            return Math.max(0, Math.min(59, minute));
        } catch (NumberFormatException e) {
            System.err.println("Invalid minute string: " + minuteStr);
            return defaultValue;
        }
    }
}