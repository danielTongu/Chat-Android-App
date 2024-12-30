package com.example.chatandroidapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.databinding.ItemTaskBinding;
import com.example.chatandroidapp.models.Task;

import java.util.List;

/**
 * TasksAdapter handles displaying tasks and managing user interactions like
 * marking tasks as completed, editing, and deleting.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private final List<Task> tasks; // List of tasks to display
    private final TaskAdapterListener listener; // Callback listener for interactions

    /**
     * Constructor for TasksAdapter.
     *
     * @param tasks    The list of tasks to display.
     * @param listener The listener for task interactions.
     */
    public TaskAdapter(List<Task> tasks, TaskAdapterListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskBinding binding = ItemTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }


    /**
     * Interface for task interaction callbacks.
     */
    public interface TaskAdapterListener {
        /**
         * Called when a task's completion status is toggled.
         *
         * @param task The task whose completion status changed.
         */
        void onTaskCompletedChanged(Task task);

        /**
         * Called when a task is to be edited.
         *
         * @param task The task to edit.
         */
        void onEditTask(Task task);

        /**
         * Called when a task is to be deleted.
         *
         * @param task The task to delete.
         */
        void onDeleteTask(Task task);
    }


    /**
     * ViewHolder class for individual task items.
     */
    public class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        public TaskViewHolder(@NonNull ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a task object to the view.
         *
         * @param task The task to display.
         */
        public void bind(final Task task) {
            // Bind task data to UI components
            binding.taskTitle.setText(task.title);
            binding.taskDate.setText(task.completionDate);
            binding.taskTime.setText(task.completionTime);
            binding.taskDescription.setText(task.description);

            // Temporarily disable the listener while updating the state, then enable it
            binding.taskCompleted.setOnCheckedChangeListener(null); // Prevent unwanted callbacks during recycling
            binding.taskCompleted.setChecked(task.isCompleted);// Set the checkbox state
            binding.taskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onTaskCompletedChanged(task);
            });

            // Handle edit button click
            binding.editButton.setOnClickListener(v -> listener.onEditTask(task));

            // Handle delete button click
            binding.deleteButton.setOnClickListener(v -> listener.onDeleteTask(task));
        }
    }
}