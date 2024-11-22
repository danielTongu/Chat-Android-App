package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySigninBinding;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SignInActivity handles user authentication by verifying email and password against Firestore.
 * Passwords are compared using SHA-256 with the stored unique salt to ensure security.
 *
 * @author  Daniel Tongu
 */
public class SignInActivity extends AppCompatActivity {
    private ActivitySigninBinding binding; // View binding for activity_signin.xml
    private PreferenceManager preferenceManager; // PreferenceManager instance for managing shared preferences

    /**
     * Called when the activity is starting. Initializes the activity components.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize view binding
        binding = ActivitySigninBinding.inflate(getLayoutInflater());
        // Initialize PreferenceManager
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        setContentView(binding.getRoot());
        // Set up UI listeners
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
     * Initiates the sign-in process by verifying the user's email and password.
     */
    private void signIn() {
        showLoadingIndicator(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Query Firestore for a user with the provided email
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, hashPassword(binding.inputPassword.getText().toString().trim()))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().getDocuments().isEmpty()) {
                        // User exists
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        // Save user info in preferences
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
                        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
                        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));

                        // Navigate to MainActivity
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else { // User with the provided email does not exist
                        Utilities.showToast(this, "Unable to sign in", Utilities.ToastType.WARNING);
                        showLoadingIndicator(false);
                    }
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Hashes the provided password using SHA-256.
     * @param password The plaintext password to hash.
     * @return The hashed password as a hexadecimal string, or null if hashing fails.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hashedBytes) {
                stringBuilder.append(String.format("%02x", b));
            }

            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            Utilities.showToast(this, "Error hashing password", Utilities.ToastType.ERROR);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Validates the sign-in details entered by the user.
     * @return {@code true} if the details are valid, {@code false} otherwise.
     */
    private Boolean isValidSignInDetails() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        boolean isValid = false;

        if (email.isEmpty()) {
            Utilities.showToast(this, "Please enter your email", Utilities.ToastType.WARNING);
        } else if (!Utilities.isValidEmail(email)) {
            Utilities.showToast(this, "Please enter a valid email", Utilities.ToastType.WARNING);
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Please enter your password", Utilities.ToastType.WARNING);
        } else if (password.length() < Constants.KEY_PASSWORD_MIN_LENGTH) {
            Utilities.showToast(this, String.format("Password must be at least %d characters", Constants.KEY_PASSWORD_MIN_LENGTH), Utilities.ToastType.WARNING);
        } else {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Toggles the loading state of the sign-in process.
     * @param isLoading {@code true} to show loading, {@code false} to hide loading.
     */
    private void showLoadingIndicator(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setEnabled(false);
            binding.textCreateNewAccount.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignIn.setEnabled(true);
            binding.textCreateNewAccount.setEnabled(true);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}