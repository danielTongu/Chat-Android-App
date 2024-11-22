package com.example.chatandroidapp.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * MessagingService handles incoming messages and token refreshes from Firebase Cloud Messaging.
 * It extends FirebaseMessagingService to receive notifications and data messages.
 */
public class MessagingService extends FirebaseMessagingService {

    /**
     * Called when a new token for the default Firebase project is generated.
     * This occurs after initial startup of the app, and whenever the token is refreshed.
     * @param token The new token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Log the new FCM token
        Log.d("FCM", "Token: " + token);

        //Send the token to your server to keep it updated
    }

    /**
     * Called when a message is received.
     * @param message The message that was received.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        // Log the received message's body if it contains a notification
        if (message.getNotification() != null) {
            Log.d("FCM", "460 Message: " + message.getNotification().getBody());
        }

        // Handle the message and display a notification or update the UI as needed
    }
}