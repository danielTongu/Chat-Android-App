package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityTaskWriterBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

/**
 * TaskWriterActivity manages the creation and editing of tasks.
 * Users can set task details, including title, description, completion date, and time.
 */
public class TaskWriterActivity extends AppCompatActivity {

    private ActivityTaskWriterBinding binding;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    private boolean isEditing = false;
    private String taskId;
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
        taskId = getIntent().getStringExtra("taskId");
    }

    /**
     * Sets up listeners for DatePicker, TimePicker, and action buttons.
     */
    private void setupListeners() {
        // Toggle DatePicker visibility
        binding.toggleDatePickerVisibility.setOnClickListener(v -> {
            toggleVisibility(binding.taskCompletionDatePicker);
            if (binding.taskCompletionDatePicker.getVisibility() == View.VISIBLE) {
                binding.taskCompletionTimePicker.setVisibility(View.GONE); // Hide TimePicker if visible
            }
        });

        // Toggle TimePicker visibility
        binding.toggleTimePickerVisibility.setOnClickListener(v -> {
            toggleVisibility(binding.taskCompletionTimePicker);
            if (binding.taskCompletionTimePicker.getVisibility() == View.VISIBLE) {
                binding.taskCompletionDatePicker.setVisibility(View.GONE); // Hide DatePicker if visible
            }
        });

        // DatePicker Listener: Update date on change
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

        // TimePicker Listener: Update time on change
        binding.taskCompletionTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            updateSelectedTime();
        });

        // Save and Cancel Buttons
        binding.buttonSaveTask.setOnClickListener(v -> saveTask());
        binding.buttonCancelTask.setOnClickListener(v -> finish());
    }

    /**
     * Toggles the visibility of the provided view.
     * @param view The view to toggle visibility.
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
        if (taskId != null) {
            isEditing = true;
            binding.titleTaskEditor.setText("Edit Task");

            String userId = preferenceManager.getString(Constants.KEY_ID, "");
            db.collection("Users")
                    .document(userId)
                    .collection("Tasks")
                    .document(taskId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        task = documentSnapshot.toObject(Task.class);
                        if (task != null) {
                            populateFields(task);
                        }
                    })
                    .addOnFailureListener(e -> showToast("Failed to load task details."));
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
     */
    private void saveTask() {
        String title = binding.inputTaskTitle.getText().toString().trim();
        String description = binding.inputTaskDescription.getText().toString().trim();
        String date = binding.selectedDate.getText().toString();
        String time = binding.selectedTime.getText().toString();

        if (!validateInputs(title, description, date, time)) return;

        if (isEditing) {
            task.setTitle(title);
            task.setDescription(description);
            task.setCompletionDate(date);
            task.setCompletionTime(time);
        } else {
            task = new Task(null, title, description, null, date, time);
        }

        String userId = preferenceManager.getString(Constants.KEY_ID, "");

        db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    showToast("Task saved successfully!");
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to save task."));
    }

    /**
     * Validates user inputs.
     */
    private boolean validateInputs(String title, String description, String date, String time) {
        if (title.isEmpty() || description.isEmpty() || date.equals("yyyy-mm-dd") || time.equals("HH:MM")) {
            showToast("Please fill in all fields.");
            return false;
        }
        return true;
    }

    /**
     * Displays a toast message.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}