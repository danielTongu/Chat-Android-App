/**
 * MessagesAdapter handles the display of chat messages in the RecyclerView.
 */
package com.example.chatandroidapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ItemMessageBinding;
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
 * Adapter class for displaying chat messages.
 * Each message can be displayed as "sent" by the current user or "received" from another user.
 * For received messages, the adapter attempts to load the sender's details from a Firestore-based cache.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    /** The list of Message objects to be displayed. */
    private final List<Message> messagesList;

    /**ID of the current user, used to differentiate between sent and received messages. */
    private final String currentUserId;

    /** A reference to the Firestore database for fetching sender user data of received messages.*/
    private final FirebaseFirestore firestore;

    /**A cache for storing sender User objects, preventing repeated Firestore lookups.*/
    private final ConcurrentHashMap<String, User> userCache;

    /** The context of the Activity or Fragment using this adapter.*/
    private final Context context;


    /**
     * Constructs a new MessagesAdapter.
     *
     * @param messagesList The list of messages to display.
     * @param context      The context, used for accessing shared preferences and resources.
     */
    public MessagesAdapter(List<Message> messagesList, Context context) {
        this.messagesList = messagesList;
        this.context = context;

        PreferenceManager preferenceManager = PreferenceManager.getInstance(context);
        this.currentUserId = preferenceManager.getString(Constants.KEY_ID, "");
        this.firestore = FirebaseFirestore.getInstance();
        this.userCache = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new ViewHolder to represent a message item.
     *
     * @param parent   The parent ViewGroup into which the new view will be added.
     * @param viewType The view type of the new View (not used here since layout is shared).
     * @return A new MessageViewHolder instance.
     */
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageBinding binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MessageViewHolder(binding);
    }

    /**
     * Binds a Message object at the specified position to the given ViewHolder.
     *
     * @param holder   The ViewHolder to bind data to.
     * @param position The position of the Message in the list.
     */
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messagesList.get(position);
        holder.bind(message);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The size of messagesList.
     */
    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    /**
     * ViewHolder class for the MessagesAdapter. Uses ItemMessageBinding to access views without R.id references.
     * Handles both "sent" and "received" message layouts within the same item view.
     */
    public class MessageViewHolder extends RecyclerView.ViewHolder {
        /**
         * Binding object generated from item_message.xml layout.
         */
        private final ItemMessageBinding binding;

        /**
         * Constructs a new MessageViewHolder with the given binding.
         *
         * @param binding The ItemMessageBinding for accessing layout views.
         */
        public MessageViewHolder(ItemMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a Message object's data to the ViewHolder's views.
         *
         * @param message The Message to display.
         */
        public void bind(final Message message) {
            boolean isCurrentUserSender = message.senderId.equals(currentUserId);

            if (isCurrentUserSender) {
                showSentMessage(message);
            } else {
                showReceivedMessage(message);
            }
        }

        /**
         * Displays a message as "sent" by the current user.
         *
         * @param message The sent Message object.
         */
        private void showSentMessage(Message message) {
            binding.receivedMessageLayout.setVisibility(View.GONE);

            binding.sentMessageLayout.setVisibility(View.VISIBLE);
            binding.sentMessage.setText(message.content);
            binding.sentTimestamp.setText(formatDate(message.sentDate));
            binding.sentNameOrPhoneOrEmail.setText("Me");
        }

        /**
         * Displays a message as "received" from another user.
         *
         * @param message The received Message object.
         */
        private void showReceivedMessage(final Message message) {
            binding.sentMessageLayout.setVisibility(View.GONE);

            binding.receivedMessageLayout.setVisibility(View.VISIBLE);
            binding.receivedMessage.setText(message.content);
            binding.receivedTimestamp.setText(formatDate(message.sentDate));

            // Try to retrieve the sender's details from the cache or Firestore
            if (userCache.containsKey(message.senderId)) {
                User cachedSender = userCache.get(message.senderId);
                if (cachedSender != null) {
                    bindUserData(cachedSender);
                } else {
                    fetchSenderAndBindData(message.senderId);
                }
            } else {
                fetchSenderAndBindData(message.senderId);
            }
        }

        /**
         * Populates the UI with sender's data (for received messages).
         *
         * @param sender The User object representing the sender.
         */
        private void bindUserData(@NonNull User sender) {
            // Determine the best display name
            String displayName;
            if (!TextUtils.isEmpty(sender.firstName) && !TextUtils.isEmpty(sender.lastName)) {
                displayName = sender.firstName + " " + sender.lastName;
            } else if (!TextUtils.isEmpty(sender.email)) {
                displayName = sender.email;
            } else if (!TextUtils.isEmpty(sender.phone)) {
                displayName = sender.phone;
            } else {
                displayName = "Unknown";
            }

            binding.receivedNameOrPhoneOrEmail.setText(displayName);
            binding.receivedProfilePicture.setImageResource(R.drawable.ic_profile);

            // If there's a custom profile image, decode and display it
            if (!TextUtils.isEmpty(sender.image)) {
                Bitmap senderBitmap = User.getBitmapFromEncodedString(sender.image);
                if (senderBitmap != null) {
                    binding.receivedProfilePicture.setImageBitmap(senderBitmap);
                }
            }
        }

        /**
         * Fetches a User's information from Firestore using the given senderId and, upon completion,
         * binds the user data to the UI or sets default user data if fetching fails or the user is not found.
         *
         * @param senderId The ID of the user to fetch from Firestore.
         */
        private void fetchSenderAndBindData(final String senderId) {
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .document(senderId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User sender = new User(senderId);
                        if (documentSnapshot.exists()) {
                            sender = documentSnapshot.toObject(User.class);
                        }
                        if(sender != null) {
                            userCache.put(senderId, sender);
                            bindUserData(sender);
                        }

                    })
                    .addOnFailureListener(e -> {
                        User sender = new User(senderId);
                        userCache.put(senderId, sender);
                        bindUserData(sender);
                        Log.e("MESSAGES_ADAPTER", "Failed to fetch user data for senderId: " + senderId, e);
                    });
        }
    }

    /**
     * Formats a Date object into a readable string (yyyy-MM-dd HH:mm).
     * Returns an empty string if the date is null.
     *
     * @param date The Date object to format.
     * @return The formatted date string or "" if null.
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(date);
    }
}