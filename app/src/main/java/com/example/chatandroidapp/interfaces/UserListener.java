package com.example.chatandroidapp.interfaces;

import com.example.chatandroidapp.models.User;

/**
 * UserListener is an interface that defines a callback method for handling user click events.
 * Implementing this interface allows other classes to respond when a user is clicked in a list or UI component.
 *
 * @author Daniel Tongu
 */
public interface UserListener {

    /**
     * Callback method triggered when a user is clicked.
     * @param user The User object representing the clicked user.
     */
    void OnUserClicked(User user);
}