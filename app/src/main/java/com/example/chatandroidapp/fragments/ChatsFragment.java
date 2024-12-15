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

import com.example.chatandroidapp.activities.ChatActivity;
import com.example.chatandroidapp.activities.ChatCreatorActivity;
import com.example.chatandroidapp.adapters.ChatsAdapter;
import com.example.chatandroidapp.databinding.FragmentChatsBinding;
import com.example.chatandroidapp.module.Chat;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChatsFragment manages and displays a list of chats the user is involved in.
 * It fetches data from Firestore, displays recent messages, and allows navigation to individual chats.
 */
public class ChatsFragment extends Fragment {
    private static final String TAG = "CHATS_FRAGMENT";

    private FragmentChatsBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private ChatsAdapter chatsAdapter;

    private List<Chat> chatsList;
    private List<ListenerRegistration> listenerRegistrations;

    /**
     * Inflates the fragment layout and initializes required components.
     *
     * @param inflater LayoutInflater to inflate the layout.
     * @param container Container for the fragment.
     * @param savedInstanceState Saved instance state for the fragment.
     * @return The inflated layout's root view.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);

        init();
        loadChats();
        setListeners();

        return binding.getRoot();
    }

    /**
     * Initializes key components such as Firestore, preference manager, RecyclerView, and adapter.
     */
    private void init() {
        preferenceManager = PreferenceManager.getInstance(requireContext());
        database = FirebaseFirestore.getInstance();
        chatsList = new ArrayList<>();
        listenerRegistrations = new ArrayList<>();

        chatsAdapter = new ChatsAdapter(chatsList, this::openChat, listenerRegistrations);
        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChats.setAdapter(chatsAdapter);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.processMessage.setVisibility(View.GONE);
    }

    /**
     * Loads chats from Firestore where the user is a participant.
     * Sorts chats by the most recent activity and updates the RecyclerView.
     */
    private void loadChats() {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");

        ListenerRegistration chatListener = database.collection(Constants.KEY_COLLECTION_CHATS)
                .whereArrayContains("userIdList", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading chats", error);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.processMessage.setVisibility(View.VISIBLE);
                        binding.processMessage.setText("Failed to load chats. Check your connection.");
                        return;
                    }

                    if (snapshots != null) {
                        handleChatChanges(snapshots.getDocumentChanges());
                    }
                });

        listenerRegistrations.add(chatListener);
    }

    /**
     * Handles real-time updates to the chat list from Firestore.
     *
     * @param documentChanges List of document changes received from Firestore.
     */
    private void handleChatChanges(List<DocumentChange> documentChanges) {
        boolean dataChanged = false;

        for (DocumentChange change : documentChanges) {
            Chat chat = change.getDocument().toObject(Chat.class);
            chat.id = change.getDocument().getId();

            switch (change.getType()) {
                case ADDED:
                    chatsList.add(chat);
                    dataChanged = true;
                    break;

                case MODIFIED:
                    for (int i = 0; i < chatsList.size(); i++) {
                        if (chatsList.get(i).id.equals(chat.id)) {
                            chatsList.set(i, chat);
                            dataChanged = true;
                            break;
                        }
                    }
                    break;

                case REMOVED:
                    chatsList.removeIf(existingChat -> existingChat.id.equals(chat.id));
                    dataChanged = true;
                    break;
            }
        }

        if (dataChanged) {
            updateUI();
        }
    }

    /**
     * Updates the UI by sorting and refreshing the chat list.
     */
    private void updateUI() {
        // Sort chats by the most recent activity (createdDate descending)
        Collections.sort(chatsList, (c1, c2) -> {
            if (c1.createdDate == null && c2.createdDate == null) { return 0; }
            if (c1.createdDate == null) { return 1; }
            if (c2.createdDate == null) { return -1; }
            return c2.createdDate.compareTo(c1.createdDate);
        });

        // Update visibility based on the chat list size
        if (chatsList.isEmpty()) {
            binding.processMessage.setVisibility(View.VISIBLE);
            binding.processMessage.setText("No chats yet.");
            binding.recyclerViewChats.setVisibility(View.GONE);
        } else {
            binding.processMessage.setVisibility(View.GONE);
            binding.recyclerViewChats.setVisibility(View.VISIBLE);
        }

        chatsAdapter.notifyDataSetChanged();
        binding.progressBar.setVisibility(View.GONE);
    }

    /**
     * Navigates to ChatActivity when a chat item is clicked.
     *
     * @param chat Chat object containing the chat details.
     */
    private void openChat(Chat chat) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_ID, chat.id);
        startActivity(intent);
    }

    /**
     * Sets listeners for UI components, such as the Floating Action Button.
     */
    private void setListeners() {
        binding.fabNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ChatCreatorActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Removes Firestore listeners and cleans up resources to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration listener : listenerRegistrations) {
            listener.remove();
        }
        listenerRegistrations.clear();
        binding = null;
    }
}