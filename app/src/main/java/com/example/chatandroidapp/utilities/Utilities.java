package com.example.chatandroidapp.utilities;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.chatandroidapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class provides utility methods for the application, such as displaying
 * custom Toast messages, validating email formats, and encoding images.
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
        View layout = inflater.inflate(R.layout.container_toast_custom, null);
        ImageView imageView = layout.findViewById(R.id.imageToast);
        TextView textView = layout.findViewById(R.id.textToast);
        textView.setText(message);

        int iconResId;
        int bgColor = ContextCompat.getColor(context, R.color.black); // Default background color
        int textColor = ContextCompat.getColor(context, R.color.white); // Default text color

        switch (type) {
            case INFO:
                iconResId = R.drawable.ic_info;
                textColor = ContextCompat.getColor(context, R.color.info);
                break;
            case WARNING:
                iconResId = R.drawable.ic_warning;
                textColor = ContextCompat.getColor(context, R.color.warning);
                break;
            case ERROR:
                iconResId = R.drawable.ic_error;
                textColor = ContextCompat.getColor(context, R.color.error);
                break;
            case SUCCESS:
                iconResId = R.drawable.ic_success;
                textColor = ContextCompat.getColor(context, R.color.success);
                break;
            default:
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                return;
        }

        imageView.setImageResource(iconResId);
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(textColor));
        ViewCompat.setBackgroundTintList(layout, ColorStateList.valueOf(bgColor));
        textView.setTextColor(textColor);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.setDuration(Toast.LENGTH_LONG);
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

    /**
     * Decodes a Base64-encoded string into a Bitmap image.
     * @param encodedImage The Base64-encoded image string.
     * @return A Bitmap representation of the decoded image.
     */
    public static Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Formats the given Date object into a human-readable string.
     *
     * @param date The Date object to format.
     * @return A formatted date-time string.
     */
    public static String formatDateTime(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        return formatter.format(date);
    }

}