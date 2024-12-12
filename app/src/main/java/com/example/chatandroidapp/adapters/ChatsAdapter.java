// ChatsAdapter.java
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
import com.example.chatandroidapp.module.Chat;
import com.example.chatandroidapp.module.Message;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.makeramen.roundedimageview.RoundedImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter for displaying chats in a RecyclerView.
 */
public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    private final List<Chat> chats;
    private final ChatClickListener listener;
    private final FirebaseFirestore firestore;
    private final ConcurrentHashMap<String, User> userCache; // Cache to store fetched users

    /**
     * Interface for handling click events on chat items.
     */
    public interface ChatClickListener {
        void onChatClicked(Chat chat);
    }

    /**
     * Constructs the ChatsAdapter.
     *
     * @param chats    List of chats to display.
     * @param listener Listener for handling chat item clicks.
     */
    public ChatsAdapter(List<Chat> chats, ChatClickListener listener) {
        this.chats = chats;
        this.listener = listener;
        this.firestore = FirebaseFirestore.getInstance();
        this.userCache = new ConcurrentHashMap<>();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_chats_recycler_item, parent, false);
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
     * ViewHolder for a single chat item.
     */
    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final RoundedImageView profilePicture;
        private final TextView recentSenderName;
        private final TextView recentMessage;
        private final TextView recentTimestamp;
        private final TextView chatId;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePicture = itemView.findViewById(R.id.recentMessageSenderProfilePicture);
            recentSenderName = itemView.findViewById(R.id.recentSenderNameOrPhoneOrEmail);
            recentMessage = itemView.findViewById(R.id.recentMessage);
            recentTimestamp = itemView.findViewById(R.id.recentMessageTimestamp);
            chatId = itemView.findViewById(R.id.messageChatId);

            // Handle click events on the chat item
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
         * Binds chat data to the view holder.
         *
         * @param chat Chat object containing data to display.
         */
        public void bind(Chat chat) {
            // Hide chatId TextView as it's for internal use
            chatId.setText(chat.id);
            chatId.setVisibility(View.GONE);

            if (!TextUtils.isEmpty(chat.recentMessageId)) {
                // Fetch the recent message using recentMessageId
                firestore.collection(Constants.KEY_COLLECTION_MESSAGES)
                        .document(chat.recentMessageId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Message recentMsg = documentSnapshot.toObject(Message.class);
                                if (recentMsg != null) {
                                    // Fetch the sender's user data
                                    String senderId = recentMsg.senderId;
                                    if (userCache.containsKey(senderId)) {
                                        // If sender is already cached, use it
                                        User sender = userCache.get(senderId);
                                        bindMessageData(sender, recentMsg, itemView.getContext());
                                    } else {
                                        // Fetch sender's user document from Firestore
                                        firestore.collection(Constants.KEY_COLLECTION_USERS)
                                                .document(senderId)
                                                .get()
                                                .addOnSuccessListener(userDoc -> {
                                                    if (userDoc.exists()) {
                                                        User sender = userDoc.toObject(User.class);
                                                        if (sender != null) {
                                                            userCache.put(senderId, sender); // Cache the user
                                                            bindMessageData(sender, recentMsg, itemView.getContext());
                                                        } else {
                                                            // If User object is null, set default views
                                                            setDefaultViews();
                                                        }
                                                    } else {
                                                        // If User document does not exist, set default views
                                                        setDefaultViews();
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    // Handle failure in fetching user
                                                    Log.e("ChatsAdapter", "Failed to fetch user data for senderId: " + senderId, e);
                                                    setDefaultViews();
                                                });
                                    }
                                } else {
                                    // If Message object is null, set default views
                                    setDefaultViews();
                                }
                            } else {
                                // If Message document does not exist, set default views
                                setDefaultViews();
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Handle failure in fetching Message
                            Log.e("ChatsAdapter", "Failed to fetch recent message for chatId: " + chat.id, e);
                            setDefaultViews();
                        });
            } else {
                // If recentMessageId is empty or null, set default views
                setDefaultViews();
            }
        }

        /**
         * Binds the recent message and sender's data to the UI components.
         *
         * @param sender  User object representing the sender.
         * @param recentMsg Message object representing the recent message.
         * @param context Context for image decoding or other utilities.
         */
        private void bindMessageData(User sender, Message recentMsg, android.content.Context context) {
            // Determine sender's display name (FirstName + LastName > Email > Phone)
            String senderName;
            if (!TextUtils.isEmpty(sender.firstName) && !TextUtils.isEmpty(sender.lastName)) {
                senderName = sender.firstName + " " + sender.lastName;
            } else if (!TextUtils.isEmpty(sender.email)) {
                senderName = sender.email;
            } else {
                senderName = sender.phone;
            }

            recentSenderName.setText(senderName);
            recentMessage.setText(recentMsg.content);

            // Format and set the timestamp
            Date messageDate = recentMsg.sentDate;
            recentTimestamp.setText(formatTimestamp(messageDate));

            // Set sender's profile picture if available
            if (!TextUtils.isEmpty(sender.image)) {
                Bitmap senderImageBitmap = User.getBitmapFromEncodedString(sender.image);
                if (senderImageBitmap != null) {
                    profilePicture.setImageBitmap(senderImageBitmap);
                } else {
                    profilePicture.setImageResource(R.drawable.ic_profile); // Default icon
                }
            } else {
                profilePicture.setImageResource(R.drawable.ic_profile); // Default icon
            }
        }

        /**
         * Sets default texts and images when recent message data is unavailable.
         */
        private void setDefaultViews() {
            recentSenderName.setText("Unknown Sender");
            recentMessage.setText("No messages yet");
            recentTimestamp.setText("");
            profilePicture.setImageResource(R.drawable.ic_profile); // Default icon
        }

        /**
         * Formats a Date object into a readable timestamp.
         *
         * @param date Date object to format.
         * @return Formatted date string.
         */
        private String formatTimestamp(Date date) {
            if (date == null) {
                return "";
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
        }
    }
}