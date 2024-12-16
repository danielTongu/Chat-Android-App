package com.example.chatandroidapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.adapters.TaskAdapter;
import com.example.chatandroidapp.databinding.FragmentTaskBinding;
import com.example.chatandroidapp.models.Task;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * TaskFragment handles task management, including filtering, displaying, and deleting outdated tasks.
 */
public class TaskFragment extends Fragment {
    private FragmentTaskBinding binding; // View Binding
    private TaskAdapter taskAdapter; // Adapter for the RecyclerView
    private List<Task> allTasks; // All tasks
    private List<Task> taskList; // Filtered or displayed tasks
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db; // Firestore instance

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        preferenceManager = PreferenceManager.getInstance(requireContext());

        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        setupRecyclerView();
        fetchTasksFromFirestore();
        deleteOutdatedTasks(); // Trigger cleanup process
        setupListeners();
        return binding.getRoot();
    }

    /**
     * Sets up the RecyclerView with a linear layout and adapter.
     */
    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        allTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.TaskAdapterListener() {
            @Override
            public void onTaskCompletedChanged(Task task) {
                task.setCompleted(!task.isCompleted());
                db.collection("Users")
                        .document(preferenceManager.getString(Constants.KEY_ID, ""))
                        .collection("Tasks")
                        .document(task.getId())
                        .update("completed", task.isCompleted());
                taskAdapter.notifyDataSetChanged();
            }

            @Override
            public void onEditTask(Task task) {
                Toast.makeText(getContext(), "Edit task: " + task.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteTask(Task task) {
                db.collection("Users")
                        .document(preferenceManager.getString(Constants.KEY_ID, ""))
                        .collection("Tasks")
                        .document(task.getId())
                        .delete()
                        .addOnSuccessListener(unused -> {
                            allTasks.remove(task);
                            taskList.remove(task);
                            taskAdapter.notifyDataSetChanged();
                        });
            }
        });

        binding.tasksRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.tasksRecyclerView.setAdapter(taskAdapter);
    }

    /**
     * Sets up listeners for the calendar and reset button.
     */
    private void setupListeners() {
        // Handle calendar date selection
        binding.tasksCoordinator.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String selectedDate = (month + 1) + "/" + dayOfMonth + "/" + year;
            binding.taskHeader.setText("Tasks for Selected Date");
            filterTasksByDate(selectedDate);
        });

        // Reset button to show all non-completed tasks
        binding.resetButton.setOnClickListener(v -> {
            showAllNonCompletedTasks();
        });
    }

    /**
     * Displays all non-completed tasks and resets the UI.
     */
    private void showAllNonCompletedTasks() {
        taskList.clear();
        for (Task task : allTasks) {
            if (!task.isCompleted()) {
                taskList.add(task);
            }
        }
        binding.taskHeader.setText("To do");
        binding.resetButton.setVisibility(View.GONE);
        taskAdapter.notifyDataSetChanged();
    }

    /**
     * Filters tasks by the selected date.
     *
     * @param date The selected date in MM/dd/yyyy format.
     */
    private void filterTasksByDate(String date) {
        taskList.clear();
        for (Task task : allTasks) {
            if (task.getCompletionDate().equals(date)) {
                taskList.add(task);
            }
        }

        if (taskList.isEmpty()) {
            Toast.makeText(getContext(), "No tasks found for the selected date.", Toast.LENGTH_SHORT).show();
        } else {
            binding.resetButton.setVisibility(View.VISIBLE);
        }
        taskAdapter.notifyDataSetChanged();
    }

    /**
     * Fetches tasks from Firestore.
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
                            task.setId(doc.getId());
                            allTasks.add(task);
                            if (!task.isCompleted()) {
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
                        if (task != null && isTaskOutdated(task.createdDate)) {
                            db.collection("Users")
                                    .document(preferenceManager.getString(Constants.KEY_ID, ""))
                                    .collection("Tasks")
                                    .document(doc.getId())
                                    .delete();
                        }
                    }
                });
    }

    /**
     * Checks if a task is older than 90 days.
     *
     * @param createdDate The date the task was created (in MM/dd/yyyy format).
     * @return True if the task is older than 90 days, false otherwise.
     */
    private boolean isTaskOutdated(String createdDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        try {
            Date taskDate = sdf.parse(createdDate);
            if (taskDate != null) {
                long diffInMillis = System.currentTimeMillis() - taskDate.getTime();
                long daysDiff = diffInMillis / (1000 * 60 * 60 * 24);
                return daysDiff > 90;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}