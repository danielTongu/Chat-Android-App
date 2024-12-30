package com.example.chatandroidapp.activities;

import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityTaskWriterBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Handles the creation, editing, and deletion of tasks in the chat application.
 * Updates Firestore with new tasks, modified tasks, and task removals.
 */
public class TaskWriterActivity extends AppCompatActivity {

    /** View binding to access UI elements */
    private ActivityTaskWriterBinding binding;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** Preference manager for accessing user-specific data */
    private PreferenceManager preferenceManager;

    /** A Calendar instance for managing selected date and time */
    private final Calendar selectedDateTime = Calendar.getInstance();

    /** Indicates if the activity is editing an existing task */
    private boolean isEditing = false;

    /** Task being edited, null if creating a new task */
    private Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTaskWriterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setupListeners();
        loadTaskDataIfEditing();
        showLoading(false, null);
    }

    /**
     * Initializes the Firestore database instance and preference manager.
     */
    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(this);
    }

    /**
     * Sets up event listeners for UI components, including visibility toggles,
     * date/time pickers, and button actions.
     */
    private void setupListeners() {
        setupToggleVisibilityListener(binding.layoutDate, binding.datePicker, binding.timePicker);
        setupToggleVisibilityListener(binding.layoutTime, binding.timePicker, binding.datePicker);

        binding.datePicker.init(
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH),
                new DatePicker.OnDateChangedListener() {
                    /**
                     * Called when the user changes the date in the DatePicker.
                     *
                     * @param view The DatePicker view.
                     * @param year The selected year.
                     * @param monthOfYear The selected month (0-based).
                     * @param dayOfMonth The selected day of the month.
                     */
                    @Override
                    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        selectedDateTime.set(Calendar.YEAR, year);
                        selectedDateTime.set(Calendar.MONTH, monthOfYear);
                        selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateSelectedDate();
                    }
                }
        );

        binding.timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            /**
             * Called when the user changes the time in the TimePicker.
             *
             * @param view      The TimePicker view.
             * @param hourOfDay The selected hour (24-hour format).
             * @param minute    The selected minute.
             */
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                updateSelectedTime();
            }
        });

        binding.buttonSaveTask.setOnClickListener(view -> saveTask());
        binding.buttonCancelTask.setOnClickListener(view -> finish());
    }

    /**
     * Sets up toggle visibility logic for date/time pickers.
     *
     * @param toggleButton The button that toggles the visibility.
     * @param viewToShow   The view to show when toggled.
     * @param viewToHide   The view to hide when toggled.
     */
    private void setupToggleVisibilityListener(View toggleButton, final View viewToShow, final View viewToHide) {
        toggleButton.setOnClickListener(view -> {
            toggleVisibility(viewToShow);
            if (viewToShow.getVisibility() == View.VISIBLE) {
                viewToHide.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Toggles the visibility of the provided view.
     *
     * @param view The view whose visibility is toggled.
     */
    private void toggleVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the selected date TextView based on the currently selected date in the calendar.
     */
    private void updateSelectedDate() {
        binding.inputDate.setText(String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH) + 1,
                selectedDateTime.get(Calendar.DAY_OF_MONTH)));
    }

    /**
     * Updates the selected time TextView based on the currently selected time in the calendar.
     */
    private void updateSelectedTime() {
        binding.inputTime.setText(String.format(Locale.getDefault(),
                "%02d:%02d",
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE)));
    }

    /**
     * Loads task data for editing if a task is passed to the activity.
     */
    private void loadTaskDataIfEditing() {
        Object receivedTask = getIntent().getSerializableExtra("Task");
        if (receivedTask instanceof Task) {
            isEditing = true;
            task = (Task) receivedTask;
            binding.textTitle.setText("Edit Task");
            populateFields(task);
        } else {
            binding.textTitle.setText("Create New Task");
        }
    }

    /**
     * Populates the UI fields with the given task's data.
     *
     * @param task The task whose data will populate the fields.
     */
    private void populateFields(Task task) {
        binding.inputTitle.setText(task.title);
        binding.inputDescription.setText(task.description);
        binding.inputDate.setText(task.completionDate);
        binding.inputTime.setText(task.completionTime);
    }

    /**
     * Gathers input data and either updates an existing task or creates a new one.
     */
    private void saveTask() {
        String title = binding.inputTitle.getText().toString().trim();
        String description = binding.inputDescription.getText().toString().trim();
        String date = binding.inputDate.getText().toString();
        String time = binding.inputTime.getText().toString();

        if (!validateInputs(title, date, time)) { return; }

        if (isEditing) {
            updateTask(title, description, date, time);
        } else {
            createTask(title, description, date, time);
        }
    }

    /**
     * Validates the task input fields for correctness.
     *
     * @param title The entered task title.
     * @param date  The entered task completion date.
     * @param time  The entered task completion time.
     * @return True if all inputs are valid, false otherwise.
     */
    private boolean validateInputs(String title, String date, String time) {
        boolean isValid = false;
        if (title.isEmpty()) {
            Utilities.showToast(this, "Please enter a title.", Utilities.ToastType.WARNING);
        } else if (!date.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) {
            Utilities.showToast(this, "Please enter a valid date (yyyy-MM-dd).", Utilities.ToastType.WARNING);
        } else if (!time.matches("^[0-9]{2}:[0-9]{2}$")) {
            Utilities.showToast(this, "Please enter a valid time (HH:mm).", Utilities.ToastType.WARNING);
        } else {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Updates an existing task in Firestore under the current user's Tasks subcollection.
     *
     * @param title       The updated title of the task.
     * @param description The updated description of the task.
     * @param date        The updated completion date of the task.
     * @param time        The updated completion time of the task.
     */
    private void updateTask(String title, String description, String date, String time) {
        showLoading(true, "Updating task...");

        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        if (userId.isEmpty()) {
            Utilities.showToast(this, "User not logged in.", Utilities.ToastType.ERROR);
            showLoading(false, null);
            return;
        }

        if (task == null || task.id.isEmpty()) {
            Utilities.showToast(this, "Invalid task data.", Utilities.ToastType.ERROR);
            showLoading(false, null);
            return;
        }

        // Reference to the specific task document
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection("Tasks")
                .document(task.id)
                .update(
                        "title", title,
                        "description", description,
                        "completionDate", date,
                        "completionTime", time
                )
                .addOnSuccessListener(aVoid -> {
                    Utilities.showToast(TaskWriterActivity.this, "", Utilities.ToastType.SUCCESS);
                    showLoading(false, null);
                    setResult(RESULT_OK); // Notify success
                    finish(); // Close the activity after successful update
                })
                .addOnFailureListener(e -> logCriticalError("Failed to update task.", e));
    }

    /**
     * Creates a new task and adds it to Firestore under the current user's Tasks subcollection.
     *
     * @param title       The title of the task.
     * @param description The description of the task.
     * @param date        The completion date of the task.
     * @param time        The completion time of the task.
     */
    private void createTask(String title, String description, String date, String time) {
        showLoading(true, "Creating task...");

        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        if (userId.isEmpty()) {
            Utilities.showToast(this, "User not logged in.", Utilities.ToastType.ERROR);
            showLoading(false, null);
            return;
        }

        // Create the Task object with the pre-generated ID
        String taskId = db.collection(Constants.KEY_COLLECTION_USERS).document(userId).collection("Tasks").document().getId();
        Task newTask = new Task(taskId, userId, title, description, date, time);

        // Set the Task data in Firestore using the helper function
        setTaskData(userId, newTask);
    }

    /**
     * Sets the Task data in Firestore under the specified Tasks subcollection.
     *
     * @param userId      The ID of the user creating the task.
     * @param task        The Task object to be saved.
     */
    private void setTaskData(String userId, Task task) {
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection("Tasks")
                .document(task.id)
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Utilities.showToast(TaskWriterActivity.this, "", Utilities.ToastType.SUCCESS);
                    showLoading(false, null);
                    setResult(RESULT_OK); // Notify success
                    finish(); // Close the activity after successful creation
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to set task data.", e);
                });
    }

    /**
     * Logs critical errors, shows a toast message, and hides the loading indicator.
     *
     * @param message The error message to display.
     * @param e       The exception that caused the error.
     */
    private void logCriticalError(String message, Exception e) {
        showLoading(false, null);
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        android.util.Log.e("TaskEditorActivity", message, e);
    }

    /**
     * Shows or hides the loading indicator and manages UI component states.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display alongside the loading indicator.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonSaveTask.setEnabled(!isLoading);
        binding.buttonCancelTask.setEnabled(!isLoading);
        binding.inputTitle.setEnabled(!isLoading);
        binding.inputDescription.setEnabled(!isLoading);
        binding.inputDate.setEnabled(!isLoading);
        binding.inputTime.setEnabled(!isLoading);
    }
}