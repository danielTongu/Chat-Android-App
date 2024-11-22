package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.adapters.UsersAdapter;
import com.example.chatandroidapp.databinding.ActivityUserBinding;
import com.example.chatandroidapp.listeners.UserListener;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * UserActivity displays a list of all registered users in the app, excluding the currently signed-in user.
 * It allows the user to initiate a chat by selecting another user from the list.
 *
 * @author Daniel Tongu
 */
public class UserActivity extends AppCompatActivity implements UserListener {

    private ActivityUserBinding binding; // View binding for activity_user.xml
    private PreferenceManager preferenceManager; // Manages shared preferences

    /**
     * Called when the activity is first created.
     * Initializes the UI, sets up listeners, and fetches the list of users.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUserBinding.inflate(getLayoutInflater());
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        setContentView(binding.getRoot());
        setListeners();
        getUsers();
    }

    /**
     * Sets up listeners for interactive UI components.
     * Currently, it handles navigation back to the previous screen.
     */
    private void setListeners() {
        // Navigate back to the previous activity when the back icon is clicked
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Fetches the list of all registered users from Firestore, excluding the currently signed-in user.
     * Populates the RecyclerView with the list of users.
     */
    private void getUsers() {
        loading(true); // Show loading indicator while fetching users
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Query the "Users" collection in Firestore
        database.collection(Constants.KEY_COLLECTION_USERS).get()
                .addOnCompleteListener(task -> {
                    loading(false);// Hide loading indicator after the query completes

                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();

                        // Iterate through the query results to build the user list
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            // Skip the currently signed-in user
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }

                            // Create a User object from Firestore data
                            User user = new User();
                            user.firstName = queryDocumentSnapshot.getString(Constants.KEY_FIRST_NAME);
                            user.lastName = queryDocumentSnapshot.getString(Constants.KEY_LAST_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();

                            // Add the user to the list
                            users.add(user);
                        }

                        // If there are users, display them in the RecyclerView
                        if (!users.isEmpty()) {
                            UsersAdapter usersAdapter = new UsersAdapter(users, this);
                            binding.usersRecyclerView.setAdapter(usersAdapter);
                            binding.usersRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            // Show an error message if no users are available
                            showErrorMessage();
                        }
                    } else {
                        // Show an error message if the query failed
                        showErrorMessage();
                    }
                });
    }

    /**
     * Displays an error message if no users are found or if the query fails.
     */
    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", "No User Available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    /**
     * Toggles the visibility of the progress bar based on the loading state.
     * @param isLoading true to show the progress bar, false to hide it.
     */
    private void loading(Boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Handles the user selection event from the RecyclerView.
     * Navigates to the ChatActivity with the selected user's details.
     * @param user The selected user to initiate a chat with.
     */
    @Override
    public void OnUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user); // Pass the selected user to ChatActivity
        startActivity(intent);
        finish(); // Finish the current activity to prevent stacking
    }
}