package com.example.chatandroidapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.activities.MessagingActivity;
import com.example.chatandroidapp.databinding.ItemChatBinding;
import com.example.chatandroidapp.models.Chat;
import com.example.chatandroidapp.models.Message;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatsAdapter manages the display of chat items in the RecyclerView for ChatsFragment.
 * Each item shows the most recent message, timestamp, and sender details.
 * When a chat item is clicked, it launches the MessagingActivity with the existing chat ID.
 */
public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    /**
     * List of Chat objects to be displayed in the RecyclerView.
     */
    private final List<Chat> chatList;

    /**
     * Android Context for inflating layouts and starting activities.
     */
    private final Context context;

    /**
     * Reference to Firebase Firestore for fetching message/user data on demand.
     */
    private final FirebaseFirestore firestore;

    /**
     * In-memory cache for storing User objects to avoid repeated Firestore lookups.
     */
    private final ConcurrentHashMap<String, User> userCache;

    /**
     * For retrieving the current user ID and other preferences if needed.
     */
    private final PreferenceManager preferenceManager;

    /**
     * Constructor for ChatsAdapter.
     *
     * @param chatList The list of Chat objects to display.
     * @param context  The context for layout inflater and starting activities.
     */
    public ChatsAdapter(List<Chat> chatList, Context context) {
        this.chatList = chatList;
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.userCache = new ConcurrentHashMap<>();
        this.preferenceManager = PreferenceManager.getInstance(context);
    }

    /**
     * Creates and returns a new ChatViewHolder to display a chat item.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new view (not used here).
     * @return A new instance of ChatViewHolder.
     */
    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ChatViewHolder(binding);
    }

    /**
     * Binds data from the given Chat object to the specified ViewHolder.
     * Also sets an OnClickListener to launch the MessagingActivity for this chat item.
     *
     * @param holder   The ChatViewHolder which should be updated to represent the contents of the Chat.
     * @param position The position of the Chat item in the chatList.
     */
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        final Chat chat = chatList.get(position);
        holder.bind(chat);
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, MessagingActivity.class);
            intent.putExtra(Constants.KEY_ID, chat.id);
            context.startActivity(intent);
        });
    }

    /**
     * Returns the total number of chat items in the data set.
     *
     * @return The size of chatList.
     */
    @Override
    public int getItemCount() {
        return chatList.size();
    }

    /**
     * ChatViewHolder holds references to the views for each chat item (item_chat.xml).
     * It is responsible for binding the data for a single Chat.
     */
    public class ChatViewHolder extends RecyclerView.ViewHolder {

        /**
         * Binding object for item_chat.xml layout.
         */
        private final ItemChatBinding binding;

        /**
         * Constructor that accepts the binding for item_chat.xml.
         *
         * @param itemChatBinding The ItemChatBinding object for accessing views without findViewById.
         */
        public ChatViewHolder(@NonNull ItemChatBinding itemChatBinding) {
            super(itemChatBinding.getRoot());
            binding = itemChatBinding;
        }

        /**
         * Binds a Chat object's data to this ViewHolder's views.
         *
         * @param chat The Chat object to display.
         */
        public void bind(final Chat chat) {
            binding.messageChatId.setText(chat.id);

            if (chat.recentMessageId.isEmpty()) {
                binding.chatMessageUserName.setText("");
                binding.chatMessageContent.setText("No message");
                binding.chatMessageTimestamp.setText("");
                binding.chatMessageUserImage.setImageResource(R.drawable.ic_profile);
            } else {
                fetchRecentMessageAndBind(chat);
            }
        }

        /**
         * Fetches the recent message from Firestore using the given Chat's recentMessageId.
         * Once fetched, we display the message content/timestamp and the sender's data.
         *
         * @param chat The Chat object containing the recentMessageId and chatId.
         */
        private void fetchRecentMessageAndBind(final Chat chat) {
            firestore.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chat.id)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(chat.recentMessageId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Message recentMessage = documentSnapshot.toObject(Message.class);
                            if (recentMessage != null) {
                                binding.chatMessageContent.setText(recentMessage.content);
                                binding.chatMessageTimestamp.setText(formatDate(recentMessage.sentDate));
                                fetchSenderAndBindData(recentMessage.senderId);
                                return;
                            }
                        }
                        showNoMessageData();
                    })
                    .addOnFailureListener(e -> {
                        showNoMessageData();
                        Log.e("CHATS_ADAPTER", "Error fetching recent message: " + chat.recentMessageId, e);
                    });
        }

        /**
         * Fetches the sender (User) from Firestore. Once retrieved, we display the sender's details
         * in the chat preview (profile picture, display name).
         *
         * @param senderId The ID of the user who sent the recent message.
         */
        private void fetchSenderAndBindData(final String senderId) {
            if (userCache.containsKey(senderId)) {
                User cachedUser = userCache.get(senderId);
                if (cachedUser != null) {
                    bindSenderData(cachedUser);
                    return;
                }
            }
            fetchUserFromFirestore(senderId);
        }

        /**
         * Actually fetches a User object from Firestore and then caches (even if null),
         * ensuring we don't repeatedly fetch the same user.
         *
         * @param senderId The user ID to fetch from Firestore.
         */
        private void fetchUserFromFirestore(final String senderId) {
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                userCache.put(senderId, user);
                                bindSenderData(user);
                                return;
                            }
                        }
                        userCache.put(senderId, new User(senderId));
                        showDefaultSenderData();

                    })
                    .addOnFailureListener(e -> {
                        userCache.put(senderId, new User(senderId));
                        showDefaultSenderData();
                        Log.e("ChatsAdapter", "Failed to fetch user for senderId=" + senderId, e);
                    });
        }

        /**
         * Binds the sender's data (display name, profile picture) to this ViewHolder's UI elements.
         *
         * @param user The sender's User object retrieved from Firestore.
         */
        private void bindSenderData(User user) {
            String displayName;
            if (!user.firstName.isEmpty() && !user.lastName.isEmpty()) {
                displayName = user.firstName + " " + user.lastName;
            } else if (!user.email.isEmpty()) {
                displayName = user.email;
            } else if (!user.phone.isEmpty()) {
                displayName = user.phone;
            } else {
                displayName = "unknown sender";
            }

            binding.chatMessageUserName.setText(displayName);
            binding.chatMessageUserImage.setImageResource(R.drawable.ic_profile);

            if (!user.image.isEmpty()) {
                android.graphics.Bitmap senderBitmap = User.getBitmapFromEncodedString(user.image);
                if (senderBitmap != null) {
                    binding.chatMessageUserImage.setImageBitmap(senderBitmap);
                }
            }
        }

        /**
         * Formats a Date into "yyyy-MM-dd HH:mm", or returns an empty string if null.
         *
         * @param date The Date to format.
         * @return Formatted date string or "" if date is null.
         */
        private String formatDate(Date date) {
            if (date == null) {
                return "";
            }
            java.text.SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return dateFormat.format(date);
        }

        /**
         * When there's no valid recent message, we clear or hide data in the preview.
         */
        private void showNoMessageData() {
            binding.chatMessageContent.setText("");
            binding.chatMessageTimestamp.setText("");
            showDefaultSenderData();
        }

        /**
         * Sets a default sender name/picture if the user is unavailable.
         */
        private void showDefaultSenderData() {
            binding.chatMessageUserName.setText("unknown sender");
            binding.chatMessageUserImage.setImageResource(R.drawable.ic_profile);
        }
    }
}