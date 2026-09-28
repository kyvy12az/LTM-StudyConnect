package com.studyconnect.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreatePostDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String content;
    private String subject;
    private List<Long> attachmentIds = new ArrayList<>();

    public CreatePostDTO() {
    }

    public CreatePostDTO(String title, String content, String subject) {
        this.title = title;
        this.content = content;
        this.subject = subject;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<Long> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<Long> attachmentIds) {
        this.attachmentIds = attachmentIds == null
                ? new ArrayList<>()
                : attachmentIds;
    }

    @Override
    public String toString() {
        return "CreatePostDTO{" +
                "title='" + title + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }
}
