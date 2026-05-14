package com.travelog.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;


import androidx.core.app.NotificationCompat;


import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.travelog.FeedActivity;
import com.travelog.R;


import java.util.Map;

import javax.annotation.Nullable;


public class PostsNotificationService extends Service {
    private static final String POST_CHANNEL_ID = "POST_CHANNEL_ID";

    private boolean mAfterFirstDBLoad;

    private static final String TAG = "PostsNotificationService";

    @Override
    public void onCreate()
    {
        Log.d(TAG, "onCreate: start");
        super.onCreate();
        Log.d(TAG, "onCreate: done");
    }


    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: start");

        mAfterFirstDBLoad = false;
        createPostNotificationChannel();

        sendNotification("The awsomest photo platform","",1, true);
        listenToChangesInPosts();

        // If we get killed, after returning from here, restart
        Log.d(TAG, "onStartCommand: done");
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: start");

        Log.d(TAG, "onDestroy: done");
    }


    private void listenToChangesInPosts()
    {
        Log.d(TAG, "listenToChangesInPosts: start");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();


        firestore.collection("posts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }


                        if (!mAfterFirstDBLoad) {
                            Log.w(TAG, "listen:first update is ignored");
                            mAfterFirstDBLoad = true;
                            return;
                        }


                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    Log.d(TAG, "New post: " + dc.getDocument().getData());
                                    sendPostNotification(dc.getDocument().getData(), dc.getDocument().getId().hashCode());
                                    break;
                                case MODIFIED:
                                    Log.d(TAG, "Modified post: " + dc.getDocument().getData());
                                    break;
                                case REMOVED:
                                    Log.d(TAG, "Removed post: " + dc.getDocument().getData());
                                    break;
                            }
                        }


                    }
                });
        Log.d(TAG, "listenToChangesInPosts: done");
    }


    private void sendPostNotification(Map<String,Object> post, int notificationId)
    {
        Log.d(TAG, "sendPostNotification: start");

        String nickName = "N/A";
        String title = "N/A";

        if(post.get("ownerNickname") != null)
            nickName = post.get("ownerNickname").toString();

        if(post.get("title") != null)
            title = post.get("title").toString();

        sendNotification(nickName, title, notificationId, false);

        Log.d(TAG, "sendPostNotification: done");
    }

    private void sendNotification(String title, String content, int notificationId, boolean startForeground)
    {
        Log.d(TAG, "sendNotification: start");

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null || !notificationManager.areNotificationsEnabled())
        {
            Log.d(TAG, "sendNotification: notifications are not allowed");
            return;
        }

        // Create an Intent for the activity you want to start.
        Intent resultIntent = new Intent(getApplicationContext(), FeedActivity.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back
        // stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack.
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, POST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(resultPendingIntent);

        Notification notification = builder.build();

        if(startForeground)
            startForeground(notificationId, notification);
        else
            notificationManager.notify(notificationId, notification);

        Log.d(TAG, "sendNotification: done");
    }


    private void createPostNotificationChannel() {
        Log.d(TAG, "createPostNotificationChannel: start");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "New post channel";
            String description = "A new post channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(POST_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Log.d(TAG, "createPostNotificationChannel: done");
    }
}

