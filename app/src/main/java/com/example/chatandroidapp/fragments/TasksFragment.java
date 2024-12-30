package com.example.chatandroidapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.TaskWriterActivity;
import com.example.chatandroidapp.adapters.TaskAdapter;
import com.example.chatandroidapp.databinding.FragmentTaskBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * TasksFragment manages task display, filtering, and navigation to TaskEditorActivity for adding or editing tasks.
 * It handles fetching tasks from Firestore, filtering them by date or completion status,
 * and provides UI interactions for marking tasks complete, editing, and deleting.
 */
public class TasksFragment extends Fragment {
    /** ViewBinding instance for fragment_tasks.xml */
    private FragmentTaskBinding binding;

    /** Adapter for displaying tasks in a RecyclerView */
    private TaskAdapter tasksAdapter;

    /** Full list of tasks obtained from Firestore */
    private List<Task> tasksList;

    /** Current filtered list of tasks displayed to the user */
    private List<Task> tasksListFiltered;

    /** PreferenceManager for storing user-related data */
    private PreferenceManager preferenceManager;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** ActivityResultLauncher for handling results from TaskEditorActivity */
    private final ActivityResultLauncher<Intent> taskEditorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    fetchTasksFromFirestore();
                }
            });

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container          The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        preferenceManager = PreferenceManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        fetchTasksFromFirestore();
        setupListeners();

        return binding.getRoot();
    }

    /**
     * Sets up the RecyclerView with a LinearLayoutManager and a TasksAdapter.
     * The adapter uses an explicit listener for handling task actions.
     */
    private void setupRecyclerView() {
        showLoading(true, "setting up recycler...");

        tasksListFiltered = new ArrayList<>();
        tasksList = new ArrayList<>();

        tasksAdapter = new TaskAdapter(tasksListFiltered, new TaskAdapter.TaskAdapterListener() {
            @Override
            public void onTaskCompletedChanged(Task task) {
                toggleTaskCompletion(task);
            }

            @Override
            public void onEditTask(Task task) {
                navigateToTaskEditorActivity(task);
            }

            @Override
            public void onDeleteTask(Task task) {
                deleteTask(task);
            }
        });

        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTasks.setAdapter(tasksAdapter);
    }

    /**
     * Toggles the completion status of a given task and updates Firestore accordingly.
     *
     * @param task The task whose completion status should be toggled.
     */
    private void toggleTaskCompletion(Task task) {
        showLoading(true, null);
        task.isCompleted = !task.isCompleted;

        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .document(task.id)
                .update("completed", task.isCompleted)
                .addOnSuccessListener(unused -> {
                    int position = tasksList.indexOf(task);
                    if (position != -1) {
                        tasksAdapter.notifyItemChanged(position);
                    }
                    showLoading(false, null);
                })
                .addOnFailureListener(e -> logCriticalError("Failed to update task completion", e));
    }

    private void logCriticalError(String message, Exception e) {
        showLoading(false, message);
        Utilities.showToast(getContext(), message, Utilities.ToastType.ERROR);
        String TAG = "TASKS_FRAGMENT";
        android.util.Log.e(TAG, message, e);
    }

    /**
     * Shows or hides the loading indicator and manages UI component states.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display alongside the loading indicator.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.tasksCoordinator.setEnabled(!isLoading);
        binding.buttonAddTask.setEnabled(!isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (message == null) {
            binding.recyclerViewTasks.setVisibility(View.VISIBLE);
            binding.textProgressMessage.setVisibility(View.GONE);
        } else {
            binding.recyclerViewTasks.setVisibility(View.GONE);
            binding.textProgressMessage.setVisibility(View.VISIBLE);
            binding.textProgressMessage.setText(message);
        }
    }

    /**
     * Navigates to TaskEditorActivity for adding a new task or editing an existing one.
     *
     * @param task The task to edit, or null if creating a new task.
     */
    private void navigateToTaskEditorActivity(Task task) {
        Intent intent = new Intent(getContext(), TaskWriterActivity.class);
        if (task != null) {
            intent.putExtra("Task", task);
        }
        taskEditorLauncher.launch(intent);
    }

    /**
     * Deletes the current task from Firestore under the current user's Tasks subcollection.
     *
     * @param task The task to delete.
     */
    private void deleteTask(Task task) {
        if (task == null || task.id.isEmpty()) {
            Utilities.showToast(getContext(), "Invalid task data.", Utilities.ToastType.ERROR);
            return;
        }

        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        if (userId.isEmpty()) {
            Utilities.showToast(getContext(), "User not logged in.", Utilities.ToastType.ERROR);
            return;
        }

        showLoading(true, null);

        // Reference to the specific task document
        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection("Tasks")
                .document(task.id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Lastly, remove from the local lists
                    int position = tasksList.indexOf(task);
                    if (position != -1) {
                        tasksList.remove(task);
                        tasksListFiltered.remove(task);
                        tasksAdapter.notifyItemRemoved(position);
                    }
                    showLoading(false, null);
                })
                .addOnFailureListener(e -> logCriticalError("Failed to delete task", e));
    }

    /**
     * Fetches tasks from Firestore using the user's list of task document IDs.
     * Filters out outdated or completed tasks as necessary.
     */
    private void fetchTasksFromFirestore() {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");

        if (userId.isEmpty()) {
            Utilities.showToast(getContext(), "User not logged in.", Utilities.ToastType.ERROR);
            return;
        }

        showLoading(true, "fetching tasks...");

        db.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection("Tasks").get()
                .addOnSuccessListener(taskSnapshots -> {
                    tasksList.clear();
                    tasksListFiltered.clear();

                    for (DocumentSnapshot doc : taskSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            if (task.isOutdated() && task.isCompleted) {
                                deleteTask(task);
                            } else {
                                tasksList.add(task);
                                if (!task.isCompleted) {
                                    tasksListFiltered.add(task);
                                }
                            }
                        }
                    }
                    updateTaskListUI(null, false);
                    showLoading(false, null);
                })
                .addOnFailureListener(e -> logCriticalError("Failed to retrieve task data.", e));
    }

    /**
     * Updates the task list title and notifies the adapter based on the filtered tasks.
     *
     * @param formattedDate   The formatted date string to display in the title (can be null for general scenarios).
     * @param showResetButton Whether to show the reset button for filtered views.
     */
    private void updateTaskListUI(@Nullable String formattedDate, boolean showResetButton) {
        String title;

        if (formattedDate != null) {
            title = tasksListFiltered.isEmpty()
                    ? String.format("No tasks for %s", formattedDate)
                    : String.format("Tasks for %s", formattedDate);
        } else {
            title = tasksList.isEmpty() ? "No tasks" : "All Pending Tasks";
        }

        binding.textTitleRecyclerView.setText(title);
        binding.buttonAllPendingTasks.setVisibility(showResetButton ? View.VISIBLE : View.GONE);
        tasksAdapter.notifyDataSetChanged();
    }

    /**
     * Sets up listeners for UI elements such as the CalendarView and the buttons.
     * Handles date selection and task list resetting.
     */
    private void setupListeners() {
        binding.tasksCoordinator.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            /**
             * Triggered when the user changes the date on the CalendarView.
             *
             * @param view The CalendarView whose date was changed.
             * @param year The year component of the selected date.
             * @param month The month component of the selected date (0-indexed).
             * @param dayOfMonth The day of the month selected.
             */
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                long selectedDateMillis = selectedCalendar.getTimeInMillis();
                filterTasksByDate(selectedDateMillis);
            }
        });

        binding.buttonAllPendingTasks.setOnClickListener(view -> showAllNonCompletedTasks());
        binding.buttonAddTask.setOnClickListener(view -> navigateToTaskEditorActivity(null));
    }

    /**
     * Filters the tasks by the selected date.
     * Outdated completed tasks are deleted.
     * Updates the UI to show tasks for the selected date.
     *
     * @param selectedDateMillis The date selected by the user, in milliseconds.
     */
    private void filterTasksByDate(long selectedDateMillis) {
        showLoading(true, null);
        SimpleDateFormat calendarFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedSelectedDate = calendarFormatter.format(new Date(selectedDateMillis));

        tasksListFiltered.clear();

        Iterator<Task> iterator = tasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            try {
                String taskCompletionDate = task.completionDate;
                if (task.isOutdated() && task.isCompleted) {
                    deleteTask(task);
                    iterator.remove();
                } else if (taskCompletionDate.equals(formattedSelectedDate)) {
                    tasksListFiltered.add(task);
                }
            } catch (Exception e) {
                logCriticalError("Error processing task date.", e);
            }
        }

        updateTaskListUI(formattedSelectedDate, true);
        showLoading(false, null);
    }

    /**
     * Shows all non-completed tasks.
     * Deletes outdated completed tasks first, then updates the UI.
     */
    private void showAllNonCompletedTasks() {
        showLoading(true, "loading tasks...");
        tasksListFiltered.clear();

        Iterator<Task> iterator = tasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.isOutdated() && task.isCompleted) {
                deleteTask(task);
                iterator.remove();
            } else if (!task.isCompleted) {
                tasksListFiltered.add(task);
            }
        }

        updateTaskListUI(null, false);
        showLoading(false, null);
    }

    /**
     * Called when the view is destroyed.
     * Cleans up binding references to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}