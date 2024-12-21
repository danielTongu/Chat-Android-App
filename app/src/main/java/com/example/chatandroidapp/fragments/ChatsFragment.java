package com.example.chatandroidapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatandroidapp.activities.CreateChatActivity;
import com.example.chatandroidapp.adapters.ChatsAdapter;
import com.example.chatandroidapp.databinding.FragmentChatsBinding;
import com.example.chatandroidapp.models.Chat;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatsFragment display a list of chat previews in a RecyclerView.
 * Users can tap on a chat to open it in MessagingActivity or
 * click on a FloatingActionButton (FAB) to proceed to a ChatCreatorActivity to start a new chat.
 */
public class ChatsFragment extends Fragment {

    /** Binding for fragment_chats.xml layout. */
    private FragmentChatsBinding binding;

    /** Firestore reference for real-time chat updates. */
    private FirebaseFirestore firestore;
    /** Adapter for displaying chat previews. */

    private ChatsAdapter chatsAdapter;

    /** List of Chat objects shown in the RecyclerView. */
    private final List<Chat> chatList = new ArrayList<>();

    /** Real-time listener registration for removing snapshot listener on cleanup. */
    private ListenerRegistration chatListenerRegistration;

    /** Used for filtering chats for the current user if desired. */
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

        initRecyclerView();
        listenForChats();
        setListeners();
    }

    /**
     * Cleans up resources (like snapshot listeners) when the fragment's view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }
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
     * Sets up a snapshot listener to Firestore's "chats" collection. Whenever a chat is added,
     * updated, or removed, the UI will reflect it in real-time.
     * We filter out chats the current user is not a participant of by checking chat.userIdList.
     */
    private void listenForChats() {
        binding.progressBar.setVisibility(View.VISIBLE);

        final String currentUserId = preferenceManager.getString(Constants.KEY_ID, "");

        // Listen for changes in all chats
        chatListenerRegistration = firestore.collection(Constants.KEY_COLLECTION_CHATS)
                .addSnapshotListener((snapshots, error) -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        // Log or handle the error. For now, we just show empty state.
                        showEmptyState(true);
                        return;
                    }
                    if (snapshots == null) {
                        showEmptyState(true);
                        return;
                    }

                    chatList.clear();
                    // Iterate over all Chat documents
                    for (QueryDocumentSnapshot snapshot : snapshots) {
                        Chat chat = snapshot.toObject(Chat.class);
                        if (chat != null && chat.userIdList != null) {
                            if (chat.userIdList.contains(currentUserId)) {
                                chatList.add(chat);
                            }
                        }
                    }
                    if (chatList.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        chatsAdapter.notifyDataSetChanged();
                        binding.recyclerViewChats.smoothScrollToPosition(chatList.size() - 1);
                    }
                });
    }

    /**
     * Configures click listeners for UI elements, such as the FloatingActionButton that starts
     * a new chat in MessagingActivity.
     */
    private void setListeners() {
        binding.fabNewChat.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), CreateChatActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Shows or hides the "No chats available" state depending on whether there are chats in the list.
     *
     * @param isEmpty True if chatList is empty, otherwise false.
     */
    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.textProgressMessage.setVisibility(View.VISIBLE);
            binding.recyclerViewChats.setVisibility(View.GONE);
        } else {
            binding.textProgressMessage.setVisibility(View.GONE);
            binding.recyclerViewChats.setVisibility(View.VISIBLE);
        }
    }
}