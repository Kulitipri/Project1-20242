package com.project1;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// class này dùng để đọc file config.properties
// class này dùng để lưu trữ các thông tin cấu hình của bot
public class BotConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = BotConfig.class.getClassLoader().getResourceAsStream("config.properties")) { 
            if (input == null) {
                throw new IllegalStateException("config.properties not found");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    // lấy giá trị từ config.properties
    public static String get(String key) {
        return props.getProperty(key);
    }

    public static String getTelegramToken() {
        return get("telegram.token");
    }

    public static String getTelegramBotname() {
        return get("telegram.botname");
    }

    public static String getAirtableToken() {
        return get("airtable.token");
    }

    public static String getAirtableBaseId() {
        return get("airtable.base_id");
    }

    public static String getAirtableTableName() {
        return get("airtable.table_name");
    }
}