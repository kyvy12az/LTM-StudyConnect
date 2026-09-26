package com.studyconnect.server.model.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseTest {

    public static void main(String[] args) {
        try (
                Connection connection =
                        DatabaseConnection.getConnection()
        ) {
            System.out.println(
                    "Kết nối MySQL thành công."
            );

            System.out.println(
                    "Database: "
                            + connection.getCatalog()
            );

            DatabaseMetaData metadata =
                    connection.getMetaData();

            System.out.println(
                    "MySQL: "
                            + metadata.getDatabaseProductVersion()
            );

            System.out.println(
                    "JDBC Driver: "
                            + metadata.getDriverVersion()
            );

            System.out.println("Các bảng:");

            try (
                    ResultSet tables =
                            metadata.getTables(
                                    connection.getCatalog(),
                                    null,
                                    "%",
                                    new String[]{"TABLE"}
                            )
            ) {
                while (tables.next()) {
                    System.out.println(
                            "- "
                                    + tables.getString(
                                    "TABLE_NAME"
                            )
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println(
                    "Kết nối thất bại: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}