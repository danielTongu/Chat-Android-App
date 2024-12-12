// SignInActivity.java
package com.example.chatandroidapp.activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.module.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * SignInActivity handles user authentication via email/password or phone/OTP.
 * It includes network connectivity checks, UI state management to prevent multiple sign-in attempts,
 * and user feedback mechanisms.
 */
public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SIGN_IN_ACTIVITY";
    private ActivitySignInBinding binding;
    private FirebaseAuth firebaseAuth;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using View Binding
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize PreferenceManager
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());

        Log.d(TAG, "onCreate: Initializing SignInActivity");
        setupUI();
        setListeners();
    }

    /**
     * Configures default settings for the UI, such as input visibility
     * based on the selected sign-in method (email or phone).
     */
    private void setupUI() {
        Log.d(TAG, "setupUI: Configuring UI for sign-in options");

        // Configure CountryCodePicker
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);

        // Listener for RadioGroup to toggle input fields based on sign-in method
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioSignInWithEmail) {
                Log.d(TAG, "setupUI: User selected email sign-in");
                binding.inputEmail.setVisibility(View.VISIBLE);
                binding.inputPassword.setVisibility(View.VISIBLE);
                binding.ccpAndPhoneLayout.setVisibility(View.GONE);
                binding.buttonSignIn.setText("Sign In");
            } else if (checkedId == R.id.radioSignInWithPhone) {
                Log.d(TAG, "setupUI: User selected phone sign-in");
                binding.inputEmail.setVisibility(View.GONE);
                binding.inputPassword.setVisibility(View.GONE);
                binding.ccpAndPhoneLayout.setVisibility(View.VISIBLE);
                binding.buttonSignIn.setText("Send Code");
            }
        });

        // Set initial visibility based on default selected RadioButton
        binding.radioGroupSignInMethod.check(R.id.radioSignInWithPhone);
    }

    /**
     * Sets up click listeners for the sign-in button and navigation to the
     * account creation activity.
     */
    private void setListeners() {
        Log.d(TAG, "setListeners: Setting listeners for buttons");

        // Sign-In button listener
        binding.buttonSignIn.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Sign-in button clicked");
            if (isNetworkAvailable()) {
                handleSignIn();
            } else {
                Utilities.showToast(this, "No internet connection. Please check your network settings.", Utilities.ToastType.ERROR);
            }
        });

        // Create New Account link listener
        binding.textCreateNewAccount.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Redirecting to SignUpActivity");
            startActivity(new Intent(getApplicationContext(), SignUpActivity.class));
        });
    }

    /**
     * Handles the sign-in process based on the selected sign-in method.
     * Disables UI components during processing to prevent multiple sign-in attempts.
     */
    private void handleSignIn() {
        // Disable UI components to prevent multiple clicks
        setUIEnabled(false);

        if (binding.radioSignInWithPhone.isChecked()) {
            signInWithPhone();
        } else {
            signInWithEmail();
        }
    }

    /**
     * Enables or disables the sign-in button and radio buttons.
     *
     * @param enabled True to enable UI components, false to disable.
     */
    private void setUIEnabled(boolean enabled) {
        binding.buttonSignIn.setEnabled(enabled);
        binding.radioGroupSignInMethod.setEnabled(enabled);
        if (!enabled) {
            // Show progress bar when disabling UI
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            // Hide progress bar when enabling UI
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Initiates phone-based sign-in by redirecting to the OTP verification activity.
     */
    private void signInWithPhone() {
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();
        if (!isValidPhoneNumber(phoneNumber)) {
            // Re-enable UI components if validation fails
            setUIEnabled(true);
            return;
        }

        Log.d(TAG, "signInWithPhone: Redirecting to OTP verification for phone sign-in");

        // Save phone number to SharedPreferences
        preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);

        // Redirect to OtpVerificationActivity with action type
        Intent intent = new Intent(SignInActivity.this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, Constants.ACTION_SIGN_IN);
        startActivity(intent);

        // Re-enable UI components after initiating OTP verification
        setUIEnabled(true);
    }

    /**
     * Validates the entered phone number.
     *
     * @param phoneNumber The phone number entered by the user.
     * @return True if the phone number is valid, false otherwise.
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        boolean isValid = !phoneNumber.isEmpty() && binding.countryCodePicker.isValidFullNumber();
        if (!isValid) {
            Utilities.showToast(this, "Please enter a valid phone number.", Utilities.ToastType.WARNING);
            binding.inputPhoneNumber.setError("Invalid phone number");
        }
        return isValid;
    }

    /**
     * Initiates email-based sign-in and fetches user data upon successful authentication.
     */
    private void signInWithEmail() {
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (!isValidEmailAndPassword(email, password)) {
            // Re-enable UI components if validation fails
            setUIEnabled(true);
            return;
        }

        Log.d(TAG, "signInWithEmail: Attempting email/password sign-in");

        // Attempt to sign in with email and password
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setUIEnabled(true);// Re-enable UI components after processing
                    if (task.isSuccessful()) {
                        Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);
                        fetchUserDataAndNavigate(firebaseAuth.getCurrentUser().getUid());
                    } else {
                        Log.e(TAG, "signInWithEmail: Email/password sign-in failed", task.getException());
                        Utilities.showToast(this, "Authentication failed. Please check your credentials.", Utilities.ToastType.ERROR);
                    }
                });
    }

    /**
     * Validates the email and password entered by the user.
     *
     * @param email    The email entered by the user.
     * @param password The password entered by the user.
     * @return True if the email and password are valid, false otherwise.
     */
    private boolean isValidEmailAndPassword(String email, String password) {
        if (email.isEmpty()) {
            Utilities.showToast(this, "Please enter your email.", Utilities.ToastType.WARNING);
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utilities.showToast(this, "Please enter a valid email.", Utilities.ToastType.WARNING);
            return false;
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Please enter your password.", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Fetches user data from Firestore using the user's UID and saves it to SharedPreferences.
     * Navigates to MainActivity upon successful data retrieval.
     *
     * @param userId The UID of the authenticated user.
     */
    private void fetchUserDataAndNavigate(String userId) {
        Log.d(TAG, "fetchUserDataAndNavigate: Fetching user data for UID: " + userId);

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            saveUserPreferences(user);
                            navigateToMainActivity();
                        } else {
                            Log.e(TAG, "fetchUserDataAndNavigate: User object is null");
                            Utilities.showToast(this, "Failed to retrieve user data.", Utilities.ToastType.ERROR);
                        }
                    } else {
                        Log.e(TAG, "fetchUserDataAndNavigate: User document does not exist");
                        Utilities.showToast(this, "User not found.", Utilities.ToastType.ERROR);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "fetchUserDataAndNavigate: Failed to fetch user data", e);
                    Utilities.showToast(this, "Failed to retrieve user data. Please sign in again.", Utilities.ToastType.ERROR);
                });
    }

    /**
     * Saves user data to SharedPreferences.
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
     * Navigates to MainActivity after saving user preferences.
     */
    private void navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Checks if the device has an active network connection.
     *
     * @return True if network is available, false otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            // For Android API level 23 and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
            } else {
                // For older Android versions
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }
}