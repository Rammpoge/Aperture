package com.travelog.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class Comment {
    private String commentId;
    private String text;
    private String ownerUid;
    private String ownerNickname;
    private Timestamp createdAt;
    private String parentCommentId; // If this is a reply, this will be the ID of the parent comment

    public Comment() {}

    public Comment(String text, String ownerUid, String ownerNickname, Timestamp createdAt) {
        this.text = text;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
    }

    public Comment(String text, String ownerUid, String ownerNickname, Timestamp createdAt, String parentCommentId) {
        this.text = text;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
        this.parentCommentId = parentCommentId;
    }

    @Exclude
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getOwnerNickname() { return ownerNickname; }
    public void setOwnerNickname(String ownerNickname) { this.ownerNickname = ownerNickname; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(String parentCommentId) { this.parentCommentId = parentCommentId; }
}
