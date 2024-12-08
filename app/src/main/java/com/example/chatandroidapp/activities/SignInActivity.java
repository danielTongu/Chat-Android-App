package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

/**
 * SignInActivity handles user authentication via email/password or phone/OTP.
 */
public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        setDefaultCountry();
        setListeners();
    }

    /**
     * Sets the default country for the CountryCodePicker.
     */
    private void setDefaultCountry() {
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
    }

    /**
     * Sets up listeners for the UI elements.
     */
    private void setListeners() {
        // Toggle input fields and enable CountryCodePicker for phone sign-in
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSignInWithEmail) {
                binding.inputPassword.setVisibility(View.VISIBLE);
                binding.countryCodePicker.setVisibility(View.GONE);
                binding.inputEmailOrPhone.setHint("Enter Email");
                binding.inputEmailOrPhone.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else {
                binding.inputPassword.setVisibility(View.GONE);
                binding.countryCodePicker.setVisibility(View.VISIBLE);
                binding.inputEmailOrPhone.setHint("Enter Phone Number");
                binding.inputEmailOrPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                binding.countryCodePicker.registerCarrierNumberEditText(binding.inputEmailOrPhone);
            }
        });

        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                if (binding.radioSignInWithPhone.isChecked()) {
                    signInWithPhone();
                } else {
                    signInWithEmail();
                }
            }
        });

        binding.textCreateNewAccount.setOnClickListener(v -> showAccountCreationOptions());
    }

    /**
     * Signs in using email and password.
     */
    private void signInWithEmail() {
        showLoadingIndicator(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmailOrPhone.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, User.hashPassword(binding.inputPassword.getText().toString().trim()))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        saveUserToPreferences(Objects.requireNonNull(task.getResult().getDocuments().get(0)));
                        navigateToMainActivity();
                    } else {
                        Utilities.showToast(this, "Unable to sign in with email", Utilities.ToastType.WARNING);
                        showLoadingIndicator(false);
                    }
                })
                .addOnFailureListener(exception -> {
                    Utilities.showToast(this, exception.getMessage(), Utilities.ToastType.ERROR);
                    showLoadingIndicator(false);
                });
    }

    /**
     * Signs in using a phone number, redirecting to OTPVerificationActivity.
     */
    private void signInWithPhone() {
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();

        if (phoneNumber.isEmpty()) {
            Utilities.showToast(this, "Please enter a valid phone number", Utilities.ToastType.WARNING);
            return;
        }

        Intent intent = new Intent(SignInActivity.this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_PHONE, phoneNumber);
        startActivity(intent);
    }

    /**
     * Validates the sign-in details entered by the user.
     */
    private boolean isValidSignInDetails() {
        String emailOrPhone = binding.inputEmailOrPhone.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (emailOrPhone.isEmpty()) {
            Utilities.showToast(this, "Please enter your email or phone number", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.radioSignInWithEmail.isChecked() && !Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()) {
            Utilities.showToast(this, "Please enter a valid email", Utilities.ToastType.WARNING);
            return false;
        } else if (binding.radioSignInWithPhone.isChecked() && !binding.countryCodePicker.isValidFullNumber()) {
            Utilities.showToast(this, "Please enter a valid phone number", Utilities.ToastType.WARNING);
            return false;
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Please enter your password", Utilities.ToastType.WARNING);
            return false;
        } else if (password.length() < Constants.KEY_PASSWORD_MIN_LENGTH) {
            Utilities.showToast(this, String.format("Password must be at least %d characters", Constants.KEY_PASSWORD_MIN_LENGTH), Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Saves the user's data to shared preferences.
     */
    private void saveUserToPreferences(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
        preferenceManager.putString(Constants.KEY_FIRST_NAME, documentSnapshot.getString(Constants.KEY_FIRST_NAME));
        preferenceManager.putString(Constants.KEY_LAST_NAME, documentSnapshot.getString(Constants.KEY_LAST_NAME));
        preferenceManager.putString(Constants.KEY_EMAIL, documentSnapshot.getString(Constants.KEY_EMAIL));
        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
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
     * Displays a dialog to choose the account creation method.
     */
    private void showAccountCreationOptions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Choose Sign-Up Method")
                .setItems(new String[]{"Sign Up with Email", "Sign Up with Phone"}, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(getApplicationContext(), SignUpWithEmailActivity.class));
                    } else {
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
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.textCreateNewAccount.setEnabled(!isLoading);
    }
}