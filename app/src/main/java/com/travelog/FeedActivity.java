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

import com.google.firebase.auth.FirebaseAuth;

public class FeedActivity extends AppCompatActivity {

    private String nickname;
    private int age;
    private String level;


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



}