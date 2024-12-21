package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Handles user authentication via email/password or phone/OTP.
 * Manages UI states, input validations, and navigation after successful authentication.
 */
public class SignInActivity extends AppCompatActivity {

    /** Key used to store whether the user is signed in. */
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    /** Action type for sign-in, used when starting OtpVerificationActivity. */
    public static final String ACTION_SIGN_IN = "signIn";

    /** Tag for logging and debugging purposes. */
    private static final String TAG = "SIGN_IN_ACTIVITY";

    /** Binding object for accessing views defined in activity_sign_in.xml. */
    private ActivitySignInBinding binding;

    /** Manages application preferences like login status, user info, etc. */
    private PreferenceManager preferenceManager;

    /** Firebase Firestore instance for database operations. */
    private FirebaseFirestore firestore;

    /**
     * Called when the activity is created.
     * Initializes UI, checks existing login state, and sets up event listeners.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     * previously being shut down then this Bundle contains the data it most recently
     * supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();

        // If user is already signed in, go directly to MainActivity
        if (preferenceManager.getBoolean(KEY_IS_SIGNED_IN, false)) {
            navigateToMainActivity();
        } else {
            setupUI();
            setListeners();
        }
    }

    /**
     * Initializes Firestore instance and PreferenceManager for user data management.
     */
    private void initializeComponents() {
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Configures UI elements, sets default values, and registers views.
     */
    private void setupUI() {
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);
    }

    /**
     * Sets up various click listeners for UI interactions.
     * Also sets up the default sign-in method (phone by default).
     */
    private void setListeners() {
        binding.radioGroupSignInMethod.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            /**
             * Called when the checked radio button has changed.
             *
             * @param group     The group in which the checked radio button has changed.
             * @param checkedId The unique identifier of the newly checked radio button.
             */
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                toggleInputFields(checkedId);
            }
        });

        binding.radioGroupSignInMethod.check(R.id.radioSignInWithPhone); // Set default sign-in method to phone

        binding.buttonSignIn.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the sign-in button is clicked.
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) { handleSignIn(); }
        });

        binding.textCreateNewAccount.setOnClickListener(new View.OnClickListener() {
            /**
             * Called when the "Create New Account" text is clicked.
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            }
        });
    }

    /**
     * Toggles between email/password fields and phone fields based on the sign-in method selected.
     *
     * @param checkedId The ID of the currently checked radio button.
     */
    private void toggleInputFields(int checkedId) {
        if (checkedId == R.id.radioSignInWithEmail) {
            // Show email and password fields, hide phone fields
            binding.inputEmail.setVisibility(View.VISIBLE);
            binding.inputPassword.setVisibility(View.VISIBLE);
            binding.layoutCcpAndPhone.setVisibility(View.GONE);
            binding.buttonSignIn.setText("Sign In");
        } else {
            // Show phone fields, hide email and password fields
            binding.inputEmail.setVisibility(View.GONE);
            binding.inputPassword.setVisibility(View.GONE);
            binding.layoutCcpAndPhone.setVisibility(View.VISIBLE);
            binding.buttonSignIn.setText("Send Code");
        }
    }

    /**
     * Determines which sign-in method is chosen (phone or email) and proceeds accordingly.
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
     * Validates and initiates phone-based sign-in.
     * If valid, navigates the user to OTP verification screen.
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
     * Checks if the provided phone number is valid. If invalid, shows a warning.
     *
     * @param phoneNumber The phone number to validate.
     * @return True if valid; otherwise false.
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty() || !binding.countryCodePicker.isValidFullNumber()) {
            Utilities.showToast(this, "Invalid phone number.", Utilities.ToastType.WARNING);
            binding.inputPhoneNumber.setError("Invalid phone number");
            return false;
        }
        return true;
    }

    /**
     * Navigates the user to the OTP verification screen for phone-based authentication.
     */
    private void navigateToOtpVerification() {
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, ACTION_SIGN_IN);
        startActivity(intent);
        showLoading(false, "");
    }

    /**
     * Validates user inputs and initiates email-based sign-in if inputs are valid.
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
     * Authenticates the user with Firestore by checking email and hashed password.
     *
     * @param email    The user-entered email address.
     * @param password The user-entered password.
     */
    private void authenticateUser(final String email, final String password) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    /**
                     * Called when the Firestore query completes.
                     * @param task The completed task for the Firestore query.
                     */
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            verifyPassword(task.getResult().getDocuments().get(0), password);
                        } else {
                            showAuthenticationError();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when the Firestore query fails.
                     * @param e The exception that caused the failure.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showAuthenticationError();
                    }
                });
    }

    /**
     * Verifies if the provided password matches the stored hashed password.
     *
     * @param document Firestore document containing user details.
     * @param password The user-entered password.
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
     * Saves user details (e.g., name, phone, email, etc.) to shared preferences upon successful authentication.
     * @param user The authenticated user's details.
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
     * Navigates the user to the MainActivity, clearing the activity stack to prevent
     * back navigation to the sign-in screen.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // Clear the activity stack to ensure users can't go back to sign-in after logging in
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Checks whether the entered email and password are valid.
     *
     * @param email    The entered email address.
     * @param password The entered password.
     * @return True if both fields are valid; otherwise false.
     */
    private boolean isValidEmailAndPassword(String email, String password) {
        if (email.isEmpty()) {
            Utilities.showToast(this, "Enter your email.", Utilities.ToastType.WARNING);
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utilities.showToast(this, "Enter a valid email.", Utilities.ToastType.WARNING);
            return false;
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Enter your password.", Utilities.ToastType.WARNING);
            return false;
        }
        return true;
    }

    /**
     * Shows an error message when authentication fails and resets the loading indicator.
     */
    private void showAuthenticationError() {
        Utilities.showToast(this, "Invalid email or password.", Utilities.ToastType.ERROR);
        showLoading(false, "");
    }

    /**
     * Displays or hides a loading indicator while processing sign-in requests.
     * Disables user interaction when loading is active.
     *
     * @param isLoading True to show loading indicator, false to hide.
     * @param message   An optional message to display alongside the loading indicator.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.layoutProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textProgressMessage.setText(message);

        // Disable/enable interactive elements
        binding.radioSignInWithEmail.setEnabled(!isLoading);
        binding.radioSignInWithPhone.setEnabled(!isLoading);
        binding.inputPhoneNumber.setEnabled(!isLoading);
        binding.layoutCcpAndPhone.setEnabled(!isLoading);
        binding.inputEmail.setEnabled(!isLoading);
        binding.inputPassword.setEnabled(!isLoading);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.textCreateNewAccount.setEnabled(!isLoading);
    }
}