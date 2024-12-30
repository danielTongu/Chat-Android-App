package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivityMainBinding;
import com.example.chatandroidapp.fragments.ChatsFragment;
import com.example.chatandroidapp.fragments.ProfileFragment;
import com.example.chatandroidapp.fragments.TasksFragment;
import com.example.chatandroidapp.interfaces.SearchableView;
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
        } else {
            assignFirebaseTokenToUser(userId);
        }
    }

    /**
     * Sets up the UI components, such as bottom navigation and search view.
     */
    private void initializeUI() {
        setUpBottomNavigation();
        setUpSearchView();
    }

    /**
     * Assigns the Firebase token to the user and updates Firestore if needed.
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
                .addOnFailureListener(e -> showErrorAndFinish("Failed to retrieve Firebase token"));
    }

    /**
     * Updates the Firebase token in Firestore and saves it locally.
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
                .addOnSuccessListener(aVoid -> preferenceManager.putString(Constants.KEY_FCM_TOKEN, token))
                .addOnFailureListener(e -> showErrorAndFinish("Failed to update Firebase token in Firestore"));
    }

    /**
     * Sets up bottom navigation for fragment transitions.
     */
    private void setUpBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile); // Default fragment
    }

    /**
     * Handles bottom navigation item selection.
     *
     * @param item The selected navigation item.
     * @return True if handled, false otherwise.
     */
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        final int itemId = item.getItemId();

        if(itemId == R.id.navigation_profile){
            selectedFragment = new ProfileFragment();
        }
        else if(itemId == R.id.navigation_chats){
            selectedFragment = new ChatsFragment();
        }
        else if(itemId == R.id.navigation_tasks){
            selectedFragment = new TasksFragment();
        }


        if (selectedFragment != null) {
            int visibility =  selectedFragment instanceof SearchableView ? View.VISIBLE : View.GONE;
            binding.searchView.setVisibility(visibility);
            loadFragment(selectedFragment);
        }

        return selectedFragment != null;
    }

    /**
     * Loads a fragment into the container if it is not already active.
     *
     * @param fragment The fragment to load.
     */
    private void loadFragment(Fragment fragment) {
        if (activeFragment == null || !activeFragment.getClass().equals(fragment.getClass())) {
            binding.progressBar.setVisibility(View.VISIBLE);

            fragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit();

            activeFragment = fragment;

            binding.navHostFragment.postDelayed(() -> binding.progressBar.setVisibility(View.GONE), 300);
        }
    }

    /**
     * Configures views that support the search functionality.
     */
    private void setUpSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (activeFragment instanceof SearchableView) {
                    ((SearchableView) activeFragment).filterData(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (activeFragment instanceof SearchableView) {
                    ((SearchableView) activeFragment).filterData(newText);
                }
                return true;
            }
        });
    }

    /**
     * Displays an error message and closes the activity.
     *
     * @param message The error message to display.
     */
    private void showErrorAndFinish(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        preferenceManager.clear();
        Log.e(TAG, message);
        finish();
    }
}