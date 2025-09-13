package com.pizzamania.push;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM";

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New FCM token: " + token);
        saveToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = "PizzaMania";
        String body = "You have a new message";
        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) title = message.getNotification().getTitle();
            if (message.getNotification().getBody() != null) body = message.getNotification().getBody();
        } else if (message.getData() != null) {
            if (message.getData().get("title") != null) title = message.getData().get("title");
            if (message.getData().get("body") != null) body = message.getData().get("body");
        }
        NotificationHelper.INSTANCE.show(getApplicationContext(), title, body);
    }

    private void saveToken(String token) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            Map<String, Object> meta = new HashMap<>();
            meta.put("createdAt", System.currentTimeMillis());
            if (uid != null) {
                db.collection("users").document(uid).collection("fcmTokens")
                        .document(token).set(meta);
            } else {
                db.collection("anonTokens").document(token).set(meta);
            }
        } catch (Exception ignored) { }
    }
}
