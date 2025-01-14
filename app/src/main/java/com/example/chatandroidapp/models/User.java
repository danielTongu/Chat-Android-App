package com.example.chatandroidapp.models;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chatandroidapp.activities.SignUpActivity;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The User class represents a user in the chat application.
 * It includes utilities for encoding, decoding, and validating user information, including profile images.
 */
public class User implements Serializable {
    /**
     * Unique identifier for the user (Primary Key).
     */
    @PropertyName("id")
    public String id = "";

    /**
     * User's first name.
     */
    @PropertyName("firstName")
    public String firstName = "unknown";

    /**
     * User's last name.
     */
    @PropertyName("lastName")
    public String lastName = "";

    /**
     * Base64 encoded string of the user's profile image.
     */
    @PropertyName("image")
    public String image = "";

    /**
     * User's phone number.
     */
    @PropertyName("phone")
    public String phone = "";

    /**
     * User's email address.
     */
    @PropertyName("email")
    public String email = "";

    /**
     * SHA-256 hashed password of the user.
     */
    @PropertyName("hashedPassword")
    public String hashedPassword = "";

    /**
     * Firebase Cloud Messaging token for push notifications.
     */
    @PropertyName("fcmToken")
    public String fcmToken = "";

    /**
     * List of chat IDs that the user participates in.
     */
    @PropertyName("chatIds")
    public List<String> chatIds = new ArrayList<>();

    /**
     * Server-side timestamp indicating when the user was created.
     */
    @ServerTimestamp
    @PropertyName("createdDate")
    public Date createdDate = null;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public User() {
    }

    /**
     * Constructs a User with the specified ID.
     *
     * @param id The unique identifier for the user.
     */
    public User(String id) {
        this.id = id;
    }

    // --- IMAGE HANDLING FUNCTIONS ---

    /**
     * Encodes a Bitmap image to a Base64 string after resizing and compressing it.
     *
     * @param bitmap The Bitmap image to encode.
     * @return A Base64 encoded string representation of the image.
     */
    public static String encodeImage(Bitmap bitmap) {
        // Define the desired width for the preview image
        int previewWidth = 150;
        // Calculate the height to maintain the aspect ratio
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();

        // Create a scaled bitmap for the preview
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();

        // Compress the bitmap into JPEG format with 50% quality
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();

        // Encode the byte array into a Base64 string
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * Decodes a Base64-encoded string into a Bitmap image.
     *
     * @param encodedImage The Base64-encoded image string.
     * @return A Bitmap representation of the decoded image.
     */
    public static Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Validates a user's profile image to ensure it is non-null and meets the application's requirements.
     *
     * @param image The image to validate.
     * @return The validated Bitmap image.
     * @throws IllegalArgumentException If the image is invalid.
     */
    public static Bitmap validateImage(Bitmap image) throws IllegalArgumentException {
        if (image == null) {
            throw new IllegalArgumentException("Profile image cannot be null.");
        }
        return image;
    }

    // --- VALIDATION FUNCTIONS ---

    /**
     * Validates the user's first name.
     *
     * @param firstName The first name to validate.
     * @return The trimmed and validated first name.
     * @throws IllegalArgumentException If the first name is invalid.
     */
    public static String validateFirstName(String firstName) throws IllegalArgumentException {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty.");
        }
        firstName = firstName.trim();
        if (firstName.length() < 2) {
            throw new IllegalArgumentException("First name must be at least 2 characters long.");
        }
        return firstName;
    }

    /**
     * Validates the user's last name.
     *
     * @param lastName The last name to validate.
     * @return The trimmed and validated last name.
     * @throws IllegalArgumentException If the last name is invalid.
     */
    public static String validateLastName(String lastName) throws IllegalArgumentException {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty.");
        }
        lastName = lastName.trim();
        if (lastName.length() < 2) {
            throw new IllegalArgumentException("Last name must be at least 2 characters long.");
        }
        return lastName;
    }

    /**
     * Validates the user's email address.
     *
     * @param email The email address to validate.
     * @return The trimmed and validated email address.
     * @throws IllegalArgumentException If the email address is invalid.
     */
    public static String validateEmail(String email) throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        email = email.trim().toLowerCase();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        return email;
    }

    /**
     * Validates the user's phone number.
     *
     * @param phone The phone number to validate.
     * @return The trimmed and validated phone number.
     * @throws IllegalArgumentException If the phone number is invalid.
     */
    public static String validatePhone(String phone) throws IllegalArgumentException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        phone = phone.trim();
        Pattern phonePattern = Pattern.compile("^\\+?[0-9]{7,15}$");
        if (!phonePattern.matcher(phone).matches()) {
            throw new IllegalArgumentException("Invalid phone number format.");
        }
        return phone;
    }

    /**
     * Validates the user's password.
     *
     * @param password The password to validate.
     * @return The validated password.
     * @throws IllegalArgumentException If the password is invalid.
     */
    public static String validatePassword(String password) throws IllegalArgumentException {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (password.length() < SignUpActivity.KEY_PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(String.format("Password must be at least %d characters long.",
                    SignUpActivity.KEY_PASSWORD_MIN_LENGTH));
        }
        return password;
    }

    /**
     * Hashes the user's password using SHA-256.
     *
     * @param password The password to hash.
     * @return The SHA-256 hashed password as a hexadecimal string.
     * @throws IllegalArgumentException If the password is null or empty.
     * @throws RuntimeException         If the SHA-256 algorithm is not available.
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password.", e);
        }
    }

    /**
     * Returns a string representation of the user, combining first and last names.
     *
     * @return The user's full name.
     */
    @NonNull
    @Override
    public String toString() {
        return String.format("%s %s", firstName, lastName);
    }

    /**
     * Callback interface for handling image selection results.
     */
    public interface ImageSelectionCallback {
        /**
         * Called when an image is successfully selected.
         *
         * @param bitmap The selected Bitmap image.
         */
        void onImageSelected(Bitmap bitmap);

        /**
         * Called when image selection fails.
         *
         * @param errorMessage The error message.
         */
        void onImageSelectionFailed(String errorMessage);
    }

    /**
     * Handles the selection of a profile image by the user.
     * This function uses a `Fragment` to handle activity result registration for modern APIs.
     */
    public static class ImageHandler {
        private final Fragment fragment;
        private final ImageSelectionCallback callback;

        /**
         * Constructor for ImageHandler.
         *
         * @param fragment The fragment to handle the image selection flow.
         * @param callback The callback to handle the selected image.
         */
        public ImageHandler(Fragment fragment, ImageSelectionCallback callback) {
            this.fragment = fragment;
            this.callback = callback;
        }

        /**
         * Opens the image picker for selecting a profile image.
         */
        public void openImagePicker() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            fragment.registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                            handleImageSelection(result.getData().getData());
                        }
                    }
            ).launch(intent);
        }

        /**
         * Handles the image selection result, decodes the image, and passes it to the callback.
         *
         * @param imageUri The URI of the selected image.
         */
        private void handleImageSelection(Uri imageUri) {
            try (InputStream inputStream = fragment.requireContext().getContentResolver().openInputStream(imageUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                callback.onImageSelected(bitmap);
            } catch (Exception e) {
                callback.onImageSelectionFailed("Failed to load the selected image.");
            }
        }
    }
}