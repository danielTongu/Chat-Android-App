package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.adapters.MessagesAdapter;
import com.example.chatandroidapp.databinding.ActivityMessagingBinding;
import com.example.chatandroidapp.models.Chat;
import com.example.chatandroidapp.models.Message;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * MessagingActivity handles the core messaging functionality, including sending,
 * receiving, and deleting messages in real-time, along with chat and participant management.
 */
public class MessagingActivity extends AppCompatActivity {
    private final List<Message> messageList = new ArrayList<>(); // List of messages in the chat
    private final List<User> userList = new ArrayList<>(); // List of users in the chat

    private ActivityMessagingBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private String chatId = null; // ID of the current chat
    private Chat currentChat = null; // Current chat details
    private MessagesAdapter messagesAdapter;
    private ListenerRegistration messagesListener; // Listener for real-time updates

    // ============================== Lifecycle Methods ==============================

    /**
     * Initializes the activity, sets up listeners, and begins listening for messages if applicable.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMessagingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        checkIntentData();
        setListeners();
    }

    /**
     * Cleans up resources and removes listeners to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }

    // ============================== Initialization ==============================

    /**
     * Initializes Firestore, UI components, and adapters.
     */
    private void initializeComponents() {
        showLoading(true, "initializing...");
        database = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        messagesAdapter = new MessagesAdapter(messageList, this);

        binding.messagesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerview.setAdapter(messagesAdapter);
    }

    /**
     * Checks intent data to determine the chat context (new or existing).
     */
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_ID)) {
            chatId = intent.getStringExtra(Constants.KEY_ID);
            fetchChatDetails();
        } else if (intent.hasExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST)) {
            handleNewChat(intent);
        } else {
            Utilities.showToast(this, "Invalid chat data. Please try again.", Utilities.ToastType.ERROR);
            finish();
        }
    }

    /**
     * Sets up click listeners for UI components.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonSendMessage.setOnClickListener(v -> handleSendMessage());
        binding.buttonShowChatInfo.setOnClickListener(v -> showChatInfo());
        binding.buttonDeleteChat.setOnClickListener(v -> deleteChat());
    }

    // ============================== Handling Existing Chats ==============================

    /**
     * Fetches chat details from Firestore.
     */
    private void fetchChatDetails() {
        showLoading(true, "fetching chat data...");

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .get()
                .addOnSuccessListener(chatDocument -> {
                    if (chatDocument.exists()) {
                        currentChat = chatDocument.toObject(Chat.class);
                        if (currentChat != null && currentChat.userIdList != null && !currentChat.userIdList.isEmpty()) {
                            fetchUserDetails(currentChat.userIdList);
                            showLoading(false, null);
                        } else {
                            Utilities.showToast(MessagingActivity.this, "Chat data is invalid.", Utilities.ToastType.ERROR);
                            finish();
                        }
                    } else {
                        Utilities.showToast(MessagingActivity.this, "Chat does not exist.", Utilities.ToastType.ERROR);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to fetch chat details. Please try again.", e);
                    finish();
                });
    }

    /**
     * Fetches user details based on a list of user IDs and populates the userList.
     * If a user ID does not correspond to an existing user (i.e., the user has deleted their account),
     * the user ID is removed from the chat's userIdList.
     *
     * @param userIds List of user IDs to fetch details for.
     */
    private void fetchUserDetails(List<String> userIds) {
        if (userIds.isEmpty()) {
            Utilities.showToast(this, "No users found in this chat.", Utilities.ToastType.ERROR);
            return;
        }

        showLoading(true, "fetching users details...");
        userList.clear();

        // Convert userIds to a Set for efficient removal
        Set<String> remainingUserIds = new HashSet<>(userIds);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereIn("id", userIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            userList.add(user);
                            remainingUserIds.remove(user.id); // Remove found user IDs
                        }
                    }

                    List<String> missingUserIds = new ArrayList<>(remainingUserIds);

                    if (!missingUserIds.isEmpty()) {
                        currentChat.userIdList.removeAll(missingUserIds);
                        updateChatUserIdsInFirestore(currentChat.userIdList, missingUserIds);
                        // Optionally, notify the user about the removal
                        String removedUsers = String.join(", ", missingUserIds);
                        Utilities.showToast(this, "Removed users: " + removedUsers, Utilities.ToastType.WARNING);
                    }

                    showLoading(false, null);
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to fetch user details.", e);
                    listenForMessages();
                });
    }

    /**
     * Updates the chat's userIdList in Firestore after removing non-existent users.
     *
     * @param updatedUserIds The updated list of user IDs.
     * @param removedUserIds The list of user IDs that were removed.
     */
    private void updateChatUserIdsInFirestore(List<String> updatedUserIds, List<String> removedUserIds) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update("userIdList", updatedUserIds)
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to update chat participants.", e);
                    Utilities.showToast(this, "Failed to update chat participants.", Utilities.ToastType.ERROR);
                });
    }

    // ============================== Handling New Chats ==============================

    /**
     * Handles the initialization and setup for a new chat.
     *
     * @param intent The intent containing selected users and initial message.
     */
    private void handleNewChat(Intent intent) {
        showLoading(true, "initializing new chat...");

        List<User> selectedUsers = (List<User>) intent.getSerializableExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST);
        if (selectedUsers != null && !selectedUsers.isEmpty()) {
            userList.addAll(selectedUsers);
            String initialMessage = intent.getStringExtra(ChatCreatorActivity.KEY_INITIAL_MESSAGE);

            if (initialMessage != null && !initialMessage.isEmpty()) {
                binding.inputMessage.setText(initialMessage);
                handleSendMessage(); // Sends the initial message
            } else {
                showLoading(false, null);// Enable buttons if there's no initial message
            }
        } else {
            Utilities.showToast(this, "No users selected for the chat.", Utilities.ToastType.ERROR);
            finish();
        }
    }

    // ============================== Messaging Functionality ==============================

    /**
     * Listens for real-time messages in the current chat and updates the RecyclerView.
     */
    private void listenForMessages() {
        messagesListener = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .orderBy("sentDate")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        logCriticalError("Failed to listen for messages.", e);
                        return;
                    }

                    if (snapshots != null) {
                        showLoading(true, null);
                        messageList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }
                        messagesAdapter.notifyDataSetChanged();

                        if (!messageList.isEmpty()) {
                            binding.messagesRecyclerview.smoothScrollToPosition(messageList.size() - 1);
                        }
                        showLoading(false, null);
                    }
                });
    }

    /**
     * Deletes the current chat if the user is the creator.
     */
    private void deleteChat() {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to delete chat. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        showLoading(true, "validating chat...");

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .get()
                .addOnSuccessListener(chatDocument -> {
                    if (chatDocument.exists()) {
                        Chat chat = chatDocument.toObject(Chat.class);
                        if (chat != null && chat.creatorId.equals(preferenceManager.getString(Constants.KEY_ID, ""))) {
                            deleteChatMessagesFromFirestore(chat);
                        } else {
                            Utilities.showToast(MessagingActivity.this, "You do not have permission to delete this chat.", Utilities.ToastType.ERROR);
                            showLoading(false, null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to verify permissions. Please try again.", e);
                });
    }

    private void deleteChatMessagesFromFirestore(Chat chat) {
        showLoading(true, "deleting chat messages...");

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No messages to delete, proceed to delete chat
                        deleteChatFromFirestore(chat);
                        return;
                    }

                    // Create a batch to delete all messages
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    WriteBatch batch = db.batch();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(document.getReference());
                    }

                    // Commit the batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> deleteChatFromFirestore(chat))
                            .addOnFailureListener(e -> logCriticalError("Failed to delete chat messages. Please try again.", e));
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to fetch chat messages for deletion.", e);
                });
    }

    /**
     * Deletes the chat from Firestore and updates user records accordingly.
     *
     * @param chat The chat object to delete.
     */
    private void deleteChatFromFirestore(Chat chat) {
        showLoading(true, "deleting chat...");

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .delete()
                .addOnSuccessListener(unused -> {
                    for (String userId : chat.userIdList) {
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .update("chatIds", FieldValue.arrayRemove(chatId));
                    }
                    Utilities.showToast(MessagingActivity.this, "Chat deleted successfully", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to delete chat. Please try again.", e);
                });
    }

    /**
     * Sends a message or creates a new chat with an initial message.
     */
    private void handleSendMessage() {
        String messageContent = binding.inputMessage.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Utilities.showToast(this, "Message content cannot be empty.", Utilities.ToastType.WARNING);
            return;
        }

        if (chatId == null) {
            createChatWithInitialMessage(messageContent);
        } else {
            sendMessage(messageContent);
        }
        binding.inputMessage.setText(null);
    }

    /**
     * Sends a message to the current chat.
     *
     * @param messageContent The message content.
     */
    private void sendMessage(String messageContent) {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to send message. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        showLoading(true, "sending message...");

        try {
            String messageId = database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document().getId();

            Message message = new Message(messageId, chatId, preferenceManager.getString(Constants.KEY_ID, ""), messageContent);

            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.KEY_COLLECTION_MESSAGES)
                    .document(messageId)
                    .set(message)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            updateRecentMessage(message);
                        } else {
                            Utilities.showToast(this, "Failed to send message. Please try again.", Utilities.ToastType.ERROR);
                        }
                        showLoading(false, null);
                    });

        } catch (IllegalArgumentException e) {
            logCriticalError("Failed to send message. Please try again.", e);
        }
    }

    /**
     * Updates the most recent message for the chat.
     *
     * @param message The recent message.
     */
    private void updateRecentMessage(Message message) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update("recentMessageId", message.id)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Utilities.showToast(this, "Failed to update recent message. Please try again.", Utilities.ToastType.ERROR);
                    }
                });
    }


    /**
     * Creates a new chat and sends the first message.
     *
     * @param initialMessage The first message content.
     */
    private void createChatWithInitialMessage(final String initialMessage) {
        showLoading(true, "creating new chat...");

        List<String> userIds = getUserIdsFromList(userList);
        userIds.add(preferenceManager.getString(Constants.KEY_ID, ""));

        String newChatId = database.collection(Constants.KEY_COLLECTION_CHATS).document().getId();

        try {
            Chat chat = new Chat(newChatId, preferenceManager.getString(Constants.KEY_ID, ""), userIds, "");
            database.collection(Constants.KEY_COLLECTION_CHATS)
                    .document(newChatId)
                    .set(chat)
                    .addOnSuccessListener(unused -> {
                        chatId = newChatId;
                        currentChat = chat;
                        updateChatIdsForUsers(userIds);
                        sendMessage(initialMessage);
                        showLoading(false, null);
                        listenForMessages();
                    })
                    .addOnFailureListener(e -> {
                        logCriticalError("Failed to create chat. Please try again.", e);
                    });
        } catch (IllegalArgumentException e) {
            logCriticalError("Failed to create chat. Please try again.", e);
        }
    }

    /**
     * Extracts user IDs from the user list.
     *
     * @param users List of User objects.
     * @return List of user IDs.
     */
    private List<String> getUserIdsFromList(List<User> users) {
        List<String> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.id);
        }
        return ids;
    }


    /**
     * Updates chat IDs for all users in the chat.
     *
     * @param userIds List of user IDs participating in the chat.
     */
    private void updateChatIdsForUsers(List<String> userIds) {
        for (String userId : userIds) {
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId)
                    .update("chatIds", FieldValue.arrayUnion(chatId));
        }
    }

    // ============================== Chat Information ==============================

    /**
     * Displays information about the chat and its participants.
     */
    private void showChatInfo() {
        if (currentChat == null) {
            Utilities.showToast(this, "Chat information is unavailable.", Utilities.ToastType.ERROR);
            return;
        }

        if (userList.isEmpty()) {
            Utilities.showToast(this, "Unable to load chat information. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        String creatorName = findUserNameById(currentChat.creatorId);
        String chatInfo = formatChatInfo(userList, creatorName, currentChat.createdDate);
        displayChatInfoDialog(chatInfo);
    }

    /**
     * Finds a user's name in the list by their ID.
     *
     * @param userId The user ID to search for.
     * @return The user's full name, or "Unknown" if not found.
     */
    private String findUserNameById(String userId) {
        for (User user : userList) {
            if (user.id.equals(userId)) {
                return user.firstName + " " + user.lastName;
            }
        }
        return "Unknown";
    }

    /**
     * Formats chat information into a readable string.
     *
     * @param users       List of users in the chat.
     * @param creatorName Name of the chat creator.
     * @param createdDate Date when the chat was created.
     * @return Formatted string containing chat information.
     */
    private String formatChatInfo(List<User> users, String creatorName, Date createdDate) {
        StringBuilder info = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());

        info.append("Creator: ").append(creatorName).append("\n");
        info.append("Created Date: ").append(createdDate != null ? sdf.format(createdDate) : "N/A").append("\n\n");
        info.append("Participants:\n");
        for (User user : users) {
            info.append(user.firstName).append(" ").append(user.lastName).append("\n");
        }
        return info.toString();
    }

    /**
     * Displays the chat information in a dialog.
     *
     * @param chatInfo Formatted chat information string.
     */
    private void displayChatInfoDialog(String chatInfo) {
        new AlertDialog.Builder(this)
                .setTitle("Chat Information")
                .setMessage(chatInfo)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Logs critical errors for debugging purposes.
     *
     * @param message The error message.
     * @param e       The exception causing the error.
     */
    private void logCriticalError(String message, Exception e) {
        showLoading(false, null);
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        android.util.Log.e("MESSAGING_ACTIVITY", message, e);
    }

    /**
     * Displays or hides the loading UI.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display during loading.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textProgressMessage.setVisibility(message == null ? View.GONE : View.VISIBLE);
        binding.textProgressMessage.setText(message);
        binding.buttonSendMessage.setEnabled(!isLoading);
        binding.buttonDeleteChat.setEnabled(!isLoading);
    }
}