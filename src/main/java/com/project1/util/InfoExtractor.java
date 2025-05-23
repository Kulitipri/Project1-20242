package com.project1.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoExtractor {

    // trích xuất thông tin từ message
    public static Map<String, List<String>> extractInfo(String text) {
        Map<String, List<String>> result = new HashMap<>();

        // Các mẫu regex để tìm kiếm thông tin
        String timePattern = "\\b(\\d{1,2}h(\\d{1,2})?\\s?(sáng|chiều|tối)?)\\b|\\b(\\d{1,2}\\s?(giờ)?\\s?(sáng|chiều|tối))\\b|\\b(sáng mai|chiều mai|tối mai|ngày kia|ngày kìa|chiều nay|tối nay|mai|ngày mai|nay|hôm nay)\\b";
        String locationPattern = "\\b((phòng|P)\\s?\\d{1,4}|(tầng)\\s?\\d{1,2}|((lớp|tiết|ca)\\s?[A-Z]?\\d{1,2})|((tòa nhà|toà nhà|tòa|toà)?\\s?[A-Z]{1}\\d{0,2}\\s?\\d{1,4}))\\b";
        String subjectPattern = "\\b(xstk|Xác suất thống kê|Vật lý đại cương|vldc|vlđc|Triết|Kinh tế chính trị|Chủ nghĩa xã hội khoa học|Tư tưởng Hồ Chí Minh|Lịch sử Đảng|Pháp luật đại cương|Giáo dục thể chất|Giải tích|Cơ sở dữ liệu|csdl|dtb|Mạng máy tính|TDTT|QP|CNXH|TTHCM|LSĐ)\\b";

        // Tìm kiếm các thông tin trong văn bản
        result.put("Time", findMatches(timePattern, text));
        result.put("Location", findMatches(locationPattern, text));
        result.put("Subject", findMatches(subjectPattern, text));

        return result;
    }

    //Method dùng để tìm kiếm các chuỗi khớp với mẫu regex trong văn bản
    private static List<String> findMatches(String pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        return matches;
    }
}
