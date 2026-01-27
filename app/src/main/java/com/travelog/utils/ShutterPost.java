package com.travelog.utils;

import com.google.firebase.Timestamp;

public class ShutterPost {
    private String title;
    private String description;
    private String ownerUid;
    private String ownerNickname;
    private Timestamp createdAt;
    private String imageUrl;
    
    // New camera metadata fields
    private String camera;
    private String lens;
    private String shutterSpeed;
    private String aperture;

    public ShutterPost() {}

    public ShutterPost(String title, String description, String ownerUid, String ownerNickname, Timestamp createdAt, String imageUrl) {
        this.title = title;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
    }

    public ShutterPost(String title, String description, String ownerUid, String ownerNickname, Timestamp createdAt, String imageUrl, String camera, String lens, String shutterSpeed, String aperture) {
        this.title = title;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
        this.imageUrl = imageUrl;
        this.camera = camera;
        this.lens = lens;
        this.shutterSpeed = shutterSpeed;
        this.aperture = aperture;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }
    public String getOwnerNickname() { return ownerNickname; }
    public void setOwnerNickname(String ownerNickname) { this.ownerNickname = ownerNickname; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCamera() { return camera; }
    public void setCamera(String camera) { this.camera = camera; }
    public String getLens() { return lens; }
    public void setLens(String lens) { this.lens = lens; }
    public String getShutterSpeed() { return shutterSpeed; }
    public void setShutterSpeed(String shutterSpeed) { this.shutterSpeed = shutterSpeed; }
    public String getAperture() { return aperture; }
    public void setAperture(String aperture) { this.aperture = aperture; }
}
