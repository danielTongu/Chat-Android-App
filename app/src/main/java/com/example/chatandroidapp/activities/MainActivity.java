package com.example.chatandroidapp.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivityMainBinding;
import com.example.chatandroidapp.interfaces.SearchableFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setUpBottomNavigation();
        setUpSearchView();
    }

    private void setUpBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.navigation_profile) {
                //selectedFragment = new ProfileFragment();
            } else if (item.getItemId() == R.id.navigation_chats) {
                //selectedFragment = new ChatsFragment();
            } else if (item.getItemId() == R.id.navigation_contacts) {
                //selectedFragment = new ContactsFragment();
            } else if (item.getItemId() == R.id.navigation_tasks) {
                //selectedFragment = new TasksFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });

        // Set default fragment
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_chats);
    }

    private void loadFragment(Fragment fragment) {
        // Show ProgressBar
        binding.progressBar.setVisibility(View.VISIBLE);

        fragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();

        activeFragment = fragment;

        // Simulate fragment load time
        binding.navHostFragment.postDelayed(() -> {
            // Hide ProgressBar after fragment is loaded
            binding.progressBar.setVisibility(View.GONE);
        }, 300); // Simulate 300ms delay
    }

    private void setUpSearchView() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(query);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (activeFragment instanceof SearchableFragment) {
                    ((SearchableFragment) activeFragment).filterData(newText);
                }
                return false;
            }
        });
    }
}