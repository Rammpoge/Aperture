package com.travelog.utils;

import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.travelog.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostsAdapter extends
        RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private static final String TAG = "PostsAdapter";
    private List<ShutterPost> posts;

    public PostsAdapter(List<ShutterPost> posts) {
        this.posts = posts;
    }

    public void setPosts(List<ShutterPost> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ShutterPost post = posts.get(position);

        Log.d(TAG, "onBindViewHolder: adding post item #" + position);

        holder.titleTextView.setText(post.getTitle());
        holder.descriptionTextView.setText(post.getDescription());
        holder.dateTextView.setText(timestampToString(post.getCreatedAt()));
        holder.ownerTextView.setText(post.getOwnerNickname());

        // Display Camera and Lens info
        String cameraInfo = "";
        if (post.getCamera() != null && !post.getCamera().isEmpty()) {
            cameraInfo = post.getCamera();
        }
        if (post.getLens() != null && !post.getLens().isEmpty()) {
            cameraInfo += (cameraInfo.isEmpty() ? "" : " • ") + post.getLens();
        }
        holder.cameraInfoTextView.setText(cameraInfo);
        holder.cameraInfoTextView.setVisibility(cameraInfo.isEmpty() ? View.GONE : View.VISIBLE);

        // Display Shutter Speed and Aperture
        String settingsInfo = "";
        if (post.getShutterSpeed() != null && !post.getShutterSpeed().isEmpty()) {
            settingsInfo = post.getShutterSpeed();
        }
        if (post.getAperture() != null && !post.getAperture().isEmpty()) {
            settingsInfo += (settingsInfo.isEmpty() ? "" : " • ") + post.getAperture();
        }
        holder.settingsInfoTextView.setText(settingsInfo);
        holder.settingsInfoTextView.setVisibility(settingsInfo.isEmpty() ? View.GONE : View.VISIBLE);
        
        holder.metadataContainer.setVisibility((cameraInfo.isEmpty() && settingsInfo.isEmpty()) ? View.GONE : View.VISIBLE);

        // Load profile picture
        String profilePicturePath = "images/profile-pics/" + post.getOwnerUid() + ".jpg";
        String profilePictureUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);

        Glide.with(holder.itemView)
                .load(profilePictureUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.profileImageView);

        // Load post image if it exists
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            holder.postImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView)
                    .load(post.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.postImageView);
        } else {
            holder.postImageView.setVisibility(View.GONE);
            holder.metadataContainer.setVisibility(View.GONE); // Metadata is usually tied to the image
        }

    }
    private String timestampToString(Timestamp timestamp) {
        if (timestamp == null) return "Unknown";
        Date messageDate = timestamp.toDate();

        boolean isToday = DateUtils.isToday(messageDate.getTime());

        SimpleDateFormat fmt;
        if (isToday) {
            fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        }

        return fmt.format(messageDate);
    }

    @Override
    public int getItemCount() {
        return posts == null ? 0 : posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView descriptionTextView;
        TextView dateTextView;
        TextView ownerTextView;
        TextView cameraInfoTextView;
        TextView settingsInfoTextView;
        View metadataContainer;
        ImageView postImageView;
        ImageView profileImageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
            descriptionTextView = itemView.findViewById(R.id.tv_post_description);
            dateTextView = itemView.findViewById(R.id.tv_post_created_at);
            ownerTextView = itemView.findViewById(R.id.tv_post_owner);
            cameraInfoTextView = itemView.findViewById(R.id.tv_post_camera_info);
            settingsInfoTextView = itemView.findViewById(R.id.tv_post_settings_info);
            metadataContainer = itemView.findViewById(R.id.ll_metadata);
            postImageView = itemView.findViewById(R.id.iv_post_image_main);
            profileImageView = itemView.findViewById(R.id.iv_post_image);
        }
    }
}
