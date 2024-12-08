package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivityMainBinding;
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
 * It manages navigation between fragments and loads user data.
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

        String userId = preferenceManager.getString(Constants.KEY_USER_ID);
        if (userId == null || userId.isEmpty()) {
            Log.w(TAG, "onCreate: User ID not found in preferences");
            Utilities.showToast(this, "User not authenticated. Please sign in.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        loadUserDetails(preferenceManager.getString(Constants.KEY_USER_ID));
    }

    /**
     * Fetches the user's details from Firestore and saves them to shared preferences.
     *
     * @param userId The Firestore document ID for the user.
     */
    private void loadUserDetails(String userId) {
        Log.d(TAG, "loadUserDetails: Fetching user details from Firestore");

        if (userId == null) {
            Log.w(TAG, "loadUserDetails: User ID not found in preferences");
            Utilities.showToast(this, "User not authenticated. Please sign in.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "loadUserDetails: User details fetched successfully");

                        // Save details to preferences
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_PHONE, documentSnapshot.getString(Constants.KEY_PHONE));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));

                        // Assign token
                        assignFirebaseTokenToUser(userId);

                        // Initialize UI after loading data
                        initializeUI();
                    } else {
                        Log.e(TAG, "loadUserDetails: User document does not exist");
                        Utilities.showToast(this, "User account does not exist in the system. Please sign up again.", Utilities.ToastType.ERROR);
                        preferenceManager.clear();
                        finish();
                    }
                })
                .addOnFailureListener(exception -> {
                    Log.e(TAG, "loadUserDetails: Failed to fetch user details", exception);
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    finish();
                });
    }



    /**
     * Retrieves the Firebase token and updates it in Firestore.
     *
     * @param userId The user's Firestore document ID.
     */
    private void assignFirebaseTokenToUser(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "assignFirebaseTokenToUser: Token retrieved successfully: " + token);

                    FirebaseFirestore database = FirebaseFirestore.getInstance();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put(Constants.KEY_FCM_TOKEN, token);

                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .document(userId)
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                Log.d(TAG, "assignFirebaseTokenToUser: Token assigned successfully");
                                preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "assignFirebaseTokenToUser: Failed to assign token", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "assignFirebaseTokenToUser: Failed to retrieve token", e));
    }



    /**
     * Initializes the UI components, such as fragments and navigation, after user data is loaded.
     */
    private void initializeUI() {
        Log.d(TAG, "initializeUI: Setting up UI components");
        setUpBottomNavigation();
        setUpSearchView();
        Utilities.showToast(this, "Welcome!", Utilities.ToastType.SUCCESS);
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
                //selectedFragment = new ChatsFragment();
            } else if (item.getItemId() == R.id.navigation_tasks) {
                Log.d(TAG, "setUpBottomNavigation: Tasks fragment selected");
                //selectedFragment = new TasksFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        // Set default fragment to ProfileFragment
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_profile);
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