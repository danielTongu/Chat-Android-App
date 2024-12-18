package com.example.chatandroidapp.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * The Message class represents a single chat message exchanged between users.
 * All fields are immutable and final, except for `sentDate` which is auto-assigned by Firestore.
 */
public class Message implements Serializable, Comparable<Message> {

    /** Server-side timestamp for when the message was sent. Auto-assigned by Firestore. */
    @ServerTimestamp
    @PropertyName("sentDate")
    public final Date sentDate;

    /** Unique identifier for the message (Primary Key). */
    @PropertyName("id")
    public final String id;

    /** ID of the chat this message belongs to. */
    @PropertyName("chatId")
    public final String chatId;

    /** ID of the user who sent the message. */
    @PropertyName("senderId")
    public final String senderId;

    /** The actual message content. */
    @PropertyName("content")
    public final String content;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     * Initializes all fields to null or empty values.
     */
    public Message() {
        this.sentDate = null;
        this.id = "";
        this.chatId = "";
        this.senderId = "";
        this.content = "";
    }

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
        this.sentDate = null; // Auto-assigned by Firestore
    }

    // ==================== VALIDATION METHODS ====================

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

    // ==================== OVERRIDDEN METHODS ====================

    @NonNull
    @Override
    public String toString() {
        return content;
    }

    /**
     * Compares two messages based on their sent dates.
     *
     * @param other The other message to compare against.
     * @return A negative integer, zero, or a positive integer as this message's sentDate
     *         is earlier than, equal to, or later than the specified message's sentDate.
     */
    @Override
    public int compareTo(Message other) {
        if (this.sentDate == null && other.sentDate == null) {
            return 0; // Both dates are null
        }
        if (this.sentDate == null) {
            return -1; // Null dates are considered earlier
        }
        if (other.sentDate == null) {
            return 1; // Null dates are considered earlier
        }
        return this.sentDate.compareTo(other.sentDate); // Compare dates chronologically
    }

}