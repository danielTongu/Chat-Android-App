// ChatsFragment.java
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
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ChatsFragment manages and displays a list of chats the user is involved in.
 * It supports real-time updates to show recent messages and allows users to open individual chats.
 */
public class ChatsFragment extends Fragment {

    private static final String TAG = "ChatsFragment";

    private FragmentChatsBinding binding;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private List<Chat> chatsList;
    private ChatsAdapter chatsAdapter;

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        chatsAdapter = new ChatsAdapter(chatsList, new ChatsAdapter.ChatClickListener() {
            @Override
            public void onChatClicked(Chat chat) {
                openChat(chat);
            }
        });

        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChats.setAdapter(chatsAdapter);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.processMessage.setVisibility(View.GONE);
    }

    /**
     * Loads chats from Firestore where the user is a participant.
     */
    private void loadChats() {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        Log.d(TAG, "Loading chats for user ID: " + userId);

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .whereArrayContains(Constants.KEY_USER_ID_LIST, userId)
                .addSnapshotListener(new ChatEventListener());
    }

    /**
     * Opens the selected chat by navigating to ChatActivity.
     *
     * @param chat The selected chat object.
     */
    private void openChat(Chat chat) {
        Log.d(TAG, "Opening chat with ID: " + chat.id);

        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_ID, chat.id);
        startActivity(intent);
    }

    /**
     * Sets listeners for UI components, such as the Floating Action Button.
     */
    private void setListeners() {
        binding.fabNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "FAB clicked. Navigating to ChatCreatorActivity.");
                Intent intent = new Intent(getContext(), ChatCreatorActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Cleans up resources when the fragment is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Event listener for handling real-time updates to the chat list from Firestore.
     */
    private class ChatEventListener implements EventListener<QuerySnapshot> {
        @Override
        public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
            if (error != null) {
                Log.e(TAG, "Error loading chats", error);
                binding.progressBar.setVisibility(View.GONE);
                binding.processMessage.setVisibility(View.VISIBLE);
                binding.processMessage.setText("Failed to load chats. Check your connection.");
                return;
            }

            if (snapshots != null) {
                for (DocumentChange change : snapshots.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.ADDED) {
                        Chat chat = change.getDocument().toObject(Chat.class);
                        chat.id = change.getDocument().getId(); // Assign Firestore document ID
                        Log.d(TAG, "New chat added: " + chat.id);
                        chatsList.add(chat);
                    } else if (change.getType() == DocumentChange.Type.MODIFIED) {
                        Chat updatedChat = change.getDocument().toObject(Chat.class);
                        updatedChat.id = change.getDocument().getId(); // Assign Firestore document ID
                        for (int i = 0; i < chatsList.size(); i++) {
                            if (chatsList.get(i).id.equals(updatedChat.id)) {
                                chatsList.set(i, updatedChat);
                                break;
                            }
                        }
                        Log.d(TAG, "Chat modified: " + updatedChat.id);
                    } else if (change.getType() == DocumentChange.Type.REMOVED) {
                        String removedChatId = change.getDocument().getId();
                        chatsList.removeIf(chat -> chat.id.equals(removedChatId));
                        Log.d(TAG, "Chat removed: " + removedChatId);
                    }
                }

                // Sort chats by the most recent message timestamp (descending)
                Collections.sort(chatsList, (c1, c2) -> {
                    if (c1.createdDate == null && c2.createdDate == null) return 0;
                    if (c1.createdDate == null) return 1;
                    if (c2.createdDate == null) return -1;
                    return c2.createdDate.compareTo(c1.createdDate);
                });

                if (!chatsList.isEmpty()) {
                    binding.processMessage.setVisibility(View.GONE);
                    binding.recyclerViewChats.setVisibility(View.VISIBLE);
                } else {
                    binding.processMessage.setVisibility(View.VISIBLE);
                    binding.processMessage.setText("No chats yet.");
                }

                chatsAdapter.notifyDataSetChanged();
            }

            binding.progressBar.setVisibility(View.GONE);
        }
    }
}