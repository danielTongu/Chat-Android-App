package com.example.chatandroidapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.activities.OtpVerificationActivity;
import com.example.chatandroidapp.activities.SignInActivity;
import com.example.chatandroidapp.databinding.FragmentProfileBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileFragment allows users to view and update their profile details,
 * including phone number, email, password, and profile picture.
 */
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;

    private static final int REQUEST_IMAGE_PICK = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        preferenceManager = PreferenceManager.getInstance(requireContext());
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        loadUserDetails();
        setListeners();
        return binding.getRoot();
    }

    /**
     * Loads user details into the UI from shared preferences.
     */
    private void loadUserDetails() {
        binding.inputFirstName.setText(preferenceManager.getString(Constants.KEY_FIRST_NAME));
        binding.inputLastName.setText(preferenceManager.getString(Constants.KEY_LAST_NAME));
        binding.inputEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        binding.inputPhoneNumber.setText(preferenceManager.getString(Constants.KEY_PHONE));

        String encodedImage = preferenceManager.getString(Constants.KEY_IMAGE);
        if (!TextUtils.isEmpty(encodedImage)) {
            binding.imageProfile.setImageBitmap(User.getBitmapFromEncodedString(encodedImage));
        }
    }

    /**
     * Sets up listeners for profile update actions.
     */
    private void setListeners() {
        binding.layoutImage.setOnClickListener(v -> openImagePicker());

        binding.buttonUpdateProfile.setOnClickListener(v -> updateProfileDetails());

        binding.buttonLogout.setOnClickListener(v -> logOutUser());

        binding.inputPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                initiatePhoneVerification();
            }
        });
    }

    /**
     * Opens the image picker for profile picture update.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), data.getData());
                binding.imageProfile.setImageBitmap(bitmap);

                String encodedImage = Utilities.encodeImage(bitmap);
                updateFirestoreField(Constants.KEY_IMAGE, encodedImage);
            } catch (Exception e) {
                Utilities.showToast(requireContext(), "Failed to set image", Utilities.ToastType.ERROR);
            }
        }
    }

    /**
     * Updates user profile details (name, email, password) in Firestore.
     */
    private void updateProfileDetails() {
        String firstName = binding.inputFirstName.getText().toString().trim();
        String lastName = binding.inputLastName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String newPassword = binding.inputNewPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(email)) {
            Utilities.showToast(requireContext(), "First name, last name, and email are required", Utilities.ToastType.WARNING);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FIRST_NAME, firstName);
        updates.put(Constants.KEY_LAST_NAME, lastName);
        updates.put(Constants.KEY_EMAIL, email);

        if (!TextUtils.isEmpty(newPassword)) {
            if (!newPassword.equals(confirmPassword)) {
                Utilities.showToast(requireContext(), "Passwords do not match", Utilities.ToastType.WARNING);
                return;
            }
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(newPassword).addOnSuccessListener(unused ->
                                Utilities.showToast(requireContext(), "Password updated", Utilities.ToastType.SUCCESS))
                        .addOnFailureListener(e ->
                                Utilities.showToast(requireContext(), "Failed to update password", Utilities.ToastType.ERROR));
            }
        }

        updateFirestore(updates, "Profile updated successfully");
    }

    /**
     * Logs out the user, removes the Firebase token, and clears local data.
     */
    private void logOutUser() {
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null) {
            removeFirebaseTokenFromUser(userId);
        }

        firebaseAuth.signOut();
        preferenceManager.clear();

        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Removes the Firebase token from Firestore.
     *
     * @param userId The user's Firestore document ID.
     */
    private void removeFirebaseTokenFromUser(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, null);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> Log.d("PROFILE_FRAGMENT", "removeFirebaseTokenFromUser: Token removed successfully"))
                .addOnFailureListener(e -> Log.e("PROFILE_FRAGMENT", "removeFirebaseTokenFromUser: Failed to remove token", e));
    }

    /**
     * Initiates phone number verification if changed.
     */
    private void initiatePhoneVerification() {
        String newPhoneNumber = binding.inputPhoneNumber.getText().toString().trim();
        String currentPhoneNumber = preferenceManager.getString(Constants.KEY_PHONE);

        if (!TextUtils.isEmpty(newPhoneNumber) && !newPhoneNumber.equals(currentPhoneNumber)) {
            Intent intent = new Intent(requireContext(), OtpVerificationActivity.class);
            intent.putExtra(Constants.KEY_PHONE, newPhoneNumber);
            intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_UPDATE_PHONE);
            startActivity(intent);
        }
    }

    /**
     * Updates a specific field in Firestore and preferences.
     */
    private void updateFirestoreField(String key, String value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(key, value);

        updateFirestore(updates, "Profile updated successfully");
        preferenceManager.putString(key, value);
    }

    /**
     * Updates multiple fields in Firestore with a success message.
     */
    private void updateFirestore(Map<String, Object> updates, String successMessage) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> Utilities.showToast(requireContext(), successMessage, Utilities.ToastType.SUCCESS))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to update profile", Utilities.ToastType.ERROR));
    }
}