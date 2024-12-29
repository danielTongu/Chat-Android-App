package com.example.chatandroidapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.TaskEditorActivity;
import com.example.chatandroidapp.adapters.TaskAdapter;
import com.example.chatandroidapp.databinding.FragmentTasksBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * TasksFragment manages task display, filtering, and navigation to TaskWriterActivity for adding or editing tasks.
 */
public class TasksFragment extends Fragment {

    private FragmentTasksBinding binding; // View Binding for the fragment layout
    private TaskAdapter taskAdapter; // Adapter for the RecyclerView
    private List<Task> allTasks; // Full list of tasks from Firestore
    private List<Task> taskList; // Filtered or displayed list of tasks
    private PreferenceManager preferenceManager; // PreferenceManager for user data
    private FirebaseFirestore db; // Firestore database instance

    /**
     * Initializes the fragment's UI and data when the view is created.
     *
     * @param inflater           The LayoutInflater used to inflate the fragment layout.
     * @param container          The parent container for the fragment.
     * @param savedInstanceState The previously saved state of the fragment.
     * @return The root view of the fragment layout.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        preferenceManager = PreferenceManager.getInstance(requireContext());
        db = FirebaseFirestore.getInstance();

        setupRecyclerView();
        fetchTasksFromFirestore();
        deleteOutdatedTasks();
        setupListeners();

        return binding.getRoot();
    }

    /**
     * Sets up the RecyclerView with a LinearLayoutManager and TaskAdapter.
     */
    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        allTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.TaskAdapterListener() {
            @Override
            public void onTaskCompletedChanged(Task task) {
                task.isCompleted = !task.isCompleted;
                db.collection("Users")
                        .document(preferenceManager.getString(Constants.KEY_ID, ""))
                        .collection("Tasks")
                        .document(task.id)
                        .update("completed", task.isCompleted);
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onEditTask(Task task) {
                navigateToTaskWriter(task); // Navigate to TaskWriterActivity for editing
            }

            @Override
            public void onDeleteTask(Task task) {
                deleteTaskFromFirestore(task);
            }
        });

        binding.recyclerViewTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewTasks.setAdapter(taskAdapter);
    }

    /**
     * Sets up listeners for UI elements like the calendar and Floating Action Button.
     */
    private void setupListeners() {
        // Handle calendar date selection
        binding.tasksCoordinator.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = (month + 1) + "/" + dayOfMonth + "/" + year;
            binding.textTitleRecyclerView.setText("Tasks for Selected Date");
            filterTasksByDate(selectedDate);
        });

        // Reset button to show all non-completed tasks
        binding.buttonAllPendingTasks.setOnClickListener(v -> showAllNonCompletedTasks());

        // Floating Action Button to add a new task
        binding.buttonAddTask.setOnClickListener(v -> navigateToTaskWriter(null)); // Navigate to TaskWriterActivity for adding a task
    }

    /**
     * Filters tasks by the selected date and updates the RecyclerView.
     *
     * @param date The selected date in MM/dd/yyyy format.
     */
    private void filterTasksByDate(String date) {
        taskList.clear();
        for (Task task : allTasks) {
            if (task.completionDate.equals(date)) {
                taskList.add(task);
            }
        }

        if (taskList.isEmpty()) {
            binding.buttonAllPendingTasks.setVisibility(View.GONE);
            binding.textTitleRecyclerView.setText("No tasks for selected date");
        } else {
            binding.buttonAllPendingTasks.setVisibility(View.VISIBLE);
        }

        taskAdapter.notifyDataSetChanged();
    }

    /**
     * Displays all non-completed tasks and resets the UI.
     */
    private void showAllNonCompletedTasks() {
        taskList.clear();
        for (Task task : allTasks) {
            if (!task.isCompleted) {
                taskList.add(task);
            }
        }
        binding.textTitleRecyclerView.setText("To do");
        binding.buttonAllPendingTasks.setVisibility(View.GONE);
        taskAdapter.notifyDataSetChanged();
    }

    /**
     * Fetches tasks from Firestore and updates the task lists.
     */
    private void fetchTasksFromFirestore() {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTasks.clear();
                    taskList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null) {
                            allTasks.add(task);
                            if (!task.isCompleted) {
                                taskList.add(task);
                            }
                        }
                    }
                    taskAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Deletes tasks that are completed and older than 90 days.
     */
    private void deleteOutdatedTasks() {
        db.collection("Users")
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .collection("Tasks")
                .whereEqualTo("completed", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Task task = doc.toObject(Task.class);
                        if (task != null && task.isOutdated()) {
                            db.collection("Users")
                                    .document(preferenceManager.getString(Constants.KEY_ID, ""))
                                    .collection("Tasks")
                                    .document(task.id)
                                    .delete();
                        }
                    }
                });
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
                .document(task.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    allTasks.remove(task);
                    taskList.remove(task);
                    taskAdapter.notifyDataSetChanged();
                });
    }

    /**
     * Navigates to TaskWriterActivity for adding or editing a task.
     *
     * @param task The task to edit, or null if adding a new task.
     */
    private void navigateToTaskWriter(Task task) {
        Intent intent = new Intent(getContext(), TaskEditorActivity.class);
        if (task != null) {
            intent.putExtra("TASK", task); // Pass the task if editing
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