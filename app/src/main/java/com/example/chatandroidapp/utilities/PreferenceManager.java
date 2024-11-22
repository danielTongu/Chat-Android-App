package com.example.chatandroidapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class manages the application's shared preferences, providing methods
 * to store and retrieve various types of data.
 *
 * @author Daniel Tongu
 */
public class PreferenceManager {

    private static PreferenceManager instance;
    private final SharedPreferences sharedPreferences;

    /**
     * Private constructor to enforce Singleton pattern.
     * @param context The context used to access SharedPreferences.
     */
    private PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Provides the Singleton instance of PreferenceManager.
     * @param context The context used to access SharedPreferences.
     * @return The Singleton instance of PreferenceManager.
     */
    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Saves a boolean value in SharedPreferences.
     * @param key   The key under which the value is saved.
     * @param value The boolean value to save.
     */
    public void putBoolean(String key, Boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply(); // Save the changes
    }

    /**
     * Retrieves a boolean value from SharedPreferences.
     * @param key The key of the value to retrieve.
     * @return The boolean value associated with the key, or false if not found.
     */
    public Boolean getBoolean(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    /**
     * Saves a string value in SharedPreferences.
     * @param key   The key under which the value is saved.
     * @param value The string value to save.
     */
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply(); // Save the changes
    }

    /**
     * Retrieves a string value from SharedPreferences.
     * @param key The key of the value to retrieve.
     * @return The string value associated with the key, or null if not found.
     */
    public String getString(String key) {
        return sharedPreferences.getString(key, null);
    }

    /**
     * Clears all values from SharedPreferences.
     */
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply(); // Save the changes
    }
}