package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySigninBinding;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.ToastType;
import com.example.chatandroidapp.utilities.Utilities; // Import Utilities class
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * SignInActivity handles the user sign-in functionality for the chat application.
 *
 * @author Daniel Tongu
 */
public class SignInActivity extends AppCompatActivity {

    // View binding for activity_signin.xml
    private ActivitySigninBinding binding;

    // PreferenceManager to manage shared preferences
    private PreferenceManager preferenceManager;

    /**
     * Called when the activity is starting. Initializes the activity components.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());

        setContentView(binding.getRoot());
        setListeners();
    }

    /**
     * Sets up the listeners for the UI elements.
     */
    private void setListeners() {
        // Navigate to SignUpActivity when "Create New Account" text is clicked
        binding.textCreateNewAccount.setOnClickListener(v -> startActivity(
                new Intent(getApplicationContext(), SignUpActivity.class)
        ));

        // Handle sign-in button click
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
    }

    /**
     * Authenticates the user with the provided email and password.
     */
    private void signIn() {
        showLoadingIndicator(true);
        Utilities.showToast(this, "Authenticating...", ToastType.INFO);
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Query the database for a user matching the entered email and password
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString().trim())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().getDocuments().isEmpty()) {
                        Utilities.showToast(this, "Authentication successful.", ToastType.SUCCESS);
                        // If authentication is successful, save user details in preferences and navigate to MainActivity
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Utilities.showToast(this, "Unable to sign in", ToastType.ERROR);
                    }
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), ToastType.ERROR);
                });
        showLoadingIndicator(false);
    }

    /**
     * Validates the sign-in details entered by the user.
     *
     * @return true if the details are valid, false otherwise.
     */
    private Boolean isValidSignInDetails() {
        boolean isValid = false;

        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your email", ToastType.WARNING);
        } else if (!Utilities.isValidEmail(binding.inputEmail.getText().toString().trim())) {
            Utilities.showToast(this, "Please enter a valid email", ToastType.WARNING);
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your password", ToastType.WARNING);
        } else {
            isValid =  true;
        }

        return isValid;
    }

    /**
     * Toggles the loading state of the sign-in process.
     *
     * @param isLoading true to show loading, false to hide loading.
     */
    private void showLoadingIndicator(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.textCreateNewAccount.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.textCreateNewAccount.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}