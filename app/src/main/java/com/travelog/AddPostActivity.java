package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.travelog.utils.ShutterPost;
import com.travelog.utils.SupabaseStorageHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AddPostActivity extends AppCompatActivity {

    private TextInputEditText postTitle;
    private TextInputEditText postDescription;
    private TextInputEditText postCamera;
    private TextInputEditText postLens;
    private TextInputEditText postShutterSpeed;
    private TextInputEditText postAperture;
    private MaterialButton sendPost;
    private MaterialButton selectImageBtn;
    private ImageView imagePreview;

    private String ownerNickname;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imagePreview.setImageURI(uri);
                    imagePreview.setVisibility(View.VISIBLE);
                    extractExifData(uri);
                }
            }
    );

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
        postCamera = findViewById(R.id.post_camera);
        postLens = findViewById(R.id.post_lens);
        postShutterSpeed = findViewById(R.id.post_shutter_speed);
        postAperture = findViewById(R.id.post_aperture);
        
        sendPost = findViewById(R.id.send_post);
        selectImageBtn = findViewById(R.id.select_image_btn);
        imagePreview = findViewById(R.id.image_preview);

        selectImageBtn.setOnClickListener(v -> mGetContent.launch("image/*"));

        sendPost.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageAndSendPost();
            } else {
                sendPost(null);
            }
        });
    }

    private void extractExifData(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return;
            
            ExifInterface exif = new ExifInterface(inputStream);
            
            String model = exif.getAttribute(ExifInterface.TAG_MODEL);
            String make = exif.getAttribute(ExifInterface.TAG_MAKE);
            String shutter = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            String aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER);
            String lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL);

            if (make != null || model != null) {
                String cameraName = (make != null ? make : "") + (model != null ? " " + model : "");
                postCamera.setText(cameraName.trim());
            }
            
            if (lensModel != null) {
                postLens.setText(lensModel);
            }

            if (shutter != null) {
                try {
                    double shutterValue = Double.parseDouble(shutter);
                    if (shutterValue < 1.0) {
                        postShutterSpeed.setText("1/" + Math.round(1.0 / shutterValue));
                    } else {
                        postShutterSpeed.setText(shutterValue + "s");
                    }
                } catch (NumberFormatException e) {
                    postShutterSpeed.setText(shutter);
                }
            }

            if (aperture != null) {
                postAperture.setText("f/" + aperture);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error reading EXIF data", e);
        }
    }

    private void uploadImageAndSendPost() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            File tempFile = File.createTempFile("upload", ".jpg", getCacheDir());
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            inputStream.close();

            String fileName = "posts/" + UUID.randomUUID().toString() + ".jpg";
            SupabaseStorageHelper.uploadPicture(tempFile, fileName, (success, url, error) -> {
                if (success) {
                    sendPost(url);
                } else {
                    Toast.makeText(this, "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error preparing image for upload", e);
            Toast.makeText(this, "Error preparing image", Toast.LENGTH_SHORT).show();
        }
    }

    private ShutterPost createTravelPost(String imageUrl) {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        ownerNickname = sharedPreferences.getString("nickname", "N/A");

        return new ShutterPost(
                postTitle.getText().toString(),
                postDescription.getText().toString(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                ownerNickname,
                Timestamp.now(),
                imageUrl,
                postCamera.getText().toString(),
                postLens.getText().toString(),
                postShutterSpeed.getText().toString(),
                postAperture.getText().toString()
        );
    }

    public void sendPost(String imageUrl) {
        Log.d(TAG, "sendPost: start");
        ShutterPost post = createTravelPost(imageUrl);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddPostActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
