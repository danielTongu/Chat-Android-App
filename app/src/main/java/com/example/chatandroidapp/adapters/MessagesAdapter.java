// MessagesAdapter.java
package com.example.chatandroidapp.adapters;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivityChatRecyclerItemBinding;
import com.example.chatandroidapp.module.Message;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.module.Chat;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for displaying chat messages in the RecyclerView.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.BaseViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUserId;
    private final FirebaseFirestore firestore;
    private final ConcurrentHashMap<String, User> userCache; // Cache to store fetched users

    /**
     * Constructor for MessagesAdapter.
     *
     * @param messages List of chat messages to display.
     * @param context  Context for accessing resources and preferences.
     */
    public MessagesAdapter(List<Message> messages, android.content.Context context) {
        this.messages = messages;
        PreferenceManager preferenceManager = PreferenceManager.getInstance(context);
        this.currentUserId = preferenceManager.getString(Constants.KEY_ID);
        this.firestore = FirebaseFirestore.getInstance();
        this.userCache = new ConcurrentHashMap<>();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.senderId.equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ActivityChatRecyclerItemBinding binding = ActivityChatRecyclerItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(binding);
        } else {
            return new ReceivedMessageViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Abstract base ViewHolder for shared functionality.
     */
    abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        final ActivityChatRecyclerItemBinding binding;

        BaseViewHolder(ActivityChatRecyclerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        abstract void bind(Message message);
    }

    /**
     * ViewHolder for sent messages.
     */
    class SentMessageViewHolder extends BaseViewHolder {

        SentMessageViewHolder(ActivityChatRecyclerItemBinding binding) {
            super(binding);
        }

        @Override
        void bind(Message message) {
            binding.sentMessageLayout.setVisibility(View.VISIBLE);
            binding.receivedMessageLayout.setVisibility(View.GONE);
            binding.sentMessage.setText(message.content);
            binding.sentTimestamp.setText(formatDate(message.sentDate));
        }
    }

    /**
     * ViewHolder for received messages.
     */
    class ReceivedMessageViewHolder extends BaseViewHolder {

        private final RoundedImageView senderImage;
        private final TextView senderName;
        private final TextView receivedMessage;
        private final TextView receivedTimestamp;

        ReceivedMessageViewHolder(ActivityChatRecyclerItemBinding binding) {
            super(binding);
            senderImage = binding.receivedProfilePicture;
            senderName = binding.receivedNameOrPhoneOrEmail;
            receivedMessage = binding.receivedMessage;
            receivedTimestamp = binding.receivedTimestamp;
        }

        @Override
        void bind(Message message) {
            binding.receivedMessageLayout.setVisibility(View.VISIBLE);
            binding.sentMessageLayout.setVisibility(View.GONE);
            receivedMessage.setText(message.content);
            receivedTimestamp.setText(formatDate(message.sentDate));

            // Fetch and display sender's details
            String senderId = message.senderId;
            if (userCache.containsKey(senderId)) {
                User sender = userCache.get(senderId);
                bindUserData(sender);
            } else {
                // Fetch sender's User data from Firestore
                firestore.collection(Constants.KEY_COLLECTION_USERS)
                        .document(senderId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User sender = documentSnapshot.toObject(User.class);
                                if (sender != null) {
                                    userCache.put(senderId, sender); // Cache the user
                                    bindUserData(sender);
                                } else {
                                    setDefaultUserData();
                                }
                            } else {
                                setDefaultUserData();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MessagesAdapter", "Failed to fetch user data for senderId: " + senderId, e);
                            setDefaultUserData();
                        });
            }
        }

        /**
         * Binds the fetched User data to the UI components.
         *
         * @param sender The User object representing the sender.
         */
        private void bindUserData(User sender) {
            // Determine sender's display name (FirstName + LastName > Email > Phone)
            String displayName;
            if (!TextUtils.isEmpty(sender.firstName) && !TextUtils.isEmpty(sender.lastName)) {
                displayName = sender.firstName + " " + sender.lastName;
            } else if (!TextUtils.isEmpty(sender.email)) {
                displayName = sender.email;
            } else {
                displayName = sender.phone;
            }
            senderName.setText(displayName);

            // Set sender's profile picture if available
            if (!TextUtils.isEmpty(sender.image)) {
                Bitmap senderImageBitmap = User.getBitmapFromEncodedString(sender.image);
                if (senderImageBitmap != null) {
                    senderImage.setImageBitmap(senderImageBitmap);
                } else {
                    senderImage.setImageResource(R.drawable.ic_profile); // Default icon
                }
            } else {
                senderImage.setImageResource(R.drawable.ic_profile); // Default icon
            }
        }

        /**
         * Sets default user data when sender's details are unavailable.
         */
        private void setDefaultUserData() {
            senderName.setText("Unknown Sender");
            senderImage.setImageResource(R.drawable.ic_profile); // Default icon
        }
    }

    /**
     * Formats a Date object into a readable string.
     *
     * @param date The Date object to format.
     * @return A formatted string.
     */
    private static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
    }
}