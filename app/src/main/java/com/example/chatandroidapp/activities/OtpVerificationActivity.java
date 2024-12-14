package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityOtpVerificationBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

/**
 * Handles OTP verification for account-related actions such as sign-up, sign-in, or phone number updates.
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
        setupUI();
        handleIntentData();
    }

    /**
     * Initializes Firebase components and shared preferences.
     */
    private void initializeComponents() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Configures UI elements and sets up listeners.
     */
    private void setupUI() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String otp = binding.inputOtp.getText().toString().trim();
            if (validateOtp(otp)) {
                verifyOtp(otp);
            }
        });
    }

    /**
     * Handles data passed via Intent and initiates OTP sending.
     */
    private void handleIntentData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Constants.KEY_ACTION_TYPE)) {
            actionType = intent.getStringExtra(Constants.KEY_ACTION_TYPE);
            phoneNumber = fetchPhoneNumber(intent);
            if (phoneNumber != null) {
                binding.textPhoneNumber.setText(String.format("Phone Number: %s", phoneNumber));
                sendOtp(phoneNumber);
            } else {
                showErrorAndFinish("Phone number not available.");
            }
        } else {
            showErrorAndFinish("No action specified.");
        }
    }

    /**
     * Fetches the phone number based on the action type.
     *
     * @param intent The Intent containing data.
     * @return The phone number if available; otherwise, null.
     */
    private String fetchPhoneNumber(Intent intent) {
        switch (actionType) {
            case Constants.ACTION_UPDATE_PHONE:
                return intent.getStringExtra(Constants.KEY_PHONE);
            case Constants.ACTION_SIGN_IN:
            case Constants.ACTION_SIGN_UP:
                return preferenceManager.getString(Constants.KEY_PHONE, "");
            default:
                return null;
        }
    }

    /**
     * Displays an error message and exits the activity.
     *
     * @param message The error message to display.
     */
    private void showErrorAndFinish(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        Log.e(TAG, message);
        finish();
    }

    /**
     * Sends an OTP to the specified phone number.
     *
     * @param phoneNumber The phone number to send the OTP to.
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
                        Log.e(TAG, "OTP sending failed", e);
                        showLoading(false, "Failed to send OTP. Please try again.");
                    }

                    @Override
                    public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        showLoading(false, "OTP sent successfully. Please check your phone.");
                        Utilities.showToast(OtpVerificationActivity.this, "OTP sent", Utilities.ToastType.SUCCESS);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Validates the OTP entered by the user.
     *
     * @param otp The entered OTP.
     * @return True if valid; otherwise, false.
     */
    private boolean validateOtp(String otp) {
        if (otp.isEmpty()) {
            binding.inputOtp.setError("Please enter the OTP.");
            return false;
        }
        if (otp.length() < 6) {
            binding.inputOtp.setError("OTP must be 6 digits.");
            return false;
        }
        return true;
    }

    /**
     * Verifies the OTP against the verification ID.
     *
     * @param otp The OTP entered by the user.
     */
    private void verifyOtp(String otp) {
        if (verificationId == null) {
            Utilities.showToast(this, "Verification ID not available.", Utilities.ToastType.ERROR);
            return;
        }
        showLoading(true, "Verifying OTP...");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        handleVerificationSuccess(credential);
    }

    /**
     * Handles successful OTP verification and proceeds with the action type.
     *
     * @param credential The verified PhoneAuthCredential.
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
     * Proceeds with the action based on the action type.
     */
    private void proceedWithAction() {
        switch (actionType) {
            case Constants.ACTION_SIGN_UP:
                setAndNavigateToMainActivity();
                break;
            case Constants.ACTION_SIGN_IN:
                getAndNavigateToMainActivity();
                break;
            case Constants.ACTION_UPDATE_PHONE:
                updatePhoneNumberInFirestore();
                break;
            default:
                navigateToMainActivity();
        }
    }

    /**
     * Saves user details for sign-up and navigates to MainActivity.
     */
    private void setAndNavigateToMainActivity() {
        User user = new User();
        user.id = firestore.collection(Constants.KEY_COLLECTION_USERS).document().getId();
        user.phone = phoneNumber;
        user.firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME, "");
        user.lastName = preferenceManager.getString(Constants.KEY_LAST_NAME, "");
        user.image = preferenceManager.getString(Constants.KEY_IMAGE, "");

        firestore.collection(Constants.KEY_COLLECTION_USERS).document(user.id)
                .set(user)
                .addOnSuccessListener(unused -> {
                    saveUserPreferences(user);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> showError("Failed to save user details."));
    }

    /**
     * Retrieves user data for sign-in and navigates to MainActivity.
     */
    private void getAndNavigateToMainActivity() {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        User user = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);
                        saveUserPreferences(user);
                        navigateToMainActivity();
                    } else {
                        showError("User not found. Please sign up.");
                    }
                })
                .addOnFailureListener(e -> showError("Failed to fetch user details."));
    }

    /**
     * Updates the user's phone number in Firestore.
     */
    private void updatePhoneNumberInFirestore() {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(Constants.KEY_PHONE, phoneNumber)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> showError("Failed to update phone number."));
    }

    /**
     * Saves user preferences locally.
     *
     * @param user The User object.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
    }

    /**
     * Navigates to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Displays a loading message.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        binding.buttonVerifyOtp.setEnabled(!isLoading);
        binding.inputOtp.setEnabled(!isLoading);
        binding.processMessage.setText(message);
        binding.processMessage.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    /**
     * Displays an error message and resets the UI.
     */
    private void showError(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        showLoading(false, null);
    }
}