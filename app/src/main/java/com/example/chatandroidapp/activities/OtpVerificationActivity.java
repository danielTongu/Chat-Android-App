package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityOtpVerificationBinding;
import com.example.chatandroidapp.fragments.ProfileFragment;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

/**
 * Handles OTP verification for actions such as updating phone number, signing in, registering,
 * or deleting a user account.
 */
public class OtpVerificationActivity extends AppCompatActivity {
    private static final String TAG = "OTP_VERIFICATION";

    private ActivityOtpVerificationBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String verificationId;
    private String phoneNumber;
    private String actionType;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeComponents();
        setupListeners();
        handleIntentData();
    }

    /**
     * Initializes Firebase components, Firestore instance, and shared preference manager.
     */
    private void initializeComponents() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Sets up the UI, including the back button and OTP verification button.
     */
    private void setupListeners() {
        binding.buttonBack.setOnClickListener(view -> {
            if (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
                finish();// Close the activity and return to the ProfileFragment
            } else {
                onBackPressed();// Regular back navigation
            }
        });

        binding.buttonVerifyOtp.setOnClickListener(view -> {
            String otp = binding.inputOtp.getText().toString().trim();
            if (validateOtp(otp)) {
                verifyOtp(otp);
            }
        });
    }

    /**
     * Processes intent data and sets up the action type and phone number.
     */
    private void handleIntentData() {
        showLoading(true, "Determining action...");

        phoneNumber = preferenceManager.getString(Constants.KEY_PHONE, "");
        if (phoneNumber.isEmpty()) {
            showErrorAndFinish("Phone number unavailable.", "handleIntentData: Number empty");
            return;
        }

        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(Constants.KEY_ACTION_TYPE)) {
            showErrorAndFinish("Action unspecified .", "handleIntentData: Action unspecified.");
            return;
        }
        actionType = intent.getStringExtra(Constants.KEY_ACTION_TYPE);

        // Determine the phone number based on the action type
        if (ProfileFragment.ACTION_UPDATE_PHONE.equals(actionType)) {
            phoneNumber = intent.getStringExtra(Constants.KEY_PHONE);
            binding.buttonVerifyOtp.setText("Update Phone");
        } else if (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
            binding.buttonVerifyOtp.setText("Delete Account");
        } else if (SignUpActivity.ACTION_SIGN_UP.equals(actionType)) {
            binding.buttonVerifyOtp.setText("Sign Up");
        } else if (SignInActivity.ACTION_SIGN_IN.equals(actionType)) {
            binding.buttonVerifyOtp.setText("Sign In");
        }

        binding.textPhoneNumber.setText(String.format("Phone Number: %s", phoneNumber));
        sendOtp(phoneNumber);
    }

    /**
     * Sends an OTP to the specified phone number using Firebase's PhoneAuthProvider.
     *
     * @param phoneNumber The phone number to which the OTP should be sent.
     */
    private void sendOtp(String phoneNumber) {
        showLoading(true, "Sending OTP...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        handleVerificationSuccess(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showError("Failed to send OTP. Please try again.");
                    }

                    @Override
                    public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        showLoading(false, "OTP sent successfully.\nPlease check your phone.");
                        Utilities.showToast(OtpVerificationActivity.this, "", Utilities.ToastType.SUCCESS);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Validates the OTP entered by the user.
     *
     * @param otp The OTP entered by the user.
     * @return True if OTP is valid, false otherwise.
     */
    private boolean validateOtp(String otp) {
        if (otp.isEmpty()) {
            binding.inputOtp.setError("Please enter the OTP.");
            return false;
        } else if (otp.length() < 6) {
            binding.inputOtp.setError("OTP must be 6 digits.");
            return false;
        }
        return true;
    }

    /**
     * Verifies the OTP entered by the user.
     *
     * @param otp The OTP entered by the user.
     */
    private void verifyOtp(String otp) {
        showLoading(true, "Verifying OTP...");

        if (verificationId == null) {
            showError("Verification ID not available.");
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        handleVerificationSuccess(credential);
    }

    /**
     * Handles successful OTP verification based on the action type.
     *
     * @param credential The verified credential.
     */
    private void handleVerificationSuccess(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                proceedWithAction();
            } else {
                showError("OTP verification failed.");
            }
        });
    }

    /**
     * Proceeds with the next step based on the action type.
     */
    private void proceedWithAction() {
        switch (actionType) {
            case ProfileFragment.ACTION_DELETE_ACCOUNT:
                deleteUserAccount();
                break;
            case SignUpActivity.ACTION_SIGN_UP:
                registerNewUser();
                break;
            case SignInActivity.ACTION_SIGN_IN:
                authenticateExistingUser();
                break;
            case ProfileFragment.ACTION_UPDATE_PHONE:
                updatePhoneNumber();
                break;
            default:
                showErrorAndFinish("Action unknown.", "proceedWithAction: Action unknown: " + actionType);
                break;
        }
    }

    /**
     * Deletes the user's account using SignInActivity's static method.
     */
    private void deleteUserAccount() {
        String userId = preferenceManager.getString(Constants.KEY_ID, "");
        SignInActivity.deleteUserAndTasks(firestore, firebaseAuth, userId, this);
        logOutAndNavigateToSignIn();
    }

    /**
     * Registers a new user in Firestore and navigates to MainActivity.
     */
    private void registerNewUser() {
        showLoading(true, "Registering new user...");

        final User user = new User();
        user.image = preferenceManager.getString(Constants.KEY_IMAGE, "");
        user.firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME, "");
        user.lastName = preferenceManager.getString(Constants.KEY_LAST_NAME, "");
        user.phone = phoneNumber;
        user.id = firestore.collection(Constants.KEY_COLLECTION_USERS).document().getId();

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .addOnSuccessListener(unused -> {
                    saveUserPreferences(user);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    showErrorAndFinish("Failed to register user", "registerNewUser: " + e);
                });
    }

    /**
     * Queries Firestore for the user by the phone number:
     * - If found, saves preferences and navigates to MainActivity.
     * - If not found, displays an error.
     */
    private void authenticateExistingUser() {
        showLoading(true, "Authenticating user...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, phoneNumber)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Called upon successful retrieval of user data.
                     * @param queryDocumentSnapshots The query result.
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            User user = documentSnapshot.toObject(User.class);
                            saveUserPreferences(user);
                            navigateToMainActivity();
                        } else {
                            showError("User not found. Please sign up.");
                        }
                    }
                })
                .addOnFailureListener(e -> showError("Failed to fetch user details."));
    }

    /**
     * Updates the user's phone number in Firestore.
     */
    private void updatePhoneNumber() {
        showLoading(true, "Saving new phone number...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(Constants.KEY_PHONE, phoneNumber)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to update phone number.");
                    Log.e(TAG, "updatePhoneNumber: " + e);
                });
    }

    /**
     * Saves user preferences locally using the provided User object.
     *
     * @param user The user object whose details are to be saved in preferences.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(SignInActivity.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
    }

    /**
     * Navigates to MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Logs out and navigates to the sign-in screen.
     */
    private void logOutAndNavigateToSignIn() {
        preferenceManager.clear();
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Displays or hides the loading UI.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.layoutProgress.setVisibility(isLoading || message != null ? View.VISIBLE : View.GONE);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textProgressMessage.setText(message);
    }

    /**
     * Displays an error message and hides the loading indicator.
     *
     * @param message The error message to display.
     */
    private void showError(String message) {
        showLoading(false, message);
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
    }

    /**
     * Displays an error message and closes the activity.
     *
     * @param toastMessage     The error message to display.
     * @param exceptionMessage The error message to log.
     */
    private void showErrorAndFinish(String toastMessage, String exceptionMessage) {
        showLoading(false, null);
        Utilities.showToast(this, toastMessage, Utilities.ToastType.ERROR);
        Log.e(TAG, exceptionMessage);
        finish();
    }
}