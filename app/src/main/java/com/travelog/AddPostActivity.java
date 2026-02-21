package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddPostActivity extends AppCompatActivity {

    private TextInputEditText postTitle;
    private TextInputEditText postDescription;
    private TextInputEditText postCamera;
    private TextInputEditText postLens;
    private TextInputEditText postShutterSpeed;
    private TextInputEditText postAperture;
    private AutoCompleteTextView postCategory;
    private MaterialButton sendPost;
    private MaterialButton selectImageBtn;
    private RecyclerView rvImagePreviews;
    private ImagePreviewAdapter previewAdapter;

    private String ownerNickname;
    private List<Uri> selectedImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedImageUris.addAll(uris);
                    if (selectedImageUris.size() > 10) {
                        selectedImageUris = selectedImageUris.subList(0, 10);
                        Toast.makeText(this, "Only the first 10 images were selected", Toast.LENGTH_SHORT).show();
                    }
                    rvImagePreviews.setVisibility(View.VISIBLE);
                    previewAdapter.notifyDataSetChanged();
                    extractExifData(selectedImageUris.get(0));
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
        postCategory = findViewById(R.id.post_category);
        
        sendPost = findViewById(R.id.send_post);
        selectImageBtn = findViewById(R.id.select_image_btn);
        rvImagePreviews = findViewById(R.id.rv_image_previews);

        rvImagePreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        previewAdapter = new ImagePreviewAdapter();
        rvImagePreviews.setAdapter(previewAdapter);

        // Setup Category Dropdown
        String[] categories = new String[]{"Landscape", "Portrait", "Street", "Nature", "Architecture", "Wildlife", "Macro", "Event", "Astro", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        postCategory.setAdapter(adapter);

        selectImageBtn.setOnClickListener(v -> mGetContent.launch("image/*"));

        sendPost.setOnClickListener(v -> {
            if (!selectedImageUris.isEmpty()) {
                uploadImagesAndSendPost();
            } else {
                sendPost(new ArrayList<>());
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

    private void uploadImagesAndSendPost() {
        List<String> uploadedUrls = new ArrayList<>();
        AtomicInteger remaining = new AtomicInteger(selectedImageUris.size());

        for (Uri uri : selectedImageUris) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
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
                        uploadedUrls.add(url);
                    } else {
                        Log.e(TAG, "Image upload failed: " + error);
                    }
                    
                    if (remaining.decrementAndGet() == 0) {
                        sendPost(uploadedUrls);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing image for upload", e);
                if (remaining.decrementAndGet() == 0) {
                    sendPost(uploadedUrls);
                }
            }
        }
    }

    private ShutterPost createTravelPost(List<String> imageUrls) {
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        ownerNickname = sharedPreferences.getString("nickname", "N/A");

        return new ShutterPost(
                postTitle.getText().toString(),
                postDescription.getText().toString(),
                FirebaseAuth.getInstance().getCurrentUser().getUid(),
                ownerNickname,
                Timestamp.now(),
                imageUrls,
                postCamera.getText().toString(),
                postLens.getText().toString(),
                postShutterSpeed.getText().toString(),
                postAperture.getText().toString(),
                postCategory.getText().toString()
        );
    }

    public void sendPost(List<String> imageUrls) {
        Log.d(TAG, "sendPost: start");
        ShutterPost post = createTravelPost(imageUrls);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(AddPostActivity.this, "Post saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(AddPostActivity.this, "Error saving post: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageURI(selectedImageUris.get(position));
        }

        @Override
        public int getItemCount() {
            return selectedImageUris.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.iv_preview);
            }
        }
    }
}
