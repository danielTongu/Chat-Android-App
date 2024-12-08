package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.auth.FirebaseAuth;

/**
 * SignInActivity handles user authentication via email/password or phone/OTP.
 * On successful authentication, FirebaseAuth sets the current user,
 * making their UID accessible globally.
 */
public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SIGN_IN_ACTIVITY";
    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        firebaseAuth = FirebaseAuth.getInstance();

        Log.d(TAG, "onCreate: Initializing SignInActivity");
        setupUI();
        setListeners();
    }

    /**
     * Configures default settings for the UI, such as input types.
     */
    private void setupUI() {
        Log.d(TAG, "setupUI: Configuring UI for sign-in options");

        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSignInWithEmail) {
                Log.d(TAG, "setupUI: User selected email sign-in");
                binding.inputPassword.setVisibility(View.VISIBLE);
                binding.countryCodePicker.setVisibility(View.GONE);
                binding.inputEmailOrPhone.setHint("Enter Email");
                binding.inputEmailOrPhone.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                Log.d(TAG, "setupUI: User selected phone sign-in");
                binding.inputPassword.setVisibility(View.GONE);
                binding.countryCodePicker.setVisibility(View.VISIBLE);
                binding.inputEmailOrPhone.setHint("Enter Phone Number");
                binding.inputEmailOrPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                binding.countryCodePicker.registerCarrierNumberEditText(binding.inputEmailOrPhone);
            }
        });
    }

    /**
     * Sets up listeners for buttons and navigation actions.
     */
    private void setListeners() {
        Log.d(TAG, "setListeners: Setting listeners for buttons");

        binding.buttonSignIn.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Sign-in button clicked");
            if (isValidSignInDetails()) {
                if (binding.radioSignInWithPhone.isChecked()) {
                    Log.d(TAG, "onClick: Proceeding with phone sign-in");
                    signInWithPhone();
                } else {
                    Log.d(TAG, "onClick: Proceeding with email sign-in");
                    signInWithEmail();
                }
            }
        });

        binding.textCreateNewAccount.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Create new account clicked");
            showAccountCreationOptions();
        });
    }

    /**
     * Validates the sign-in details entered by the user.
     */
    private boolean isValidSignInDetails() {
        Log.d(TAG, "isValidSignInDetails: Validating sign-in details");
        boolean isValid = false;
        String emailOrPhone = binding.inputEmailOrPhone.getText().toString().trim();

        if (binding.radioSignInWithEmail.isChecked()) {
            String password = binding.inputPassword.getText().toString().trim();

            if (emailOrPhone.isEmpty()) {
                Log.w(TAG, "isValidSignInDetails: Email field is empty");
                Utilities.showToast(this, "Please enter your email", Utilities.ToastType.WARNING);
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
                Log.w(TAG, "isValidSignInDetails: Invalid email format");
                Utilities.showToast(this, "Please enter a valid email", Utilities.ToastType.WARNING);
            } else if (password.isEmpty()) {
                Log.w(TAG, "isValidSignInDetails: Password field is empty");
                Utilities.showToast(this, "Please enter your password", Utilities.ToastType.WARNING);
            } else {
                isValid = true;
                Log.d(TAG, "isValidSignInDetails: Email and password are valid");
            }
        } else if (binding.radioSignInWithPhone.isChecked()) {
            if (!binding.countryCodePicker.isValidFullNumber()) {
                Log.w(TAG, "isValidSignInDetails: Invalid phone number format");
                Utilities.showToast(this, "Please enter a valid phone number", Utilities.ToastType.WARNING);
            } else {
                isValid = true;
                Log.d(TAG, "isValidSignInDetails: Phone number is valid");
            }
        }

        return isValid;
    }

    /**
     * Handles phone sign-in by redirecting to OtpVerificationActivity.
     */
    private void signInWithPhone() {
        Log.d(TAG, "signInWithPhone: Redirecting to OTP verification for phone sign-in");
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();

        Intent intent = new Intent(SignInActivity.this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_PHONE, phoneNumber);
        intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_SIGN_IN);
        startActivity(intent);
        finish();
    }

    /**
     * Handles email/password sign-in.
     */
    private void signInWithEmail() {
        Log.d(TAG, "signInWithEmail: Attempting email/password sign-in");
        showLoadingIndicator(true);

        String email = binding.inputEmailOrPhone.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    showLoadingIndicator(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail: Email/password sign-in successful");
                        navigateToMainActivity();
                    } else {
                        Log.e(TAG, "signInWithEmail: Email/password sign-in failed", task.getException());
                        Utilities.showToast(this, "Authentication failed. Please try again.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Navigates to MainActivity after successful authentication.
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Displays a dialog to choose between email and phone for account creation.
     */
    private void showAccountCreationOptions() {
        Log.d(TAG, "showAccountCreationOptions: Showing account creation options dialog");
        new android.app.AlertDialog.Builder(this)
                .setTitle("Choose Sign-Up Method")
                .setItems(new String[]{"Sign Up with Email", "Sign Up with Phone"}, (dialog, which) -> {
                    if (which == 0) {
                        Log.d(TAG, "showAccountCreationOptions: Redirecting to email sign-up");
                        startActivity(new Intent(getApplicationContext(), SignUpWithEmailActivity.class));
                    } else {
                        Log.d(TAG, "showAccountCreationOptions: Redirecting to phone sign-up");
                        startActivity(new Intent(getApplicationContext(), SignUpWithPhoneActivity.class));
                    }
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Toggles the loading state of the sign-in process.
     */
    private void showLoadingIndicator(boolean isLoading) {
        Log.d(TAG, "showLoadingIndicator: Setting loading state to " + isLoading);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.textCreateNewAccount.setEnabled(!isLoading);
    }
}