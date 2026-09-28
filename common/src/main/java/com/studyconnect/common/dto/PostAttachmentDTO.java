package com.studyconnect.common.dto;

import java.io.Serializable;

public class PostAttachmentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private String originalName;
    private String mimeType;
    private String attachmentType;
    private long sizeBytes;

    public PostAttachmentDTO() {
    }

    public PostAttachmentDTO(long id, String originalName, String mimeType, String attachmentType, long sizeBytes) {
        this.id = id;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.attachmentType = attachmentType;
        this.sizeBytes = sizeBytes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
}
