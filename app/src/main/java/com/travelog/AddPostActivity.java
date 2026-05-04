package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.travelog.utils.GeminiManager;
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
    private MaterialButton generateAiDescBtn;
    private BottomNavigationView bottomNavigationView;
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
        generateAiDescBtn = findViewById(R.id.btn_generate_ai_description);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        rvImagePreviews = findViewById(R.id.rv_image_previews);

        rvImagePreviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        previewAdapter = new ImagePreviewAdapter();
        rvImagePreviews.setAdapter(previewAdapter);

        // Setup Bottom Navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_add);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, FeedActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                return true;
            } else if (itemId == R.id.nav_my_posts) {
                Intent intent = new Intent(this, MyPostsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        // Setup Category Dropdown
        String[] categories = new String[]{"Landscape", "Portrait", "Street", "Nature", "Architecture", "Wildlife", "Macro", "Event", "Astro", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        postCategory.setAdapter(adapter);

        selectImageBtn.setOnClickListener(v -> mGetContent.launch("image/*"));

        generateAiDescBtn.setOnClickListener(v -> generateAiDescription());

        sendPost.setOnClickListener(v -> {
            if (!selectedImageUris.isEmpty()) {
                uploadImagesAndSendPost();
            } else {
                sendPost(new ArrayList<>());
            }
        });
    }

    private void generateAiDescription() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image first", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Bitmap> bitmaps = new ArrayList<>();
        try {
            for (Uri imageUri : selectedImageUris) {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                    bitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                }

                // Convert to software bitmap if needed
                if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }
                bitmaps.add(bitmap);
            }

            Toast.makeText(this, "AI is analyzing the photos...", Toast.LENGTH_SHORT).show();

            GeminiManager.getInstance().sendImagesAndText(
                    bitmaps,
                    "Analyze these photographs and provide a single structured description representing the overall theme of the post. Use exactly these fields. Keep each field's answer under 13 words:\n" +
                            "genre: (choose from: Landscape, Portrait, Street, Nature, Architecture, Wildlife, Macro, Event, Astro, Other)\n" +
                            "theme:\n" +
                            "feeling:\n" +
                            "colors:",
                    this,
                    new GeminiManager.GeminiCallback() {
                        @Override
                        public void onSuccess(String result) {
                            postDescription.setText(result);
                            
                            // Try to extract and set the category
                            try {
                                String[] lines = result.split("\n");
                                for (String line : lines) {
                                    if (line.toLowerCase().startsWith("genre:")) {
                                        String category = line.substring(6).trim();
                                        // Basic cleanup in case AI adds punctuation
                                        category = category.replaceAll("[^a-zA-Z]", "");
                                        
                                        // Match against allowed categories
                                        String[] validCategories = new String[]{"Landscape", "Portrait", "Street", "Nature", "Architecture", "Wildlife", "Macro", "Event", "Astro", "Other"};
                                        for (String valid : validCategories) {
                                            if (valid.equalsIgnoreCase(category)) {
                                                postCategory.setText(valid, false);
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("AddPostActivity", "Error parsing AI category", e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            Log.e("AddPostActivity", "Gemini error", error);
                            Toast.makeText(AddPostActivity.this, "AI analysis failed", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } catch (IOException e) {
            Log.e("AddPostActivity", "Image load error", e);
            Toast.makeText(this, "Failed to load images for analysis", Toast.LENGTH_SHORT).show();
        }
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
