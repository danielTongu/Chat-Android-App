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
import com.example.chatandroidapp.module.ChatMessage;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * The ChatActivity class manages real-time chat functionality between two users.
 * It connects to Firebase Firestore to send and receive messages,
 * updates the UI in real-time, and maintains chat history.
 *
 * @author Daniel Tongu
 */
public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding; // View binding for activity_chat.xml
    private User receiverUser; // Holds information about the user receiving the messages
    private List<ChatMessage> chatMessages; // List to store chat messages
    private ChatAdapter chatAdapter; // Adapter for managing chat data in RecyclerView
    private PreferenceManager preferenceManager; // SharedPreferences manager for storing app preferences
    private FirebaseFirestore database; // Firebase Firestore instance for database operations

    /**
     * EventListener to handle real-time updates from Firestore.
     * Listens for added chat messages and updates the UI.
     */
    private final EventListener<QuerySnapshot> eventListener = (((value, error) -> {
        if (error != null) {
            Log.e("ChatActivity", "Error listening for messages", error);
            return;
        }
        if (value != null) {
            int count = chatMessages.size();

            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    // Parse new messages added to the chat collection
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.senderFirstName = documentChange.getDocument().getString(Constants.KEY_SENDER_FIRST_NAME);
                    chatMessage.senderLastName = documentChange.getDocument().getString(Constants.KEY_SENDER_LAST_NAME);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessages.add(chatMessage);
                }
            }

            // Sort messages based on their timestamp
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));

            // Notify adapter to update UI
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE); // Display chat messages
        }
        binding.progressBar.setVisibility(View.GONE); // Hide loading indicator
    }));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        loadReceiverDetails();// Load details of the receiver
        setListeners();// Set UI event listeners
        init();// Initialize chat components

        ListenMessage();// Start listening for incoming messages
    }

    /**
     * Initializes necessary components, including PreferenceManager,
     * ChatAdapter, and FirebaseFirestore instance.
     */
    private void init() {
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image), // Get the receiver's profile image
                preferenceManager.getString(Constants.KEY_USER_ID) // Current user ID
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance(); // Initialize Firestore
    }

    /**
     * Sends a chat message to Firestore. Resets the input field after sending.
     */
    private void sendMessages() {
        // Retrieve the message from the input field
        String inputMessage = binding.inputMessage.getText().toString().trim();

        // Check if the message is empty
        if (inputMessage.isEmpty()) {
            Utilities.showToast(this, "Cannot send an empty message", Utilities.ToastType.WARNING);
            return;
        }
        // Prepare message data to send
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_FIRST_NAME, preferenceManager.getString(Constants.KEY_FIRST_NAME));
        message.put(Constants.KEY_SENDER_LAST_NAME, preferenceManager.getString(Constants.KEY_LAST_NAME));
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, inputMessage);
        message.put(Constants.KEY_TIMESTAMP, new Date());

        // Add the message to the Firestore chat collection
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        binding.inputMessage.setText(null);
    }

    /**
     * Listens for real-time updates to chat messages from Firestore.
     * Adds a snapshot listener to both sender and receiver chat queries.
     */
    private void ListenMessage() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    /**
     * Decodes a Base64-encoded string into a Bitmap image.
     * @param encodedImage The Base64-encoded image string.
     * @return A Bitmap representation of the decoded image.
     */
    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Loads the receiver's details (first name, last name, and profile image)
     * from the intent and displays them in the toolbar.
     */
    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        assert receiverUser != null; // Ensure the user is not null
        binding.textName.setText(String.format("%s %s", receiverUser.firstName, receiverUser.lastName)); // Display full name
    }

    /**
     * Sets up UI event listeners for buttons and other interactive components.
     */
    private void setListeners() {
        // Navigate back to the previous screen
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        // Send a message when the send button is clicked
        binding.layoutSend.setOnClickListener(v -> sendMessages());
    }

    /**
     * Converts a Date object into a readable string format.
     * @param date The Date object to format.
     * @return A formatted date string (e.g., "MM dd yyyy - hh:mm").
     */
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MM dd yyyy - hh:mm", Locale.getDefault()).format(date);
    }
}