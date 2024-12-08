package com.example.chatandroidapp.module;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The ChatMessage class represents a single chat message exchanged between users.
 * It contains information about the sender, message content, and timestamps.
 * The timestamps are immutable and set during object creation.
 *
 * This class is compatible with serialization frameworks like Firestore.
 *
 * @author Daniel Tongu
 */
public class ChatMessage {

    /**
     * The date and time when the message was sent, in a human-readable string format.
     * For example, "2024-12-08 10:15 PM". This field is immutable.
     */
    public final String dateTime;

    /**
     * The Date object representing the timestamp of the message,
     * used for sorting and comparisons. This field is immutable.
     */
    public final Date dateObject;

    /**
     * The ID of the user who sent the message.
     */
    public String senderId;

    /**
     * The content of the chat message.
     */
    public String message;

    /**
     * Default constructor required for serialization and deserialization.
     * Sets the timestamp to the current date and time.
     */
    public ChatMessage() {
        this.dateObject = new Date(); // Default to current date and time
        this.dateTime = formatDateTime(this.dateObject);
        this.senderId = "";
        this.message = "";
    }

    /**
     * Parameterized constructor for creating a ChatMessage with specified values.
     *
     * @param senderId The ID of the message sender.
     * @param message  The content of the message.
     */
    public ChatMessage(String senderId, String message) {
        this.dateObject = new Date(); // Set the timestamp to the current date and time
        this.dateTime = formatDateTime(this.dateObject);
        this.senderId = senderId;
        this.message = message;
    }

    /**
     * Formats the given Date object into a human-readable string.
     *
     * @param date The Date object to format.
     * @return A formatted date-time string.
     */
    private static String formatDateTime(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        return formatter.format(date);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "dateTime='" + dateTime + '\'' +
                ", dateObject=" + dateObject +
                ", senderId='" + senderId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}