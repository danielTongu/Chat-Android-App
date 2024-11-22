package com.example.chatandroidapp.module;

import java.io.Serializable;

/**
 * The User class represents a user in the chat application.
 * It stores the user's personal and authentication-related details and implements
 * the Serializable interface to allow passing User objects between activities.
 *
 * @author Daniel Tongu
 */
public class User implements Serializable {

    /** The first name of the user.*/
    public String firstName;

    /** The last name of the user.*/
    public String lastName;

    /** The Base64-encoded string representing the user's profile image.*/
    public String image;

    /** The email address of the user.*/
    public String email;

    /** The Firebase Cloud Messaging (FCM) token for the user, used for push notifications.*/
    public String token;

    /** The unique ID of the user, typically assigned by Firestore.*/
    public String id;
}