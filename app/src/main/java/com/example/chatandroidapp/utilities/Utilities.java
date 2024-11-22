package com.example.chatandroidapp.utilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.chatandroidapp.R;

/**
 * This class provides utility methods for the application, such as displaying
 * custom Toast messages, validating email formats, and encoding images.
 *
 * @author Daniel Tongu
 */
public class Utilities {

    /**
     * Enum representing different types of Toast messages.
     */
    public enum ToastType {
        INFO,
        WARNING,
        ERROR,
        SUCCESS,
        DEFAULT
    }

    /**
     * Displays a Toast message with the default type.
     * @param context The context to use for displaying the Toast.
     * @param message The message to display in the Toast.
     */
    public static void showToast(Context context, String message) {
        showToast(context, message, ToastType.DEFAULT);
    }

    /**
     * Displays a Toast message with the specified type.
     * @param context The context to use for displaying the Toast.
     * @param message The message to display in the Toast.
     * @param type    The type of the message: INFO, WARNING, ERROR, SUCCESS, or DEFAULT.
     */
    public static void showToast(Context context, String message, ToastType type) {
        if (type == null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            return;
        }
        displayCustomToast(context, message, type);
    }

    /**
     * Displays a custom Toast with styling based on the provided ToastType.
     * @param context The context to use for displaying the Toast.
     * @param message The message to display in the Toast.
     * @param type    The type of the message: INFO, WARNING, ERROR, or SUCCESS.
     */
    private static void displayCustomToast(Context context, String message, ToastType type) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_custom, null);

        // Initialize UI components within the custom Toast layout
        LinearLayout toastRoot = layout.findViewById(R.id.toast_layout_root);
        ImageView imageView = layout.findViewById(R.id.image);
        TextView textView = layout.findViewById(R.id.text);
        textView.setText(message);

        // Variables to hold resource IDs for background and icon based on ToastType
        int backgroundDrawableId;
        int iconResId;
        int textColor = ContextCompat.getColor(context, R.color.white);

        // Determine the styling based on the ToastType
        switch (type) {
            case INFO:
                backgroundDrawableId = R.drawable.background_toast_info;
                iconResId = R.drawable.ic_info;
                break;
            case WARNING:
                textColor = ContextCompat.getColor(context, R.color.black);
                backgroundDrawableId = R.drawable.background_toast_warning;
                iconResId = R.drawable.ic_warning;
                break;
            case ERROR:
                backgroundDrawableId = R.drawable.background_toast_error;
                iconResId = R.drawable.ic_error;
                break;
            case SUCCESS:
                backgroundDrawableId = R.drawable.background_toast_success;
                iconResId = R.drawable.ic_success;
                break;
            default:
                // If ToastType is DEFAULT or unrecognized, display a standard Toast
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                return;
        }

        // Apply the background drawable to the Toast layout
        toastRoot.setBackgroundResource(backgroundDrawableId);
        // Set the appropriate icon for the Toast
        imageView.setImageResource(iconResId);
        // Apply color filter to the icon
        imageView.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
        // Set the text color for the message
        textView.setTextColor(textColor);

        // Configure and display the custom Toast
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 100); // Position the Toast at the bottom with an offset
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    /**
     * Validates whether the provided string is a valid email address.
     * @param email The email string to validate.
     * @return {@code true} if the email is valid, {@code false} otherwise.
     */
    public static boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Encodes a Bitmap image to a Base64 string after resizing and compressing it.
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
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
    }

}