package com.example.chatandroidapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chatandroidapp.activities.OtpVerificationActivity;
import com.example.chatandroidapp.activities.SignInActivity;
import com.example.chatandroidapp.databinding.FragmentProfileBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileFragment allows users to view and update their profile details,
 * including phone number, email, password, and profile picture.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "PROFILE_FRAGMENT";

    private FragmentProfileBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private ProfileImageHandler profileImageHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        initializeComponents();
        loadUserDetails();
        setListeners();

        return binding.getRoot();
    }

    /**
     * Initializes Firebase, shared preferences, and profile image handler.
     */
    private void initializeComponents() {
        preferenceManager = PreferenceManager.getInstance(requireContext());
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        profileImageHandler = new ProfileImageHandler(this) {
            @Override
            protected void handleImageSelection(Uri imageUri) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    binding.imageProfile.setImageBitmap(bitmap);

                    String encodedImage = User.encodeImage(bitmap);
                    updateFirestoreField(Constants.KEY_IMAGE, encodedImage);
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                } catch (Exception e) {
                    Utilities.showToast(requireContext(), "Failed to set image", Utilities.ToastType.ERROR);
                    Log.e(TAG, "handleImageSelection: Failed to set image", e);
                }
            }
        };
    }

    /**
     * Loads user details from SharedPreferences into the UI.
     */
    private void loadUserDetails() {
        binding.inputFirstName.setText(preferenceManager.getString(Constants.KEY_FIRST_NAME, ""));
        binding.inputLastName.setText(preferenceManager.getString(Constants.KEY_LAST_NAME, ""));
        binding.inputEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL, ""));
        binding.inputPhoneNumber.setText(preferenceManager.getString(Constants.KEY_PHONE, ""));

        String encodedImage = preferenceManager.getString(Constants.KEY_IMAGE, null);
        if (!TextUtils.isEmpty(encodedImage)) {
            binding.imageProfile.setImageBitmap(User.getBitmapFromEncodedString(encodedImage));
        }
    }

    /**
     * Sets listeners for profile actions like image updates, profile updates, and logout.
     */
    private void setListeners() {
        binding.layoutImage.setOnClickListener(v -> profileImageHandler.openImagePicker());
        binding.buttonUpdateProfile.setOnClickListener(v -> updateProfileDetails());
        binding.buttonLogout.setOnClickListener(v -> logOutUser());
        binding.inputPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) initiatePhoneVerification();
        });
    }

    /**
     * Updates the user's profile details in Firestore and SharedPreferences.
     */
    private void updateProfileDetails() {
        String firstName = binding.inputFirstName.getText().toString().trim();
        String lastName = binding.inputLastName.getText().toString().trim();
        String email = binding.inputEmail.getText().toString().trim();
        String newPassword = binding.inputNewPassword.getText().toString().trim();
        String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

        try {
            firstName = User.validateFirstName(firstName);
            lastName = User.validateLastName(lastName);
            email = User.validateEmail(email);

            if (!TextUtils.isEmpty(newPassword)) {
                newPassword = User.validatePassword(newPassword);
                if (!newPassword.equals(confirmPassword)) {
                    throw new IllegalArgumentException("Passwords do not match.");
                }
                updatePassword(newPassword);
            }

            updateFirestoreDetails(firstName, lastName, email);

        } catch (IllegalArgumentException e) {
            Utilities.showToast(requireContext(), e.getMessage(), Utilities.ToastType.WARNING);
        }
    }

    /**
     * Updates the user's password in FirebaseAuth.
     *
     * @param newPassword The new password to set.
     */
    private void updatePassword(String newPassword) {
        firebaseAuth.getCurrentUser().updatePassword(newPassword)
                .addOnSuccessListener(unused -> Utilities.showToast(requireContext(), "Password updated successfully", Utilities.ToastType.SUCCESS))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to update password", Utilities.ToastType.ERROR));
    }

    /**
     * Updates a specific field in Firestore and logs the update.
     *
     * @param key   The field key to update.
     * @param value The new value for the field.
     */
    private void updateFirestoreField(String key, String value) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put(key, value);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> Log.d(TAG, "updateFirestoreField: Updated " + key + " successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "updateFirestoreField: Failed to update " + key, e));
    }

    /**
     * Updates the user's Firestore details and SharedPreferences.
     *
     * @param firstName The new first name.
     * @param lastName  The new last name.
     * @param email     The new email.
     */
    private void updateFirestoreDetails(String firstName, String lastName, String email) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FIRST_NAME, firstName);
        updates.put(Constants.KEY_LAST_NAME, lastName);
        updates.put(Constants.KEY_EMAIL, email);

        updateFirestore(updates, "Profile updated successfully");

        preferenceManager.putString(Constants.KEY_FIRST_NAME, firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, lastName);
        preferenceManager.putString(Constants.KEY_EMAIL, email);
    }

    /**
     * Logs out the user, clears local data, and navigates to the sign-in screen.
     */
    private void logOutUser() {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        if (!TextUtils.isEmpty(userId)) {
            removeFirebaseTokenFromFirestore(userId);
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
    private void removeFirebaseTokenFromFirestore(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, null);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> Log.d(TAG, "Firebase token removed successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to remove Firebase token", e));
    }

    /**
     * Initiates phone verification if the phone number has changed.
     */
    private void initiatePhoneVerification() {
        String newPhoneNumber = binding.inputPhoneNumber.getText().toString().trim();
        String currentPhoneNumber = preferenceManager.getString(Constants.KEY_PHONE, "");

        if (!TextUtils.isEmpty(newPhoneNumber) && !newPhoneNumber.equals(currentPhoneNumber)) {
            Intent intent = new Intent(requireContext(), OtpVerificationActivity.class);
            intent.putExtra(Constants.KEY_PHONE, newPhoneNumber);
            intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_UPDATE_PHONE);
            startActivity(intent);
        }
    }

    /**
     * Updates multiple fields in Firestore and shows a success message.
     *
     * @param updates        The fields to update.
     * @param successMessage The message to display upon successful update.
     */
    private void updateFirestore(Map<String, Object> updates, String successMessage) {
        String userId = firebaseAuth.getCurrentUser().getUid();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> Utilities.showToast(requireContext(), successMessage, Utilities.ToastType.SUCCESS))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to update profile", Utilities.ToastType.ERROR));
    }

    /**
     * Abstract class for handling profile image selection.
     */
    private static abstract class ProfileImageHandler {

        private final ActivityResultLauncher<Intent> pickImageLauncher;

        ProfileImageHandler(Fragment fragment) {
            pickImageLauncher = fragment.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            handleImageSelection(result.getData().getData());
                        }
                    }
            );
        }

        /**
         * Opens the image picker.
         */
        void openImagePicker() {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        }

        /**
         * Abstract method to handle image selection.
         *
         * @param imageUri The URI of the selected image.
         */
        protected abstract void handleImageSelection(Uri imageUri);
    }
}