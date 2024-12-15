package com.example.chatandroidapp.utilities;

/**
 * Constants is a utility class that holds all the constant values used across the application.
 * This includes keys for SharedPreferences, database collections, user attributes, and Toast types.
 *
 *@author  Daniel Tongu
 */
public class Constants {
    public static final int KEY_PASSWORD_MIN_LENGTH = 5;
    public static final String KEY_ACTION_TYPE = "actionType";
    public static final String ACTION_SIGN_UP = "signUp";
    public static final String ACTION_SIGN_IN = "signIn";
    public static final String ACTION_UPDATE_PHONE = "updatePhone";

    // Collection Names
    public static final String KEY_COLLECTION_USERS = "Users";
    public static final String KEY_COLLECTION_CHATS = "Chats";
    public static final String KEY_COLLECTION_MESSAGES = "Messages";

    // User Fields
    public static final String KEY_USER_ID = "id";
    public static final String KEY_FIRST_NAME = "firstName";
    public static final String KEY_LAST_NAME = "lastName";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_HASHED_PASSWORD = "hashedPassword";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_CREATED_DATE = "createdDate";

    // Chat Fields
    public static final String KEY_CHAT_ID = "id";
    public static final String KEY_CREATOR_ID = "creatorId";
    public static final String KEY_USER_ID_LIST = "userIdList";
    public static final String KEY_RECENT_MESSAGE_ID = "recentMessageId";

    // Message Fields
    public static final String KEY_MESSAGE_ID = "id";
    public static final String KEY_CHAT_ID_FIELD = "chatId";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_SENT_DATE = "sentDate";

    // Other Keys
    public static final String KEY_ID = "id"; // Used for retrieving current user ID from preferences

    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
}