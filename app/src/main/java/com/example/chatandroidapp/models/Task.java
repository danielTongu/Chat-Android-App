package com.example.chatandroidapp.models;

import com.google.firebase.firestore.PropertyName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Task model representing a single task in the app.
 */
public class Task {
    private String id; // Firestore document ID (excluded from serialization)

    @PropertyName("title")
    private String title; // Title of the task

    @PropertyName("description")
    private String description; // Description of the task

    @PropertyName("createdDate")
    public final String createdDate; // Date when the task was created, set automatically

    @PropertyName("completionDate")
    private String completionDate; // Date when the task is to be completed

    @PropertyName("completionTime")
    private String completionTime; // Time when the task is to be completed

    @PropertyName("completed")
    private boolean completed; // Whether the task is completed or not

    /**
     * Default constructor required for Firestore.
     * Initializes the createdDate to the current date.
     */
    public Task() {
        this.createdDate = getCurrentDate();
    }

    /**
     * Gets the Firestore document ID.
     * @return The document ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the Firestore document ID.
     * @param id The document ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the title of the task.
     * @return The task title.
     */
    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the task.
     * @param title The task title.
     */
    @PropertyName("title")
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the description of the task.
     * @return The task description.
     */
    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the task.
     * @param description The task description.
     */
    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the completion date of the task.
     * @return The task's completion date in MM/dd/yyyy format.
     */
    @PropertyName("completionDate")
    public String getCompletionDate() {
        return completionDate;
    }

    /**
     * Sets the completion date of the task.
     * @param completionDate The task's completion date in MM/dd/yyyy format.
     */
    @PropertyName("completionDate")
    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    /**
     * Gets the completion time of the task.
     * @return The task's completion time.
     */
    @PropertyName("completionTime")
    public String getCompletionTime() {
        return completionTime;
    }

    /**
     * Sets the completion time of the task.
     * @param completionTime The task's completion time.
     */
    @PropertyName("completionTime")
    public void setCompletionTime(String completionTime) {
        this.completionTime = completionTime;
    }

    /**
     * Gets the completion status of the task.
     * @return True if the task is completed, false otherwise.
     */
    @PropertyName("completed")
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Sets the completion status of the task.
     * @param completed True if the task is completed, false otherwise.
     */
    @PropertyName("completed")
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * Gets the current date in MM/dd/yyyy format.
     * @return The current date as a string.
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}