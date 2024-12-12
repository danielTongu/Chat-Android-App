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
import com.example.chatandroidapp.module.User;

import java.util.List;

/**
 * UsersAdapter handles the display of users in the RecyclerView,
 * allowing selection for initiating a chat.
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    /**
     * Interface to handle user selection callbacks.
     */
    public interface OnUserSelectedListener {
        void onUserSelected(User user, boolean selected);
    }

    private final List<User> usersList;
    private final OnUserSelectedListener listener;

    public UsersAdapter(List<User> usersList, OnUserSelectedListener listener) {
        this.usersList = usersList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_chat_creator_recycler_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersList.get(position);
        holder.textUserName.setText(user.firstName + " " + user.lastName);

        // Load user profile image if available
        if (user.image != null && !user.image.isEmpty()) {
            // Decode the Base64 image string to Bitmap
            Bitmap userBitmap = User.getBitmapFromEncodedString(user.image);
            if (userBitmap != null) {
                holder.imageUserProfile.setImageBitmap(userBitmap);
            } else {
                holder.imageUserProfile.setImageResource(R.drawable.ic_profile); // Fallback image
            }
        } else {
            holder.imageUserProfile.setImageResource(R.drawable.ic_profile); // Fallback image
        }

        // Set user phone or email
        if (user.phone != null && !user.phone.isEmpty()) {
            holder.textUserInfo.setText(user.phone);
        } else {
            holder.textUserInfo.setText(user.email);
        }

        // Handle checkbox state
        holder.checkBox.setOnCheckedChangeListener(null); // Prevent unwanted callbacks during recycling
        holder.checkBox.setChecked(usersList.contains(user));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onUserSelected(user, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    /**
     * ViewHolder class for user items.
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserProfile;
        TextView textUserName;
        TextView textUserInfo;
        CheckBox checkBox;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageUserProfile = itemView.findViewById(R.id.userProfilePicture);
            textUserName = itemView.findViewById(R.id.userFirstAndLastName);
            textUserInfo = itemView.findViewById(R.id.userPhoneNumberOrEmail);
            checkBox = itemView.findViewById(R.id.userCheckBox);
        }
    }
}