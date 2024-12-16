package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.adapters.MessagesAdapter;
import com.example.chatandroidapp.databinding.ActivityChatBinding;
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
 * ChatActivity manages real-time messaging between users.
 * It handles sending, receiving, and deleting messages in a chat, as well as
 * displaying chat information and managing chat participants.
 */
public class ChatActivity extends AppCompatActivity {

    // ----------------------------- Variables -----------------------------
    private ActivityChatBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;

    private String chatId; // ID of the current chat
    private boolean isNewChat = false; // Determines if this is a new or existing chat
    private List<User> selectedUsers; // Selected users for new chats
    private List<Message> messagesList; // List of messages in the chat
    private MessagesAdapter messagesAdapter; // Adapter for the RecyclerView
    private ListenerRegistration messagesListener; // Firestore listener for real-time updates

    // ----------------------------- Lifecycle Methods -----------------------------

    /**
     * Initializes the activity, sets up listeners, and begins listening for messages if applicable.
     *
     * @param savedInstanceState The saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        checkIntentData();
        setListeners();

        if (!isNewChat && chatId != null) {
            listenForMessages();
        }
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

        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(messagesList, this);
        binding.messagesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.messagesRecyclerview.setAdapter(messagesAdapter);

        toggleEmptyState(true); // Start with the empty state visible
    }

    /**
     * Checks the intent for chat or user data and initializes the chat accordingly.
     */
    private void checkIntentData() {
        Intent intent = getIntent();
        if (intent.hasExtra(Constants.KEY_ID)) {
            isNewChat = false;
            chatId = intent.getStringExtra(Constants.KEY_ID);
        } else if (intent.hasExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST)) {
            isNewChat = true;
            selectedUsers = (List<User>) intent.getSerializableExtra(ChatCreatorActivity.KEY_SELECTED_USERS_LIST);
            String initialMessage = intent.getStringExtra(ChatCreatorActivity.KEY_INITIAL_MESSAGE);
            if (initialMessage != null && !initialMessage.isEmpty()) {
                binding.inputMessage.setText(initialMessage);
                handleSendMessage();
            }
        } else {
            showErrorAndExit("Invalid chat data. Please try again.");
        }
    }

    // ----------------------------- Listener Setup -----------------------------

    /**
     * Sets up UI listeners for buttons and other user interactions.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonSendMessage.setOnClickListener(v -> handleSendMessage());
        binding.buttonShowChatInfo.setOnClickListener(v -> showChatInfo());
        binding.buttonDeleteChat.setOnClickListener(v -> deleteChat());
    }

    // ----------------------------- Messaging and Chat Management -----------------------------

    /**
     * Listens for new messages in the chat and updates the RecyclerView.
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
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Message message = doc.toObject(Message.class);
                            if (message != null) {
                                messagesList.add(message);
                                messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                                binding.messagesRecyclerview.smoothScrollToPosition(messagesList.size() - 1);
                            }
                        }
                        toggleEmptyState(messagesList.isEmpty());
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
     * Sends a message in the current chat.
     *
     * @param messageContent The content of the message to send.
     */
    private void sendMessage(String messageContent) {
        if (chatId == null) {
            Utilities.showToast(this, "Unable to send message. Please try again.", Utilities.ToastType.ERROR);
            return;
        }

        String senderId = preferenceManager.getString(Constants.KEY_ID, "");
        Message message = new Message(null, chatId, senderId, messageContent);

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.KEY_COLLECTION_MESSAGES)
                .add(message)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateRecentMessage(message);
                        Utilities.showToast(ChatActivity.this, "", Utilities.ToastType.SUCCESS);
                    } else {
                        logCriticalError("Failed to send message.", task.getException());
                        Utilities.showToast(ChatActivity.this, "Failed to send message. Please try again.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Updates the most recent message in Firestore for the chat.
     *
     * @param message The recent message to update.
     */
    private void updateRecentMessage(Message message) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .update("recentMessageId", message.id);
    }

    /**
     * Creates a new chat in Firestore and sends the initial message.
     *
     * @param initialMessage The content of the initial message.
     */
    private void createChatWithInitialMessage(String initialMessage) {
        List<String> userIds = new ArrayList<>();
        for (User user : selectedUsers) {
            userIds.add(user.id);
        }
        userIds.add(preferenceManager.getString(Constants.KEY_ID, ""));

        String newChatId = database.collection(Constants.KEY_COLLECTION_CHATS).document().getId();

        Chat chat = new Chat(newChatId, preferenceManager.getString(Constants.KEY_ID, ""), userIds, "");

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(newChatId)
                .set(chat)
                .addOnSuccessListener(unused -> {
                    chatId = newChatId;
                    isNewChat = false;
                    updateChatIdsForUsers(userIds);
                    sendMessage(initialMessage);
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to create chat in Firestore.", e);
                    Utilities.showToast(ChatActivity.this, "Failed to create chat. Please try again.", Utilities.ToastType.ERROR);
                });
    }

    /**
     * Updates the `chatIds` field for all users in the chat.
     *
     * @param userIds List of user IDs participating in the chat.
     */
    private void updateChatIdsForUsers(List<String> userIds) {
        for (String userId : userIds) {
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId)
                    .update("chatIds", FieldValue.arrayUnion(chatId))
                    .addOnFailureListener(e -> logCriticalError("Failed to update chatIds for user: " + userId, e));
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
                            Utilities.showToast(ChatActivity.this, "You do not have permission to delete this chat.", Utilities.ToastType.ERROR);
                        }
                    } else {
                        Utilities.showToast(ChatActivity.this, "Chat does not exist.", Utilities.ToastType.ERROR);
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    logCriticalError("Failed to verify chat deletion permissions.", e);
                    Utilities.showToast(ChatActivity.this, "An error occurred. Please try again.", Utilities.ToastType.ERROR);
                });

    }

    /**
     * Deletes the chat from Firestore and updates user records.
     *
     * @param chat The chat object to delete.
     */
    private void deleteChatFromFirestore(Chat chat) {
        database.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .delete()
                .addOnSuccessListener(unused -> {
                    for (String userId : chat.userIdList) {
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .document(userId)
                                .update("chatIds", FieldValue.arrayRemove(chatId));
                    }
                    Utilities.showToast(ChatActivity.this, "", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to delete chat.", e);
                    Utilities.showToast(ChatActivity.this, "Failed to delete chat. Please try again.", Utilities.ToastType.ERROR);
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
                Utilities.showToast(ChatActivity.this, "Chat information is unavailable.", Utilities.ToastType.ERROR);
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
     * @param callback A callback to handle the fetched chat details.
     */
    private void fetchChatDetails(String chatId, OnChatFetchedCallback callback) {
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
                    logCriticalError("Failed to load chat information.", e);
                    callback.onChatFetched(null);
                });
    }

    /**
     * Fetches and formats participant details from Firestore.
     *
     * @param userIdList The list of user IDs to fetch details for.
     * @param callback   A callback to handle the formatted participant details.
     */
    private void fetchParticipantDetails(List<String> userIdList, OnDetailsFetchedCallback callback) {
        binding.progressBar.setVisibility(View.VISIBLE);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereIn("id", userIdList)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> participantDetails = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            String contactInfo = user.phone != null && !user.phone.isEmpty()
                                    ? user.phone
                                    : user.email != null ? user.email : "No contact info available";
                            participantDetails.add(user.firstName + " " + user.lastName + " (" + contactInfo + ")");
                        }
                    }
                    callback.onDetailsFetched(participantDetails);
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    logCriticalError("Failed to load participant details.", e);
                    callback.onDetailsFetched(null);
                });
    }

    /**
     * Formats the chat and participant details into a readable string.
     *
     * @param chat               The chat object.
     * @param participantDetails The formatted participant details.
     * @return A string containing the formatted chat information.
     */
    private String formatChatInfo(Chat chat, List<String> participantDetails) {
        String creator = chat.creatorId.equals(preferenceManager.getString(Constants.KEY_ID, "")) ? "You" : chat.creatorId;
        return new StringBuilder()
                .append("Created At: ").append(chat.createdDate).append("\n")
                .append("Creator: ").append(creator).append("\n\n")
                .append("Participants:\n").append(String.join("\n", participantDetails))
                .toString();
    }

    /**
     * Displays the chat information in a dialog.
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
     * @param isEmpty True if the RecyclerView has no content.
     */
    private void toggleEmptyState(boolean isEmpty) {
        binding.processMessage.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.messagesRecyclerview.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Logs critical errors for developer debugging.
     *
     * @param message The error message to log.
     * @param e       The exception that occurred.
     */
    private void logCriticalError(String message, Exception e) {
        android.util.Log.e("ChatActivity", message, e);
    }

    /**
     * Shows an error message and exits the activity.
     *
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