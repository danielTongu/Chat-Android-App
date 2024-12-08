package com.example.chatandroidapp.module;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The User class represents a user in the chat application.
 * It includes personal details, tasks grouped by date, and utility methods for email validation,
 * password hashing, and image encoding/decoding.
 */
public class User implements Serializable {

    /** The date when the user was created, stored as a formatted string. */
    public final String createdDate;

    /** The first name of the user. */
    public String firstName;

    /** The last name of the user. */
    public String lastName;

    /** The Base64-encoded string representing the user's profile image. */
    public String image;

    /** The phone number of the user. */
    public String phone;

    /** The email address of the user. */
    public String email;

    /** The hashed version of the user's password for secure storage. */
    public String hashedPassword;

    /** The Firebase Cloud Messaging (FCM) token for the user, used for push notifications. */
    public String token;

    /** A list of unique chat room IDs the user participates in. */
    public List<String> chatIds;

    /** A map of tasks grouped by date, where the key is a date string and the value is a list of tasks. */
    public Map<String, List<String>> tasksByDate;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     * Automatically sets the creation date to the current date and time.
     */
    public User() {
        this.createdDate = getCurrentFormattedDate();
        this.chatIds = new ArrayList<>();
        this.tasksByDate = new HashMap<>();
    }

    /**
     * Constructor for email-based sign-up.
     */
    public User(String firstName, String lastName, String email, String hashedPassword, String image) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.hashedPassword = hashedPassword;
        this.image = image;
    }

    /**
     * Constructor for phone-based sign-up.
     */
    public User(String firstName, String lastName, String phone, String image) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.image = image;
    }

    /**
     * Adds a task to a specific date.
     */
    public void addTask(String date, String task) {
        tasksByDate.putIfAbsent(date, new ArrayList<>());
        tasksByDate.get(date).add(task);
    }

    /**
     * Retrieves tasks for a specific date.
     */
    public List<String> getTasksForDate(String date) {
        return tasksByDate.getOrDefault(date, new ArrayList<>());
    }

    /**
     * Removes a specific task from a specific date.
     */
    public void removeTask(String date, String task) {
        if (tasksByDate.containsKey(date)) {
            tasksByDate.get(date).remove(task);
            if (tasksByDate.get(date).isEmpty()) {
                tasksByDate.remove(date);
            }
        }
    }

    // --- Static Utility Methods ---

    /**
     * Validates whether the provided string is a valid email address.
     */
    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Hashes the provided password using SHA-256.
     *
     * @param password The plaintext password to hash.
     * @return The hashed password as a hexadecimal string, or {@code null} if hashing fails.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(password.getBytes());

            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : hashedBytes) {
                stringBuilder.append(String.format("%02x", b));
            }

            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encodes a Bitmap image to a Base64 string after resizing and compressing it.
     *
     * @param bitmap The Bitmap image to encode.
     * @return A Base64 encoded string representation of the image.
     */
    public static String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();

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
     * Gets the current date formatted as "yyyy-MM-dd HH:mm:ss".
     *
     * @return A formatted string representation of the current date and time.
     */
    public static String getCurrentFormattedDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date());
    }
}