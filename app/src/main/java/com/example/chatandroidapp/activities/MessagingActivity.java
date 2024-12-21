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

import java.util.ArrayList;
import java.util.List;

/**
 * MessagingActivity manages real-time messaging between users.
 * It handles sending, receiving, and deleting messages in a chat, as well as
 * displaying chat information and managing chat participants.
 */
public class MessagingActivity extends AppCompatActivity {
    private ActivityMessagingBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;


    private String chatId;              // ID of the current chat
    private boolean isNewChat = false;  // Determines if this is a new or existing chat
    private List<User> userList;        // Users participating in this chat (new chat scenario)
    private final List<Message> messageList = new ArrayList<>();
    private MessagesAdapter messagesAdapter;    // Adapter for the RecyclerView
    private ListenerRegistration messagesListener;  // Firestore listener for real-time updates


    // ----------------------------- Lifecycle Methods -----------------------------

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

        if (!isNewChat && chatId != null) { listenForMessages(); }
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

    // ----------------------------- Initialization -----------------------------

    /**
     * Initializes components like Firestore, PreferenceManager, RecyclerView, and adapter.
     */
    private void initializeComponents() {
        database = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        messagesAdapter = new MessagesAdapter(messageList, this);
        binding.messagesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerview.setAdapter(messagesAdapter);

        // Start by showing an empty state (no messages)
        toggleEmptyState(true);
    }

    /**
     * Checks the intent for chat or user data and initializes the chat accordingly.
     */
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_ID)) {
            // Existing chat scenario
            isNewChat = false;
            chatId = intent.getStringExtra(Constants.KEY_ID);
        } else if (intent.hasExtra(CreateChatActivity.KEY_SELECTED_USERS_LIST)) {
            // New chat scenario
            isNewChat = true;
            userList = (List<User>) intent.getSerializableExtra(CreateChatActivity.KEY_SELECTED_USERS_LIST);

            // If there's an initial message (from CreateChatActivity), send it now
            String initialMessage = intent.getStringExtra(CreateChatActivity.KEY_INITIAL_MESSAGE);
            if (initialMessage != null && !initialMessage.isEmpty()) {
                binding.inputMessage.setText(initialMessage);
                handleSendMessage(); // create chat + send message
            }
        } else {
            // No valid extras found
            showErrorAndExit("Invalid chat data. Please try again.");
        }
    }

    /**
     * Sets up UI listeners for buttons and other user interactions without using lambdas.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonSendMessage.setOnClickListener(v -> handleSendMessage());
        binding.buttonShowChatInfo.setOnClickListener(v -> showChatInfo());
        binding.buttonDeleteChat.setOnClickListener(v -> deleteChat());
    }


    // ----------------------------- Messaging and Chat Management -----------------------------


    /**
     * Listens for new messages in the chat and updates the RecyclerView in real-time.
     */
    private void listenForMessages() {
        messagesListener = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .orderBy("sentDate")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        logCriticalError("listenForMessages: Failed to listen for messages.", e);
                        return;
                    }

                    if (snapshots != null) {
                        // Clear the list before re-adding to avoid duplicates
                        messageList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messageList.add(message);
                            }
                        }
                        messagesAdapter.notifyDataSetChanged();
                        binding.messagesRecyclerview.smoothScrollToPosition(messageList.size() - 1);
                        toggleEmptyState(messageList.isEmpty());
                    }
                });
    }

    /**
     * Handles the "Send" button click to send a message in the chat.
     */
    private void handleSendMessage() {
        String messageContent = binding.inputMessage.getText().toString().trim();
        if (messageContent.isEmpty()) {
            Utilities.showToast(this, "Message content cannot be empty.", Utilities.ToastType.WARNING);
            return;
        }

        binding.buttonSendMessage.setEnabled(false);

        if (isNewChat) {
            createChatWithInitialMessage(messageContent);
        } else {
            sendMessage(messageContent);
        }

        binding.inputMessage.setText(null);
        binding.buttonSendMessage.setEnabled(true);
    }

    /**
     * Creates a new chat in Firestore and sends the initial message.
     *
     * @param initialMessage The content of the initial message.
     */
    private void createChatWithInitialMessage(final String initialMessage) {
        List<String> userIds = new ArrayList<>();
        for (User user : userList) {
            userIds.add(user.id);
        }
        // Include the current user
        userIds.add(preferenceManager.getString(Constants.KEY_ID, ""));

        // Generate a new chat ID
        final String newChatId = database.collection(Constants.KEY_COLLECTION_CHATS).document().getId();
        final Chat chat = new Chat(newChatId, preferenceManager.getString(Constants.KEY_ID, ""), userIds, "");

        // Create the new chat in Firestore
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(newChatId)
                .set(chat)
                .addOnSuccessListener(unused -> {
                    chatId = newChatId;
                    isNewChat = false;
                    updateChatIdsForUsers(chat.userIdList);
                    sendMessage(initialMessage);
                    listenForMessages(); // Now that we have a valid chat, listen for messages
                })
                .addOnFailureListener(e -> {
                    logCriticalError("createChatWithInitialMessage: Failed to create chat in Firestore.", e);
                    Utilities.showToast(MessagingActivity.this, "Failed to create chat. Please try again.", Utilities.ToastType.ERROR);
                });
    }


    /**
     * Sends a message in the current chat.
     * @param messageContent The content of the message to send.
     */
    private void sendMessage(final String messageContent) {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to send message. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        String senderId = preferenceManager.getString(Constants.KEY_ID, "");

        // Generate a new message ID
        final String messageId = database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .document()
                .getId();

        // Create the Message object
        final Message message = new Message(messageId, chatId, senderId, messageContent);

        // Save the message to Firestore
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .document(messageId)
                .set(message)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateRecentMessage(message);
                    } else {
                        logCriticalError("sendMessage: Failed to send message.", task.getException());
                        Utilities.showToast(MessagingActivity.this, "Failed to send message. Please try again.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Updates the most recent message in Firestore for the chat.
     * @param message The recent message to update.
     */
    private void updateRecentMessage(final Message message) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update("recentMessageId", message.id);
    }

    /**
     * Updates the `chatIds` field for all users in the chat.
     * @param userIds List of user IDs participating in the chat.
     */
    private void updateChatIdsForUsers(List<String> userIds) {
        for (final String userId : userIds) {
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId)
                    .update("chatIds", FieldValue.arrayUnion(chatId))
                    .addOnFailureListener(e -> logCriticalError("updateChatIdsForUsers: Failed to update chatIds for user: " + userId, e));
        }
    }

    /**
     * Deletes the current chat if the user is the creator.
     */
    private void deleteChat() {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to delete chat. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .get()
                .addOnSuccessListener(chatDocument -> {
                    if (chatDocument.exists()) {
                        Chat chat = chatDocument.toObject(Chat.class);
                        if (chat != null && chat.creatorId.equals(preferenceManager.getString(Constants.KEY_ID, ""))) {
                            deleteChatFromFirestore(chat);
                        } else {
                            Utilities.showToast(MessagingActivity.this, "You do not have permission to delete this chat.", Utilities.ToastType.ERROR);
                        }
                    } else {
                        Utilities.showToast(MessagingActivity.this, "Chat does not exist.", Utilities.ToastType.ERROR);
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    logCriticalError("deleteChat: Failed to verify chat deletion permissions.", e);
                    Utilities.showToast(MessagingActivity.this, "An error occurred. Please try again.", Utilities.ToastType.ERROR);
                });
    }

    /**
     * Deletes the chat from Firestore and updates user records accordingly.
     *
     * @param chat The chat object to delete.
     */
    private void deleteChatFromFirestore(final Chat chat) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .delete()
                .addOnSuccessListener(unused -> {
                    for (final String userId : chat.userIdList) {
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .update("chatIds", FieldValue.arrayRemove(chatId));
                    }
                    Utilities.showToast(MessagingActivity.this, "Chat deleted successfully", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> {
                    logCriticalError("deleteChatFromFirestore: Failed to delete chat.", e);
                    Utilities.showToast(MessagingActivity.this, "Failed to delete chat. Please try again.", Utilities.ToastType.ERROR);
                });
    }


    // ----------------------------- Chat Info and Helpers -----------------------------


    /**
     * Retrieves and displays chat information, including participants and the creator.
     */
    private void showChatInfo() {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to load chat information. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        fetchChatDetails(chatId, chat -> {
            if (chat == null) {
                Utilities.showToast(MessagingActivity.this, "Chat information is unavailable.", Utilities.ToastType.ERROR);
                return;
            }
            fetchParticipantDetails(chat.userIdList, participantDetails -> {
                String chatInfo = formatChatInfo(chat, participantDetails);
                displayChatInfoDialog(chatInfo);
            });
        });
    }

    /**
     * Fetches the chat details from Firestore.
     *
     * @param chatId   The ID of the chat to fetch.
     * @param callback A callback to handle the fetched chat.
     */
    private void fetchChatDetails(String chatId, final OnChatFetchedCallback callback) {
        binding.progressBar.setVisibility(View.VISIBLE);
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .get()
                .addOnSuccessListener(chatDocument -> {
                    if (chatDocument.exists()) {
                        Chat chat = chatDocument.toObject(Chat.class);
                        callback.onChatFetched(chat);
                    } else {
                        callback.onChatFetched(null);
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    logCriticalError("fetchChatDetails: Failed to load chat information.", e);
                    callback.onChatFetched(null);
                });
    }

    /**
     * Fetches participant details from Firestore and returns them in a callback.
     *
     * @param userIdList The list of participant IDs in this chat.
     * @param callback   A callback to handle the fetched participant details.
     */
    private void fetchParticipantDetails(final List<String> userIdList, final OnDetailsFetchedCallback callback) {
        binding.progressBar.setVisibility(View.VISIBLE);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereIn("id", userIdList)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> participantDetails = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            String contactInfo = !user.phone.isEmpty()
                                    ? user.phone
                                    : !user.email.isEmpty()
                                    ? user.email
                                    : "No contact info";

                            participantDetails.add(
                                    user.firstName + " " + user.lastName + " (" + contactInfo + ")"
                            );
                        }
                    }
                    callback.onDetailsFetched(participantDetails);
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    logCriticalError("fetchParticipantDetails: Failed to load participant details.", e);
                    callback.onDetailsFetched(null);
                });
    }

    /**
     * Formats the chat and participant details into a readable string.
     *
     * @param chat               The chat object with creation date, creator, etc.
     * @param participantDetails The participant details retrieved.
     * @return A single string containing all relevant chat info.
     */
    private String formatChatInfo(Chat chat, List<String> participantDetails) {
        String creatorId = preferenceManager.getString(Constants.KEY_ID, "");
        String creatorName = chat.creatorId.equals(creatorId) ? "You" : chat.creatorId;

        return "Created At: " + (chat.createdDate != null ? chat.createdDate.toString() : "N/A") + "\n" +
                "Creator: " + creatorName + "\n\n" +
                "Participants:\n" + String.join("\n", participantDetails);
    }

    /**
     * Displays the chat information in an AlertDialog.
     *
     * @param chatInfo The formatted chat information string.
     */
    private void displayChatInfoDialog(String chatInfo) {
        new AlertDialog.Builder(this)
                .setTitle("Chat Information")
                .setMessage(chatInfo)
                .setPositiveButton("OK", null)
                .show();
    }


    // ----------------------------- Utility -----------------------------


    /**
     * Toggles the empty state message visibility based on the RecyclerView content.
     *
     * @param isEmpty True if the RecyclerView has no messages.
     */
    private void toggleEmptyState(boolean isEmpty) {
        binding.textProgressMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.messagesRecyclerview.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Logs critical errors for developer debugging.
     *
     * @param message The error message to log.
     * @param e       The exception that occurred.
     */
    private void logCriticalError(String message, Exception e) {
        android.util.Log.e("CHAT_ACTIVITY", message, e);
    }

    /**
     * Shows an error message and exits the activity.
     * @param message The error message to display.
     */
    private void showErrorAndExit(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        finish();
    }


    // ----------------------------- Callback Interfaces -----------------------------


    /**
     * Callback interface for handling fetched chat details.
     */
    private interface OnChatFetchedCallback {
        void onChatFetched(Chat chat);
    }

    /**
     * Callback interface for handling fetched participant details.
     */
    private interface OnDetailsFetchedCallback {
        void onDetailsFetched(List<String> participantDetails);
    }
}