package com.project1.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoExtractor {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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

    // Regex được cải tiến để khớp chính xác hơn
    private static final String TIME_PATTERN = "\\b(\\d{1,2}h(\\d{2})?(\\s?(am|pm|sáng|chiều|tối))?)\\b" +
            "|\\b(\\d{1,2}\\s?(giờ)?(\\s?(am|pm|sáng|chiều|tối))?)\\b" +
            "|\\b(today|tomorrow|ngày mai|nay|hôm nay|sáng mai|chiều mai|tối mai|ngày kia|ngày kìa|chiều nay|tối nay)\\b";
    
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

        // Tìm kiếm các thông tin
        List<String> rawTimes = findMatches(TIME_PATTERN, text);
        List<String> formattedTimes = convertTimes(rawTimes);
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
    private static List<String> convertTimes(List<String> times) {
        List<String> formattedTimes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);

        // Xử lý kết hợp các phần thời gian trong fullText
        String timeContext = "";
        for (String time : times) {
            timeContext += time + " ";
        }
        timeContext = timeContext.trim();

        // Nếu không tìm thấy thời gian cụ thể, mặc định là hôm nay 8:00
        if (timeContext.isEmpty()) {
            formattedTimes.add(now.toLocalDate().atStartOfDay().withHour(8).format(OUTPUT_FORMAT));
            return formattedTimes;
        }

        LocalDateTime result = parseTimeExpression(timeContext, now);
        if (result != null) {
            formattedTimes.add(result.format(OUTPUT_FORMAT));
        } else {
            // Nếu không parse được, mặc định là ngày mai 8:00
            result = now.toLocalDate().plusDays(1).atStartOfDay().withHour(8);
            formattedTimes.add(result.format(OUTPUT_FORMAT));
        }

        return formattedTimes;
    }

    private static LocalDateTime parseTimeExpression(String time, LocalDateTime baseTime) {
        time = time.toLowerCase().trim();
        LocalDateTime result = baseTime.toLocalDate().atStartOfDay(); // Bắt đầu từ 00:00 của ngày

        // Xử lý ngày
        if (time.contains("today") || time.contains("nay") || time.contains("hôm nay")) {
            result = result.withYear(baseTime.getYear()).withMonth(baseTime.getMonthValue()).withDayOfMonth(baseTime.getDayOfMonth());
        } else if (time.contains("tomorrow") || time.contains("ngày mai")) {
            result = result.plusDays(1);
        } else if (time.contains("ngày kia") || time.contains("ngày kìa")) {
            result = result.plusDays(2);
        }

        // Xử lý giờ và phút
        int hour = -1;
        int minute = 0;
        Pattern hourPattern = Pattern.compile("(\\d{1,2})(h(\\d{2})?)?");
        Matcher matcher = hourPattern.matcher(time);
        if (matcher.find()) {
            hour = Integer.parseInt(matcher.group(1));
            if (matcher.group(3) != null) {
                minute = Integer.parseInt(matcher.group(3));
            }
        }

        // Áp dụng giờ và phút nếu có
        if (hour != -1) {
            result = result.withHour(hour).withMinute(minute);
        }

        // Xử lý thời gian trong ngày (sáng, chiều, tối) và điều chỉnh giờ
        if (time.contains("sáng") || time.contains("am")) {
            if (hour == -1) result = result.withHour(8).withMinute(0); // Mặc định sáng 8:00
        } else if (time.contains("chiều") || time.contains("pm")) {
            if (hour != -1 && hour < 12) result = result.withHour(hour + 12); // Chuyển sang 24h
            else if (hour == -1) result = result.withHour(14).withMinute(0); // Mặc định chiều 14:00
        } else if (time.contains("tối")) {
            if (hour == -1) result = result.withHour(19).withMinute(0); // Mặc định tối 19:00
        }

        // Đảm bảo không trả về thời gian quá khứ
        return result.isBefore(LocalDateTime.now(VIETNAM_ZONE)) ? null : result;
    }
}