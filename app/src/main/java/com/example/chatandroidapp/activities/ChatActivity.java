package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.adapters.MessagesAdapter;
import com.example.chatandroidapp.databinding.ActivityChatBinding;
import com.example.chatandroidapp.module.Chat;
import com.example.chatandroidapp.module.Message;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatActivity manages real-time messaging between users.
 * Handles both new and existing chats, including chat creation, message sending,
 * and displaying real-time chat messages.
 */
public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "CHAT_ACTIVITY";

    private ActivityChatBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;

    private String chatId; // ID of the current chat
    private boolean isNewChat = false; // Indicates if the chat is new
    private List<User> selectedUsers; // List of selected users for a new chat

    private List<Message> messagesList;
    private MessagesAdapter messagesAdapter;

    private ListenerRegistration messagesListener;

    /**
     * Initializes the activity and sets up chat functionality.
     *
     * @param savedInstanceState The saved state of the activity (if any).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: Initializing components");
        initializeComponents();

        Log.d(TAG, "onCreate: Checking intent data");
        checkIntentData();

        Log.d(TAG, "onCreate: Setting up listeners");
        setListeners();

        if (!isNewChat && chatId != null) {
            Log.d(TAG, "onCreate: Existing chat detected. Listening for messages.");
            listenForMessages();
        }
    }

    /**
     * Initializes Firebase, PreferenceManager, RecyclerView, and adapter.
     */
    private void initializeComponents() {
        database = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messagesList, this);

        binding.messagesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerview.setAdapter(messagesAdapter);

        showLoadingState(true, null);
    }

    /**
     * Checks the intent data to determine if the chat is new or existing.
     * Sets the appropriate fields like `chatId` or `selectedUsers`.
     */
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_ID)) {
            isNewChat = false;
            chatId = intent.getStringExtra(Constants.KEY_ID);
            Log.d(TAG, "checkIntentData: Existing chat with ID: " + chatId);
        } else if (intent.hasExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST)) {
            isNewChat = true;
            selectedUsers = (List<User>) intent.getSerializableExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST);
            Log.d(TAG, "checkIntentData: New chat with selected users: " + selectedUsers);
        } else {
            Log.e(TAG, "checkIntentData: Invalid intent data");
            showErrorAndExit("Invalid chat data.");
        }
    }

    /**
     * Sets up listeners for UI elements like the back button and send button.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> {
            Log.d(TAG, "setListeners: Back button clicked");
            onBackPressed();
        });

        binding.buttonSendMessage.setOnClickListener(v -> {
            Log.d(TAG, "setListeners: Send button clicked");
            handleSendMessage();
        });
    }

    /**
     * Handles the "Send" button click.
     */
    private void handleSendMessage() {
        String messageContent = binding.inputMessage.getText().toString().trim();
        Log.d(TAG, "handleSendMessage: Message content: " + messageContent);

        if (messageContent.isEmpty()) {
            Log.w(TAG, "handleSendMessage: Empty message content");
            showError("Message content cannot be empty.");
            return;
        }

        binding.buttonSendMessage.setEnabled(false);

        if (isNewChat) {
            Log.d(TAG, "handleSendMessage: Creating new chat with initial message");
            createChatWithInitialMessage(messageContent);
        } else {
            Log.d(TAG, "handleSendMessage: Sending message in existing chat with ID: " + chatId);
            sendMessage(messageContent);
        }

        binding.inputMessage.setText(null);
        binding.buttonSendMessage.setEnabled(true);
    }

    /**
     * Creates a new chat in Firestore and sends the first message.
     *
     * @param initialMessage The initial message content.
     */
    private void createChatWithInitialMessage(String initialMessage) {
        Log.d(TAG, "createChatWithInitialMessage: Creating new chat");

        List<String> userIds = new ArrayList<>();
        for (User user : selectedUsers) {
            userIds.add(user.id);
        }
        //userIds.add(preferenceManager.getString(Constants.KEY_ID, ""));

        String newChatId = database.collection(Constants.KEY_COLLECTION_CHATS).document().getId();
        Log.d(TAG, "createChatWithInitialMessage: Generated chat ID: " + newChatId);

        try {
            Chat chat = new Chat(newChatId, preferenceManager.getString(Constants.KEY_ID, ""), userIds, "");
            Log.d(TAG, "createChatWithInitialMessage: Chat object created successfully");

            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(newChatId)
                    .set(chat)
                    .addOnSuccessListener(unused -> {
                        chatId = newChatId;
                        isNewChat = false;
                        Log.d(TAG, "createChatWithInitialMessage: Chat created successfully with ID: " + chatId);
                        sendMessage(initialMessage);
                        listenForMessages();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "createChatWithInitialMessage: Failed to create chat in Firestore", e);
                        showError("Failed to create chat. Please try again.");
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "createChatWithInitialMessage: Chat construction failed", e);
            showError(e.getMessage());
        }
    }

    /**
     * Sends a message in the current chat.
     *
     * @param messageContent The content of the message.
     */
    private void sendMessage(String messageContent) {
        if (chatId == null) {
            Log.e(TAG, "sendMessage: Chat ID is null. Cannot send message.");
            showError("Cannot send message. Please try again.");
            return;
        }

        String messageId = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .document().getId();

        String senderId = preferenceManager.getString(Constants.KEY_ID, "");

        Log.d(TAG, "sendMessage: messageId = " + messageId);
        Log.d(TAG, "sendMessage: chatId = " + chatId);
        Log.d(TAG, "sendMessage: senderId = " + senderId);
        Log.d(TAG, "sendMessage: messageContent = " + messageContent);

        try {
            Message message = new Message(messageId, chatId, senderId, messageContent);
            Log.d(TAG, "sendMessage: Message object created successfully: " + message);

            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(messageId)
                    .set(message)
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "sendMessage: Message sent successfully with ID: " + messageId);
                        updateRecentMessageId(messageId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "sendMessage: Failed to send message", e);
                        showError("Failed to send message. Please try again.");
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "sendMessage: Message validation failed", e);
            showError(e.getMessage());
        }
    }

    /**
     * Updates the recent message ID in Firestore.
     *
     * @param messageId The ID of the most recent message.
     */
    private void updateRecentMessageId(String messageId) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update(Constants.KEY_RECENT_MESSAGE_ID, messageId)
                .addOnSuccessListener(unused -> Log.d(TAG, "updateRecentMessageId: Recent message ID updated"))
                .addOnFailureListener(e -> Log.e(TAG, "updateRecentMessageId: Failed to update recent message ID", e));
    }

    /**
     * Listens for real-time updates to messages in the chat.
     */
    private void listenForMessages() {
        if (chatId == null) {
            Log.e(TAG, "listenForMessages: Chat ID is null. Cannot listen for messages.");
            showError("Cannot load messages. Please try again.");
            return;
        }

        messagesListener = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .orderBy(Constants.KEY_SENT_DATE)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "listenForMessages: Error listening for messages", error);
                        showError("Failed to load messages.");
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Message message = dc.getDocument().toObject(Message.class);
                                messagesList.add(message);
                                messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                                binding.messagesRecyclerview.smoothScrollToPosition(messagesList.size() - 1);
                            }
                        }
                        toggleEmptyState(messagesList.isEmpty());
                    }
                    showLoadingState(false, null);
                });
    }

    /**
     * Toggles the empty state message if there are no chat messages.
     *
     * @param isEmpty True if the message list is empty, false otherwise.
     */
    private void toggleEmptyState(boolean isEmpty) {
        binding.processMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.messagesRecyclerview.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            binding.processMessage.setText("No messages yet.");
        }
    }

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        Log.e(TAG, "showError: " + message);
        showLoadingState(false, message);
    }

    /**
     * Updates the UI to display loading or error states.
     *
     * @param isLoading True to show the loading indicator, false otherwise.
     * @param message   An optional error or status message.
     */
    private void showLoadingState(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.processMessage.setVisibility(message != null ? View.VISIBLE : View.GONE);

        if (message != null) {
            binding.processMessage.setText(message);
        }
    }

    /**
     * Displays an error message and exits the activity.
     *
     * @param message The error message to display.
     */
    private void showErrorAndExit(String message) {
        Log.e(TAG, "showErrorAndExit: " + message);
        showError(message);
        finish();
    }

    /**
     * Cleans up resources and removes Firestore listeners to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}