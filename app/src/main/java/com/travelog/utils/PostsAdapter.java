package com.travelog.utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.travelog.R;

public class PostsAdapter extends
        RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private static final String TAG = "PostsAdapter";

    public PostsAdapter() {
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: adding post item #" + position);
        holder.titleTextView.setText("Post #" + position);

        holder.descriptionTextView.setText("Description #" + position);
        holder.dateTextView.setText("Date #" + position);
        holder.ownerTextView.setText("Owner #" + position);
        if(position % 2 == 0){
            holder.postImageView.setImageResource(R.drawable.ic_launcher_foreground);
            holder.profileImageView.setImageResource(R.drawable.ic_launcher_background);
        }else {
            holder.postImageView.setImageResource(R.drawable.ic_launcher_background);
            holder.profileImageView.setImageResource(R.drawable.ic_launcher_foreground);
        }

    }

    @Override
    public int getItemCount() {
        return 100;
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
