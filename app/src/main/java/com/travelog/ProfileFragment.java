package com.travelog;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

public class ProfileFragment extends Fragment {

    private String nickname;
    private RecyclerView recyclerView;
    private PostsAdapter postsAdapter;
    private List<ShutterPost> posts = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        readUserData();

        TextView pageTitle = view.findViewById(R.id.pageTitle);
        if (nickname != null && !nickname.equals("N/A")) {
            pageTitle.setText(nickname + "'s Posts");
        }

        recyclerView = view.findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postsAdapter = new PostsAdapter(posts);
        recyclerView.setAdapter(postsAdapter);

        loadPosts();

        return view;
    }

    private void readUserData() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("userInfo", Context.MODE_PRIVATE);
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
                        post.setPostId(doc.getId());
                        posts.add(post);
                    }
                    postsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load posts: " + e.getMessage()));
    }
}
