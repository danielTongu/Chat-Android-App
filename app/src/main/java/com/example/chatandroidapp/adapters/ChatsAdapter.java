package com.example.chatandroidapp.adapters;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.models.Chat;
import com.example.chatandroidapp.models.Message;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying chats in a RecyclerView.
 * Ensures only valid data is displayed and prevents memory leaks.
 */
public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private static final String TAG = "CHATS_ADAPTER";

    private final List<Chat> chats;
    private final ChatClickListener listener;
    private final FirebaseFirestore firestore;
    private final List<ListenerRegistration> listenerRegistrations;

    /**
     * Interface for handling chat item clicks.
     */
    public interface ChatClickListener {
        void onChatClicked(Chat chat);
    }

    /**
     * Constructor for ChatsAdapter.
     *
     * @param chats    List of Chat objects to display.
     * @param listener Listener for handling chat item clicks.
     * @param listenerRegistrations List to track Firestore listeners for cleanup.
     */
    public ChatsAdapter(List<Chat> chats, ChatClickListener listener, List<ListenerRegistration> listenerRegistrations) {
        this.chats = chats;
        this.listener = listener;
        this.firestore = FirebaseFirestore.getInstance();
        this.listenerRegistrations = listenerRegistrations;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);
        holder.bind(chat);
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    /**
     * ViewHolder for displaying individual chat items.
     */
    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderName;
        private final TextView recentMessage;
        private final TextView timestamp;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.recentSenderNameOrPhoneOrEmail);
            recentMessage = itemView.findViewById(R.id.recentMessage);
            timestamp = itemView.findViewById(R.id.recentMessageTimestamp);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onChatClicked(chats.get(position));
                    }
                }
            });
        }

        /**
         * Binds chat data to the view, including recent message and sender details.
         * Only displays valid chats with complete data.
         *
         * @param chat Chat object containing the data.
         */
        public void bind(Chat chat) {
            if (TextUtils.isEmpty(chat.recentMessageId)) {
                Log.d(TAG, "Skipping chat with no recent message ID: " + chat.id);
                return;
            }

            fetchRecentMessage(chat);
        }

        /**
         * Fetches the recent message and its sender's details from Firestore.
         *
         * @param chat Chat object containing the recentMessageId.
         */
        private void fetchRecentMessage(Chat chat) {
            ListenerRegistration messageListener = firestore.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chat.id)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(chat.recentMessageId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            Log.e(TAG, "Skipping chat with invalid recent message for chat ID: " + chat.id, error);
                            return;
                        }

                        Message recentMessage = snapshot.toObject(Message.class);
                        if (recentMessage != null) {
                            fetchSenderDetails(recentMessage);
                        }
                    });

            listenerRegistrations.add(messageListener);
        }

        /**
         * Fetches the sender's details from Firestore.
         *
         * @param recentMessage Message object containing the senderId.
         */
        private void fetchSenderDetails(Message recentMessage) {
            ListenerRegistration senderListener = firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .document(recentMessage.senderId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null || !snapshot.exists()) {
                            Log.e(TAG, "Skipping chat with invalid sender for sender ID: " + recentMessage.senderId, error);
                            return;
                        }

                        User sender = snapshot.toObject(User.class);
                        if (sender != null) {
                            bindMessageToView(recentMessage, sender);
                        }
                    });

            listenerRegistrations.add(senderListener);
        }

        /**
         * Binds the recent message and sender details to the view.
         *
         * @param message Message object containing recent message details.
         * @param sender  User object representing the sender.
         */
        private void bindMessageToView(Message message, User sender) {
            recentMessage.setText(message.content);
            timestamp.setText(formatTimestamp(message.sentDate));
            senderName.setText(getDisplayName(sender));
        }

        /**
         * Formats a Date object into a readable timestamp.
         *
         * @param date Date object to format.
         * @return Formatted date string.
         */
        private String formatTimestamp(Date date) {
            if (date == null) return "";
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
        }

        /**
         * Returns a display name for the sender.
         *
         * @param sender User object of the sender.
         * @return Display name or fallback text.
         */
        private String getDisplayName(User sender) {
            if (!TextUtils.isEmpty(sender.firstName) && !TextUtils.isEmpty(sender.lastName)) {
                return sender.firstName + " " + sender.lastName;
            } else if (!TextUtils.isEmpty(sender.email)) {
                return sender.email;
            } else {
                return sender.phone != null ? sender.phone : "Unknown Sender";
            }
        }
    }

    /**
     * Removes all Firestore listeners to prevent memory leaks.
     */
    public void cleanupListeners() {
        for (ListenerRegistration listener : listenerRegistrations) {
            listener.remove();
        }
        listenerRegistrations.clear();
    }
}