package com.example.chatandroidapp.fragments;

import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.TaskEditorActivity;
import com.example.chatandroidapp.adapters.TasksAdapter;
import com.example.chatandroidapp.databinding.FragmentTasksBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
    private FragmentTasksBinding binding;

    /** Adapter for displaying tasks in a RecyclerView */
    private TasksAdapter tasksAdapter;

    /** Full list of tasks obtained from Firestore */
    private List<Task> allTasksList;

    /** Current filtered list of tasks displayed to the user */
    private List<Task> filteredTasksList;

    /** PreferenceManager for storing user-related data */
    private PreferenceManager preferenceManager;

    /** Firestore database instance */
    private FirebaseFirestore db;

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
        binding = FragmentTasksBinding.inflate(inflater, container, false);
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
        filteredTasksList = new ArrayList<>();
        allTasksList = new ArrayList<>();

        tasksAdapter = new TasksAdapter(filteredTasksList, new TasksAdapter.TaskAdapterListener() {
            /**
             * Triggered when a task's completion state is toggled.
             * @param task The task to toggle completion for.
             */
            @Override
            public void onTaskCompletedChanged(Task task) {
                toggleTaskCompletion(task);
            }

            /**
             * Triggered when the user requests to edit a task.
             * @param task The task to edit.
             */
            @Override
            public void onEditTask(Task task) {
                navigateToTaskEditorActivity(task);
            }

            /**
             * Triggered when the user requests to delete a task.
             * @param task The task to delete.
             */
            @Override
            public void onDeleteTask(Task task) {
                deleteTaskFromFirestore(task);
            }
        });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tasksRecyclerView.setAdapter(tasksAdapter);
    }

    /**
     * Sets up listeners for UI elements such as the CalendarView and the Floating Action Button (FAB).
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

        binding.resetButton.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the reset button is clicked. Shows all non-completed tasks.
             * @param v The View that was clicked.
             */
            @Override
            public void onClick(View v) {
                showAllNonCompletedTasks();
            }
        });

        binding.fabAddTask.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the FAB is clicked. Navigates to TaskEditorActivity to add a new task.
             * @param v The View that was clicked.
             */
            @Override
            public void onClick(View v) {
                navigateToTaskEditorActivity(null);
            }
        });
    }

    /**
     * Toggles the completion status of a given task and updates Firestore accordingly.
     * @param task The task whose completion status should be toggled.
     */
    private void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());

        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .document(task.getId())
                .update("completed", task.isCompleted())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when the completion update is successful.
                     * @param unused Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        int position = allTasksList.indexOf(task);
                        if (position != -1) {
                            tasksAdapter.notifyItemChanged(position);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called if the update fails.
                     * @param e The exception that occurred.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utilities.showToast(getContext(), "Failed to update task completion", Utilities.ToastType.ERROR);
                        Log.e("TasksFragment", "Failed to update task completion: " + e.getMessage());
                    }
                });
    }

    /**
     * Fetches tasks from Firestore.
     * Outdated and completed tasks are deleted.
     * All pending tasks are displayed initially.
     */
    private void fetchTasksFromFirestore() {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Called when tasks are successfully fetched from Firestore.
                     * @param queryDocumentSnapshots The snapshots of the fetched documents.
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        allTasksList.clear();
                        filteredTasksList.clear();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Task task = doc.toObject(Task.class);
                            if (task != null) {
                                task.setId(doc.getId());

                                // Delete outdated and completed tasks
                                if (task.isOutdated() && task.isCompleted()) {
                                    deleteTaskFromFirestore(task);
                                } else {
                                    allTasksList.add(task);
                                    if (!task.isCompleted()) {
                                        filteredTasksList.add(task);
                                    }
                                }
                            }
                        }

                        binding.resetButton.setVisibility(View.GONE);
                        binding.taskHeader.setText(allTasksList.isEmpty() ? "No tasks" : "All Pending Tasks");
                        tasksAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called if fetching tasks fails.
                     * @param e The exception that caused the failure.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utilities.showToast(getContext(), "Failed to fetch tasks.", Utilities.ToastType.ERROR);
                        Log.e("TasksFragment", "Failed to fetch tasks: " + e.getMessage());
                    }
                });
    }

    /**
     * Filters the tasks by the selected date. Outdated completed tasks are deleted.
     * Updates the UI to show tasks for the selected date.
     *
     * @param selectedDateMillis The date selected by the user, in milliseconds.
     */
    private void filterTasksByDate(long selectedDateMillis) {
        SimpleDateFormat calendarFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedSelectedDate = calendarFormatter.format(new Date(selectedDateMillis));

        filteredTasksList.clear();

        Iterator<Task> iterator = allTasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            try {
                String taskCompletionDate = task.getCompletionDate();
                if (task.isOutdated() && task.isCompleted()) {
                    deleteTaskFromFirestore(task);
                    iterator.remove();
                } else if (taskCompletionDate.equals(formattedSelectedDate)) {
                    filteredTasksList.add(task);
                }
            } catch (Exception e) {
                Utilities.showToast(getContext(), "Error processing task date.", Utilities.ToastType.ERROR);
                Log.e("DEBUG", "Error processing task date: " + e.getMessage());
            }
        }

        binding.resetButton.setVisibility(View.VISIBLE);

        if (filteredTasksList.isEmpty()) {
            binding.taskHeader.setText(String.format("No tasks for %s", formattedSelectedDate));
        } else {
            binding.taskHeader.setText(String.format("Tasks for %s", formattedSelectedDate));
        }

        tasksAdapter.notifyDataSetChanged();
    }

    /**
     * Shows all non-completed tasks. Deletes outdated completed tasks first, then updates the UI.
     */
    private void showAllNonCompletedTasks() {
        filteredTasksList.clear();

        Iterator<Task> iterator = allTasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.isOutdated() && task.isCompleted()) {
                deleteTaskFromFirestore(task);
                iterator.remove();
            } else if (!task.isCompleted()) {
                filteredTasksList.add(task);
            }
        }

        binding.taskHeader.setText(allTasksList.isEmpty() ? "No task" : "All Pending Tasks");
        binding.resetButton.setVisibility(View.GONE);
        tasksAdapter.notifyDataSetChanged();
    }

    /**
     * Deletes a task from Firestore and updates the local lists and RecyclerView.
     * @param task The task to delete.
     */
    private void deleteTaskFromFirestore(final Task task) {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .document(task.getId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when the task is successfully deleted from Firestore.
                     * @param unused Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        int position = allTasksList.indexOf(task);
                        if (position != -1) {
                            allTasksList.remove(task);
                            filteredTasksList.remove(task);
                            tasksAdapter.notifyItemRemoved(position);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called if the deletion fails.
                     * @param e The exception that occurred.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Utilities.showToast(getContext(), "Failed to delete task", Utilities.ToastType.ERROR);
                        Log.e("TasksFragment", "Failed to delete task: " + e.getMessage());
                    }
                });
    }

    /**
     * Navigates to TaskEditorActivity for adding a new task or editing an existing one.
     * @param task The task to edit, or null if creating a new task.
     */
    private void navigateToTaskEditorActivity(Task task) {
        Intent intent = new Intent(getContext(), TaskEditorActivity.class);
        if (task != null) {
            intent.putExtra("Task", task);
        }
        startActivity(intent);
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