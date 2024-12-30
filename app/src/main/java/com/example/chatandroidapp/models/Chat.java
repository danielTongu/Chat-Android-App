package com.example.chatandroidapp.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Represents a conversation (chat) between multiple users.
 * Fields include chat details like ID, creator, participants, recent message, and creation date.
 */
public class Chat implements Serializable, Comparable<Chat> {

    /**
     * Server-side timestamp for when the chat was created. Auto-assigned by Firestore.
     */
    @ServerTimestamp
    @PropertyName("createdDate")
    public final Date createdDate;

    /**
     * Unique identifier for the chat.
     */
    @PropertyName("id")
    public final String id;

    /**
     * ID of the user who initiated the chat.
     */
    @PropertyName("creatorId")
    public final String creatorId;

    /**
     * List of user IDs participating in the chat.
     */
    @PropertyName("userIdList")
    public final List<String> userIdList;

    /**
     * ID of the most recent message in the chat. Mutable field.
     */
    @PropertyName("recentMessageId")
    public String recentMessageId;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     * Initializes all fields to null or empty values.
     */
    public Chat() {
        createdDate = null;
        id = "";
        creatorId = "";
        userIdList = new ArrayList<>();
        recentMessageId = "";
    }

    /**
     * Parameterized constructor to create a new Chat instance.
     *
     * @param id              Unique identifier for the chat.
     * @param creatorId       ID of the user who created the chat.
     * @param userIdList      List of user IDs participating in the chat.
     * @param recentMessageId ID of the most recent message in the chat.
     */
    public Chat(String id, String creatorId, List<String> userIdList, String recentMessageId) {
        this.id = validateId(id);
        this.creatorId = validateCreatorId(creatorId);
        this.userIdList = validateUserIdList(userIdList);
        this.recentMessageId = validateRecentMessageId(recentMessageId);
        this.createdDate = null; // Auto-assigned by Firestore
    }

    // ==================== VALIDATION METHODS ====================

    /**
     * Validates the chat ID.
     *
     * @param id The chat ID to validate.
     * @return The validated chat ID.
     * @throws IllegalArgumentException If the chat ID is null or empty.
     */
    public static String validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty.");
        }
        return id.trim();
    }

    /**
     * Validates the creator ID.
     *
     * @param creatorId The creator ID to validate.
     * @return The validated creator ID.
     * @throws IllegalArgumentException If the creator ID is null or empty.
     */
    public static String validateCreatorId(String creatorId) {
        if (creatorId == null || creatorId.trim().isEmpty()) {
            throw new IllegalArgumentException("Creator ID cannot be null or empty.");
        }
        return creatorId.trim();
    }

    /**
     * Validates the list of user IDs.
     *
     * @param userIdList The list of user IDs to validate.
     * @return The validated list of user IDs.
     * @throws IllegalArgumentException If the list is null, empty, or contains invalid IDs.
     */
    public static List<String> validateUserIdList(List<String> userIdList) {
        if (userIdList == null || userIdList.isEmpty()) {
            throw new IllegalArgumentException("User ID list cannot be null or empty.");
        }
        List<String> validatedList = new ArrayList<>();
        for (String userId : userIdList) {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("User ID in the list cannot be null or empty.");
            }
            validatedList.add(userId.trim());
        }
        return validatedList;
    }

    /**
     * Validates the recent message ID.
     *
     * @param recentMessageId The recent message ID to validate.
     * @return The validated recent message ID.
     * @throws IllegalArgumentException If the recent message ID is null.
     */
    public static String validateRecentMessageId(String recentMessageId) {
        if (recentMessageId == null) {
            throw new IllegalArgumentException("Recent Message ID cannot be null.");
        }
        return recentMessageId.trim();
    }

    // ==================== OVERRIDDEN METHODS ====================

    /**
     * Converts the Chat object into a readable string format.
     *
     * @return A string containing chat details.
     */
    @NonNull
    @Override
    public String toString() {
        String formattedDate = createdDate != null
                ? new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(createdDate)
                : "N/A";

        return String.format(Locale.getDefault(),
                "Chat ID: %s, Participants: %d, Recent Message: %s, Creator: %s, Created Date: %s",
                id, userIdList.size(), recentMessageId, creatorId, formattedDate
        );
    }

    /**
     * Compares two chats based on their creation date.
     *
     * @param other The other Chat object to compare to.
     * @return A negative integer, zero, or a positive integer as this Chat's createdDate
     * is earlier than, equal to, or later than the specified Chat's createdDate.
     */
    @Override
    public int compareTo(Chat other) {
        if (this.createdDate == null && other.createdDate == null) {
            return 0; // Both dates are null
        }
        if (this.createdDate == null) {
            return 1; // Null dates come last
        }
        if (other.createdDate == null) {
            return -1; // Null dates come last
        }
        return this.createdDate.compareTo(other.createdDate);
    }
}