package com.example.chatandroidapp.utilities;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class manages the application's shared preferences, providing methods
 * to store and retrieve various types of data.
 *
 * <p>Uses the Singleton pattern to ensure a single instance of the manager.</p>
 *
 * @author Daniel Tongu
 */
public class PreferenceManager {
    /**
     * Key for the name of the SharedPreferences file.
     */
    private static final String KEY_PREFERENCE_NAME = "chatAppPreference";

    private static volatile PreferenceManager instance; // Volatile for thread-safe singleton
    private final SharedPreferences sharedPreferences;

    /**
     * Private constructor to enforce Singleton pattern.
     *
     * @param context The context used to access SharedPreferences.
     */
    private PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(KEY_PREFERENCE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Provides the Singleton instance of PreferenceManager using double-checked locking for thread safety.
     *
     * @param context The context used to access SharedPreferences.
     * @return The Singleton instance of PreferenceManager.
     */
    public static PreferenceManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PreferenceManager.class) {
                if (instance == null) {
                    instance = new PreferenceManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // --- BOOLEAN METHODS ---

    /**
     * Saves a boolean value in SharedPreferences.
     *
     * @param key   The key under which the value is saved.
     * @param value The boolean value to save.
     */
    public void putBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Retrieves a boolean value from SharedPreferences.
     *
     * @param key          The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The boolean value associated with the key, or the default value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // --- STRING METHODS ---

    /**
     * Saves a string value in SharedPreferences.
     *
     * @param key   The key under which the value is saved.
     * @param value The string value to save.
     */
    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    /**
     * Retrieves a string value from SharedPreferences.
     *
     * @param key          The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The string value associated with the key, or the default value.
     */
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // --- INT METHODS ---

    /**
     * Saves an integer value in SharedPreferences.
     *
     * @param key   The key under which the value is saved.
     * @param value The integer value to save.
     */
    public void putInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    /**
     * Retrieves an integer value from SharedPreferences.
     *
     * @param key          The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The integer value associated with the key, or the default value.
     */
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // --- LONG METHODS ---

    /**
     * Saves a long value in SharedPreferences.
     *
     * @param key   The key under which the value is saved.
     * @param value The long value to save.
     */
    public void putLong(String key, long value) {
        sharedPreferences.edit().putLong(key, value).apply();
    }

    /**
     * Retrieves a long value from SharedPreferences.
     *
     * @param key          The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The long value associated with the key, or the default value.
     */
    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    // --- FLOAT METHODS ---

    /**
     * Saves a float value in SharedPreferences.
     *
     * @param key   The key under which the value is saved.
     * @param value The float value to save.
     */
    public void putFloat(String key, float value) {
        sharedPreferences.edit().putFloat(key, value).apply();
    }

    /**
     * Retrieves a float value from SharedPreferences.
     *
     * @param key          The key of the value to retrieve.
     * @param defaultValue The default value to return if the key is not found.
     * @return The float value associated with the key, or the default value.
     */
    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    // --- CLEAR AND REMOVE METHODS ---

    /**
     * Removes a specific key-value pair from SharedPreferences.
     *
     * @param key The key of the value to remove.
     */
    public void remove(String key) {
        sharedPreferences.edit().remove(key).apply();
    }

    /**
     * Clears all values from SharedPreferences.
     */
    public void clear() {
        sharedPreferences.edit().clear().apply();
    }
}