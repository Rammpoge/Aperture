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
    private List<TravelPost> posts;

    public PostsAdapter(List<TravelPost> posts) {
        this.posts = posts;
    }

    public void setPosts(List<TravelPost> posts) {
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        TravelPost post = posts.get(position);

        Log.d(TAG, "onBindViewHolder: adding post item #" + position);

        holder.titleTextView.setText(post.getTitle());
        holder.descriptionTextView.setText(post.getDescription());
        holder.dateTextView.setText(timestampToString(post.getCreatedAt()));
        holder.ownerTextView.setText(post.getOwnerNickname());
        String profilePicturePath = "images/profile-pics/" + post.getOwnerUid() + ".jpg";
        String profilePictureUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);

        Glide.with(holder.itemView)
                .load(profilePictureUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.profileImageView);

    }
    private String timestampToString(Timestamp timestamp) {

        Date messageDate = timestamp.toDate();

        boolean isToday = DateUtils.isToday(messageDate.getTime());

        SimpleDateFormat fmt;
        if (isToday) {
            // only show hour:minute, e.g. "14:35"
            fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            // only show date, e.g. "Aug 03, 2025"
            fmt = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        }

        return fmt.format(messageDate);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: returning " + posts.size());
        return posts.size();
    }



    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView titleTextView;
        TextView descriptionTextView;
        TextView dateTextView;
        TextView ownerTextView;
        ImageView postImageView;
        ImageView profileImageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_post_title);
            descriptionTextView = itemView.findViewById(R.id.tv_post_description);
            dateTextView = itemView.findViewById(R.id.tv_post_created_at);
            ownerTextView = itemView.findViewById(R.id.tv_post_owner);
            postImageView = itemView.findViewById(R.id.iv_post_image_main);
            profileImageView = itemView.findViewById(R.id.iv_post_image);



        }
    }

}
