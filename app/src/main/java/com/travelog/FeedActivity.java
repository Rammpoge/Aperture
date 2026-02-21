package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.travelog.utils.PostsAdapter;
import com.travelog.utils.ShutterPost;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedActivity extends AppCompatActivity {

    private String nickname;
    private int age;
    private String level;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<ShutterPost> allPosts = new ArrayList<>();
    private List<ShutterPost> filteredPosts = new ArrayList<>();

    private Chip categoryFilterChip;
    private Chip equipmentFilterChip;

    private Set<String> selectedCategories = new HashSet<>();
    private Set<String> selectedEquipments = new HashSet<>();

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

        ImageButton logOutButton = findViewById(R.id.logOutButton);
        TextView pageTitle = findViewById(R.id.pageTitle);
        ImageButton addPostButton = findViewById(R.id.addPostButton);
        ImageButton myPostsButton = findViewById(R.id.myPostsButton);

        categoryFilterChip = findViewById(R.id.chip_filter_category);
        equipmentFilterChip = findViewById(R.id.chip_filter_equipment);

        categoryFilterChip.setOnClickListener(v -> showCategoryFilterDialog());
        equipmentFilterChip.setOnClickListener(v -> showEquipmentFilterDialog());

        myPostsButton.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, MyPostsActivity.class);
            startActivity(intent);
        });

        addPostButton.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, AddPostActivity.class);
            startActivity(intent);
        });

        logOutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        readUserData();
        pageTitle.setText(nickname + " (lvl." + level + ")");

        initRecyclerView();
        registerToNewPosts();
    }

    private void showCategoryFilterDialog() {
        String[] categories = new String[]{"Unassigned", "Landscape", "Portrait", "Street", "Nature", "Architecture", "Wildlife", "Macro", "Event", "Astro", "Other"};
        boolean[] checkedItems = new boolean[categories.length];
        for (int i = 0; i < categories.length; i++) {
            checkedItems[i] = selectedCategories.contains(categories[i]);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Categories")
                .setMultiChoiceItems(categories, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedCategories.add(categories[which]);
                    } else {
                        selectedCategories.remove(categories[which]);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    updateCategoryChipText();
                    applyFilters();
                })
                .setNegativeButton("Clear All", (dialog, which) -> {
                    selectedCategories.clear();
                    updateCategoryChipText();
                    applyFilters();
                })
                .show();
    }

    private void updateCategoryChipText() {
        if (selectedCategories.isEmpty()) {
            categoryFilterChip.setText("All Categories");
        } else if (selectedCategories.size() == 1) {
            categoryFilterChip.setText(selectedCategories.iterator().next());
        } else {
            categoryFilterChip.setText("Categories (" + selectedCategories.size() + ")");
        }
    }

    private void showEquipmentFilterDialog() {
        Set<String> equipmentSet = new HashSet<>();
        equipmentSet.add("Unassigned");
        for (ShutterPost post : allPosts) {
            if (post.getCamera() != null && !post.getCamera().isEmpty()) {
                equipmentSet.add(post.getCamera());
            }
        }
        
        String[] equipments = equipmentSet.toArray(new String[0]);
        boolean[] checkedItems = new boolean[equipments.length];
        for (int i = 0; i < equipments.length; i++) {
            checkedItems[i] = selectedEquipments.contains(equipments[i]);
        }
        
        new AlertDialog.Builder(this)
                .setTitle("Select Equipment")
                .setMultiChoiceItems(equipments, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedEquipments.add(equipments[which]);
                    } else {
                        selectedEquipments.remove(equipments[which]);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    updateEquipmentChipText();
                    applyFilters();
                })
                .setNegativeButton("Clear All", (dialog, which) -> {
                    selectedEquipments.clear();
                    updateEquipmentChipText();
                    applyFilters();
                })
                .show();
    }

    private void updateEquipmentChipText() {
        if (selectedEquipments.isEmpty()) {
            equipmentFilterChip.setText("All Equipment");
        } else if (selectedEquipments.size() == 1) {
            equipmentFilterChip.setText(selectedEquipments.iterator().next());
        } else {
            equipmentFilterChip.setText("Equipment (" + selectedEquipments.size() + ")");
        }
    }

    private void applyFilters() {
        filteredPosts.clear();
        for (ShutterPost post : allPosts) {
            boolean categoryMatch = false;
            if (selectedCategories.isEmpty()) {
                categoryMatch = true;
            } else {
                String postCat = post.getCategory();
                if (selectedCategories.contains("Unassigned")) {
                    if (postCat == null || postCat.isEmpty() || postCat.equals("Category")) {
                        categoryMatch = true;
                    }
                }
                if (!categoryMatch && postCat != null && selectedCategories.contains(postCat)) {
                    categoryMatch = true;
                }
            }
            
            boolean equipmentMatch = false;
            if (selectedEquipments.isEmpty()) {
                equipmentMatch = true;
            } else {
                String postCam = post.getCamera();
                if (selectedEquipments.contains("Unassigned")) {
                    if (postCam == null || postCam.isEmpty()) {
                        equipmentMatch = true;
                    }
                }
                if (!equipmentMatch && postCam != null && selectedEquipments.contains(postCam)) {
                    equipmentMatch = true;
                }
            }
            
            if (categoryMatch && equipmentMatch) {
                filteredPosts.add(post);
            }
        }
        postsAdapter.notifyDataSetChanged();
    }

    private void readUserData(){
        SharedPreferences sharedPreferences = getSharedPreferences("userInfo", MODE_PRIVATE);
        nickname = sharedPreferences.getString("nickname", "N/A");
        age = sharedPreferences.getInt("age", 0);
        level = sharedPreferences.getString("level", "67420");
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new PostsAdapter(filteredPosts);
        recyclerView.setAdapter(postsAdapter);
    }

    private void registerToNewPosts() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("posts")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            ShutterPost post = dc.getDocument().toObject(ShutterPost.class);
                            switch (dc.getType()) {
                                case ADDED:
                                    allPosts.add(0, post);
                                    break;
                                case MODIFIED:
                                    // Handle modification if necessary
                                    break;
                                case REMOVED:
                                    allPosts.removeIf(p -> p.getCreatedAt().equals(post.getCreatedAt()) && p.getOwnerUid().equals(post.getOwnerUid()));
                                    break;
                            }
                        }
                        applyFilters();
                    }
                });
    }
}
