package com.studyconnect.client.service;

import com.studyconnect.client.network.tcp.TCPClient;
import com.studyconnect.common.dto.CreatePostDTO;
import com.studyconnect.common.dto.PostDTO;
import com.studyconnect.common.protocol.ActionType;
import com.studyconnect.common.protocol.Request;
import com.studyconnect.common.protocol.Response;
import com.studyconnect.common.util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostService {

    private final TCPClient tcpClient;
    private String authToken;

    public PostService(
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

    // gửi yêu cầu tạo bài viết mới
    public Response<PostDTO> createPost(
            CreatePostDTO createPostDTO
    ) throws IOException {
        requireLogin();

        if (createPostDTO == null) {
            throw new IllegalArgumentException(
                    "Thông tin bài viết không được null"
            );
        }

        String jsonData = JsonUtils.toJson(createPostDTO);

        Request<String> request = new Request<>(
                ActionType.CREATE_POST,
                authToken,
                jsonData
        );

        Response<String> rawResponse =
                tcpClient.sendRequest(request);

        return convertPostResponse(rawResponse);
    }

    // lấy tất cả bài viết
    public Response<List<PostDTO>> getPosts()
            throws IOException {
        return getPosts(null);
    }

    // lấy bài viết theo chủ đề
    public Response<List<PostDTO>> getPosts(
            String subject
    ) throws IOException {
        requireLogin();

        String jsonData;

        if (subject == null || subject.isBlank()) {
            jsonData = "";
        } else {
            jsonData = JsonUtils.toJson(subject.trim());
        }

        Request<String> request = new Request<>(
                ActionType.GET_POSTS,
                authToken,
                jsonData
        );

        Response<String> rawResponse =
                tcpClient.sendRequest(request);

        return convertPostListResponse(rawResponse);
    }

    // lấy thông tin chi tiết của một bài viết
    public Response<PostDTO> getPostDetail(
            long postId
    ) throws IOException {
        requireLogin();

        if (postId <= 0) {
            throw new IllegalArgumentException(
                    "Mã bài viết không hợp lệ"
            );
        }

        Request<String> request = new Request<>(
                ActionType.GET_POST_DETAIL,
                authToken,
                JsonUtils.toJson(postId)
        );

        Response<String> rawResponse =
                tcpClient.sendRequest(request);

        return convertPostResponse(rawResponse);
    }

    private Response<PostDTO> convertPostResponse(
            Response<String> rawResponse
    ) {
        PostDTO postDTO = null;

        if (rawResponse.isSuccess()
                && rawResponse.getData() != null
                && !rawResponse.getData().isBlank()) {

            postDTO = JsonUtils.fromJson(
                    rawResponse.getData(),
                    PostDTO.class
            );
        }

        Response<PostDTO> response = new Response<>(
                rawResponse.getRequestId(),
                rawResponse.getStatusCode(),
                rawResponse.getMessage(),
                postDTO
        );

        response.setTimestamp(rawResponse.getTimestamp());

        return response;
    }

    private Response<List<PostDTO>> convertPostListResponse(
            Response<String> rawResponse
    ) {
        List<PostDTO> posts = new ArrayList<>();

        if (rawResponse.isSuccess()
                && rawResponse.getData() != null
                && !rawResponse.getData().isBlank()) {

            PostDTO[] postArray = JsonUtils.fromJson(
                    rawResponse.getData(),
                    PostDTO[].class
            );

            if (postArray != null) {
                posts.addAll(Arrays.asList(postArray));
            }
        }

        Response<List<PostDTO>> response = new Response<>(
                rawResponse.getRequestId(),
                rawResponse.getStatusCode(),
                rawResponse.getMessage(),
                posts
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