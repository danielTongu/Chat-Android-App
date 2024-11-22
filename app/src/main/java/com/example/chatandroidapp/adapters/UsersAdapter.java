package com.example.chatandroidapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ItemContainerUserBinding;
import com.example.chatandroidapp.listeners.UserListener;
import com.example.chatandroidapp.module.User;

import java.util.List;

/**
 * The UsersAdapter class is a RecyclerView adapter that binds user data to a user item layout.
 * It handles displaying a list of users and allows interaction with each user through a click listener.
 *
 * @author Daniel Tongu
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private final List<User> users; // List of users to display
    private final UserListener userListener; // Listener to handle user clicks

    /**
     * Constructor for the UsersAdapter.
     * @param users        List of User objects to display.
     * @param userListener Listener for handling user click events.
     */
    public UsersAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the user item layout
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);

        // Return a new UserViewHolder
        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));// Bind user data to the view holder
    }

    @Override
    public int getItemCount() {
        return users.size();// Return the total number of users
    }

    /**
     * Decodes a Base64-encoded string into a Bitmap image.
     * @param encodedImage The Base64-encoded image string.
     * @return A Bitmap representation of the decoded image.
     */
    private Bitmap getUserImage(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            return null; // Return null so the default image can be used
        }
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * ViewHolder class for individual user items.
     * It binds user data to the UI components in the user item layout.
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerUserBinding binding; // Binding for the user item layout

        /**
         * Constructor for UserViewHolder.
         * @param itemContainerUserBinding Binding object for the user item layout.
         */
        public UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        /**
         * Binds a User object to the UI components in the user item layout.
         * @param user The User object containing the data to display.
         */
        void setUserData(User user) {
            binding.textName.setText(String.format("%s %s", user.firstName, user.lastName));
            binding.textEmail.setText(user.email);

            if (user.image != null && !user.image.isEmpty()) {
                binding.imageProfile.setImageBitmap(getUserImage(user.image));
            } else {
                binding.imageProfile.setImageResource(R.drawable.ic_person);
            }

            binding.getRoot().setOnClickListener(v -> userListener.OnUserClicked(user));
        }
    }
}