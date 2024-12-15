package com.example.chatandroidapp.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Handles user authentication with email/password or phone/OTP.
 * Includes UI state management, validations, and navigation.
 */
public class SignInActivity extends AppCompatActivity {
    public static final String ACTION_SIGN_IN = "signIn";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    private static final String TAG = "SIGN_IN_ACTIVITY";

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();

        if (preferenceManager.getBoolean(KEY_IS_SIGNED_IN, false)) {
            Log.d(TAG, "User already signed in. Navigating to MainActivity.");
            navigateToMainActivity();
            return; // Skip the rest of the setup if the user is already signed in
        }

        setupUI();
        setListeners();
    }

    /**
     * Initializes required components and managers.
     */
    private void initializeComponents() {
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
        Log.d(TAG, "Components initialized");
    }

    /**
     * Configures UI settings and input visibility for sign-in methods.
     */
    private void setupUI() {
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);

        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> toggleInputFields(checkedId));
        binding.radioGroupSignInMethod.check(R.id.radioSignInWithPhone); // Default selection
    }

    /**
     * Sets up event listeners for buttons and inputs.
     */
    private void setListeners() {
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                handleSignIn();
            } else {
                Utilities.showToast(this, "No internet connection. Check your network settings.", Utilities.ToastType.ERROR);
            }
        });

        binding.textCreateNewAccount.setOnClickListener(v -> startActivity(new Intent(this, SignUpActivity.class)));
    }

    /**
     * Toggles input fields based on the selected sign-in method.
     *
     * @param checkedId ID of the selected radio button.
     */
    private void toggleInputFields(int checkedId) {
        if (checkedId == R.id.radioSignInWithEmail) {
            showEmailFields();
        } else if (checkedId == R.id.radioSignInWithPhone) {
            showPhoneFields();
        }
    }

    private void showEmailFields() {
        binding.inputEmail.setVisibility(View.VISIBLE);
        binding.inputPassword.setVisibility(View.VISIBLE);
        binding.ccpAndPhoneLayout.setVisibility(View.GONE);
        binding.buttonSignIn.setText("Sign In");
    }

    private void showPhoneFields() {
        binding.inputEmail.setVisibility(View.GONE);
        binding.inputPassword.setVisibility(View.GONE);
        binding.ccpAndPhoneLayout.setVisibility(View.VISIBLE);
        binding.buttonSignIn.setText("Send Code");
    }

    /**
     * Initiates the appropriate sign-in method based on user selection.
     */
    private void handleSignIn() {
        showLoading(true, "Processing...");
        if (binding.radioSignInWithPhone.isChecked()) {
            signInWithPhone();
        } else {
            signInWithEmail();
        }
    }

    /**
     * Handles phone-based sign-in.
     */
    private void signInWithPhone() {
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();
        if (isValidPhoneNumber(phoneNumber)) {
            preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
            navigateToOtpVerification();
        } else {
            showLoading(false, "");
        }
    }

    /**
     * Validates and processes email-based sign-in.
     */
    private void signInWithEmail() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (isValidEmailAndPassword(email, password)) {
            authenticateUser(email, password);
        } else {
            showLoading(false, "");
        }
    }

    /**
     * Authenticates the user via Firestore for email-based sign-in.
     */
    private void authenticateUser(String email, String password) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        verifyPassword(task.getResult().getDocuments().get(0), password);
                    } else {
                        showAuthenticationError();
                    }
                })
                .addOnFailureListener(e -> showAuthenticationError());
    }

    /**
     * Verifies the entered password against the stored hashed password.
     */
    private void verifyPassword(DocumentSnapshot document, String password) {
        User user = document.toObject(User.class);
        if (user != null && User.hashPassword(password).equals(user.hashedPassword)) {
            saveUserPreferences(user);
            navigateToMainActivity();
        } else {
            showAuthenticationError();
        }
    }

    /**
     * Displays authentication failure messages.
     */
    private void showAuthenticationError() {
        Utilities.showToast(this, "Invalid email or password.", Utilities.ToastType.ERROR);
        showLoading(false, "");
    }

    /**
     * Saves user details to shared preferences.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.fcmToken);
    }

    /**
     * Navigates to OTP verification activity for phone-based sign-in.
     */
    private void navigateToOtpVerification() {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, ACTION_SIGN_IN);
        startActivity(intent);
        showLoading(false, "");
    }

    /**
     * Navigates to the main activity after a successful sign-in.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Validates the entered phone number.
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty() || !binding.countryCodePicker.isValidFullNumber()) {
            Utilities.showToast(this, "Please enter a valid phone number.", Utilities.ToastType.WARNING);
            binding.inputPhoneNumber.setError("Invalid phone number");
            return false;
        }
        return true;
    }

    /**
     * Validates the entered email and password.
     */
    private boolean isValidEmailAndPassword(String email, String password) {
        boolean isValid = false;
        if (email.isEmpty()) {
            Utilities.showToast(this, "Please enter your email.", Utilities.ToastType.WARNING);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utilities.showToast(this, "Please enter a valid email.", Utilities.ToastType.WARNING);
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Please enter your password.", Utilities.ToastType.WARNING);
        } else {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Checks if there is an active internet connection.
     */
    private boolean isNetworkAvailable() {
        boolean isAvailable = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isAvailable = checkNetworkCapabilities(connectivityManager);
            } else {
                isAvailable = checkNetworkInfo(connectivityManager);
            }
        }
        return isAvailable;
    }

    private boolean checkNetworkCapabilities(ConnectivityManager connectivityManager) {
        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    private boolean checkNetworkInfo(ConnectivityManager connectivityManager) {
        return connectivityManager.getActiveNetworkInfo() != null &&
                connectivityManager.getActiveNetworkInfo().isConnected();
    }

    /**
     * Displays or hides the loading indicator with an optional message.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.radioGroupSignInMethod.setEnabled(!isLoading);
        binding.textCreateNewAccount.setVisibility(isLoading ? View.GONE : View.VISIBLE);

        if (message != null && !message.isEmpty()) {
            binding.processMessage.setText(message);
            binding.processMessage.setVisibility(View.VISIBLE);
        } else {
            binding.processMessage.setVisibility(View.GONE);
        }
    }
}