package com.studyconnect.server.model.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final String CONFIG_FILE = "database.properties";

    private static final String URL;
    private static final String USERNAME;
    private static final String PASSWORD;

    static {
        Properties properties = new Properties();

        try (
                InputStream input =
                        DatabaseConnection.class
                                .getClassLoader()
                                .getResourceAsStream(CONFIG_FILE)
        ) {
            if (input == null) {
                throw new IllegalStateException(
                        "Không tìm thấy file "
                                + CONFIG_FILE
                );
            }

            properties.load(input);

            URL = getRequiredProperty(
                    properties,
                    "db.url"
            );

            USERNAME = getRequiredProperty(
                    properties,
                    "db.username"
            );

            PASSWORD = properties.getProperty(
                    "db.password",
                    ""
            );

        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                    "Không thể đọc cấu hình database: "
                            + e.getMessage()
            );
        }
    }

    public DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static boolean testConnection() {
        try (
                Connection connection = getConnection()
        ) {
            return connection != null && connection.isValid(3);

        } catch (SQLException e) {
            System.err.println("Kết nối MySQL thất bại: " + e.getMessage());

            return false;
        }
    }

    private static String getRequiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình: " + key);
        }

        return value.trim();
    }
}
