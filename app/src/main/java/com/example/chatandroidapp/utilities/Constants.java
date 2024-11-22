package com.example.chatandroidapp.utilities;

/**
 * Constants is a utility class that holds all the constant values used across the application.
 * This includes keys for SharedPreferences, database collections, user attributes, and Toast types.
 *
 *@author  Daniel Tongu
 */
public class Constants {

    /** Key for the Users collection in Firebase Firestore.*/
    public static final String KEY_COLLECTION_USERS = "Users";

    /** Key for the user's first name.*/
    public static final String KEY_FIRST_NAME = "firstName";

    /** Key for the user's last name.*/
    public static final String KEY_LAST_NAME = "lastName";

    /** Key for the user's email.
     */
    public static final String KEY_EMAIL = "email";

    /**Key for the user's hashed password.*/
    public static final String KEY_PASSWORD = "password";

    /** Key for the user's unique ID.*/
    public static final String KEY_USER_ID = "userid";

    /** Key indicating if the user is signed in.*/
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    /** Key for the name of the SharedPreferences file.*/
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";

    /** Key for the user's profile image.*/
    public static final String KEY_IMAGE = "image";

    /** Key for the Firebase Cloud Messaging (FCM) token.*/
    public static final String KEY_FCM_TOKEN = "fcmToken";

    /** Key for user-related data.*/
    public static final String KEY_USER = "user";

    /** Key for the Chat collection in Firebase Firestore.*/
    public static final String KEY_COLLECTION_CHAT = "chat";

    /** Key for the sender's ID in a chat message.*/
    public static final String KEY_SENDER_ID = "senderId";

    /** Key for the receiver's ID in a chat message.*/
    public static final String KEY_RECEIVER_ID = "receiverId";

    /** Key for the message content in a chat.*/
    public static final String KEY_MESSAGE  = "message";

    /** Key for the timestamp of a chat message.*/
    public static final String KEY_TIMESTAMP = "timeStamp";

    /** Key for the minimum length recommended for a password.*/
    public static final int KEY_PASSWORD_MIN_LENGTH = 5;
}