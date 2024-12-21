package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityTaskEditorBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

/**
 * Manages the creation and editing of tasks.
 * Allows users to set task titles, descriptions, and completion dates/times.
 */
public class TaskEditorActivity extends AppCompatActivity {
    /** ViewBinding object for accessing layout views */
    private ActivityTaskEditorBinding binding;
    /** Firestore database instance */
    private FirebaseFirestore db;
    /** Preference manager for storing and retrieving user preferences */
    private PreferenceManager preferenceManager;

    /** Indicates if the user is editing an existing task */
    private boolean isEditing = false;
    /** The task being edited, null if creating a new task */
    private Task task;

    /** A Calendar instance for managing selected date and time */
    private final Calendar selectedDateTime = Calendar.getInstance();

    /**
     * Called when the activity is created.
     * Initializes UI and checks if editing an existing task.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTaskEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setupListeners();
        checkIfEditing();
    }

    /**
     * Initializes Firebase Firestore and PreferenceManager.
     */
    private void init() {
        db = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(this);
    }

    /**
     * Sets up event listeners for UI components, including visibility toggles,
     * date/time pickers, and button actions. Uses anonymous inner classes instead of lambda expressions.
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

        binding.buttonSaveTask.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the save button is clicked. Saves the task.
             *
             * @param v The clicked view.
             */
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        binding.buttonCancelTask.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the cancel button is clicked. Finishes the activity.
             *
             * @param v The clicked view.
             */
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * Sets up toggle visibility logic for date/time pickers.
     *
     * @param toggleButton The button that toggles the visibility.
     * @param viewToShow   The view to show when toggled.
     * @param viewToHide   The view to hide when toggled.
     */
    private void setupToggleVisibilityListener(View toggleButton, final View viewToShow, final View viewToHide) {
        toggleButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the toggle button is clicked.
             *
             * @param v The clicked view.
             */
            @Override
            public void onClick(View v) {
                toggleVisibility(viewToShow);
                if (viewToShow.getVisibility() == View.VISIBLE) {
                    viewToHide.setVisibility(View.GONE);
                }
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
     * Checks if the activity was started with a Task object for editing.
     * If yes, initializes the UI fields with the task data.
     */
    private void checkIfEditing() {
        Object receivedTask = getIntent().getSerializableExtra("Task");
        if (receivedTask instanceof Task) {
            isEditing = true;
            task = (Task) receivedTask;
            binding.textTitle.setText("Edit Task");
            populateFields(task);
        } else {
            binding.textTitle.setText("Creating New Task");
        }
    }

    /**
     * Populates the input fields with data from the provided task.
     *
     * @param task The task whose data will populate the fields.
     */
    private void populateFields(Task task) {
        binding.inputTitle.setText(task.getTitle());
        binding.inputDescription.setText(task.getDescription());
        binding.inputDate.setText(task.getCompletionDate());
        binding.inputTime.setText(task.getCompletionTime());
    }

    /**
     * Gathers user input and either updates an existing task or creates a new one.
     */
    private void saveTask() {
        String title = binding.inputTitle.getText().toString().trim();
        String description = binding.inputDescription.getText().toString().trim();
        String date = binding.inputDate.getText().toString();
        String time = binding.inputTime.getText().toString();

        if (!validateInputs(title, date, time)) {
            return;
        }

        if (isEditing) {
            updateExistingTask(title, description, date, time);
        } else {
            createNewTask(title, description, date, time);
        }
    }

    /**
     * Updates an existing task in Firestore with the provided details.
     *
     * @param title       The task title.
     * @param description The task description.
     * @param date        The completion date.
     * @param time        The completion time.
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
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when the task update succeeds.
                     * @param aVoid Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void aVoid) {
                        Utilities.showToast(TaskEditorActivity.this, "", Utilities.ToastType.SUCCESS);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when updating the task fails.
                     * @param e The exception that caused the failure.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utilities.showToast(TaskEditorActivity.this, "Failed to update task.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Creates a new task in Firestore using the provided details.
     *
     * @param title       The task title.
     * @param description The task description.
     * @param date        The completion date.
     * @param time        The completion time.
     */
    private void createNewTask(String title, String description, String date, String time) {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        String taskId = db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .document()
                .getId();

        Task newTask = new Task(taskId, title, description, null, date, time);

        db.collection("Users")
                .document(userId)
                .collection("Tasks")
                .document(taskId)
                .set(newTask)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when the task creation succeeds.
                     * @param aVoid Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void aVoid) {
                        Utilities.showToast(TaskEditorActivity.this, "", Utilities.ToastType.SUCCESS);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when creating the task fails.
                     * @param e The exception that caused the failure.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utilities.showToast(TaskEditorActivity.this, "Failed to create task.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Validates that all required fields are filled in correctly.
     *
     * @param title The entered title.
     * @param date  The entered completion date.
     * @param time  The entered completion time.
     * @return True if inputs are valid, false otherwise.
     */
    private boolean validateInputs(String title, String date, String time) {
        String datePattern = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$";
        String timePattern = "^[0-9]{2}:[0-9]{2}$";
        boolean isValid = false;

        // Check for empty or placeholder fields
        if(date.isEmpty()){
            Utilities.showToast(this, "Please pick/enter date", Utilities.ToastType.WARNING);
        } else if (!date.matches(datePattern)) {
            Utilities.showToast(this, "Please enter a valid date (yyyy-MM-dd)", Utilities.ToastType.WARNING);
        } else if (time.isEmpty()){
            Utilities.showToast(this, "Please pick/enter time", Utilities.ToastType.WARNING);
        } else if (!time.matches(timePattern)) {
            Utilities.showToast(this, "Please enter a valid time (HH:mm)", Utilities.ToastType.WARNING);
        } else if (title.isEmpty()) {
            Utilities.showToast(this, "Please enter title", Utilities.ToastType.WARNING);
        } else {
            isValid = true;
        }

        return isValid;
    }
}