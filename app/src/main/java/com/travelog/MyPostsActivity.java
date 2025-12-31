package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.travelog.utils.PostsAdapter;
import com.travelog.utils.ShutterPost;

import java.util.ArrayList;
import java.util.List;

public class MyPostsActivity extends AppCompatActivity {

    private String nickname;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<ShutterPost> posts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_posts);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton backToFeedButton = findViewById(R.id.backToFeedButton);
        backToFeedButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyPostsActivity.this, FeedActivity.class);
            startActivity(intent);
        });

        readUserData();

        TextView pageTitle = findViewById(R.id.pageTitle);
        if (nickname != null && !nickname.equals("N/A")) {
            pageTitle.setText(nickname + "'s Posts");
        }

        posts = new ArrayList<>();
        initRecyclerView();
        loadPosts();
    }

    private void readUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", "N/A");
    }

    private void loadPosts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .whereEqualTo("ownerUid", FirebaseAuth.getInstance().getUid())
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ShutterPost post = doc.toObject(ShutterPost.class);
                        posts.add(post);
                    }
                    postsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load posts: " + e.getMessage()));

    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter(posts);
        recyclerView.setAdapter(postsAdapter);
    }
}
