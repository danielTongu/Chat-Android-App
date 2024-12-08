package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
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
import com.hbb20.CountryCodePicker;

import java.util.concurrent.TimeUnit;

/**
 * SignUpWithPhoneActivity handles the user registration process using phone numbers and OTP.
 */
public class SignUpWithPhoneActivity extends AppCompatActivity {

    private ActivitySignUpWithPhoneBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private String verificationId; // To store verification ID for OTP validation
    private static final long OTP_TIMEOUT_SECONDS = 60L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize binding
        binding = ActivitySignUpWithPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setDefaultCountry();
        setListeners();
    }

    /**
     * Initializes Firebase Authentication and Preference Manager.
     */
    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Sets the default country for the CountryCodePicker to the USA.
     */
    private void setDefaultCountry() {
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);
    }

    /**
     * Sets up listeners for the buttons and back button.
     */
    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());

        binding.buttonSendOtp.setOnClickListener(v -> {
            if (isValidPhoneNumber()) {
                sendOtp();
            }
        });

        binding.buttonVerifyOtp.setOnClickListener(v -> {
            String otp = binding.inputOtp.getText().toString().trim();
            if (otp.isEmpty()) {
                Utilities.showToast(this, "Please enter the OTP", Utilities.ToastType.WARNING);
            } else {
                verifyOtp(otp);
            }
        });

        binding.buttonCompleteSignup.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                completeSignup();
            }
        });

        binding.textSignIn.setOnClickListener(v -> {
            // Redirect to Sign In
            Intent intent = new Intent(SignUpWithPhoneActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Sends an OTP to the provided phone number using Firebase Authentication.
     */
    private void sendOtp() {
        showLoadingIndicator(true);
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus().trim();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                        showLoadingIndicator(false);
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        Log.e("SignUpWithPhone", "Verification failed", e);
                        Utilities.showToast(SignUpWithPhoneActivity.this, e.getMessage(), Utilities.ToastType.ERROR);
                        showLoadingIndicator(false);
                    }

                    @Override
                    public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        verificationId = s;
                        Utilities.showToast(SignUpWithPhoneActivity.this, "OTP sent successfully", Utilities.ToastType.INFO);
                        disablePhoneField();
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
                disableOtpField();
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
        User user = new User(
                binding.inputFirstName.getText().toString().trim(),
                binding.inputLastName.getText().toString().trim(),
                binding.countryCodePicker.getFullNumberWithPlus().trim(),
                null // Placeholder for no image
        );

        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    Utilities.showToast(this, "Sign-up successful", Utilities.ToastType.SUCCESS);
                    saveUserDetails(user, documentReference.getId());
                    navigateToMainActivity();
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Saves the user details in shared preferences.
     */
    private void saveUserDetails(User user, String userId) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_USER_ID, userId);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
    }

    /**
     * Navigates to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Validates the phone number format.
     */
    private boolean isValidPhoneNumber() {
        String phoneNumber = binding.inputPhoneNumber.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            Utilities.showToast(this, "Please enter your phone number", Utilities.ToastType.WARNING);
            return false;
        } else if (!binding.countryCodePicker.isValidFullNumber()) {
            binding.inputPhoneNumber.setError("Invalid phone number");
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
        binding.progressBarGlobal.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.buttonSendOtp.setEnabled(!isLoading);
        binding.buttonVerifyOtp.setEnabled(!isLoading);
        binding.buttonCompleteSignup.setEnabled(!isLoading);
    }

    /**
     * Shows the OTP input and verification fields.
     */
    private void showOtpFields() {
        binding.inputOtp.setVisibility(View.VISIBLE);
        binding.buttonVerifyOtp.setVisibility(View.VISIBLE);
    }

    /**
     * Disables the phone number field and hides the Send OTP button.
     */
    private void disablePhoneField() {
        binding.inputPhoneNumber.setEnabled(false);
        binding.countryCodePicker.setCcpClickable(false);
        binding.buttonSendOtp.setVisibility(View.GONE);
    }

    /**
     * Disables the OTP field and hides the Verify OTP button.
     */
    private void disableOtpField() {
        binding.inputOtp.setEnabled(false);
        binding.buttonVerifyOtp.setVisibility(View.GONE);
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