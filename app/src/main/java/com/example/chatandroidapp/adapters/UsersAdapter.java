package com.example.chatandroidapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.models.User;

import java.util.List;

/**
 * Adapter for displaying a list of users in a RecyclerView.
 * Each user can be selected for initiating a chat.
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private final List<User> usersList;
    private final OnUserSelectedListener listener;

    /**
     * Constructor to initialize the adapter with a list of users and a selection listener.
     *
     * @param usersList List of users to be displayed.
     * @param listener  Listener for handling user selection events.
     */
    public UsersAdapter(List<User> usersList, OnUserSelectedListener listener) {
        this.usersList = usersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item view for each user
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * Binds the user data to the specified ViewHolder.
     *
     * @param holder   The ViewHolder to bind the data to.
     * @param position The position of the user in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Bind user data and set up click listener
        holder.bind(usersList.get(position), listener);
    }

    /**
     * Returns the total number of users in the list.
     *
     * @return Number of users.
     */
    @Override
    public int getItemCount() {
        return usersList.size();
    }

    /**
     * Listener interface for handling user selection events.
     */
    public interface OnUserSelectedListener {
        /**
         * Called when a user is selected or deselected.
         *
         * @param user     The user that was selected or deselected.
         * @param selected True if the user was selected, false otherwise.
         */
        void onUserSelected(User user, boolean selected);
    }

    /**
     * ViewHolder class for managing the views of each user item.
     */
    public static class UserViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView name;
        TextView contact;
        CheckBox checkBox;

        /**
         * Constructs a UserViewHolder and initializes the views.
         *
         * @param itemView The view representing an individual user item.
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.userImage);
            name = itemView.findViewById(R.id.userName);
            contact = itemView.findViewById(R.id.userContact);
            checkBox = itemView.findViewById(R.id.userIsSelected);
        }

        /**
         * Binds user data to the views and sets up the checkbox listener.
         *
         * @param user     The user data to display.
         * @param listener The listener for user selection events.
         */
        private void bind(User user, OnUserSelectedListener listener) {
            // Determine the user's full name or fallback to "unknown user"
            String userName;
            if (user.firstName != null && !user.firstName.isEmpty() && user.lastName != null && !user.lastName.isEmpty()) {
                userName = String.format("%s %s", user.firstName, user.lastName);
            } else if (user.firstName != null && !user.firstName.isEmpty()) {
                userName = user.firstName;
            } else if (user.lastName != null && !user.lastName.isEmpty()) {
                userName = user.lastName;
            } else {
                userName = "unknown user";
            }
            name.setText(userName);

            // Load user profile image or fallback to a default image
            image.setImageResource(R.drawable.ic_profile);
            if (user.image != null && !user.image.isEmpty()) {
                Bitmap userBitmap = User.getBitmapFromEncodedString(user.image);
                if (userBitmap != null) {
                    image.setImageBitmap(userBitmap);
                }
            }

            // Display the user's phone number or email, or fallback if unavailable
            if (user.phone != null && !user.phone.isEmpty()) {
                contact.setText(user.phone);
            } else if (user.email != null && !user.email.isEmpty()) {
                contact.setText(user.email);
            } else {
                contact.setText("No contact info available");
            }

            // Manage checkbox state and listener
            checkBox.setOnCheckedChangeListener(null); // Prevent callbacks during recycling
            checkBox.setChecked(false);
            checkBox.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> listener.onUserSelected(user, isChecked)
            );
        }
    }
}