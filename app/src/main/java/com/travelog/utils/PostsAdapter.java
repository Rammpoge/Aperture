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
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.travelog.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        // Display Category
        if (post.getCategory() != null && !post.getCategory().isEmpty() && !post.getCategory().equals("Category")) {
            holder.categoryChip.setText(post.getCategory());
        } else {
            holder.categoryChip.setText("Unassigned");
        }
        holder.categoryChip.setVisibility(View.VISIBLE);

        // Display Camera and Lens info
        String cameraInfo = "";
        if (post.getCamera() != null && !post.getCamera().isEmpty()) {
            cameraInfo = post.getCamera();
        } else {
            cameraInfo = "Unassigned Camera";
        }
        
        if (post.getLens() != null && !post.getLens().isEmpty()) {
            cameraInfo += " • " + post.getLens();
        }
        holder.cameraInfoTextView.setText(cameraInfo);
        holder.cameraInfoTextView.setVisibility(View.VISIBLE);

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
        
        holder.metadataContainer.setVisibility(View.VISIBLE);

        // Load profile picture
        String profilePicturePath = "images/profile-pics/" + post.getOwnerUid() + ".jpg";
        String profilePictureUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);

        Glide.with(holder.itemView)
                .load(profilePictureUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.profileImageView);

        // Setup ViewPager for images
        List<String> imageUrls = post.getImageUrls();
        if (imageUrls == null) imageUrls = new ArrayList<>();
        
        ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter(imageUrls);
        holder.viewPager.setAdapter(imagePagerAdapter);

        if (imageUrls.size() > 1) {
            holder.tabLayout.setVisibility(View.VISIBLE);
            new TabLayoutMediator(holder.tabLayout, holder.viewPager, (tab, pos) -> {}).attach();
        } else {
            holder.tabLayout.setVisibility(View.GONE);
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
        Chip categoryChip;
        View metadataContainer;
        ViewPager2 viewPager;
        TabLayout tabLayout;
        ImageView profileImageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
            descriptionTextView = itemView.findViewById(R.id.tv_post_description);
            dateTextView = itemView.findViewById(R.id.tv_post_created_at);
            ownerTextView = itemView.findViewById(R.id.tv_post_owner);
            cameraInfoTextView = itemView.findViewById(R.id.tv_post_camera_info);
            settingsInfoTextView = itemView.findViewById(R.id.tv_post_settings_info);
            categoryChip = itemView.findViewById(R.id.chip_post_category);
            metadataContainer = itemView.findViewById(R.id.ll_metadata);
            viewPager = itemView.findViewById(R.id.vp_post_images);
            tabLayout = itemView.findViewById(R.id.tab_layout_indicator);
            profileImageView = itemView.findViewById(R.id.iv_post_image);
        }
    }

    static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {
        private final List<String> imageUrls;

        ImagePagerAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Glide.with(holder.itemView)
                    .load(imageUrls.get(position))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_post_image_item);
            }
        }
    }
}
