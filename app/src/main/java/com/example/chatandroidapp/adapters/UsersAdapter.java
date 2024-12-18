// UsersAdapter.java
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
 * UsersAdapter handles the display of users in the RecyclerView, allowing selection for initiating a chat.
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private final List<User> usersList;
    private final OnUserSelectedListener listener;

    /**
     * Constructor for UserAdapter.
     *
     * @param usersList    The list of users to display.
     * @param listener The listener for user interactions.
     */
    public UsersAdapter(List<User> usersList, OnUserSelectedListener listener) {
        this.usersList = usersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }


    /**
     * Interface to handle user selection callbacks.
     */
    public interface OnUserSelectedListener {
        void onUserSelected(User user, boolean selected);
    }


    /**
     * ViewHolder class for user items.
     */
     public class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView userImage;
        TextView userName;
        TextView userContactInfo;
        CheckBox checkBox;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userProfilePicture);
            userName = itemView.findViewById(R.id.userFirstAndLastName);
            userContactInfo = itemView.findViewById(R.id.userPhoneNumberOrEmail);
            checkBox = itemView.findViewById(R.id.userCheckBox);
        }

        /**
         * Binds a user object to the view.
         * @param user The user to display.
         */
        public void bind(final User user) {
            userName.setText(String.format("%s %s", user.firstName, user.lastName));
            userImage.setImageResource(R.drawable.ic_profile); // Fallback image

            // Load user profile image if available
            if (user.image != null && !user.image.isEmpty()) {
                Bitmap userBitmap = User.getBitmapFromEncodedString(user.image);
                if (userBitmap != null) {
                    userImage.setImageBitmap(userBitmap);
                }
            }

            // Set user phone or email
            userContactInfo.setText( (user.phone != null && !user.phone.isEmpty()) ? user.phone: user.email);


            // Handle checkbox state
            checkBox.setOnCheckedChangeListener(null); // Prevent unwanted callbacks during recycling
            checkBox.setChecked(false);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onUserSelected(user, isChecked);
            });
        }
    }
}