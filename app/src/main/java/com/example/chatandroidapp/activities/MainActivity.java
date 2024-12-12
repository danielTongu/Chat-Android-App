// MainActivity.java
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
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * MainActivity serves as the entry point for authenticated users.
 * It manages navigation between fragments and ensures user data is initialized.
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

        Log.d(TAG, "onCreate: Initializing MainActivity");

        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        // Fetch user ID from preferences
        String userId = preferenceManager.getString(Constants.KEY_ID);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "onCreate: User ID not found in preferences");
            Utilities.showToast(this, "User not authenticated. Please sign in.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        // Assign Firebase token and initialize UI
        assignFirebaseTokenToUser(userId);
        initializeUI();
    }

    /**
     * Retrieves the Firebase token and updates it in Firestore if necessary.
     *
     * @param userId The user's Firestore document ID.
     */
    private void assignFirebaseTokenToUser(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "assignFirebaseTokenToUser: Token retrieved successfully: " + token);

                    String storedToken = preferenceManager.getString(Constants.KEY_FCM_TOKEN);
                    if (storedToken == null || !token.equals(storedToken)) {
                        // Update Firestore with the new token if it has changed
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(Constants.KEY_FCM_TOKEN, token);

                        FirebaseFirestore.getInstance()
                                .collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .update(updates)
                                .addOnSuccessListener(unused -> {
                                    Log.d(TAG, "assignFirebaseTokenToUser: Token updated successfully");
                                    preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "assignFirebaseTokenToUser: Failed to update token", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "assignFirebaseTokenToUser: Failed to retrieve token", e));
    }

    /**
     * Initializes the UI components, such as fragments and navigation.
     */
    private void initializeUI() {
        Log.d(TAG, "initializeUI: Setting up UI components");
        setUpBottomNavigation();
        setUpSearchView();
        String firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME);
        Utilities.showToast(this, "Welcome, " + (firstName != null ? firstName : "User") + "!", Utilities.ToastType.SUCCESS);
    }

    /**
     * Sets up the bottom navigation bar and manages fragment switching.
     */
    private void setUpBottomNavigation() {
        Log.d(TAG, "setUpBottomNavigation: Configuring bottom navigation");
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_profile) {
                Log.d(TAG, "setUpBottomNavigation: Profile fragment selected");
                selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.navigation_chats) {
                Log.d(TAG, "setUpBottomNavigation: Chats fragment selected");
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
     * Loads the specified fragment into the container.
     *
     * @param fragment The fragment to load.
     */
    private void loadFragment(Fragment fragment) {
        Log.d(TAG, "loadFragment: Loading fragment");
        binding.progressBar.setVisibility(View.VISIBLE);

        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();

        activeFragment = fragment;

        // Simulate fragment load delay
        binding.navHostFragment.postDelayed(() -> {
            binding.progressBar.setVisibility(View.GONE);
            Log.d(TAG, "loadFragment: Fragment loaded");
        }, 300);
    }

    /**
     * Configures the search view to filter data within active fragments.
     */
    private void setUpSearchView() {
        Log.d(TAG, "setUpSearchView: Setting up search view");
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: Query submitted: " + query);
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: Query text changed: " + newText);
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(newText);
                }
                return false;
            }
        });
    }
}