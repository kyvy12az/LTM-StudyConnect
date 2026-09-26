package com.studyconnect.server.service;

import com.studyconnect.common.dto.AuthResponseDTO;
import com.studyconnect.common.dto.LoginDTO;
import com.studyconnect.common.dto.RegisterDTO;
import com.studyconnect.common.dto.UserDTO;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.protocol.StatusCode;
import com.studyconnect.common.util.JsonUtils;
import com.studyconnect.server.model.dao.UserDAO;
import com.studyconnect.server.model.entity.User;
import com.studyconnect.server.network.session.SessionManager;
import com.studyconnect.server.util.PasswordUtils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]{3,30}");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserDAO userDAO;
    private final SessionManager sessionManager;

    public AuthService() {
        this.userDAO = new UserDAO();
        this.sessionManager = SessionManager.getInstance();
    }

    public Response<String> register(String requestId, RegisterDTO registerDTO) {
        String validationError = validateRegister(registerDTO);

        if (validationError != null) {
            return Response.error(requestId, StatusCode.BAD_REQUEST, validationError);
        }

        try {
            String username  = registerDTO.getUsername().trim();
            String email = registerDTO.getEmail().trim().toLowerCase();

            registerDTO.setUsername(username);
            registerDTO.setEmail(email);
            registerDTO.setFullName(registerDTO.getFullName().trim());

            if (userDAO.usernameExists(username)) {
                return Response.error(requestId, StatusCode.CONFLICT, "Tên đăng nhập đã tồn tại");
            }

            if (userDAO.emailExists(email)) {
                return Response.error(requestId, StatusCode.CONFLICT, "Email đã tồn tại");
            }

            String passwordHash = PasswordUtils.hash(registerDTO.getPassword());

            UserDTO userDTO = userDAO.createUser(registerDTO, passwordHash);

            String token = sessionManager.createSession(userDTO.getId());

            AuthResponseDTO authResponse = new AuthResponseDTO(token, userDTO);

            return Response.success(requestId, "Đăng ký thành công", JsonUtils.toJson(authResponse));
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.error(requestId, StatusCode.SERVER_ERROR, "Không thể đăng ký tài khoản");
        }
    }

    public Response<String> login(String requestId, LoginDTO loginDTO) {
        if (loginDTO == null || isBlank(loginDTO.getUsername()) || isBlank(loginDTO.getPassword())) {
            return Response.error(requestId, StatusCode.BAD_REQUEST, "Vui lòng nhập tài khoản và mật khẩu");
        }

        try {
            String account = loginDTO.getUsername().trim();

            Optional<User> optionalUser = userDAO.findByUsernameOrEmail(account);

            if (optionalUser.isEmpty()) {
                return invalidCredentials(requestId);
            }

            User user = optionalUser.get();

            if (!"ACTIVE".equals(user.getStatus())) {
                return Response.error(requestId, StatusCode.FORBIDDEN, "Tài khoản đã bị khóa");
            }

            if (!PasswordUtils.verify(loginDTO.getPassword(), user.getPasswordHash())) {
                return invalidCredentials(requestId);
            }

            userDAO.updateLastSeen(user.getId());

            UserDTO userDTO = new UserDTO(user.getId(), user.getUsername(), user.getFullname(), user.getEmail(), user.getAvatarUrl(), true);

            String token = sessionManager.createSession(user.getId());

            AuthResponseDTO authResponse = new AuthResponseDTO(token, userDTO);

            return Response.success(requestId, "Đăng nhập thành công", JsonUtils.toJson(authResponse));
        } catch (SQLException e) {
            e.printStackTrace();
            return Response.error(requestId, StatusCode.SERVER_ERROR, "Không thể đăng nhập");
        }
    }

    private Response<String> invalidCredentials(String requestId) {
        return Response.error(requestId, StatusCode.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
    }

    private String validateRegister(RegisterDTO dto) {
        if (dto == null) return "Dữ liệu đăng ký không hợp lệ";

        if (isBlank(dto.getUsername()) || !USERNAME_PATTERN.matcher(dto.getUsername().trim()).matches()) {
            return "Tên đăng nhập gồm 3-30 ký tự, " + "chỉ chứa chữ, số và dấu gạch dưới";
        }

        if (isBlank(dto.getPassword()) || dto.getPassword().length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }

        if (isBlank(dto.getFullName())) {
            return "Họ tên không được để trống";
        }

        if (isBlank(dto.getEmail()) || !EMAIL_PATTERN.matcher(dto.getEmail().trim()).matches()) {
            return "Email không hợp lệ";
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
