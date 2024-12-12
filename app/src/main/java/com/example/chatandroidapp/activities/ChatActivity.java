// ChatActivity.java
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
 * It handles both existing and new chats using parameterized constructors.
 */
public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    private ActivityChatBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;

    private String chatId;
    private boolean isNewChat = false;
    private List<User> selectedUsers;

    private List<Message> messagesList;
    private MessagesAdapter messagesAdapter;

    private ListenerRegistration messagesListener; // To manage Firestore listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using View Binding
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize components
        init();

        // Determine if the chat is new or existing based on intent data
        checkIntentData();

        // Set up UI listeners
        setListeners();

        // If it's an existing chat, start listening for messages
        if (!isNewChat && chatId != null) {
            listenForMessages();
        }
    }

    /**
     * Initializes Firestore, PreferenceManager, RecyclerView, and other essential components.
     */
    private void init() {
        database = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        // Initialize messages list and adapter
        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messagesList, this);
        binding.messagesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerview.setAdapter(messagesAdapter);

        // Show progress bar while loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.processMessage.setVisibility(View.GONE);
    }

    /**
     * Checks the intent to determine whether to load an existing chat or create a new one.
     */
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_ID)) {
            // Existing chat scenario
            chatId = intent.getStringExtra(Constants.KEY_ID);
            isNewChat = false;
            Log.d(TAG, "Opening existing chat with ID: " + chatId);
        } else if (intent.hasExtra("KEY_SELECTED_USERS")) { // Replace with actual key
            // New chat scenario
            selectedUsers = (List<User>) intent.getSerializableExtra("KEY_SELECTED_USERS"); // Replace with actual key
            isNewChat = true;
            Log.d(TAG, "Initiating new chat with users: " + selectedUsers);
        } else {
            // Invalid intent data
            Log.e(TAG, "Invalid intent data. Finishing ChatActivity.");
            finish();
        }
    }

    /**
     * Sets up listeners for UI components such as the back button and send button.
     */
    private void setListeners() {
        // Back button listener
        binding.buttonBack.setOnClickListener(v -> onBackPressed());

        // Send message button listener
        binding.buttonSendMessage.setOnClickListener(v -> {
            String messageContent = binding.inputMessage.getText().toString().trim();
            if (!messageContent.isEmpty()) {
                binding.buttonSendMessage.setEnabled(false); // Prevent multiple clicks
                if (isNewChat) {
                    createChatWithInitialMessage(messageContent);
                } else {
                    sendMessage(messageContent);
                }
                binding.inputMessage.setText(null); // Clear input field
                binding.buttonSendMessage.setEnabled(true);
            }
        });
    }

    /**
     * Creates a new chat in Firestore and sends the first message.
     *
     * @param initialMessage The content of the first message.
     */
    private void createChatWithInitialMessage(String initialMessage) {
        try {
            // Compile list of user IDs involved in the chat, including the current user
            List<String> userIds = new ArrayList<>();
            for (User user : selectedUsers) {
                userIds.add(user.id);
            }
            String currentUserId = preferenceManager.getString(Constants.KEY_ID);
            userIds.add(currentUserId);

            // Generate a new document ID for the chat
            String newChatId = database.collection(Constants.KEY_COLLECTION_CHATS).document().getId();

            // Initialize Chat object with validated and assigned fields
            Chat chat = new Chat(
                    newChatId, // id
                    currentUserId, // creatorId
                    userIds, // userIdList
                    "" // recentMessageId (initially empty)
            );

            // Add chat to Firestore with the predefined ID
            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(newChatId)
                    .set(chat)
                    .addOnSuccessListener(aVoid -> {
                        chatId = newChatId;
                        Log.d(TAG, "Chat created with ID: " + chatId);

                        // Send the initial message
                        sendMessage(initialMessage);

                        // Start listening for messages in the newly created chat
                        listenForMessages();

                        // Hide progress bar
                        binding.progressBar.setVisibility(View.GONE);

                        // Update isNewChat flag to false after creating the chat
                        isNewChat = false;
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to create chat", e);
                        showError("Unable to create chat. Please try again.");
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Validation failed while creating chat", e);
            showError(e.getMessage());
        }
    }

    /**
     * Sends a message within the current chat using parameterized constructor.
     *
     * @param messageContent The content of the message to send.
     */
    private void sendMessage(String messageContent) {
        if (chatId == null) {
            Log.e(TAG, "Chat ID is null. Cannot send message.");
            showError("Unable to send message. Please try again.");
            return;
        }

        try {
            // Generate a new document ID for the message
            String newMessageId = database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document().getId();

            // Initialize Message object with validated and assigned fields
            Message message = new Message(
                    newMessageId, // id
                    chatId,       // chatId
                    preferenceManager.getString(Constants.KEY_ID), // senderId
                    messageContent // content
            );

            // Add message to Firestore under the specific chat with the predefined ID
            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(newMessageId)
                    .set(message)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Message sent successfully with ID: " + newMessageId);
                        // Update the recentMessageId in Chat
                        updateRecentMessageId(newMessageId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message", e);
                        showError("Unable to send message. Please try again.");
                    });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Validation failed while sending message", e);
            showError(e.getMessage());
        }
    }

    /**
     * Updates the recentMessageId field in the Chat document.
     *
     * @param messageId The ID of the most recent message.
     */
    private void updateRecentMessageId(String messageId) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update(Constants.KEY_RECENT_MESSAGE_ID, messageId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Recent message ID updated"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update recent message ID", e));
    }

    /**
     * Listens for real-time updates to messages within the current chat.
     * Updates the RecyclerView when new messages are added.
     */
    private void listenForMessages() {
        if (chatId == null) {
            Log.e(TAG, "Chat ID is null. Cannot listen for messages.");
            showError("Unable to load messages.");
            return;
        }

        messagesListener = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .orderBy(Constants.KEY_SENT_DATE) // Ensure messages are ordered by sent date
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for messages", error);
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

                        // Toggle visibility based on message list
                        if (messagesList.isEmpty()) {
                            showNoMessages();
                        } else {
                            binding.processMessage.setVisibility(View.GONE);
                            binding.messagesRecyclerview.setVisibility(View.VISIBLE);
                        }
                    }

                    // Hide progress bar once messages are loaded
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    /**
     * Displays an error message to the user.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.processMessage.setVisibility(View.VISIBLE);
        binding.processMessage.setText(message);
    }

    /**
     * Displays a "No messages yet" message when the chat has no messages.
     */
    private void showNoMessages() {
        binding.processMessage.setVisibility(View.VISIBLE);
        binding.processMessage.setText("No messages yet.");
        binding.messagesRecyclerview.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to prevent memory leaks
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}