package com.example.chatandroidapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.databinding.FragmentTaskRecyclerItemBinding;
import com.example.chatandroidapp.models.Task;

import java.util.List;

/**
 * TaskAdapter handles the display of tasks in a RecyclerView and manages interactions like
 * marking tasks as completed, editing, and deleting.
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks; // List of tasks to display
    private final TaskAdapterListener listener; // Callback listener for interactions

    /**
     * Constructor for TaskAdapter.
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
        FragmentTaskRecyclerItemBinding binding = FragmentTaskRecyclerItemBinding.inflate(
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
     * ViewHolder class for individual task items.
     */
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final FragmentTaskRecyclerItemBinding binding;

        public TaskViewHolder(@NonNull FragmentTaskRecyclerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a task object to the view.
         * @param task The task to display.
         */
        public void bind(final Task task) {
            // Bind task data to UI components
            binding.taskTitle.setText(task.getTitle());
            binding.taskDate.setText(task.getCompletionDate());
            binding.taskTime.setText(task.getCompletionTime());
            binding.taskDescription.setText(task.getDescription());
            binding.taskCompleted.setChecked(task.isCompleted());

            // Handle checkbox toggle for task completion
            binding.taskCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                listener.onTaskCompletedChanged(task);
            });

            // Handle edit button click
            binding.editButton.setOnClickListener(v -> listener.onEditTask(task));

            // Handle delete button click
            binding.deleteButton.setOnClickListener(v -> listener.onDeleteTask(task));
        }
    }

    /**
     * Interface for task interaction callbacks.
     */
    public interface TaskAdapterListener {
        /**
         * Called when a task's completion status is toggled.
         * @param task The task whose completion status changed.
         */
        void onTaskCompletedChanged(Task task);

        /**
         * Called when a task is to be edited.
         * @param task The task to edit.
         */
        void onEditTask(Task task);

        /**
         * Called when a task is to be deleted.
         * @param task The task to delete.
         */
        void onDeleteTask(Task task);
    }
}