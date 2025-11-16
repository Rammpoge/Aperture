package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.travelog.utils.TravelPost;

import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {

    private String nickname;
    private int age;
    private String level;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<TravelPost> posts;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button logOutButton = findViewById(R.id.logOutButton);
        TextView pageTitle = findViewById(R.id.pageTitle);
        ImageButton addPostButton = findViewById(R.id.addPostButton);

        addPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to start AddPostActivity
                Intent intent = new Intent(FeedActivity.this, AddPostActivity.class);
                startActivity(intent);
            }
        });

        logOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                // Navigate back to LoginActivity
                Intent intent=new Intent(FeedActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        pageTitle.setText("Feed");

        readUserData();

        findViewById(R.id.pageTitle); {

            pageTitle.setText(nickname + " (lvl." + level + ")");
        }

        posts = new ArrayList<>();
        initRecyclerView();
        loadPosts();
    }

    private void readUserData(){
        Log.d(TAG, "readUserData: start");
        //about to read data from userInfo.xml
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - "N/A" is a default value if nickname is not found in the file
        nickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + nickname);
        // age - 0 is a default value if age is not found in the file
        age = sharedPreferences.getInt("age", 0);
        Log.d(TAG, "readUserData: age: " + age);
        //reading level of user
        level = sharedPreferences.getString("level", "67420");
        Log.d(TAG, "readUserData: level: " + level);
    }

    private void loadPosts() {
        Log.d(TAG, "loadPosts: start");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    posts.clear();
                    Log.d(TAG, "loadPosts succeeded: " + queryDocumentSnapshots.size() + " documents");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        TravelPost post = doc.toObject(TravelPost.class);
                        posts.add(post);
                    }
                    postsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load posts: " + e.getMessage()));
    }


    private void initRecyclerView()
    {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter(posts);
        recyclerView.setAdapter(postsAdapter);
    }




}