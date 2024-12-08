package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
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
 * OtpVerificationActivity handles OTP verification for various actions like sign-up,
 * sign-in, or updating the phone number.
 */
public class OtpVerificationActivity extends AppCompatActivity {

    private static final String TAG = "OTP_VERIFICATION_ACTIVITY";
    private ActivityOtpVerificationBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private String verificationId;
    private String phoneNumber;
    private String actionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        phoneNumber = getIntent().getStringExtra(Constants.KEY_PHONE);
        actionType = getIntent().getStringExtra(Constants.KEY_ACTION_TYPE);

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.e(TAG, "onCreate: Phone number not provided");
            Utilities.showToast(this, "Phone number not provided", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Phone number: " + phoneNumber + ", Action: " + actionType);

        setupUI();
        sendOtp(phoneNumber);
    }

    /**
     * Sets up UI components and listeners.
     */
    private void setupUI() {
        Log.d(TAG, "setupUI: Setting up UI");

        binding.textPhoneNumber.setText(String.format("Phone Number: %s", phoneNumber));

        ImageView backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "setupUI: Back button clicked");
            onBackPressed();
        });

        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String otp = binding.inputOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                Log.w(TAG, "setupUI: OTP is empty");
                Utilities.showToast(this, "Please enter the OTP", Utilities.ToastType.WARNING);
            } else {
                verifyOtp(otp);
            }
        });
    }

    /**
     * Sends OTP to the phone number using Firebase Authentication.
     */
    private void sendOtp(String phoneNumber) {
        Log.d(TAG, "sendOtp: Sending OTP to " + phoneNumber);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Log.d(TAG, "onVerificationCompleted: Auto-verification completed");
                        handleVerificationSuccess(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "onVerificationFailed: Verification failed", e);
                        Utilities.showToast(OtpVerificationActivity.this, e.getMessage(), Utilities.ToastType.ERROR);
                    }

                    @Override
                    public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        Log.d(TAG, "onCodeSent: OTP code sent successfully");
                        Utilities.showToast(OtpVerificationActivity.this, "OTP sent successfully", Utilities.ToastType.INFO);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Verifies the entered OTP.
     */
    private void verifyOtp(String otp) {
        if (verificationId == null) {
            Log.e(TAG, "verifyOtp: Verification ID is null");
            Utilities.showToast(this, "Verification ID not available", Utilities.ToastType.ERROR);
            return;
        }

        Log.d(TAG, "verifyOtp: Verifying OTP");
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        handleVerificationSuccess(credential);
    }

    /**
     * Handles OTP verification success based on the action type.
     */
    private void handleVerificationSuccess(PhoneAuthCredential credential) {
        Log.d(TAG, "handleVerificationSuccess: Signing in with credential");

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "handleVerificationSuccess: OTP verified successfully");
                switch (actionType) {
                    case Constants.ACTION_SIGN_UP:
                        createNewUserDocument();
                        break;
                    case Constants.ACTION_UPDATE_PHONE:
                        updatePhoneNumberInFirestore();
                        break;
                    default:
                        navigateToMainActivity();
                        break;
                }
            } else {
                Log.e(TAG, "handleVerificationSuccess: OTP verification failed", task.getException());
                Utilities.showToast(this, "OTP verification failed", Utilities.ToastType.ERROR);
            }
        });
    }

    /**
     * Creates a new user document in Firestore during sign-up.
     */
    private void createNewUserDocument() {
        Log.d(TAG, "createNewUserDocument: Creating Firestore document for new user");

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        User newUser = new User("", "", phoneNumber, "");

        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(newUser)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "createNewUserDocument: User document created successfully with ID: " + documentReference.getId());
                    saveUserIdToPreferences(documentReference.getId());
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createNewUserDocument: Failed to create user document", e);
                    Utilities.showToast(this, e.getMessage(), Utilities.ToastType.ERROR);
                });
    }

    /**
     * Updates the phone number in Firestore for the logged-in user.
     */
    private void updatePhoneNumberInFirestore() {
        Log.d(TAG, "updatePhoneNumberInFirestore: Updating phone number for user");
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .update(Constants.KEY_PHONE, phoneNumber)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "updatePhoneNumberInFirestore: Phone number updated successfully");
                    Utilities.showToast(this, "Phone number updated successfully", Utilities.ToastType.SUCCESS);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updatePhoneNumberInFirestore: Failed to update phone number", e);
                    Utilities.showToast(this, e.getMessage(), Utilities.ToastType.ERROR);
                });
    }

    /**
     * Saves the Firestore document ID to shared preferences.
     */
    private void saveUserIdToPreferences(String userId) {
        Log.d(TAG, "saveUserIdToPreferences: Saving user ID to preferences");
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_USER_ID, userId);
    }

    /**
     * Navigates to MainActivity upon success.
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}