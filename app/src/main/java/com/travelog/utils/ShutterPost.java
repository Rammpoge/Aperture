package com.travelog.utils;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class ShutterPost {
    private String title;
    private String description;
    private String ownerUid;
    private String ownerNickname;
    private Timestamp createdAt;
    private List<String> imageUrls;
    
    // New camera metadata fields
    private String camera;
    private String lens;
    private String shutterSpeed;
    private String aperture;
    private String category;

    public ShutterPost() {
        this.imageUrls = new ArrayList<>();
    }

    public ShutterPost(String title, String description, String ownerUid, String ownerNickname, Timestamp createdAt, List<String> imageUrls, String camera, String lens, String shutterSpeed, String aperture, String category) {
        this.title = title;
        this.description = description;
        this.ownerUid = ownerUid;
        this.ownerNickname = ownerNickname;
        this.createdAt = createdAt;
        this.imageUrls = imageUrls;
        this.camera = camera;
        this.lens = lens;
        this.shutterSpeed = shutterSpeed;
        this.aperture = aperture;
        this.category = category;
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
    
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    
    // Kept for backward compatibility if needed, though we should transition to imageUrls
    public String getImageUrl() { 
        return (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null; 
    }
    public void setImageUrl(String imageUrl) {
        if (this.imageUrls == null) this.imageUrls = new ArrayList<>();
        this.imageUrls.clear();
        this.imageUrls.add(imageUrl);
    }

    public String getCamera() { return camera; }
    public void setCamera(String camera) { this.camera = camera; }
    public String getLens() { return lens; }
    public void setLens(String lens) { this.lens = lens; }
    public String getShutterSpeed() { return shutterSpeed; }
    public void setShutterSpeed(String shutterSpeed) { this.shutterSpeed = shutterSpeed; }
    public String getAperture() { return aperture; }
    public void setAperture(String aperture) { this.aperture = aperture; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
