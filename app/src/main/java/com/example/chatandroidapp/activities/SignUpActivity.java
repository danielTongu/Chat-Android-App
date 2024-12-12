// SignUpActivity.java
package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;

/**
 * SignUpActivity handles user registration for both email and phone sign-up methods.
 * It dynamically toggles between the two methods and utilizes the User model's utilities
 * for validation, password hashing, and image encoding.
 */
public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SIGN_UP_ACTIVITY";
    private ActivitySignUpBinding binding;
    private String encodedImage; // Encoded string for the user's profile picture
    private PreferenceManager preferenceManager;
    private boolean isEmailSignUp = true; // Flag to toggle between email and phone sign-up

    // Launcher for image picker
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            encodedImage = User.encodeImage(bitmap);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                        } catch (Exception e) {
                            Utilities.showToast(this, "Failed to load image", Utilities.ToastType.ERROR);
                            Log.e(TAG, "Error loading image", e);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        initializeUI();
        setListeners();
    }

    /**
     * Initializes the default settings for UI components.
     */
    private void initializeUI() {
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            isEmailSignUp = (checkedId == binding.radioSignInWithEmail.getId());
            toggleSignUpMethodUI(isEmailSignUp);
        });

        // Default to email sign-up
        toggleSignUpMethodUI(true);

        // Set up country picker for phone sign-up
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);
    }

    /**
     * Sets up listeners for various UI interactions.
     */
    private void setListeners() {
        // Back button
        binding.buttonBack.setOnClickListener(v -> onBackPressed());

        // Profile image picker
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
        });

        // Sign-up button
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isEmailSignUp) {
                if (validateEmailSignUpDetails()) {
                    checkEmailAndSignUp();
                }
            } else {
                if (validatePhoneSignUpDetails()) {
                    navigateToOtpVerificationActivity();
                }
            }
        });

        // Navigate to sign-in
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Toggles the visibility of loading indicators.
     *
     * @param isLoading True to show loading indicators, false to hide.
     */
    private void showLoadingIndicator(boolean isLoading) {
        binding.buttonSignUp.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Toggles the UI based on the selected sign-up method (email or phone).
     *
     * @param isEmailSignUp True for email sign-up, false for phone sign-up.
     */
    private void toggleSignUpMethodUI(boolean isEmailSignUp) {
        binding.signUpEmailLayout.setVisibility(isEmailSignUp ? View.VISIBLE : View.GONE);
        binding.signUpPhoneLayout.setVisibility(isEmailSignUp ? View.GONE : View.VISIBLE);
        binding.buttonSignUp.setText(isEmailSignUp ? "Sign Up" : "Get Code");
    }

    /**
     * Validates the input details for phone-based sign-up.
     *
     * @return True if inputs are valid, false otherwise.
     */
    private boolean validatePhoneSignUpDetails() {
        String firstName = binding.inputFirstName.getText().toString().trim();
        String lastName = binding.inputLastName.getText().toString().trim();
        String phoneNumber = binding.inputPhoneNumber.getText().toString().trim();

        try {
            User.validateFirstName(firstName);
            User.validateLastName(lastName);
            User.validatePhone(binding.countryCodePicker.getFullNumberWithPlus());
            return true;
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.WARNING);
            return false;
        }
    }

    /**
     * Navigates to OTP Verification Activity for phone-based sign-up.
     */
    private void navigateToOtpVerificationActivity() {
        // Save sign-up details to SharedPreferences
        preferenceManager.putString(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_PHONE, binding.countryCodePicker.getFullNumberWithPlus());
        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);

        Intent intent = new Intent(SignUpActivity.this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_SIGN_UP); // Added action type
        Log.d(TAG, "navigateToOtpVerificationActivity: Sending intent with actionType: " + Constants.ACTION_SIGN_UP);
        startActivity(intent);
        finish();
    }

    /**
     * Validates the input details for email-based sign-up.
     *
     * @return True if inputs are valid, false otherwise.
     */
    private boolean validateEmailSignUpDetails() {
        String firstName = binding.inputFirstName.getText().toString().trim();
        String lastName = binding.inputLastName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

        try {
            User.validateFirstName(firstName);
            User.validateLastName(lastName);
            User.validateEmail(email);
            User.validatePassword(password);

            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Passwords do not match.");
            }

            return true;
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.WARNING);
            return false;
        }
    }

    /**
     * Checks if the email is already registered and proceeds with sign-up.
     */
    private void checkEmailAndSignUp() {
        showLoadingIndicator(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String email = binding.inputEmail.getText().toString().trim();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Utilities.showToast(this, "Email already in use. Please use a different email.", Utilities.ToastType.WARNING);
                            showLoadingIndicator(false);
                        } else {
                            signUpWithEmail(database);
                        }
                    } else {
                        Utilities.showToast(this, "Error checking email: " + task.getException().getMessage(), Utilities.ToastType.ERROR);
                        showLoadingIndicator(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Utilities.showToast(this, "Error checking email: " + e.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Handles email-based user registration.
     *
     * @param database Instance of FirebaseFirestore.
     */
    private void signUpWithEmail(FirebaseFirestore database) {
        String hashedPassword = User.hashPassword(binding.inputPassword.getText().toString().trim());

        if (hashedPassword == null) {
            Utilities.showToast(this, "Password hashing failed. Please try again.", Utilities.ToastType.ERROR);
            showLoadingIndicator(false);
            return;
        }

        // Create a new User object
        User user = new User();

        // Set profile image if available
        if (encodedImage != null) {
            user.image = encodedImage;
        }

        // Set user details
        user.firstName = binding.inputFirstName.getText().toString().trim();
        user.lastName = binding.inputLastName.getText().toString().trim();
        user.email = binding.inputEmail.getText().toString().trim();
        user.hashedPassword = hashedPassword;

        // Retrieve FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        user.fcmToken = task.getResult();
                        registerUserWithEmail(database, user);
                    } else {
                        Utilities.showToast(this, "Failed to retrieve FCM token.", Utilities.ToastType.ERROR);
                        showLoadingIndicator(false);
                    }
                });
    }

    /**
     * Registers the user with email in Firestore.
     *
     * @param database Instance of FirebaseFirestore.
     * @param user     User object to be registered.
     */
    private void registerUserWithEmail(FirebaseFirestore database, User user) {
        // Generate a new document ID
        String userId = database.collection(Constants.KEY_COLLECTION_USERS).document().getId();
        user.id = userId;

        // Validate the User object
        try {
            User.validateUser(user);
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.ERROR);
            showLoadingIndicator(false);
            return;
        }

        // Set the user in Firestore with the generated ID
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .addOnSuccessListener(unused -> {
                    // Save user details locally
                    saveUserPreferences(user);
                    // Navigate to MainActivity
                    navigateToMainActivity();
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Saves user details locally in SharedPreferences.
     *
     * @param user User object whose details are to be saved.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.fcmToken);
    }

    /**
     * Navigates to MainActivity after successful registration.
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}