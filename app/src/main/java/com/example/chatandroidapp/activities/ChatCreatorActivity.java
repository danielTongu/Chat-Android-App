// ChatCreatorActivity.java
package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.adapters.UsersAdapter;
import com.example.chatandroidapp.databinding.ActivityChatCreatorBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * ChatCreatorActivity allows the user to select contacts to start a chat.
 * Instead of creating the chat immediately, it passes the selected users
 * to the ChatActivity for further processing.
 */
public class ChatCreatorActivity extends AppCompatActivity implements UsersAdapter.OnUserSelectedListener {
    public static final String KEY_SELECTED_USERS_LIST = "selectedUsers";
    private static final String TAG = "CHAT_CREATOR_ACTIVITY";

    private ActivityChatCreatorBinding binding;
    private UsersAdapter userAdapter;
    private final List<User> userList = new ArrayList<>();
    private final List<User> selectedUsers = new ArrayList<>();
    private PreferenceManager preferenceManager;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatCreatorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: Initializing ChatCreatorActivity");

        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        currentUserId = preferenceManager.getString(Constants.KEY_ID, "");

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "onCreate: Current user ID not found in preferences.");
            Utilities.showToast(this, "Failed to retrieve user data. Please sign in again.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        setupUI();
        loadUsersFromFirestore();
    }

    /**
     * Sets up the UI components and event listeners.
     */
    private void setupUI() {
        Log.d(TAG, "setupUI: Configuring UI elements");

        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonStartChat.setOnClickListener(v -> navigateToChatActivity());

        binding.usersRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UsersAdapter(userList, this);
        binding.usersRecyclerview.setAdapter(userAdapter);

        toggleProgressBar(false, null);
    }

    /**
     * Loads the list of users from Firestore.
     */
    private void loadUsersFromFirestore() {
        Log.d(TAG, "loadUsersFromFirestore: Fetching users from Firestore");
        toggleProgressBar(true, "Loading contacts...");

        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    toggleProgressBar(false, null);

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "loadUsersFromFirestore: No users found in Firestore.");
                        binding.processMessage.setVisibility(View.VISIBLE);
                        binding.processMessage.setText("No contacts found.");
                        return;
                    }

                    populateUserList(queryDocumentSnapshots);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "loadUsersFromFirestore: Failed to fetch users.", e);
                    toggleProgressBar(false, null);
                    binding.processMessage.setVisibility(View.VISIBLE);
                    binding.processMessage.setText("Failed to load contacts. Please try again.");
                    Utilities.showToast(this, "Failed to load users: " + e.getMessage(), Utilities.ToastType.ERROR);
                });
    }

    /**
     * Populates the user list with data fetched from Firestore.
     *
     * @param queryDocumentSnapshots The result from Firestore query.
     */
    private void populateUserList(QuerySnapshot queryDocumentSnapshots) {
        Log.d(TAG, "populateUserList: Populating user list");

        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null && !user.id.equals(currentUserId)) {
                userList.add(user);
            }
        }

        if (userList.isEmpty()) {
            binding.processMessage.setVisibility(View.VISIBLE);
            binding.processMessage.setText("No contacts available.");
        } else {
            userAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Toggles the visibility of the progress bar and process message.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display.
     */
    private void toggleProgressBar(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        if (message != null) {
            binding.processMessage.setText(message);
            binding.processMessage.setVisibility(View.VISIBLE);
        } else {
            binding.processMessage.setVisibility(View.GONE);
        }
    }

    /**
     * Navigates to ChatActivity, passing the selected users as a Serializable object.
     */
    private void navigateToChatActivity() {
        if (selectedUsers.isEmpty()) {
            Utilities.showToast(this, "No users selected. Please select at least one contact.", Utilities.ToastType.WARNING);
            return;
        }

        Log.d(TAG, "navigateToChatActivity: Navigating to ChatActivity with selected users.");

        // Create an intent and add the selected users as a Serializable list
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(KEY_SELECTED_USERS_LIST, new ArrayList<>(selectedUsers));
        startActivity(intent);
        finish();
    }

    /**
     * Callback when a user is selected or deselected.
     *
     * @param user     The user that was selected or deselected.
     * @param selected Whether the user is selected.
     */
    @Override
    public void onUserSelected(User user, boolean selected) {
        if (selected) {
            selectedUsers.add(user);
        } else {
            selectedUsers.remove(user);
        }

        Log.d(TAG, "onUserSelected: Selected users count = " + selectedUsers.size());
        binding.buttonStartChat.setEnabled(!selectedUsers.isEmpty());
    }
}