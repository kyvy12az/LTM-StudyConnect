package com.studyconnect.server.model.dao;

import com.studyconnect.common.dto.RegisterDTO;
import com.studyconnect.common.dto.UserDTO;
import com.studyconnect.server.model.database.DatabaseConnection;
import com.studyconnect.server.model.entity.User;

import java.sql.*;
import java.util.*;

public class UserDAO {

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setString(1, email);

            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public UserDTO createUser(RegisterDTO registerDTO, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email) VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ) {
            statement.setString(1, registerDTO.getUsername());
            statement.setString(2, passwordHash);
            statement.setString(3, registerDTO.getFullName());
            statement.setString(4, registerDTO.getEmail());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("Không lấy được ID người dùng");
                }

                long userId = generatedKeys.getLong(1);

                return new UserDTO(userId, registerDTO.getUsername(), registerDTO.getFullName(), registerDTO.getEmail(), null, true);
            }
        }
    }

    public List<UserDTO> findByIds(Set<Long> userIds) throws SQLException {
        List<UserDTO> users = new ArrayList<>();

        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = String.join(
                ",",
                Collections.nCopies(
                        userIds.size(),
                        "?"
                )
        );

        String sql = """
                SELECT id, username, full_name, email, avatar_url
                FROM users
                WHERE id IN (%s)
                AND status = 'ACTIVE'
                ORDER BY COALESCE(full_name, username)
                """.formatted(placeholders);

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            int index = 1;
            for (Long userId : userIds) {
                if (userId != null) {
                    statement.setLong(
                            index++,
                            userId
                    );
                }
            }

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    UserDTO user = new UserDTO(
                            result.getLong("id"),
                            result.getString("username"),
                            result.getString("full_name"),
                            result.getString("email"),
                            result.getString("avatar_url"),
                            true
                    );

                    users.add(user);
                }
            }
        }
        return users;
    }

    public Optional<User> findByUsernameOrEmail(String account) throws SQLException {
        String sql = """
                SELECT id, username, password_hash, full_name, email, avatar_url, role, status
                FROM users
                WHERE username = ? OR email = ?
                LIMIT 1
                """;
        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setString(1, account);
            statement.setString(2, account);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();

                User user = new User();
                user.setId(result.getLong("id"));
                user.setUsername(result.getString("username"));
                user.setPasswordHash(result.getString("password_hash"));
                user.setFullname(result.getString("full_name"));
                user.setEmail(result.getString("email"));
                user.setAvatarUrl(result.getString("avatar_url"));
                user.setRole(result.getString("role"));
                user.setStatus(result.getString("status"));

                return Optional.of(user);
            }
        }
    }

    public void updateLastSeen(long userId) throws SQLException {
        String sql = """
                    UPDATE users
                    SET last_seen = CURRENT_TIMESTAMP
                    WHERE id = ?
                """;

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }
}
