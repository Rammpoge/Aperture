package com.travelog.utils;

import android.app.AlertDialog;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.travelog.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        holder.commentButton.setOnClickListener(v -> showCommentsBottomSheet(v, post));

    }

    private void showCommentsBottomSheet(View v, ShutterPost post) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext());
        View bottomSheetView = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_comments_bottom_sheet, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        RecyclerView rvComments = bottomSheetView.findViewById(R.id.rv_comments);
        EditText etComment = bottomSheetView.findViewById(R.id.et_comment);
        ImageButton btnSendComment = bottomSheetView.findViewById(R.id.btn_send_comment);

        List<Comment> comments = new ArrayList<>();
        Map<String, List<Comment>> repliesMap = new HashMap<>();
        CommentsAdapter commentsAdapter = new CommentsAdapter(comments, repliesMap, (parentComment, replyText) -> {
            sendComment(v, post, replyText, parentComment.getCommentId());
            etComment.setText("");
        });
        rvComments.setAdapter(commentsAdapter);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (post.getPostId() != null) {
            db.collection("posts").document(post.getPostId()).collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Comment comment = dc.getDocument().toObject(Comment.class);
                                comment.setCommentId(dc.getDocument().getId());
                                if (comment.getParentCommentId() == null) {
                                    comments.add(comment);
                                } else {
                                    List<Comment> replies = repliesMap.get(comment.getParentCommentId());
                                    if (replies == null) {
                                        replies = new ArrayList<>();
                                        repliesMap.put(comment.getParentCommentId(), replies);
                                    }
                                    replies.add(comment);
                                }
                            }
                        }
                        commentsAdapter.notifyDataSetChanged();
                    });
        }

        btnSendComment.setOnClickListener(v1 -> {
            String text = etComment.getText().toString().trim();
            if (!text.isEmpty()) {
                sendComment(v, post, text, null);
                etComment.setText("");
            }
        });

        bottomSheetDialog.show();
    }

    private void sendComment(View v, ShutterPost post, String text, String parentId) {
        String nickname = v.getContext().getSharedPreferences("userInfo", v.getContext().MODE_PRIVATE).getString("nickname", "Anonymous");
        Comment comment = new Comment(text, FirebaseAuth.getInstance().getUid(), nickname, Timestamp.now(), parentId);
        
        if (post.getPostId() != null) {
            FirebaseFirestore.getInstance().collection("posts").document(post.getPostId()).collection("comments").add(comment)
                    .addOnFailureListener(e -> Toast.makeText(v.getContext(), "Failed to send comment", Toast.LENGTH_SHORT).show());
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
        View commentButton;

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
            commentButton = itemView.findViewById(R.id.btn_comment);
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

    interface ReplyClickListener {
        void onReplyClick(Comment parentComment, String text);
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.ViewHolder> {
        private List<Comment> comments;
        private Map<String, List<Comment>> repliesMap;
        private ReplyClickListener replyClickListener;

        CommentsAdapter(List<Comment> comments, Map<String, List<Comment>> repliesMap, ReplyClickListener replyClickListener) {
            this.comments = comments;
            this.repliesMap = repliesMap;
            this.replyClickListener = replyClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Comment comment = comments.get(position);
            holder.tvName.setText(comment.getOwnerNickname());
            holder.tvText.setText(comment.getText());
            holder.tvTime.setText(timestampToString(comment.getCreatedAt()));

            String profilePicturePath = "images/profile-pics/" + comment.getOwnerUid() + ".jpg";
            String profilePictureUrl = SupabaseStorageHelper.getFileSupabaseUrl(profilePicturePath);
            Glide.with(holder.itemView)
                    .load(profilePictureUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivProfile);

            List<Comment> replies = repliesMap.get(comment.getCommentId());
            if (replies != null && !replies.isEmpty()) {
                holder.rvReplies.setVisibility(View.VISIBLE);
                holder.rvReplies.setAdapter(new CommentsAdapter(replies, new HashMap<>(), null));
            } else {
                holder.rvReplies.setVisibility(View.GONE);
            }

            if (replyClickListener != null) {
                holder.tvReply.setVisibility(View.VISIBLE);
                holder.tvReply.setOnClickListener(v -> {
                    // Simple reply implementation: show a dialog to write the reply
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("Reply to " + comment.getOwnerNickname());
                    final EditText input = new EditText(v.getContext());
                    builder.setView(input);
                    builder.setPositiveButton("Reply", (dialog, which) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) {
                            replyClickListener.onReplyClick(comment, text);
                        }
                    });
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                    builder.show();
                });
            } else {
                holder.tvReply.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivProfile;
            TextView tvName, tvText, tvTime, tvReply;
            RecyclerView rvReplies;
            ViewHolder(View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.iv_commenter_image);
                tvName = itemView.findViewById(R.id.tv_commenter_name);
                tvText = itemView.findViewById(R.id.tv_comment_text);
                tvTime = itemView.findViewById(R.id.tv_comment_time);
                tvReply = itemView.findViewById(R.id.tv_reply_btn);
                rvReplies = itemView.findViewById(R.id.rv_replies);
            }
        }
    }
}
