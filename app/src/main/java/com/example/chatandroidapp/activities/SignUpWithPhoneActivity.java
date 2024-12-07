package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpWithPhoneBinding;
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
 * SignUpWithPhoneActivity handles the user registration process using phone numbers and OTP.
 */
public class SignUpWithPhoneActivity extends AppCompatActivity {

    private ActivitySignUpWithPhoneBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private String verificationId; // To store verification ID for OTP validation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpWithPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        setListeners();
    }

    /**
     * Sets up listeners for the UI elements.
     */
    private void setListeners() {
        binding.buttonSendOtp.setOnClickListener(v -> {
            if (isValidPhoneNumber()) {
                sendOtp();
            }
        });

        binding.buttonVerifyOtp.setOnClickListener(v -> {
            if (binding.inputOtp.getText().toString().trim().isEmpty()) {
                Utilities.showToast(this, "Please enter the OTP", Utilities.ToastType.WARNING);
            } else {
                verifyOtp(binding.inputOtp.getText().toString().trim());
            }
        });

        binding.buttonCompleteSignup.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                completeSignup();
            }
        });

        binding.textSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUpWithPhoneActivity.this, SignInActivity.class);
            startActivity(intent);
            finish(); // Optional: Close the current activity
        });
    }

    /**
     * Sends an OTP to the provided phone number using Firebase Authentication.
     */
    private void sendOtp() {
        showLoadingIndicator(true);

        String phoneNumber = binding.inputPhoneNumber.getText().toString().trim();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        showLoadingIndicator(false);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Utilities.showToast(SignUpWithPhoneActivity.this, e.getMessage(), Utilities.ToastType.ERROR);
                        showLoadingIndicator(false);
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        verificationId = s; // Store verification ID
                        Utilities.showToast(SignUpWithPhoneActivity.this, "OTP sent successfully", Utilities.ToastType.INFO);
                        showOtpFields();
                        showLoadingIndicator(false);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Verifies the entered OTP using the verification ID.
     *
     * @param otp The OTP entered by the user.
     */
    private void verifyOtp(String otp) {
        showLoadingIndicator(true);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Utilities.showToast(this, "Phone number verified", Utilities.ToastType.SUCCESS);
                showProfileFields();
                showLoadingIndicator(false);
            } else {
                Utilities.showToast(this, "Invalid OTP", Utilities.ToastType.ERROR);
                showLoadingIndicator(false);
            }
        });
    }

    /**
     * Completes the sign-up process by adding the user to Firestore.
     */
    private void completeSignup() {
        showLoadingIndicator(true);

        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // Create a User object
        User user = new User(
                binding.inputFirstName.getText().toString().trim(),
                binding.inputLastName.getText().toString().trim(),
                binding.inputPhoneNumber.getText().toString().trim(),
                null // No image provided yet
        );

        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Utilities.showToast(this, "Sign-up successful", Utilities.ToastType.SUCCESS);

                    // Save user info in preferences
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
                    preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
                    preferenceManager.putString(Constants.KEY_PHONE, user.phone);

                    // Navigate to MainActivity
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Validates the phone number format.
     */
    private boolean isValidPhoneNumber() {
        if (binding.inputPhoneNumber.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your phone number", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Validates the sign-up details after OTP verification.
     */
    private boolean isValidSignUpDetails() {
        if (binding.inputFirstName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your first name", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.inputLastName.getText().toString().trim().isEmpty()) {
            Utilities.showToast(this, "Please enter your last name", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Toggles the visibility of the global progress bar and disables buttons.
     */
    private void showLoadingIndicator(boolean isLoading) {
        if (isLoading) {
            binding.progressBarGlobal.setVisibility(View.VISIBLE);

            // Disable all buttons during loading
            binding.buttonSendOtp.setEnabled(false);
            binding.buttonVerifyOtp.setEnabled(false);
            binding.buttonCompleteSignup.setEnabled(false);
        } else {
            binding.progressBarGlobal.setVisibility(View.GONE);

            // Enable all buttons after loading
            binding.buttonSendOtp.setEnabled(true);
            binding.buttonVerifyOtp.setEnabled(true);
            binding.buttonCompleteSignup.setEnabled(true);
        }
    }

    /**
     * Shows the OTP input and verification fields.
     */
    private void showOtpFields() {
        binding.inputOtp.setVisibility(View.VISIBLE);
        binding.buttonVerifyOtp.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the profile completion fields after OTP verification.
     */
    private void showProfileFields() {
        binding.inputFirstName.setVisibility(View.VISIBLE);
        binding.inputLastName.setVisibility(View.VISIBLE);
        binding.buttonCompleteSignup.setVisibility(View.VISIBLE);
    }
}