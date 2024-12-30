package com.example.chatandroidapp.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a Task in the application, including attributes for title, description,
 * user association, completion status, and timestamps for creation and completion.
 */
public class Task implements Serializable {

    /**
     * The unique identifier for the task (Firestore Document ID).
     */
    @PropertyName("id")
    public final String id;

    /**
     * The ID of the user who owns this task.
     */
    @PropertyName("userId")
    public final String userId;

    /**
     * The timestamp when the task was created (auto-assigned by Firestore).
     */
    @ServerTimestamp
    @PropertyName("createdDate")
    public Date createdDate;

    /**
     * The title of the task.
     */
    @PropertyName("title")
    public String title;

    /**
     * The description of the task.
     */
    @PropertyName("description")
    public String description;

    /**
     * Whether the task is completed.
     */
    @PropertyName("isCompleted")
    public boolean isCompleted;

    /**
     * The date by which the task should be completed.
     */
    @PropertyName("completionDate")
    public String completionDate;

    /**
     * The time by which the task should be completed.
     */
    @PropertyName("completionTime")
    public String completionTime;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     * Assigns default values to final fields.
     */
    public Task() {
        this.id = ""; // Default value for task ID
        this.userId = ""; // Default value for user ID
        this.createdDate = new Date(); // Default to the current date
    }

    /**
     * Parameterized constructor to create a Task instance.
     *
     * @param id             The unique identifier for the task.
     * @param userId         The ID of the user who owns the task.
     * @param title          The title of the task.
     * @param description    The description of the task.
     * @param completionDate The date by which the task should be completed.
     * @param completionTime The time by which the task should be completed.
     */
    public Task(String id, String userId, String title, String description, String completionDate, String completionTime) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.completionDate = completionDate;
        this.completionTime = completionTime;
        this.isCompleted = false;
        this.createdDate = new Date();
    }

    /**
     * Determines whether the task is outdated (created more than 90 days ago).
     *
     * @return True if the task is outdated, otherwise false.
     */
    public boolean isOutdated() {
        if (createdDate == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        long taskAgeInMillis = currentTime - createdDate.getTime();
        long taskAgeInDays = taskAgeInMillis / (1000 * 60 * 60 * 24);

        return taskAgeInDays > 90;
    }

    @NonNull
    @Override
    public String toString() {
        String formattedDate = createdDate != null
                ? new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(createdDate)
                : "N/A";

        return String.format(
                Locale.getDefault(),
                "Task[ID=%s, Title=%s, UserID=%s, IsCompleted=%b, CreatedDate=%s, CompletionDate=%s, CompletionTime=%s]",
                id, title, userId, isCompleted, formattedDate, completionDate, completionTime
        );
    }
}