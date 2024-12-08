package com.example.chatandroidapp.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * The Chat class represents a conversation between multiple users.
 * It contains information about the chat, its participants, and the messages exchanged.
 *
 * Messages are automatically sorted by the time they were sent.
 *
 * @author Daniel Tongu
 */
public class Chat {

    /** The unique identifier for this chat. */
    public String id;

    /** The ID of the user who created this chat. */
    public String creatorId;

    /** The date when this chat was created.*/
    public final Date dateCreated;

    /** A list of user IDs representing the participants in this chat. */
    public List<String> userIds;

    /** A list of messages in this chat, sorted by the time they were sent. */
    public List<ChatMessage> messages;

    /**
     * Default constructor required for serialization and deserialization.
     * Initializes an empty chat with the current date as its creation time.
     */
    public Chat() {
        this.id = "";
        this.creatorId = "";
        this.dateCreated = new Date(); // Set the creation time to the current date
        this.userIds = new ArrayList<>();
        this.messages = new ArrayList<>();
    }

    /**
     * Parameterized constructor for creating a chat with specific details.
     *
     * @param id         The unique ID for this chat.
     * @param creatorId  The ID of the user who created this chat.
     * @param userIds    A list of user IDs participating in this chat.
     */
    public Chat(String id, String creatorId, List<String> userIds) {
        this.id = id;
        this.creatorId = creatorId;
        this.dateCreated = new Date(); // Set the creation time to the current date
        this.userIds = new ArrayList<>(userIds);
        this.messages = new ArrayList<>();
    }

    /**
     * Adds a message to the chat and ensures messages are sorted by time.
     *
     * @param message The message to add.
     */
    public void addMessage(ChatMessage message) {
        this.messages.add(message);
        this.messages.sort(Comparator.comparing(m -> m.dateObject));
    }

    /**
     * Retrieves the most recent message in the chat.
     *
     * @return The latest message, or {@code null} if no messages exist.
     */
    public ChatMessage getLastMessage() {
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id='" + id + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", dateCreated=" + dateCreated +
                ", userIds=" + userIds +
                ", messages=" + messages +
                '}';
    }
}