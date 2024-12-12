// OtpVerificationActivity.java
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

/**
 * OtpVerificationActivity handles OTP verification for various user actions
 * such as account creation, sign-in, or updating phone numbers.
 */
public class OtpVerificationActivity extends AppCompatActivity {
    private static final String TAG = "OTP_VERIFICATION_ACTIVITY";
    private ActivityOtpVerificationBinding binding; // View binding for layout elements
    private FirebaseAuth firebaseAuth; // Firebase Authentication instance
    private FirebaseFirestore firestore;
    private String verificationId; // Verification ID returned by Firebase
    private String phoneNumber; // Phone number to verify
    private String actionType; // Action type (Sign Up, Sign In, Update Phone)
    private PreferenceManager preferenceManager; // SharedPreferences manager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        // Retrieve action type from intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Constants.KEY_ACTION_TYPE)) {
            actionType = intent.getStringExtra(Constants.KEY_ACTION_TYPE);
            Log.d(TAG, "onCreate: Received actionType: " + actionType);
        } else {
            Log.e(TAG, "onCreate: No action found in intent.");
            Utilities.showToast(this, "No action specified.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        // Determine the phone number based on the action type
        if (Constants.ACTION_UPDATE_PHONE.equals(actionType)) {
            phoneNumber = intent.getStringExtra(Constants.KEY_PHONE);
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.e(TAG, "onCreate: No phone number found in intent for update.");
                Utilities.showToast(this, "No phone number provided.", Utilities.ToastType.ERROR);
                finish();
                return;
            }
        } else if (Constants.ACTION_SIGN_IN.equals(actionType)) {
            phoneNumber = preferenceManager.getString(Constants.KEY_PHONE);
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.e(TAG, "onCreate: No phone number found in preferences for sign-in.");
                Utilities.showToast(this, "No phone number available.", Utilities.ToastType.ERROR);
                finish();
                return;
            }
        } else if (Constants.ACTION_SIGN_UP.equals(actionType)) {
            phoneNumber = preferenceManager.getString(Constants.KEY_PHONE);
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                Log.e(TAG, "onCreate: No phone number found in preferences for sign-up.");
                Utilities.showToast(this, "No phone number available.", Utilities.ToastType.ERROR);
                finish();
                return;
            }
        } else {
            Log.e(TAG, "onCreate: Unknown action type.");
            Utilities.showToast(this, "Unknown action type.", Utilities.ToastType.ERROR);
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Phone number to verify: " + phoneNumber);
        binding.textPhoneNumber.setText(String.format("Phone Number: %s", phoneNumber));
        setupUI();
        sendOtp(phoneNumber);
    }

    /**
     * Configures the UI components and sets up event listeners.
     */
    private void setupUI() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonVerifyOtp.setOnClickListener(v -> verifyOtp(binding.inputOtp.getText().toString().trim()));
    }

    /**
     * Sends an OTP to the provided phone number using Firebase Authentication.
     *
     * @param phoneNumber The phone number to send the OTP to.
     */
    private void sendOtp(String phoneNumber) {
        Log.d(TAG, "sendOtp: Sending OTP to " + phoneNumber);
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
                        Log.e(TAG, "onVerificationFailed: OTP sending failed", e);
                        showLoading(false, "Failed to send OTP. Please try again.");
                    }

                    @Override
                    public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        showLoading(false, "OTP sent successfully. Please check your phone.");
                        Utilities.showToast(getApplicationContext(), "", Utilities.ToastType.SUCCESS);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Verifies the OTP entered by the user.
     *
     * @param otp The OTP entered by the user.
     */
    private void verifyOtp(String otp) {
        showLoading(true, "Verifying OTP...");
        if (otp.isEmpty()) {
            Log.w(TAG, "verifyOtp: OTP is empty");
            Utilities.showToast(this, "Please enter the OTP", Utilities.ToastType.WARNING);
        } else if (verificationId == null) {
            Log.e(TAG, "verifyOtp: Verification ID is null");
            Utilities.showToast(this, "Verification ID not available", Utilities.ToastType.ERROR);
        } else {
            Log.d(TAG, "verifyOtp: Verifying OTP");
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
            handleVerificationSuccess(credential);
        }
        showLoading(false, "");
    }

    /**
     * Handles OTP verification success based on the action type.
     *
     * @param credential The PhoneAuthCredential for the verified OTP.
     */
    private void handleVerificationSuccess(PhoneAuthCredential credential) {
        Log.d(TAG, "handleVerificationSuccess: OTP verified successfully");
        Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showLoading(false, null);
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
                        break;
                }
            } else {
                Log.e(TAG, "handleVerificationSuccess: OTP verification failed", task.getException());
                Utilities.showToast(this, "OTP verification failed. Please try again.", Utilities.ToastType.ERROR);
            }
        });
    }

    /**
     * Saves a new user's details to Firestore, updates preferences, and navigates to MainActivity.
     */
    private void setAndNavigateToMainActivity() {
        Log.d(TAG, "setAndNavigateToMainActivity: Saving new user details and navigating to MainActivity");
        showLoading(true, "Saving new user details...");
        String phone = preferenceManager.getString(Constants.KEY_PHONE);

        if (phone == null || phone.isEmpty()) {
            Log.e(TAG, "setAndNavigateToMainActivity: phone object is null or empty!");
            Utilities.showToast(this, "Failed to retrieve phone number. Please try again.", Utilities.ToastType.ERROR);
        } else {
            User user = new User();
            user.id = firestore.collection(Constants.KEY_COLLECTION_USERS).document().getId();
            user.phone = phone;
            user.firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME);
            user.lastName = preferenceManager.getString(Constants.KEY_LAST_NAME);
            user.image = preferenceManager.getString(Constants.KEY_IMAGE);

            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .document(user.id)
                    .set(user)
                    .addOnSuccessListener(unused -> {
                        Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);
                        saveUserPreferences(user);
                        navigateToMainActivity();
                    })
                    .addOnFailureListener(exception -> {
                        Utilities.showToast(this, "Failed to save user data.", Utilities.ToastType.ERROR);
                    });
        }

        showLoading(false, "");
    }

    /**
     * Fetches and navigates to MainActivity during sign-in.
     */
    private void getAndNavigateToMainActivity() {
        Log.d(TAG, "getAndNavigateToMainActivity: Fetching user details from Firestore");
        showLoading(true, "Fetching user details...");


        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.id = documentSnapshot.getId(); // Ensure the ID is set
                            saveUserPreferences(user); // Save user details in preferences
                            Log.d(TAG, "User ID retrieved and saved: " + user.id);
                            navigateToMainActivity();
                        } else {
                            Log.e(TAG, "getAndNavigateToMainActivity: User object is null");
                            Utilities.showToast(this, "User data is corrupted.", Utilities.ToastType.ERROR);
                        }
                    } else {
                        Log.e(TAG, "getAndNavigateToMainActivity: No user found with the given UID");
                        Utilities.showToast(this, "User not found. Please sign up first.", Utilities.ToastType.ERROR);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getAndNavigateToMainActivity: Failed to fetch user details", e);
                    Utilities.showToast(this, "Failed to retrieve user information.", Utilities.ToastType.ERROR);
                });
        showLoading(false, "");
    }

    /**
     * Updates the user's phone number in Firestore.
     */
    private void updatePhoneNumberInFirestore() {
        Log.d(TAG, "updatePhoneNumberInFirestore: Updating phone number");
        showLoading(true, "Updating phone number...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID))
                .update(Constants.KEY_PHONE, phoneNumber)
                .addOnSuccessListener(unused -> {
                    preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
                    navigateToMainActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updatePhoneNumberInFirestore: Failed to update phone number", e);
                    showLoading(false, "");
                    Utilities.showToast(this, e.getMessage(), Utilities.ToastType.ERROR);
                });
    }

    /**
     * Saves user details to SharedPreferences.
     *
     * @param user The User object to save.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.fcmToken);
    }

    /**
     * Navigates to MainActivity.
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Toggles the visibility of the ProgressBar and processMessage TextView.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display in the processMessage TextView.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        if (message != null) {
            binding.processMessage.setText(message);
            binding.processMessage.setVisibility(View.VISIBLE);
        } else {
            binding.processMessage.setVisibility(View.GONE);
        }
    }
}