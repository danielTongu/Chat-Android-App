package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SIGN_UP_ACTIVITY";
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData().getData());
                }
            }
    );
    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private boolean isEmailSignUp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        setListeners();
    }

    /**
     * Initializes PreferenceManager.
     */
    private void initializeComponents() {
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Configures the default UI state.
     */
    private void setupUI() {
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            isEmailSignUp = (checkedId == binding.radioSignInWithEmail.getId());
            toggleSignUpMethodUI(isEmailSignUp);
        });

        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);

        toggleSignUpMethodUI(true); // Default to email sign-up
    }

    /**
     * Sets up listeners for interactive elements.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.layoutImage.setOnClickListener(v -> openImagePicker());
        binding.buttonSignUp.setOnClickListener(v -> handleSignUp());
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Toggles between email and phone sign-up layouts.
     *
     * @param isEmail True for email sign-up, false for phone sign-up.
     */
    private void toggleSignUpMethodUI(boolean isEmail) {
        binding.signUpEmailLayout.setVisibility(isEmail ? View.VISIBLE : View.GONE);
        binding.signUpPhoneLayout.setVisibility(isEmail ? View.GONE : View.VISIBLE);
        binding.buttonSignUp.setText(isEmail ? "Sign Up" : "Get Code");

        clearFocusAndHideKeyboard(); // Dismiss keyboard when toggling
    }

    /**
     * Opens the image picker for selecting a profile image.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImage.launch(intent);
    }

    /**
     * Handles sign-up process based on selected method.
     */
    private void handleSignUp() {
        if (isEmailSignUp && validateEmailSignUpDetails()) {
            checkEmailAndSignUp();
        } else if (!isEmailSignUp && validatePhoneSignUpDetails()) {
            navigateToOtpVerification();
        }
    }

    /**
     * Validates details for email sign-up.
     */
    private boolean validateEmailSignUpDetails() {
        try {
            validateCommonFields();
            String password = binding.inputPassword.getText().toString().trim();
            String confirmPassword = binding.inputConfirmNewPassword.getText().toString().trim();

            User.validatePassword(password);
            if (!password.equals(confirmPassword)) {
                binding.inputConfirmNewPassword.setError("Passwords do not match.");
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.WARNING);
            return false;
        }
    }

    /**
     * Validates details for phone sign-up.
     */
    private boolean validatePhoneSignUpDetails() {
        try {
            validateCommonFields();
            User.validatePhone(binding.countryCodePicker.getFullNumberWithPlus());
            return true;
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.WARNING);
            return false;
        }
    }

    /**
     * Validates common input fields (first and last names).
     */
    private void validateCommonFields() {
        User.validateFirstName(binding.inputFirstName.getText().toString().trim());
        User.validateLastName(binding.inputLastName.getText().toString().trim());
    }

    /**
     * Checks email availability and proceeds with sign-up if valid.
     */
    private void checkEmailAndSignUp() {
        showLoadingIndicator(true);
        String email = binding.inputEmail.getText().toString().trim();
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        binding.inputEmail.setError("Email already in use.");
                        showLoadingIndicator(false);
                    } else {
                        registerUserWithEmail(database);
                    }
                })
                .addOnFailureListener(e -> {
                    Utilities.showToast(this, "Error checking email: " + e.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Registers the user with email-based sign-up.
     */
    private void registerUserWithEmail(FirebaseFirestore database) {
        User user = createUserFromInput();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.fcmToken = task.getResult();
                        saveUserToDatabase(database, user);
                    } else {
                        Utilities.showToast(this, "Failed to retrieve FCM token.", Utilities.ToastType.ERROR);
                        showLoadingIndicator(false);
                    }
                });
    }

    /**
     * Creates a User object from input fields.
     */
    private User createUserFromInput() {
        User user = new User();
        user.firstName = binding.inputFirstName.getText().toString().trim();
        user.lastName = binding.inputLastName.getText().toString().trim();
        user.email = binding.inputEmail.getText().toString().trim();
        user.hashedPassword = User.hashPassword(binding.inputPassword.getText().toString().trim());
        user.image = encodedImage;
        user.id = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS).document().getId();
        return user;
    }

    /**
     * Saves a user to the Firestore database.
     */
    private void saveUserToDatabase(FirebaseFirestore database, User user) {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .addOnSuccessListener(unused -> {
                    savePreferences(user);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Utilities.showToast(this, e.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Saves user preferences locally.
     */
    private void savePreferences(User user) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.fcmToken);
    }

    /**
     * Navigates to OTP verification screen for phone sign-up.
     */
    private void navigateToOtpVerification() {
        saveBasicDetails();
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_SIGN_UP);
        startActivity(intent);
        finish();
    }

    /**
     * Saves basic details for phone sign-up.
     */
    private void saveBasicDetails() {
        preferenceManager.putString(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_PHONE, binding.countryCodePicker.getFullNumberWithPlus());
        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
    }

    /**
     * Displays or hides loading indicators.
     */
    private void showLoadingIndicator(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        binding.buttonSignUp.setEnabled(!isLoading);
    }

    /**
     * Navigates to the main activity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Handles image selection and encoding.
     *
     * @param imageUri URI of the selected image.
     */
    private void handleImageSelection(Uri imageUri) {
        if (imageUri != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                encodedImage = User.encodeImage(bitmap);
                binding.imageProfile.setImageBitmap(bitmap);
                binding.textAddImage.setVisibility(View.GONE);
            } catch (Exception e) {
                Utilities.showToast(this, "Failed to load image", Utilities.ToastType.ERROR);
                Log.e(TAG, "Error loading image", e);
            }
        } else {
            Utilities.showToast(this, "No image selected", Utilities.ToastType.WARNING);
        }
    }

    /**
     * Dismisses the keyboard when switching between layouts.
     */
    private void clearFocusAndHideKeyboard() {
        View view = getCurrentFocus();
        if (view instanceof EditText) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}