package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignupBinding;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * SignUpActivity handles the user registration process for the chat application.
 * It allows users to sign up with their first name, last name, email, password,
 * and an optional profile picture. Passwords are securely hashed using SHA-256 with a unique salt before storage.
 *
 * @author Daniel Tongu
 */
public class SignUpActivity extends AppCompatActivity {
    private ActivitySignupBinding binding; // View binding for activity_signup.xml
    private String encodedImage; // Encoded image string for the user's profile picture
    private PreferenceManager preferenceManager; // PreferenceManager to manage shared preferences

    /**
     * Called when the activity is starting. Initializes the activity components.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize view binding
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        // Initialize PreferenceManager
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        setContentView(binding.getRoot());
        setListeners();
    }

    /**
     * Sets up the listeners for the UI elements.
     * Includes image selection, sign-up action, and navigation to sign-in.
     */
    private void setListeners() {
        // Handle image layout click to pick an image
        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        // Handle sign-up button click
        binding.buttonSignUp.setOnClickListener(v -> {
            if (isValidateSignUpDetails()) {
                signUp();
            }
        });

        // Navigate back when "Sign In" text is clicked
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Initiates the sign-up process by creating a new user in Firestore.
     * Checks for duplicate email before adding the user.
     */
    private void signUp() {
        Utilities.showToast(this, "Onboarding...", Utilities.ToastType.INFO);
        // Show loading indicator
        showLoadingIndicator(true);

        // Initialize Firebase Firestore
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
     * Adds the new user to the Firestore database with a hashed password and unique salt.
     */
    private void addUserToDatabase() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Hash the user's password with the salt using SHA-256
        String plaintextPassword = binding.inputPassword.getText().toString().trim();
        String hashedPassword = hashPassword(plaintextPassword);

        if (hashedPassword == null) {
            Utilities.showToast(this, "Failed to secure your password. Please try again.", Utilities.ToastType.ERROR);
            showLoadingIndicator(false);
            return;
        }

        // Create a HashMap to store user data
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString().trim());
        user.put(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString().trim());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim());
        user.put(Constants.KEY_PASSWORD, hashedPassword);
        user.put(Constants.KEY_IMAGE, encodedImage != null ? encodedImage : ""); // Empty string if no image

        // Add user data to the "Users" collection
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Utilities.showToast(this, "Onboarding successful", Utilities.ToastType.SUCCESS);

                    // Save user info in preferences
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage != null ? encodedImage : "");

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
                    // Get the selected image URI
                    Uri imageUri = result.getData().getData();

                    try {
                        // Open an InputStream to the image
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        // Decode the InputStream into a Bitmap
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                        // Compress the image to reduce size
                        Bitmap compressedBitmap = compressImage(bitmap);

                        // Set the selected image in the profile ImageView
                        binding.imageProfile.setImageBitmap(compressedBitmap);
                        // Hide the "Add Image" text
                        binding.textAddImage.setVisibility(View.GONE);
                        // Encode the image to Base64
                        encodedImage = Utilities.encodeImage(compressedBitmap);

                    } catch (FileNotFoundException e) {
                        Utilities.showToast(this, "Image not found", Utilities.ToastType.ERROR);
                        e.printStackTrace();
                    }
                }
            }
    );

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
     * Compresses a Bitmap image to reduce its size.
     * @param bitmap The original Bitmap image.
     * @return The compressed Bitmap image.
     */
    private Bitmap compressImage(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Compress the bitmap into JPEG format with 50% quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    /**
     * Validates the sign-up details entered by the user.
     * @return {@code true} if the details are valid, {@code false} otherwise.
     */
    private Boolean isValidateSignUpDetails() {
        boolean isValid = false;

        if (binding.inputFirstName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your first name", Utilities.ToastType.WARNING);
        } else if (binding.inputLastName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your last name", Utilities.ToastType.WARNING);
        } else if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your email", Utilities.ToastType.WARNING);
        } else if (!Utilities.isValidEmail(binding.inputEmail.getText().toString().trim())) {
            Utilities.showToast(this, "Please enter a valid email", Utilities.ToastType.WARNING);
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter password", Utilities.ToastType.WARNING);
        } else if (binding.inputPassword.getText().toString().trim().length() < Constants.KEY_PASSWORD_MIN_LENGTH) {
            Utilities.showToast(this, String.format("Password must be at least %d characters", Constants.KEY_PASSWORD_MIN_LENGTH), Utilities.ToastType.WARNING);
        } else if (binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter password confirmation", Utilities.ToastType.WARNING);
        } else if (!binding.inputPassword.getText().toString().trim()
                .equals(binding.inputConfirmPassword.getText().toString().trim())) {
            Utilities.showToast(this, "Password and Confirm Password must be the same", Utilities.ToastType.WARNING);
        } else {
            // All validations passed
            isValid = true;
        }

        return isValid;
    }

    /**
     * Toggles the loading state of the sign-up process.
     * @param isLoading {@code true} to show loading, {@code false} to hide loading.
     */
    private void showLoadingIndicator(boolean isLoading) {
        if (isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.textSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.buttonSignUp.setVisibility(View.VISIBLE);
            binding.textSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
}