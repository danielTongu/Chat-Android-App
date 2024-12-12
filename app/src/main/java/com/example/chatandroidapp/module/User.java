// User.java
package com.example.chatandroidapp.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * The User class represents a user in the chat application.
 * It includes utilities for encoding and decoding profile images.
 */
public class User implements Serializable {
    // Public fields matching Firestore document fields
    public String id = "";                      // Unique identifier (Primary Key)
    public String firstName = "";
    public String lastName = "";
    public String image = "";                   // Base64 encoded image string
    public String phone = "";
    public String email = "";
    public String hashedPassword = "";
    public String fcmToken = "";

    @ServerTimestamp
    public Date createdDate = null;             // Server-side timestamp

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public User() {
        // createdDate will be set by Firestore
    }

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
     * Validates the user's first name.
     *
     * @param firstName The first name to validate.
     * @return The trimmed first name if valid.
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
     * @return The trimmed last name if valid.
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
     * @param email The email to validate.
     * @return The trimmed and lowercased email if valid.
     * @throws IllegalArgumentException If the email is invalid.
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
     * @return The trimmed phone number if valid.
     * @throws IllegalArgumentException If the phone number is invalid.
     */
    public static String validatePhone(String phone) throws IllegalArgumentException {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty.");
        }
        phone = phone.trim();
        // Simple regex for phone validation (can be enhanced as needed)
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
     * @return The password if valid.
     * @throws IllegalArgumentException If the password is invalid.
     */
    public static String validatePassword(String password) throws IllegalArgumentException {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
        // Additional password strength checks can be added here
        return password;
    }

    /**
     * Hashes the user's password using SHA-256.
     *
     * @param password The raw password to hash.
     * @return The hashed password as a hexadecimal string.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());
            // Convert byte array into hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // In a real-world scenario, consider better error handling
            throw new RuntimeException("Error hashing password.", e);
        }
    }

    /**
     * Validates the user's profile image.
     *
     * @param image The image to validate.
     * @return The Bitmap image if valid.
     * @throws IllegalArgumentException If the image is invalid.
     */
    public static Bitmap validateImage(Bitmap image) throws IllegalArgumentException {
        if (image == null) {
            throw new IllegalArgumentException("Profile image cannot be null.");
        }
        // Additional image validations (e.g., size, format) can be added here
        return image;
    }

    /**
     * Validates the entire User object.
     *
     * @param user The User object to validate.
     * @throws IllegalArgumentException If any field in the User object is invalid.
     */
    public static void validateUser(User user) throws IllegalArgumentException {
        if (user == null) {
            throw new IllegalArgumentException("User object cannot be null.");
        }
        validateFirstName(user.firstName);
        validateLastName(user.lastName);
        validateEmail(user.email);
        validatePhone(user.phone);
        if (user.hashedPassword == null || user.hashedPassword.isEmpty()) {
            throw new IllegalArgumentException("Hashed password cannot be empty.");
        }
        validateImage(getBitmapFromEncodedString(user.image));
    }

    @Override
    public String toString() {
        return String.format("%s %s", firstName, lastName);
    }
}