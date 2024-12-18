package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityTaskWriterBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

/**
 * TaskWriterActivity manages the creation and editing of tasks.
 */
public class TaskWriterActivity extends AppCompatActivity {
    private ActivityTaskWriterBinding binding;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    private boolean isEditing = false;
    private Task task;

    private final Calendar selectedDateTime = Calendar.getInstance(); // Calendar for date and time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaskWriterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setupListeners();
        checkIfEditing();
    }

    /**
     * Initializes Firebase, SharedPreferences, and other defaults.
     */
    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(this);
    }

    /**
     * Sets up listeners for UI interactions.
     */
    private void setupListeners() {
        setupToggleVisibilityListener(binding.toggleDatePickerVisibility,
                binding.taskCompletionDatePicker, binding.taskCompletionTimePicker
        );
        setupToggleVisibilityListener(binding.toggleTimePickerVisibility,
                binding.taskCompletionTimePicker, binding.taskCompletionDatePicker
        );

        binding.taskCompletionDatePicker.init(
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, month);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateSelectedDate();
                }
        );

        binding.taskCompletionTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            updateSelectedTime();
        });

        binding.buttonSaveTask.setOnClickListener(v -> saveTask());
        binding.buttonCancelTask.setOnClickListener(v -> finish());
    }

    /**
     * Sets up toggle visibility logic for views.
     *
     * @param toggleButton The button that toggles the visibility.
     * @param viewToShow The view to show when toggled.
     * @param viewToHide The view to hide when toggled.
     */
    private void setupToggleVisibilityListener(View toggleButton, View viewToShow, View viewToHide) {
        toggleButton.setOnClickListener(v -> {
            toggleVisibility(viewToShow);
            if (viewToShow.getVisibility() == View.VISIBLE) {
                viewToHide.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Toggles the visibility of the provided view.
     */
    private void toggleVisibility(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the selected date TextView based on the calendar.
     */
    private void updateSelectedDate() {
        String formattedDate = String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH) + 1,
                selectedDateTime.get(Calendar.DAY_OF_MONTH));
        binding.selectedDate.setText(formattedDate);
    }

    /**
     * Updates the selected time TextView based on the calendar.
     */
    private void updateSelectedTime() {
        String formattedTime = String.format(Locale.getDefault(),
                "%02d:%02d",
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE));
        binding.selectedTime.setText(formattedTime);
    }

    /**
     * Checks if editing an existing task and populates data.
     */
    private void checkIfEditing() {
        Object receivedTask = getIntent().getSerializableExtra("Task");
        if (receivedTask instanceof Task) {
            isEditing = true;
            task = (Task) receivedTask; // Cast to Task object
            binding.titleTaskEditor.setText("Edit Task");
            populateFields(task);
        } else {
            binding.titleTaskEditor.setText("Creating a new task");
        }
    }

    /**
     * Populates fields when editing a task.
     */
    private void populateFields(Task task) {
        binding.inputTaskTitle.setText(task.getTitle());
        binding.inputTaskDescription.setText(task.getDescription());
        binding.selectedDate.setText(task.getCompletionDate());
        binding.selectedTime.setText(task.getCompletionTime());
    }

    /**
     * Saves the task to Firestore.
     * Calls either `updateExistingTask` or `createNewTask` based on the context.
     */
    private void saveTask() {
        String title = binding.inputTaskTitle.getText().toString().trim();
        String description = binding.inputTaskDescription.getText().toString().trim();
        String date = binding.selectedDate.getText().toString();
        String time = binding.selectedTime.getText().toString();

        if (!validateInputs(title, description, date, time)) { return; }

        if (isEditing) {
            updateExistingTask(title, description, date, time);
        } else {
            createNewTask(title, description, date, time);
        }
    }

    /**
     * Updates an existing task in Firestore.
     */
    private void updateExistingTask(String title, String description, String date, String time) {
        task.setTitle(title);
        task.setDescription(description);
        task.setCompletionDate(date);
        task.setCompletionTime(time);

        String userId = preferenceManager.getString(Constants.KEY_ID, "");

        db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .document(task.getId())
                .set(task)
                .addOnSuccessListener(aVoid -> {
                    Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> Utilities.showToast(
                        this, "Failed to update task.", Utilities.ToastType.ERROR)
                );
    }

    /**
     * Creates a new task in Firestore.
     */
    private void createNewTask(String title, String description, String date, String time) {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");

        // Generate a new document ID
        String taskId = db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .document()
                .getId();

        // Create the new Task object with the generated ID
        Task newTask = new Task(taskId, title, description, null, date, time);

        // Save the task to Firestore using the generated ID
        db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .document(taskId)
                .set(newTask) // Use set() instead of add() to specify the document ID
                .addOnSuccessListener(aVoid -> {
                    Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> Utilities.showToast(
                        this, "Failed to create task.", Utilities.ToastType.ERROR)
                );
    }

    /**
     * Validates user inputs.
     */
    private boolean validateInputs(String title, String description, String date, String time) {
        if (title.isEmpty() || description.isEmpty() || date.equals("yyyy-mm-dd") || time.equals("HH:MM")) {
            Utilities.showToast(this, "Please fill in all fields.", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }
}