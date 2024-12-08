package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivitySignUpWithPhoneBinding;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.Utilities;

/**
 * SignUpWithPhoneActivity collects the user's phone number and redirects to OtpVerificationActivity.
 */
public class SignUpWithPhoneActivity extends AppCompatActivity {

    private static final String TAG = "SIGN_UP_WITH_PHONE_ACTIVITY";
    private ActivitySignUpWithPhoneBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpWithPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: Initializing SignUpWithPhoneActivity");

        setupUI();
        setListeners();
    }

    /**
     * Configures the default settings for the UI.
     */
    private void setupUI() {
        Log.d(TAG, "setupUI: Configuring default settings for CountryCodePicker");
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);
    }

    /**
     * Sets up the listeners for buttons and navigation actions.
     */
    private void setListeners() {
        Log.d(TAG, "setListeners: Setting up listeners for buttons");

        binding.buttonBack.setOnClickListener(v -> {
            Log.d(TAG, "setListeners: Back button clicked");
            onBackPressed();
        });

        binding.buttonSendOtp.setOnClickListener(v -> {
            Log.d(TAG, "setListeners: Send OTP button clicked");
            if (isValidPhoneNumber()) {
                redirectToOtpVerification();
            } else {
                Log.w(TAG, "setListeners: Phone number validation failed");
            }
        });

        binding.textSignIn.setOnClickListener(v -> {
            Log.d(TAG, "setListeners: Navigating to SignInActivity");
            Intent intent = new Intent(SignUpWithPhoneActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Validates the entered phone number.
     */
    private boolean isValidPhoneNumber() {
        String phoneNumber = binding.inputPhoneNumber.getText().toString().trim();
        if (phoneNumber.isEmpty()) {
            Log.w(TAG, "isValidPhoneNumber: Phone number field is empty");
            Utilities.showToast(this, "Please enter your phone number", Utilities.ToastType.WARNING);
            return false;
        } else if (!binding.countryCodePicker.isValidFullNumber()) {
            Log.w(TAG, "isValidPhoneNumber: Invalid phone number entered");
            binding.inputPhoneNumber.setError("Invalid phone number");
            return false;
        }
        Log.d(TAG, "isValidPhoneNumber: Phone number is valid");
        return true;
    }

    /**
     * Redirects to OtpVerificationActivity for OTP verification.
     */
    private void redirectToOtpVerification() {
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();
        Log.d(TAG, "redirectToOtpVerification: Redirecting to OtpVerificationActivity with phone: " + phoneNumber);

        Intent intent = new Intent(SignUpWithPhoneActivity.this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_PHONE, phoneNumber);
        intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_SIGN_UP);
        startActivity(intent);
        finish();
    }
}