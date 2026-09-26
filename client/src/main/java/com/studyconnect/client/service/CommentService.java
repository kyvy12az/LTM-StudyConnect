package com.studyconnect.client.service;

import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.common.dto.CommentDTO;
import com.studyconnect.common.dto.CreateCommentDTO;
import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommentService {

    private final TCPClient tcpClient;
    private String authToken;

    public CommentService(
            TCPClient tcpClient,
            String authToken
    ) {
        if (tcpClient == null) {
            throw new IllegalArgumentException(
                    "TCPClient không được null"
            );
        }

        this.tcpClient = tcpClient;
        this.authToken = authToken;
    }

    // tạo bình luận mới cho bài viết
    public Response<CommentDTO> createComment(
            CreateCommentDTO createCommentDTO
    ) throws IOException {
        requireLogin();

        if (createCommentDTO == null) {
            throw new IllegalArgumentException(
                    "Thông tin bình luận không được null"
            );
        }

        String jsonData = JsonUtils.toJson(createCommentDTO);

        Request<String> request = new Request<>(
                ActionType.CREATE_COMMENT,
                authToken,
                jsonData
        );

        Response<String> rawResponse =
                tcpClient.sendRequest(request);

        return convertCommentResponse(rawResponse);
    }

    // lấy danh sách bình luận của một bài viết
    public Response<List<CommentDTO>> getComments(
            long postId
    ) throws IOException {
        requireLogin();

        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bài viết không hợp lệ"
            );
        }

        Request<String> request = new Request<>(
                ActionType.GET_COMMENTS,
                authToken,
                JsonUtils.toJson(postId)
        );

        Response<String> rawResponse =
                tcpClient.sendRequest(request);

        return convertCommentListResponse(rawResponse);
    }

    private Response<CommentDTO> convertCommentResponse(
            Response<String> rawResponse
    ) {
        CommentDTO commentDTO = null;

        if (rawResponse.isSuccess()
                && rawResponse.getData() != null
                && !rawResponse.getData().isBlank()) {

            commentDTO = JsonUtils.fromJson(
                    rawResponse.getData(),
                    CommentDTO.class
            );
        }

        Response<CommentDTO> response = new Response<>(
                rawResponse.getRequestId(),
                rawResponse.getStatusCode(),
                rawResponse.getMessage(),
                commentDTO
        );

        response.setTimestamp(rawResponse.getTimestamp());

        return response;
    }

    private Response<List<CommentDTO>> convertCommentListResponse(
            Response<String> rawResponse
    ) {
        List<CommentDTO> comments = new ArrayList<>();

        if (rawResponse.isSuccess()
                && rawResponse.getData() != null
                && !rawResponse.getData().isBlank()) {

            CommentDTO[] commentArray = JsonUtils.fromJson(
                    rawResponse.getData(),
                    CommentDTO[].class
            );

            if (commentArray != null) {
                comments.addAll(
                        Arrays.asList(commentArray)
                );
            }
        }

        Response<List<CommentDTO>> response = new Response<>(
                rawResponse.getRequestId(),
                rawResponse.getStatusCode(),
                rawResponse.getMessage(),
                comments
        );

        response.setTimestamp(rawResponse.getTimestamp());

        return response;
    }

    private void requireLogin() {
        if (authToken == null || authToken.isBlank()) {
            throw new IllegalStateException(
                    "Bạn chưa đăng nhập"
            );
        }
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }
}