package com.example.chatandroidapp.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.adapters.ChatAdapter;
import com.example.chatandroidapp.databinding.ActivityChatBinding;
import com.example.chatandroidapp.module.Chat;
import com.example.chatandroidapp.module.ChatMessage;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * ChatActivity manages real-time chat functionality between users, including sending and receiving messages.
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "CHAT_ACTIVITY";
    private ActivityChatBinding binding;
    private User receiverUser;
    private Chat chat;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        database = FirebaseFirestore.getInstance();

        loadChatDetails();
        setListeners();
        init();
        listenForMessages();
    }

    /**
     * Initializes components including chat adapter and message list.
     */
    private void init() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
    }

    /**
     * Sets up event listeners for UI interactions.
     */
    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.imageInfo.setOnClickListener(v -> deleteChat());
    }

    /**
     * Loads chat details including receiver and chat metadata.
     */
    private void loadChatDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        chat = (Chat) getIntent().getSerializableExtra(Constants.KEY_CHAT);

        if (receiverUser != null) {
            binding.textName.setText(String.format("%s %s", receiverUser.firstName, receiverUser.lastName));
        } else if (chat != null) {
            binding.textName.setText("Group Chat");
        }
    }

    /**
     * Sends a new message to the chat.
     */
    private void sendMessage() {
        String messageText = binding.inputMessage.getText().toString().trim();
        if (messageText.isEmpty()) {
            Utilities.showToast(this, "Cannot send an empty message", Utilities.ToastType.WARNING);
            return;
        }

        ChatMessage message = new ChatMessage(
                preferenceManager.getString(Constants.KEY_USER_ID),
                messageText
        );

        chat.addMessage(message);

        HashMap<String, Object> chatData = new HashMap<>();
        chatData.put(Constants.KEY_MESSAGES, chat.messages);
        chatData.put(Constants.KEY_LAST_MESSAGE, message.message);
        chatData.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .document(chat.id)
                .update(chatData)
                .addOnSuccessListener(unused -> {
                    binding.inputMessage.setText(null);
                    Utilities.showToast(this, "Message sent", Utilities.ToastType.SUCCESS);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to send message", e));
    }

    /**
     * Listens for real-time message updates in the chat.
     */
    private void listenForMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .document(chat.id)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching messages", error);
                        return;
                    }
                    if (value != null && value.exists()) {
                        chat.messages = (List<ChatMessage>) value.get(Constants.KEY_MESSAGES);
                        chatAdapter.notifyDataSetChanged();
                        binding.chatRecyclerView.smoothScrollToPosition(chat.messages.size() - 1);
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Deletes the current chat if the user is the creator.
     */
    private void deleteChat() {
        if (!preferenceManager.getString(Constants.KEY_USER_ID).equals(chat.creatorId)) {
            Utilities.showToast(this, "Only the creator can delete this chat", Utilities.ToastType.WARNING);
            return;
        }

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .document(chat.id)
                .delete()
                .addOnSuccessListener(unused -> {
                    Utilities.showToast(this, "Chat deleted successfully", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete chat", e));
    }

    /**
     * Converts a Base64 string to a Bitmap.
     *
     * @param encodedImage The Base64-encoded image.
     * @return A Bitmap object.
     */
    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}