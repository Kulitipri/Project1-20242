package com.project1;

// thư viện của java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoExtractor {

    private static final Scanner scanner = new Scanner(System.in);// Khởi tạo Scanner để đọc đầu vào từ người dùng

    public static Map<String, List<String>> extractInfo(String text) {
        Map<String, List<String>> result = new HashMap<>();// tạo một map để lưu trữ thông tin trích xuất

        String timePattern = "\\b(\\d{1,2}h(\\d{1,2})?\\s?(sáng|chiều|tối)?)\\b|\\b(\\d{1,2}\\s?(giờ)?\\s?(sáng|chiều|tối))\\b|\\b(sáng mai|chiều mai|tối mai|ngày kia|ngày kìa|chiều nay|tối nay|mai|ngày mai|nay|hôm nay)\\b";
        String locationPattern = "\\b((phòng|P)\\s?\\d{1,4}|(tầng)\\s?\\d{1,2}|((lớp|tiết|ca)\\s?[A-Z]?\\d{1,2})|((tòa nhà|toà nhà|tòa|toà)?\\s?[A-Z]{1}\\d{0,2}\\s?\\d{1,4}))\\b";
        String subjectPattern = "\\b(xstk|Xác suất thống kê|Vật lý đại cương|vldc|vlđc|Triết|Kinh tế chính trị|Chủ nghĩa xã hội khoa học|Tư tưởng Hồ Chí Minh|Lịch sử Đảng|Pháp luật đại cương|Giáo dục thể chất|Giải tích|Cơ sở dữ liệu|csdl|dtb|Mạng máy tính|TDTT|QP|CNXH|TTHCM|LSĐ)\\b";

        result.put("Time", findMatches(timePattern, text));
        result.put("Location", findMatches(locationPattern, text));
        result.put("Subject", findMatches(subjectPattern, text));

        return result;
    }

    private static List<String> findMatches(String pattern, String text) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group().trim());
        }
        return matches;
    }

    // Hàm hỏi yes/no từ người dùng
    private static boolean askYesNo(String question) {
        while (true) {
            System.out.print(question + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("yes") || input.equals("y")) return true;
            if (input.equals("no") || input.equals("n")) return false;
            System.out.println("Please answer 'yes' or 'no'.");
        }
    }

    public static void main(String[] args) {
        String input = "8h tối ngày kia học vật lý đại cương ở c7 101";
        Map<String, List<String>> info = extractInfo(input);

        System.out.println("\n Schedule Class Info:");
        for (Map.Entry<String, List<String>> entry : info.entrySet()) {
            System.out.println(" - " + entry.getKey() + ": " + (entry.getValue().isEmpty() ? "NULL" : String.join(", ", entry.getValue())));
        }

        if (askYesNo("\nDo you want to schedule this class?")) {
            System.out.println("Class scheduled successfully!");
            // Gọi phương thức tạo lịch tại đây nếu có, ví dụ: createSchedule(info);
        } else {
            System.out.println("Skipping class scheduling.");
        }
    }
}
// tìm cách để cho code đọc toàn bộ tin nhắn trong nhóm
// và tìm kiếm các tin nhắn có chứa thông tin về lịch học