package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivityMainBinding;
import com.example.chatandroidapp.fragments.ChatsFragment;
import com.example.chatandroidapp.fragments.ProfileFragment;
import com.example.chatandroidapp.interfaces.SearchableFragment;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity serves as the entry point for authenticated users.
 * It manages fragment navigation, Firebase token assignment, and UI initialization.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN_ACTIVITY";
    private ActivityMainBinding binding;
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment activeFragment;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        initializeUI();
    }

    /**
     * Initializes shared preferences and assigns Firebase token to the user.
     */
    private void initializeComponents() {
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        if (userId == null || userId.isEmpty()) {
            showErrorAndFinish("User not authenticated. Please sign in.");
            return;
        }

        assignFirebaseTokenToUser(userId);
    }

    /**
     * Configures UI elements such as the search view and bottom navigation.
     */
    private void initializeUI() {
        setUpBottomNavigation();
        setUpSearchView();

        String firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME, "");
        Utilities.showToast(this, "Welcome, " + firstName + "!", Utilities.ToastType.INFO);
    }

    /**
     * Assigns the Firebase token to the user in Firestore.
     *
     * @param userId The user's Firestore document ID.
     */
    private void assignFirebaseTokenToUser(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String storedToken = preferenceManager.getString(Constants.KEY_FCM_TOKEN, "");
                    if (storedToken == null || !storedToken.equals(token)) {
                        updateTokenInFirestore(userId, token);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve Firebase token", e));
    }

    /**
     * Updates the Firebase token in Firestore and preferences.
     *
     * @param userId The user's Firestore document ID.
     * @param token  The new Firebase token.
     */
    private void updateTokenInFirestore(String userId, String token) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, token);

        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update Firebase token in Firestore", e));
    }

    /**
     * Sets up the bottom navigation bar for fragment navigation.
     */
    private void setUpBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.navigation_chats) {
                selectedFragment = new ChatsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        // Set default fragment to ChatsFragment
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_chats);
    }

    /**
     * Loads the selected fragment into the container.
     *
     * @param fragment The fragment to load.
     */
    private void loadFragment(Fragment fragment) {
        if (activeFragment != fragment) {
            binding.progressBar.setVisibility(View.VISIBLE);

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();

            activeFragment = fragment;

            // Hide progress bar after a delay to simulate smooth transition
            binding.navHostFragment.postDelayed(() -> binding.progressBar.setVisibility(View.GONE), 300);
        }
    }

    /**
     * Configures the search view for filtering data in active fragments.
     */
    private void setUpSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(newText);
                }
                return false;
            }
        });
    }

    /**
     * Displays an error message and exits the activity.
     *
     * @param message The error message to display.
     */
    private void showErrorAndFinish(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        Log.e(TAG, message);
        finish();
    }
}