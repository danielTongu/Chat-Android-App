package com.example.chatandroidapp.module;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The User class represents a user in the chat application.
 * It stores the user's personal and authentication-related details.
 */
public class User implements Serializable {

    /** The unique ID of the user, typically assigned by Firestore. */
    public String id;

    /**
     * The date when the user was created, stored as a formatted string.
     * Automatically set during object creation and cannot be changed.
     */
    public final String createdDate;

    /** The first name of the user. */
    public String firstName;

    /** The last name of the user. */
    public String lastName;

    /** The Base64-encoded string representing the user's profile image. */
    public String image;

    /** The phone number of the user. */
    public String phone;

    /** The email address of the user. */
    public String email;

    /** The hashed version of the user's password for secure storage. */
    public String hashedPassword;

    /** The Firebase Cloud Messaging (FCM) token for the user, used for push notifications. */
    public String token;

    /** A list of unique chat room IDs the user participates in. */
    public List<String> chatIds;

    /** A map of tasks, where the key is a task ID and the value is a list of tasks for that ID. */
    public Map<String, List<String>> tasksById;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     * Automatically sets the creation date to the current date and time.
     */
    public User() {
        this.createdDate = getCurrentFormattedDate();
        this.chatIds = new ArrayList<>();
        this.tasksById = new HashMap<>();
    }

    /**
     * Formats the current date as a string in the format "yyyy-MM-dd HH:mm:ss".
     * @return A formatted string representation of the current date and time.
     */
    private String getCurrentFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date());
    }
}