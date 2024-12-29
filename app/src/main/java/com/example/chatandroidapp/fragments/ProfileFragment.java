package com.example.chatandroidapp.fragments;

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
import com.example.chatandroidapp.models.User;
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
    public static final String ACTION_UPDATE_PHONE = "updatePhone";
    public static final String ACTION_DELETE_ACCOUNT = "deleteAccount";

    private static final String TAG = "PROFILE_FRAGMENT";

    private FragmentProfileBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerImagePickerLauncher(); // Ensure launcher is initialized early in lifecycle
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        initializeComponents();
        loadUserDetails();
        setListeners();

        return binding.getRoot();
    }

    /**
     * Initializes Firebase, SharedPreferences, and image handler for profile image selection.
     */
    private void initializeComponents() {
        preferenceManager = PreferenceManager.getInstance(requireContext());
        database = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    /**
     * Registers the image picker launcher to handle image selection results.
     */
    private void registerImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleImageSelection(imageUri);
                        }
                    } else {
                        Utilities.showToast(requireContext(), "No image selected", Utilities.ToastType.WARNING);
                    }
                }
        );
    }

    /**
     * Handles the selected image, updating the profile picture in the UI and Firestore.
     *
     * @param imageUri The URI of the selected image.
     */
    private void handleImageSelection(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
            binding.imageProfile.setImageBitmap(bitmap);

            String encodedImage = User.encodeImage(bitmap);
            updateFirestoreField(Constants.KEY_IMAGE, encodedImage);
            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);

            Utilities.showToast(requireContext(), "Profile image updated", Utilities.ToastType.SUCCESS);
        } catch (Exception e) {
            Log.e(TAG, "Error handling selected image", e);
            Utilities.showToast(requireContext(), "Failed to process the image", Utilities.ToastType.ERROR);
        }
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
        binding.layoutImage.setOnClickListener(v -> openImagePicker());
        binding.buttonSignOut.setOnClickListener(v -> logOutUser());
        binding.buttonUpdate.setOnClickListener(v -> updateProfileDetails());
        binding.buttonDelete.setOnClickListener(v -> initiateAccountVerification());
        binding.inputPhoneNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { initiatePhoneVerification(); }
        });
    }

    /**
     * Opens the image picker to select a profile picture.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /**
     * Updates the user's profile details in Firestore and SharedPreferences.
     */
    private void updateProfileDetails() {
        try {
            String firstName = User.validateFirstName(binding.inputFirstName.getText().toString().trim());
            String lastName = User.validateLastName(binding.inputLastName.getText().toString().trim());
            String email = User.validateEmail(binding.inputEmail.getText().toString().trim());
            String newPassword = binding.inputNewPassword.getText().toString().trim();
            String confirmPassword = binding.inputConfirmNewPassword.getText().toString().trim();

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
        Map<String, Object> updates = new HashMap<>();
        updates.put(key, value);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(updates)
                .addOnSuccessListener(unused -> Log.d(TAG, "updateFirestoreField: Updated " + key + " successfully"))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to update " + key, Utilities.ToastType.ERROR));
    }

    /**
     * Updates the user's Firestore details and SharedPreferences.
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
        if (!preferenceManager.getString(Constants.KEY_ID, "").isEmpty()) {
            removeFirebaseTokenFromFirestore();
        }

        firebaseAuth.signOut();
        preferenceManager.clear();

        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Removes the Firebase token from Firestore.
     */
    private void removeFirebaseTokenFromFirestore() {
        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, null);

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(updates)
                .addOnSuccessListener(unused -> Log.d(TAG, "Firebase token removed successfully"))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to remove Firebase token", Utilities.ToastType.ERROR));
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
            intent.putExtra(Constants.KEY_ACTION_TYPE, ACTION_UPDATE_PHONE);
            startActivity(intent);
        }
    }

    /**
     * Initiates account verification to delete the current user account.
     */
    private void initiateAccountVerification() {
        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, ACTION_DELETE_ACCOUNT);
        startActivity(intent);
    }

    /**
     * Updates multiple fields in Firestore and shows a success message.
     *
     * @param updates        The fields to update.
     * @param successMessage The message to display upon successful update.
     */
    private void updateFirestore(Map<String, Object> updates, String successMessage) {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(updates)
                .addOnSuccessListener(unused -> Utilities.showToast(requireContext(), successMessage, Utilities.ToastType.SUCCESS))
                .addOnFailureListener(e -> Utilities.showToast(requireContext(), "Failed to update profile", Utilities.ToastType.ERROR));
    }
}