package com.example.chatandroidapp.module;

import java.util.Date;

/**
 * The ChatMessage class represents a single chat message exchanged between two users.
 * It contains information about the sender, receiver, message content, and timestamp.
 *
 * @author Daniel Tongu
 */
public class ChatMessage {
    /** The date and time when the message was sent, in a readable string format.*/
    public String dateTime;

    /** The Date object representing the timestamp of the message, used for sorting and comparisons.*/
    public Date dateObject;

    /** The ID of the user who received the message.*/
    public String receiverId;

    /** The ID of the user who sent the message.*/
    public String senderId;

    /** The first name of the user who sent the message.*/
    public String senderFirstName;

    /** The last name of the user who sent the message.*/
    public String senderLastName;

    /** The content of the chat message.*/
    public String message;
}