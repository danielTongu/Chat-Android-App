package com.example.chatandroidapp.fragments;

import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.TaskWriterActivity;
import com.example.chatandroidapp.adapters.TasksAdapter;
import com.example.chatandroidapp.databinding.FragmentTasksBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * TasksFragment manages task display, filtering, and navigation to TaskWriterActivity for adding or editing tasks.
 */
public class TasksFragment extends Fragment {
    private FragmentTasksBinding binding; // View Binding for the fragment layout
    private TasksAdapter tasksAdapter; // Adapter for the RecyclerView
    private List<Task> allTasksList; // Full list of tasks from Firestore
    private List<Task> filteredTasksList; // Filtered or displayed list of tasks
    private PreferenceManager preferenceManager; // PreferenceManager for user data
    private FirebaseFirestore db; // Firestore database instance

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
     * Sets up the RecyclerView with a LinearLayoutManager and TasksAdapter.
     */
    private void setupRecyclerView() {
        filteredTasksList = new ArrayList<>();
        allTasksList = new ArrayList<>();
        tasksAdapter = new TasksAdapter(filteredTasksList, new TasksAdapter.TaskAdapterListener() {
            @Override
            public void onTaskCompletedChanged(Task task) { toggleTaskCompletion(task); }

            @Override
            public void onEditTask(Task task) { navigateToTaskWriter(task); }

            @Override
            public void onDeleteTask(Task task) { deleteTaskFromFirestore(task); }
        });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tasksRecyclerView.setAdapter(tasksAdapter);
    }

    /**
     * Sets up listeners for UI elements like the calendar and Floating Action Button.
     */
    private void setupListeners() {
        binding.tasksCoordinator.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // CalendarView provides the selected date as year, month, day
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(year, month, dayOfMonth);
            long selectedDateMillis = selectedCalendar.getTimeInMillis();

            // Filter tasks by the selected date
            filterTasksByDate(selectedDateMillis);
        });

        binding.resetButton.setOnClickListener(v -> showAllNonCompletedTasks());
        binding.fabAddTask.setOnClickListener(v -> navigateToTaskWriter(null));
    }

    /**
     * Marks a task as completed or not completed and updates it in Firestore.
     * @param task The task to toggle completion status for.
     */
    private void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());

        // Update Firestore
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .document(task.getId())
                .update("completed", task.isCompleted());

        // Find the position of the updated task
        int position = allTasksList.indexOf(task);
        if (position != -1) {
            tasksAdapter.notifyItemChanged(position);
        }
    }

    /**
     * Fetches tasks from Firestore, deletes outdated tasks, and updates the task lists.
     */
    private void fetchTasksFromFirestore() {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTasksList.clear();
                    filteredTasksList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            task.setId(doc.getId());

                            // Check if the task is outdated and delete it
                            if (task.isOutdated() && task.isCompleted()) {
                                deleteTaskFromFirestore(task);
                            } else {
                                allTasksList.add(task);
                                if (!task.isCompleted()) { filteredTasksList.add(task); }
                            }
                        }
                    }

                    binding.resetButton.setVisibility(View.GONE);
                    binding.taskHeader.setText(allTasksList.isEmpty() ? "No tasks" : "All Pending Tasks");
                    tasksAdapter.notifyItemRangeInserted(0, filteredTasksList.size());
                });
    }

    /**
     * Filters tasks by the selected date, deletes outdated tasks, and updates the RecyclerView.
     *
     * @param selectedDateMillis The selected date from the CalendarView in milliseconds.
     */
    private void filterTasksByDate(long selectedDateMillis) {
        // Format the selected date from CalendarView
        SimpleDateFormat calendarFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedSelectedDate = calendarFormatter.format(new Date(selectedDateMillis));

        filteredTasksList.clear();

        Iterator<Task> iterator = allTasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            try {
                String taskCompletionDate = task.getCompletionDate();

                if (task.isOutdated() && task.isCompleted()) {
                    deleteTaskFromFirestore(task); // Delete from db
                    iterator.remove(); // Safely remove from local list
                } else if (taskCompletionDate.equals(formattedSelectedDate)) {
                    filteredTasksList.add(task); // Add matching task to filtered list
                }
            } catch (Exception e) {
                Log.e("DEBUG", "Error processing task date: " + e.getMessage());
            }
        }

        // Update UI
        binding.resetButton.setVisibility(View.VISIBLE);

        if (filteredTasksList.isEmpty()) {
            binding.taskHeader.setText(String.format("No tasks for %s", formattedSelectedDate));
        } else {
            binding.taskHeader.setText(String.format("Tasks for %s", formattedSelectedDate));
        }

        // Notify adapter after clearing and re-populating the list
        tasksAdapter.notifyDataSetChanged();
    }

    /**
     * Displays all non-completed tasks, deletes outdated tasks, and resets the UI.
     */
    private void showAllNonCompletedTasks() {
        filteredTasksList.clear();

        // Use an Iterator to safely remove outdated tasks while iterating
        Iterator<Task> iterator = allTasksList.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();

            if (task.isOutdated() && task.isCompleted()) {
                deleteTaskFromFirestore(task); // Delete task from Firestore and local list
                iterator.remove(); // Safe removal from the local list
            } else if (!task.isCompleted()) {
                filteredTasksList.add(task);
            }
        }

        // Update UI header
        binding.taskHeader.setText(allTasksList.isEmpty() ? "No task" : "All Pending Tasks");
        binding.resetButton.setVisibility(View.GONE);

        // Notify adapter for new range after clearing
        tasksAdapter.notifyDataSetChanged();
    }

    /**
     * Deletes a task from Firestore and updates the UI.
     *
     * @param task The task to delete.
     */
    private void deleteTaskFromFirestore(Task task) {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .document(task.getId())
                .delete()
                .addOnSuccessListener(unused -> {
                    int position = allTasksList.indexOf(task);
                    if (position != -1) {
                        allTasksList.remove(task);
                        filteredTasksList.remove(task);
                        tasksAdapter.notifyItemRemoved(position);
                    }
                });
    }

    /**
     * Navigates to TaskWriterActivity for adding or editing a task.
     *
     * @param task The task to edit, or null if adding a new task.
     */
    private void navigateToTaskWriter(Task task) {
        Intent intent = new Intent(getContext(), TaskWriterActivity.class);
        if (task != null) {
            intent.putExtra("Task", task); // Pass the task if editing
        }
        startActivity(intent);
    }

    /**
     * Cleans up resources when the fragment view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}