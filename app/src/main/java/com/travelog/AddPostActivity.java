package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.type.Date;
import com.travelog.utils.TravelPost;



public class AddPostActivity extends AppCompatActivity {

    private TextInputEditText postTitle;
    private TextInputEditText postDescription;
    private MaterialButton sendPost;

    private String ownerUid;

    private String ownerNickname;

    private Date createdAt;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        postTitle = findViewById(R.id.post_title);
        postDescription = findViewById(R.id.post_description);
        sendPost = findViewById(R.id.send_post);




        sendPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPost();
            }
        });


    }

    private TravelPost createTravelPost(){
        Log.d(TAG, "readUserData: start");
        //about to read data from userInfo.xml
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);

        // nickname - "N/A" is a default value if nickname is not found in the file
        ownerNickname = sharedPreferences.getString("nickname", "N/A");
        Log.d(TAG, "readUserData: nickname: " + ownerNickname);

        TravelPost travelPost = new TravelPost(
                postTitle.getText().toString(),
                postDescription.getText().toString(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                ownerNickname,
                Timestamp.now()
        );
        return travelPost;

    }



    public void sendPost() {
        Log.d(TAG, "sendPost: start");
        TravelPost post = createTravelPost();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddPostActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close this activity and return to FeedActivity
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        Log.d(TAG, "sendPost: done");

    }
}