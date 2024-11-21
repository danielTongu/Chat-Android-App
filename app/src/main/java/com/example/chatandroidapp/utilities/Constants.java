package com.example.chatandroidapp.utilities;

/**
 * Constants is a utility class that holds all the constant values used across the application.
 * This includes keys for SharedPreferences, database collections, user attributes, and Toast types.
 *
 * @author Daniel Tongu
 */
public class Constants {

    /**
     * Key for the Users collection in Firebase Firestore.
     */
    public static final String KEY_COLLECTION_USERS = "Users";

    /**
     * Key for the user's name.
     */
    public static final String KEY_NAME = "name";

    /**
     * Key for the user's email.
     */
    public static final String KEY_EMAIL = "email";

    /**
     * Key for the user's password.
     */
    public static final String KEY_PASSWORD = "password";

    /**
     * Key for the user's ID.
     */
    public static final String KEY_USER_ID = "userid";

    /**
     * Key indicating if the user is signed in.
     */
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    /**
     * Key for the name of the SharedPreferences file.
     */
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";

    /**
     * Key for the user's profile image.
     */
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE  = "message";
    public static final String KEY_TIMESTAMP = "timeStamp";


}