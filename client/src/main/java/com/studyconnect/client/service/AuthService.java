package com.studyconnect.client.service;

import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.common.dto.AuthResponseDTO;
import com.studyconnect.common.dto.LoginDTO;
import com.studyconnect.common.dto.RegisterDTO;
import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.util.JsonUtils;

import java.io.IOException;

public class AuthService {
    private final TCPClient tcpClient;

    public AuthService(TCPClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public Response<AuthResponseDTO> register(RegisterDTO registerDTO) throws IOException {
        String json = JsonUtils.toJson(registerDTO);

        Request<String> request = new Request<>(ActionType.REGISTER, json);

        Response<String> rawResponse = tcpClient.sendRequest(request);

        return convertResponse(rawResponse);
    }

    public Response<AuthResponseDTO> login(LoginDTO loginDTO) throws IOException {
        String json = JsonUtils.toJson(loginDTO);

        Request<String> request = new Request<>(ActionType.LOGIN, json);

        Response<String> rawResponse = tcpClient.sendRequest(request);

        return convertResponse(rawResponse);
    }

    private Response<AuthResponseDTO> convertResponse(Response<String> rawResponse) {
        AuthResponseDTO authData = null;
        if (rawResponse.isSuccess() && rawResponse.getData() != null && !rawResponse.getData().isBlank()) {
            authData = JsonUtils.fromJson(rawResponse.getData(), AuthResponseDTO.class);
        }

        Response<AuthResponseDTO> response = new Response<>(
                rawResponse.getRequestId(), rawResponse.getStatusCode(), rawResponse.getMessage(), authData
        );

        response.setTimestamp(rawResponse.getTimestamp());

        return response;
    }
}
