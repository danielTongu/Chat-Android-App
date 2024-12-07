package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpWithEmailBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * SignUpWithEmailActivity handles the user registration process for the chat application.
 * It allows users to sign up with their first name, last name, email, password,
 * and an optional profile picture.
 */
public class SignUpWithEmailActivity extends AppCompatActivity {
    private ActivitySignUpWithEmailBinding binding;
    private String encodedImage; // Encoded image string for the user's profile picture
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpWithEmailBinding.inflate(getLayoutInflater());
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        setContentView(binding.getRoot());
        setListeners();
    }

    /**
     * Sets up the listeners for the UI elements.
     */
    private void setListeners() {
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidateSignUpDetails()) {
                signUp();
            }
        });

        binding.textSignIn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Initiates the sign-up process by creating a new user in Firestore.
     */
    private void signUp() {
        Utilities.showToast(this, "Onboarding...", Utilities.ToastType.INFO);
        showLoadingIndicator(true);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String email = binding.inputEmail.getText().toString().trim();

        // Check if a user with the same email already exists
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        Utilities.showToast(this, "Email already in use. Please use a different email.", Utilities.ToastType.WARNING);
                        showLoadingIndicator(false);
                    } else {
                        addUserToDatabase();
                    }
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Adds the new user to the Firestore database using the User object.
     */
    private void addUserToDatabase() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String hashedPassword = User.hashPassword(binding.inputPassword.getText().toString().trim());

        if (hashedPassword == null) {
            Utilities.showToast(this, "Failed to secure your password. Please try again.", Utilities.ToastType.ERROR);
            showLoadingIndicator(false);
            return;
        }

        // Create a User object
        User user = new User(
                binding.inputFirstName.getText().toString().trim(),
                binding.inputLastName.getText().toString().trim(),
                binding.inputEmail.getText().toString().trim(),
                hashedPassword,
                encodedImage != null ? encodedImage : ""
        );

        // Add user data to Firestore
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Utilities.showToast(this, "Onboarding successful", Utilities.ToastType.SUCCESS);

                    // Save user info in preferences
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
                    preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
                    preferenceManager.putString(Constants.KEY_EMAIL, user.email);
                    preferenceManager.putString(Constants.KEY_IMAGE, user.image);

                    // Navigate to MainActivity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Launches the image picker and handles the result.
     */
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();

                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        // Encode the image using User utility function
                        encodedImage = User.encodeImage(bitmap);

                        // Set the selected image
                        binding.imageProfile.setImageBitmap(bitmap);
                        binding.textAddImage.setVisibility(View.GONE);

                    } catch (FileNotFoundException e) {
                        Utilities.showToast(this, "Image not found", Utilities.ToastType.ERROR);
                        e.printStackTrace();
                    }
                }
            }
    );

    /**
     * Validates the sign-up details entered by the user.
     */
    private Boolean isValidateSignUpDetails() {
        if (binding.inputFirstName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your first name", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputLastName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your last name", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your email", Utilities.ToastType.WARNING);
            return false;
        } else if (!User.isValidEmail(binding.inputEmail.getText().toString().trim())) {
            Utilities.showToast(this, "Please enter a valid email", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter password", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputPassword.getText().toString().trim().length() < Constants.KEY_PASSWORD_MIN_LENGTH) {
            Utilities.showToast(this, String.format("Password must be at least %d characters", Constants.KEY_PASSWORD_MIN_LENGTH), Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter password confirmation", Utilities.ToastType.WARNING);
            return false;
        } else if (!binding.inputPassword.getText().toString().trim()
                .equals(binding.inputConfirmPassword.getText().toString().trim())) {
            Utilities.showToast(this, "Password and Confirm Password must be the same", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Toggles the loading state of the sign-up process.
     */
    private void showLoadingIndicator(boolean isLoading) {
        binding.buttonSignUp.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.textSignIn.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }
}