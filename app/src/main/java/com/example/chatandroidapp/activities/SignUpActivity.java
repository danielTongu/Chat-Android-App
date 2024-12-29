package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpBinding;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.InputStream;

/**
 * Handles user registration via email/password or phone/OTP.
 * Manages user input validation, Firestore integration, and navigation.
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SIGN_UP_ACTIVITY";
    public static final int KEY_PASSWORD_MIN_LENGTH = 5;
    public static final String ACTION_SIGN_UP = "signUp";

    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private boolean isEmailSignUp = true;

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    handleImageSelection(result.getData().getData());
                }
            }
    );

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
     * Initializes shared preferences and other necessary components.
     */
    private void initializeComponents() {
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        Log.d(TAG, "Components initialized.");
    }

    /**
     * Configures UI elements and sets default state.
     */
    private void setupUI() {
        showLoadingIndicator(true, "Setting up...");
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            isEmailSignUp = (checkedId == binding.radioSignInWithEmail.getId());
            toggleSignUpMethodUI(isEmailSignUp);
        });

        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);

        toggleSignUpMethodUI(isEmailSignUp); // Default to email sign-up
        showLoadingIndicator(false, null);
    }

    /**
     * Sets up event listeners for buttons and other UI elements.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.layoutImage.setOnClickListener(v -> openImagePicker());
        binding.buttonSignUp.setOnClickListener(v -> handleSignUp());
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Opens the image picker for selecting a profile image.
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImage.launch(intent);
    }


    /**
     * Toggles the sign-up method (email or phone) UI elements.
     *
     * @param isEmail True for email sign-up, false for phone sign-up.
     */
    private void toggleSignUpMethodUI(boolean isEmail) {
        binding.layoutSignUpEmail.setVisibility(isEmail ? View.VISIBLE : View.GONE);
        binding.layoutSignUpPhone.setVisibility(isEmail ? View.GONE : View.VISIBLE);
        binding.buttonSignUp.setText(isEmail ? "Sign Up" : "Get Code");
    }

    /**
     * Handles the sign-up process based on the selected method.
     */
    private void handleSignUp() {
        showLoadingIndicator(true, isEmailSignUp ? "Processing email sign-up..." : "Sending OTP...");

        if (isEmailSignUp && validateEmailSignUpDetails()) {
            checkEmailAndSignUp(FirebaseFirestore.getInstance());
        } else if (!isEmailSignUp && validatePhoneSignUpDetails()) {
            navigateToOtpVerification();
        } else {
            showLoadingIndicator(false, null);
        }
    }

    /**
     * Validates email-based sign-up input fields.
     *
     * @return True if valid, otherwise false.
     */
    private boolean validateEmailSignUpDetails() {
        try {
            validateCommonFields();
            String password = binding.inputPassword.getText().toString().trim();
            String confirmPassword = binding.inputConfirmPassword.getText().toString().trim();

            User.validatePassword(password);
            if (!password.equals(confirmPassword)) {
                binding.inputConfirmPassword.setError("Passwords do not match.");
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            Utilities.showToast(this, e.getMessage(), Utilities.ToastType.WARNING);
            return false;
        }
    }

    /**
     * Validates phone-based sign-up input fields.
     *
     * @return True if valid, otherwise false.
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
     * Checks email availability and registers the user if valid.
     */
    private void checkEmailAndSignUp(FirebaseFirestore database) {
        showLoadingIndicator(true, "Validating email...");
        String email = binding.inputEmail.getText().toString().trim();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        binding.inputEmail.setError("Email already in use.");
                        showLoadingIndicator(false, null);
                    } else {
                        registerUserWithEmail(database);
                    }
                })
                .addOnFailureListener(e -> {
                    Utilities.showToast(this, "Error: " + e.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false, null);
                });
    }

    /**
     * Registers the user using email-based sign-up.
     */
    private void registerUserWithEmail(FirebaseFirestore database) {
        showLoadingIndicator(true, "Registering user...");

        User user = createUserFromInput(database);
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.fcmToken = task.getResult();
                        saveUserToDatabase(database, user);
                    } else {
                        Utilities.showToast(this, "Failed to retrieve FCM token.", Utilities.ToastType.ERROR);
                        showLoadingIndicator(false, null);
                    }
                });
    }

    /**
     * Creates a User object from the input fields.
     *
     * @return User object.
     */
    private User createUserFromInput(FirebaseFirestore database) {
        User user = new User();
        user.firstName = binding.inputFirstName.getText().toString().trim();
        user.lastName = binding.inputLastName.getText().toString().trim();
        user.email = binding.inputEmail.getText().toString().trim();
        user.hashedPassword = User.hashPassword(binding.inputPassword.getText().toString().trim());
        user.image = encodedImage;
        user.id = database.collection(Constants.KEY_COLLECTION_USERS).document().getId();
        return user;
    }

    /**
     * Saves a user to Firestore database.
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
                    Utilities.showToast(this, "Error: " + e.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false, null);
                });
    }

    /**
     * Saves user preferences locally after successful registration.
     */
    private void savePreferences(User user) {
        preferenceManager.putBoolean(SignInActivity.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.fcmToken);
    }

    /**
     * Navigates to OTP verification for phone-based sign-up.
     */
    private void navigateToOtpVerification() {
        saveBasicDetails();
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, ACTION_SIGN_UP);
        startActivity(intent);
        finish();
    }

    /**
     * Saves basic details for phone-based sign-up.
     */
    private void saveBasicDetails() {
        preferenceManager.putString(Constants.KEY_FIRST_NAME, binding.inputFirstName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_LAST_NAME, binding.inputLastName.getText().toString().trim());
        preferenceManager.putString(Constants.KEY_PHONE, binding.countryCodePicker.getFullNumberWithPlus());
        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
    }

    /**
     * Displays or hides loading indicators and disables inputs during loading.
     *
     * @param isLoading Whether to display the loading state.
     * @param message   Message to display during loading.
     */
    private void showLoadingIndicator(boolean isLoading, String message) {
        binding.layoutProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textProgressMessage.setText(message);

        binding.scrollView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.buttonSignUp.setEnabled(!isLoading);
        binding.textSignIn.setEnabled(!isLoading);

        binding.radioSignInWithEmail.setEnabled(!isLoading);
        binding.radioSignInWithPhone.setEnabled(!isLoading);

        binding.inputFirstName.setEnabled(!isLoading);
        binding.inputLastName.setEnabled(!isLoading);
        binding.inputEmail.setEnabled(!isLoading);
        binding.inputPassword.setEnabled(!isLoading);
        binding.inputConfirmPassword.setEnabled(!isLoading);
        binding.inputPhoneNumber.setEnabled(!isLoading);
    }

    /**
     * Navigates to the main activity after successful sign-up.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Handles image selection and encodes the selected image.
     *
     * @param imageUri URI of the selected image.
     */
    private void handleImageSelection(Uri imageUri) {
        if (imageUri != null) {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                encodedImage = User.encodeImage(bitmap);
                binding.imageProfile.setImageBitmap(bitmap);
                binding.textUploadImage.setVisibility(View.GONE);
            } catch (Exception e) {
                Utilities.showToast(this, "Failed to load image.", Utilities.ToastType.ERROR);
                Log.e(TAG, "Error loading image", e);
            }
        } else {
            Utilities.showToast(this, "No image selected.", Utilities.ToastType.WARNING);
        }
    }
}