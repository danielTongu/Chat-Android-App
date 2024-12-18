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
 * completion status, and timestamps for creation and completion.
 */
public class Task implements Serializable {

    /** The unique identifier for the task (Firestore Document ID). */
    @PropertyName("id")
    private String id;

    /** The title of the task. */
    @PropertyName("title")
    private String title;

    /** The description of the task. */
    @PropertyName("description")
    private String description;

    /** Whether the task is completed. */
    @PropertyName("completed")
    private boolean completed;

    /** The timestamp when the task was created (auto-assigned by Firestore). */
    @ServerTimestamp
    @PropertyName("createdDate")
    private Date createdDate;

    /** The date by which the task should be completed. */
    @PropertyName("completionDate")
    private String completionDate;

    /** The time by which the task should be completed. */
    @PropertyName("completionTime")
    private String completionTime;

    /**
     * Default constructor required for Firestore serialization/deserialization.
     */
    public Task() {
        // Default constructor for Firestore
    }

    /**
     * Parameterized constructor to create a new Task instance.
     *
     * @param id             The unique identifier for the task.
     * @param title          The title of the task.
     * @param description    The description of the task.
     * @param createdDate    The date the task was created.
     * @param completionDate The date by which the task should be completed.
     * @param completionTime The time by which the task should be completed.
     */
    public Task(String id, String title, String description, Date createdDate, String completionDate, String completionTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.completed = false; // Default to not completed
        this.createdDate = createdDate;
        this.completionDate = completionDate;
        this.completionTime = completionTime;
    }

    // ==================== GETTERS ====================

    /**
     * Gets the unique identifier for the task.
     *
     * @return The unique identifier for the task.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the title of the task.
     *
     * @return The title of the task.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the description of the task.
     *
     * @return The description of the task.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets whether the task is completed.
     *
     * @return True if the task is completed, otherwise false.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Gets the date when the task was created.
     *
     * @return The creation date of the task.
     */
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the date by which the task should be completed.
     *
     * @return The completion date of the task.
     */
    public String getCompletionDate() {
        return completionDate;
    }

    /**
     * Gets the time by which the task should be completed.
     *
     * @return The completion time of the task.
     */
    public String getCompletionTime() {
        return completionTime;
    }

    // ==================== SETTERS ====================

    /**
     * Sets the unique identifier for the task.
     *
     * @param id The unique identifier to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets the title of the task.
     *
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the description of the task.
     *
     * @param description The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets whether the task is completed.
     *
     * @param completed True if the task is completed, otherwise false.
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Sets the date when the task was created.
     *
     * @param createdDate The creation date to set.
     */
    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Sets the date by which the task should be completed.
     *
     * @param completionDate The completion date to set.
     */
    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    /**
     * Sets the time by which the task should be completed.
     *
     * @param completionTime The completion time to set.
     */
    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    // ==================== UTILITY METHODS ====================

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
                "Task[ID=%s, Title=%s, Completed=%b, CreatedDate=%s, CompletionDate=%s, CompletionTime=%s]",
                id, title, completed, formattedDate, completionDate, completionTime
        );
    }
}