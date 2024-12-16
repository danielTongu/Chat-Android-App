// Message.java
package com.example.chatandroidapp.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * The Message class represents a single chat message exchanged between users.
 */
public class Message implements Serializable {
    @ServerTimestamp
    public Date sentDate = null;    // Server-side timestamp
    public String id = "";          // Unique identifier (Primary Key)
    public String chatId = "";      // ID of the chat this message belongs to
    public String senderId = "";    // ID of the user who sent the message
    public String content = "";     // The actual message content

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public Message() {}

    /**
     * Parameterized constructor to create a new Message instance.
     *
     * @param id        The unique identifier for the message.
     * @param chatId    The ID of the chat this message belongs to.
     * @param senderId  The ID of the user who sent the message.
     * @param content   The content of the message.
     * @throws IllegalArgumentException If any of the provided parameters are invalid.
     */
    public Message(String id, String chatId, String senderId, String content) throws IllegalArgumentException {
        this.id = validateId(id);
        this.chatId = validateChatId(chatId);
        this.senderId = validateSenderId(senderId);
        this.content = validateContent(content);
        this.sentDate = new Date(); // This will be overridden by Firestore's @ServerTimestamp
    }

    /**
     * Validates the message ID.
     *
     * @param id The message ID to validate.
     * @return The validated message ID.
     * @throws IllegalArgumentException If the message ID is null or empty.
     */
    public static String validateId(String id) throws IllegalArgumentException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be null or empty.");
        }
        return id.trim();
    }

    /**
     * Validates the chat ID.
     *
     * @param chatId The chat ID to validate.
     * @return The validated chat ID.
     * @throws IllegalArgumentException If the chat ID is null or empty.
     */
    public static String validateChatId(String chatId) throws IllegalArgumentException {
        if (chatId == null || chatId.trim().isEmpty()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty.");
        }
        return chatId.trim();
    }

    /**
     * Validates the sender ID.
     *
     * @param senderId The sender ID to validate.
     * @return The validated sender ID.
     * @throws IllegalArgumentException If the sender ID is null or empty.
     */
    public static String validateSenderId(String senderId) throws IllegalArgumentException {
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender ID cannot be null or empty.");
        }
        return senderId.trim();
    }

    /**
     * Validates the message content.
     *
     * @param content The message content to validate.
     * @return The validated message content.
     * @throws IllegalArgumentException If the content is null or empty.
     */
    public static String validateContent(String content) throws IllegalArgumentException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be null or empty.");
        }
        return content.trim();
    }

    @NonNull
    @Override
    public String toString() {
        return content;
    }
}