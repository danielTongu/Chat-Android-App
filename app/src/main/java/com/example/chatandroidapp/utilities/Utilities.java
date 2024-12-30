package com.example.chatandroidapp.utilities;

import android.content.Context;
import android.content.res.ColorStateList;
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
     * Displays a Toast message with the default type.
     *
     * @param context The context to use for displaying the Toast.
     * @param message The message to display in the Toast.
     */
    public static void showToast(Context context, String message) {
        showToast(context, message, ToastType.DEFAULT);
    }

    /**
     * Displays a Toast message with the specified type.
     *
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
     *
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
        int bgColor = ContextCompat.getColor(context, R.color.dark_gray); // Default background color
        int textColor = ContextCompat.getColor(context, R.color.white); // Default text color
        int duration = Toast.LENGTH_LONG;

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
                bgColor = ContextCompat.getColor(context, R.color.error);
                iconResId = R.drawable.ic_error;
                textColor = ContextCompat.getColor(context, R.color.white);
                break;
            case SUCCESS:
                duration = Toast.LENGTH_SHORT;
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
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(duration);
        toast.setView(layout);
        toast.show();
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

}