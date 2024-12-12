// Chat.java
package com.example.chatandroidapp.module;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The Chat class represents a conversation between multiple users.
 */
public class Chat {
    @ServerTimestamp
    public Date createdDate = null;             // Server-side timestamp
    public String id = "";                      // Unique identifier (Primary Key)
    public String creatorId = "";               // ID of the user who created the chat
    public List<String> userIdList = new ArrayList<>(); // List of user IDs participating in the chat
    public String recentMessageId = "";         // ID of the most recent message

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public Chat() {}

    /**
     * Parameterized constructor to create a new Chat instance.
     *
     * @param id              The unique identifier for the chat.
     * @param creatorId       The ID of the user who created the chat.
     * @param userIdList      The list of user IDs participating in the chat.
     * @param recentMessageId The ID of the most recent message in the chat.
     * @throws IllegalArgumentException If any of the provided parameters are invalid.
     */
    public Chat(String id, String creatorId, List<String> userIdList, String recentMessageId) throws IllegalArgumentException {
        this.id = validateId(id);
        this.creatorId = validateCreatorId(creatorId);
        this.userIdList = validateUserIdList(userIdList);
        this.recentMessageId = validateRecentMessageId(recentMessageId);
        this.createdDate = new Date(); // This will be overridden by Firestore's @ServerTimestamp
    }

    /**
     * Validates the chat ID.
     *
     * @param id The chat ID to validate.
     * @return The validated chat ID.
     * @throws IllegalArgumentException If the chat ID is null or empty.
     */
    private static String validateId(String id) throws IllegalArgumentException {
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
    private static String validateCreatorId(String creatorId) throws IllegalArgumentException {
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
    private static List<String> validateUserIdList(List<String> userIdList) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException If the recent message ID is null or empty.
     */
    private static String validateRecentMessageId(String recentMessageId) throws IllegalArgumentException {
        if (recentMessageId == null || recentMessageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Recent Message ID cannot be null or empty.");
        }
        return recentMessageId.trim();
    }

    @Override
    public String toString() {
        return String.format("Chat ID: %s", id);
    }
}