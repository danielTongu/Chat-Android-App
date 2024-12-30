package com.example.chatandroidapp.utilities;

/**
 * Constants is a utility class that holds all the constant values used across the application.
 * This includes keys for SharedPreferences, database collections, user attributes, and Toast types.
 */
public class Constants {
    public static final String KEY_ACTION_TYPE = "actionType";

    // Collection Names
    public static final String KEY_COLLECTION_USERS = "Users";
    public static final String KEY_COLLECTION_CHATS = "Chats";
    public static final String KEY_COLLECTION_MESSAGES = "Messages";

    // User Fields
    public static final String KEY_FIRST_NAME = "firstName";
    public static final String KEY_LAST_NAME = "lastName";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_FCM_TOKEN = "fcmToken";

    // General Keys
    public static final String KEY_ID = "id";
}