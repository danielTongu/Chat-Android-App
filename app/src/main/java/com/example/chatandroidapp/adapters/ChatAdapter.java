package com.example.chatandroidapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatandroidapp.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatandroidapp.databinding.ItemContainerSentMessageBinding;
import com.example.chatandroidapp.module.ChatMessage;

import java.util.List;

/**
 * ChatAdapter is a RecyclerView adapter that handles displaying chat messages
 * in a chat interface. It differentiates between sent and received messages and
 * uses appropriate layouts for each type.
 *
 * @author Daniel Tongu
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_SENT = 1; // View type for sent messages
    private static final int VIEW_TYPE_RECEIVED = 2; // View type for received messages
    private final List<ChatMessage> chatMessages; // List of chat messages to display
    private final String senderId; // ID of the current user (sender)
    private Bitmap receiverProfileImage; // Profile image of the receiver

    /**
     * Constructor for ChatAdapter.
     * @param chatMessages        List of ChatMessage objects representing the messages in the chat.
     * @param receiverProfileImage Bitmap of the receiver's profile image.
     * @param senderId            ID of the sender (current user).
     */
    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        this.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the appropriate layout based on the view type
        if (viewType == VIEW_TYPE_SENT) { // Inflate the layout for sent messages
            return new SentMessageViewHolder(ItemContainerSentMessageBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else { // Inflate the layout for received messages
            return new ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Bind data to the appropriate view holder based on the view type
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        // Return the total number of chat messages
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        // Determine the view type based on the sender ID
        if (chatMessages.get(position).senderId.equals(this.senderId)) {
            return VIEW_TYPE_SENT; // Message sent by the current user
        } else {
            return VIEW_TYPE_RECEIVED; // Message received from another user
        }
    }

    /**
     * ViewHolder class for sent messages.
     */
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding; // Binding for the sent message layout

        /**
         * Constructor for SentMessageViewHolder.
         * @param binding Binding object for the sent message layout.
         */
        public SentMessageViewHolder(ItemContainerSentMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the data of a sent message to the view.
         * @param chatMessage The ChatMessage object representing the sent message.
         */
        private void setData(ChatMessage chatMessage) {
            binding.textMessage.setText(chatMessage.message); // Set the message text
            binding.textDateTime.setText(chatMessage.dateTime); // Set the timestamp
        }
    }

    /**
     * ViewHolder class for received messages.
     */
    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerReceivedMessageBinding binding; // Binding for the received message layout

        /**
         * Constructor for ReceiverMessageViewHolder.
         * @param binding Binding object for the received message layout.
         */
        public ReceiverMessageViewHolder(ItemContainerReceivedMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds the data of a received message to the view.
         * @param chatMessage         The ChatMessage object representing the received message.
         * @param receiverProfileImage The profile image of the message sender.
         */
        private void setData(ChatMessage chatMessage, Bitmap receiverProfileImage) {
            binding.textMessage.setText(chatMessage.message); // Set the message text
            binding.textDateTime.setText(chatMessage.dateTime); // Set the timestamp
            if (receiverProfileImage != null) {
                binding.imageProfile.setImageBitmap(receiverProfileImage); // Set the sender's profile image
            }
        }
    }
}