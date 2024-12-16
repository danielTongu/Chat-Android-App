package com.example.chatandroidapp.utilities;

import com.example.chatandroidapp.models.User;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FirebaseUserHelper provides utility methods for fetching user details from Firestore.
 */
public class FirebaseUserHelper {

    public interface UserCallback {
        void onUserFetched(User user);
    }

    /**
     * Fetches user details from Firestore by user ID.
     *
     * @param userId   The ID of the user to fetch.
     * @param callback A callback to handle the fetched user.
     */
    public static void getUserDetails(String userId, UserCallback callback) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        callback.onUserFetched(user);
                    } else {
                        callback.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onUserFetched(null);
                });
    }
}