package com.example.chatandroidapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.ChatCreatorActivity;
import com.example.chatandroidapp.adapters.ChatsAdapter;
import com.example.chatandroidapp.databinding.FragmentChatsBinding;
import com.example.chatandroidapp.models.Chat;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ChatsFragment displays a list of chat previews in a RecyclerView.
 * Users can tap on a chat to open it in MessagingActivity or
 * click on a FloatingActionButton (FAB) to proceed to a ChatCreatorActivity to start a new chat.
 */
public class ChatsFragment extends Fragment {
    /** Tag for logging purposes. */
    private static final String TAG = "ChatsFragment";

    /** List of Chat objects shown in the RecyclerView. */
    private final List<Chat> chatList = new ArrayList<>();

    /** Map to keep track of chat listeners, key is chatId. */
    private final Map<String, ListenerRegistration> chatListeners = new HashMap<>();

    /** Binding for fragment_chats.xml layout. */
    private FragmentChatsBinding binding;

    /** Firestore reference for real-time chat updates. */
    private FirebaseFirestore firestore;

    /** Adapter for displaying chat previews. */
    private ChatsAdapter chatsAdapter;

    /** Real-time listener registration for removing snapshot listener on cleanup. */
    private ListenerRegistration userListenerRegistration;

    /** PreferenceManager instance for accessing user preferences. */
    private PreferenceManager preferenceManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called when the view has been created. Initializes Firestore, sets up the RecyclerView,
     * starts listening for chat updates, and configures UI listeners.
     *
     * @param view               The root view of the fragment.
     * @param savedInstanceState The saved instance state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(requireContext());
        showLoading(false, "No chats");

        initRecyclerView();
        listenForUserChatIds();
        setListeners();
    }

    /**
     * Cleans up resources (like snapshot listeners) when the fragment's view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListenerRegistration != null) {
            userListenerRegistration.remove();
        }
        // Remove all chat listeners
        for (Map.Entry<String, ListenerRegistration> entry : chatListeners.entrySet()) {
            entry.getValue().remove();
        }
        chatListeners.clear();
        binding = null;
    }

    /**
     * Initializes the RecyclerView with a linear layout and sets the ChatsAdapter.
     */
    private void initRecyclerView() {
        chatsAdapter = new ChatsAdapter(chatList, requireContext());
        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChats.setAdapter(chatsAdapter);
    }

    /**
     * Sets up a snapshot listener to the current user's document to monitor changes in the "chatIds" array.
     * Whenever "chatIds" changes, update the chat listeners accordingly.
     */
    private void listenForUserChatIds() {

        final String currentUserId = preferenceManager.getString(Constants.KEY_ID, "");

        // Listen to changes in the current user's document
        userListenerRegistration = firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        logCriticalError("Failed to listen for user's chatIds.", error);
                    } else if (snapshot == null || !snapshot.exists()) {
                        Utilities.showToast(getContext(), "User data not found.", Utilities.ToastType.ERROR);
                        showLoading(false, "No chats");
                    } else {
                        List<String> chatIds = (List<String>) snapshot.get("chatIds");

                        if (chatIds == null || chatIds.isEmpty()) {
                            chatList.clear();
                            chatsAdapter.notifyDataSetChanged();
                            removeAllChatListeners();
                            showLoading(false, "No chats");
                        } else {
                            updateChatListeners(new HashSet<>(chatIds), currentUserId);
                        }
                    }
                });
    }

    /**
     * Updates the chat listeners based on the provided set of chatIds.
     * Adds listeners for new chatIds and removes listeners for removed chatIds.
     *
     * @param newChatIds    The updated set of chatIds.
     * @param currentUserId The ID of the current user.
     */
    private void updateChatListeners(Set<String> newChatIds, String currentUserId) {
        Set<String> existingChatIds = chatListeners.keySet();

        // Determine which chatIds to add and which to remove
        Set<String> chatIdsToAdd = new HashSet<>(newChatIds);
        chatIdsToAdd.removeAll(existingChatIds);

        Set<String> chatIdsToRemove = new HashSet<>(existingChatIds);
        chatIdsToRemove.removeAll(newChatIds);

        // Remove listeners for removed chatIds
        for (String chatId : chatIdsToRemove) {
            ListenerRegistration registration = chatListeners.get(chatId);
            if (registration != null) {
                registration.remove();
                chatListeners.remove(chatId);
            }
            // Remove the chat from the chatList if it's no longer in chatIds
            chatList.removeIf(chat -> chat.id.equals(chatId));
            chatsAdapter.notifyDataSetChanged();
        }

        // Add listeners for new chatIds
        for (String chatId : chatIdsToAdd) {
            addChatListener(chatId, currentUserId);
        }

        // Optionally, refresh the UI
        if (!chatIdsToAdd.isEmpty() || !chatIdsToRemove.isEmpty()) {
            finalizeChatList();
        }
    }

    /**
     * Finalizes the chat list by updating the UI.
     */
    private void finalizeChatList() {
        if (chatList.isEmpty()) {
            showLoading(false, "No chats");
        } else {
            showLoading(false, null);
            chatsAdapter.notifyDataSetChanged();
            binding.recyclerViewChats.smoothScrollToPosition(chatList.size() - 1);
        }
    }

    /**
     * Adds a snapshot listener for a specific chatId.
     *
     * @param chatId        The ID of the chat to listen to.
     * @param currentUserId The ID of the current user.
     */
    private void addChatListener(String chatId, String currentUserId) {
        ListenerRegistration registration = firestore.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .addSnapshotListener((chatSnapshot, error) -> {
                    if (error != null) {
                        logCriticalError("Failed to listen for chatId: " + chatId, error);
                    } else if (chatSnapshot == null || !chatSnapshot.exists()) {
                        removeChatIdFromUser(chatId, currentUserId);
                        Utilities.showToast(getContext(), "Removed non-existing chat.", Utilities.ToastType.INFO);
                        chatList.removeIf(chat -> chat.id.equals(chatId));
                        chatsAdapter.notifyDataSetChanged();
                    } else {
                        Chat chat = chatSnapshot.toObject(Chat.class);

                        if (chat == null) {
                            removeChatIdFromUser(chatId, currentUserId);
                            Utilities.showToast(getContext(), "Removed invalid chat.", Utilities.ToastType.INFO);
                            chatList.removeIf(c -> c.id.equals(chatId));
                            chatsAdapter.notifyDataSetChanged();
                        } else {
                            verifyCreator(chat, currentUserId);
                        }
                    }
                });

        // Store the listener
        chatListeners.put(chatId, registration);
        Log.d(TAG, "Added listener for chatId: " + chatId);
    }

    /**
     * Verifies that the creator of the given chat exists in the users collection.
     * If the creator does not exist, deletes the chat and removes the chatId from user's chatIds.
     *
     * @param chat          The Chat object to verify.
     * @param currentUserId The ID of the current user.
     */
    private void verifyCreator(Chat chat, String currentUserId) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(chat.creatorId)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        // Creator exists, add or update chat in chatList
                        addOrUpdateChat(chat);
                    } else {
                        // Creator does not exist, delete chat and remove chatId
                        deleteChatAndRemoveId(chat.id, currentUserId);
                        Utilities.showToast(getContext(), "Removed chat due to missing creator.", Utilities.ToastType.WARNING);
                        // Remove from chatList if present
                        chatList.removeIf(c -> c.id.equals(chat.id));
                        chatsAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to verify creator for chatId: " + chat.id, e);
                });
    }

    /**
     * Adds a new chat to the chatList or updates it if it already exists.
     *
     * @param chat The Chat object to add or update.
     */
    private void addOrUpdateChat(Chat chat) {
        // Check if the chat already exists in the list
        int index = -1;
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).id.equals(chat.id)) {
                index = i;
                break;
            }
        }

        if (index != -1) { // Update existing chat
            chatList.set(index, chat);
        } else { // Add new chat
            chatList.add(chat);
        }

        // Notify the adapter
        chatsAdapter.notifyDataSetChanged();
        finalizeChatList();
    }

    /**
     * Deletes a chat from Firestore and removes its chatId from the user's chatIds array.
     *
     * @param chatId        The ID of the chat to delete.
     * @param currentUserId The ID of the current user.
     */
    private void deleteChatAndRemoveId(String chatId, String currentUserId) {
        firestore.collection(Constants.KEY_COLLECTION_CHATS)
                .document(chatId)
                .delete()
                .addOnSuccessListener(unused -> removeChatIdFromUser(chatId, currentUserId))
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to delete chatId: " + chatId, e);
                });
    }

    /**
     * Removes a single chat ID from the current user's chatIds array in their user document.
     *
     * @param chatId        The ID of the chat to remove.
     * @param currentUserId The ID of the current user.
     */
    private void removeChatIdFromUser(String chatId, String currentUserId) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .update("chatIds", FieldValue.arrayRemove(chatId))
                .addOnFailureListener(e -> {
                    logCriticalError("Failed to remove chatId: " + chatId + " from user's chatIds.", e);
                });
    }

    /**
     * Removes all existing chat listeners.
     */
    private void removeAllChatListeners() {
        for (Map.Entry<String, ListenerRegistration> entry : chatListeners.entrySet()) {
            entry.getValue().remove();
            Log.d(TAG, "Removed listener for chatId: " + entry.getKey());
        }
        chatListeners.clear();
    }

    /**
     * Configures click listeners for UI elements, such as the FloatingActionButton that starts
     * a new chat in InitiateChatActivity.
     */
    private void setListeners() {
        binding.fabNewChat.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), ChatCreatorActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Logs critical errors for debugging purposes and shows a toast to the user.
     *
     * @param message The error message.
     * @param e       The exception causing the error.
     */
    private void logCriticalError(String message, Exception e) {
        showLoading(false, null);
        Utilities.showToast(getContext(), message != null ? message : "An error occurred.", Utilities.ToastType.ERROR);
        Log.e(TAG, message, e);
    }

    /**
     * Displays or hides the loading UI.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display during loading.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (message != null) {
            binding.textProgressMessage.setVisibility(View.VISIBLE);
            binding.textProgressMessage.setText(message);
        } else {
            binding.textProgressMessage.setVisibility(View.GONE);
        }
    }
}