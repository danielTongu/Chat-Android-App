package com.example.chatandroidapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.databinding.ActivityOtpVerificationBinding;
import com.example.chatandroidapp.fragments.ProfileFragment;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.concurrent.TimeUnit;

/**
 * Handles OTP verification for account-related actions such as sign-up, sign-in, or phone number updates.
 */
public class OtpVerificationActivity extends AppCompatActivity {
    private static final String TAG = "OTP_VERIFICATION";

    private ActivityOtpVerificationBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String verificationId;
    private String phoneNumber;
    private String actionType;
    private PreferenceManager preferenceManager;

    /**
     * Called when the activity is created. Initializes the layout and sets up the UI and components.
     * @param savedInstanceState Saved instance state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupUI();
        handleIntentData();
    }

    /**
     * Initializes Firebase components, Firestore instance, and shared preference manager.
     */
    private void initializeComponents() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Sets up the UI by configuring click listeners and other UI-related functionalities.
     */
    private void setupUI() {
        binding.buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        binding.buttonVerifyOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String otp = binding.inputOtp.getText().toString().trim();
                if (validateOtp(otp)) {
                    verifyOtp(otp);
                }
            }
        });
    }

    /**
     * Validates the OTP entered by the user.
     *
     * @param otp The OTP entered by the user.
     * @return True if OTP is valid, false otherwise.
     */
    private boolean validateOtp(String otp) {
        showLoading(true, "Validating entry...");
        boolean isValid = false;

        if (otp.isEmpty()) {
            binding.inputOtp.setError("Please enter the OTP.");
        } else if (otp.length() < 6) {
            binding.inputOtp.setError("OTP must be 6 digits.");
        } else {
            isValid = true;
        }

        return isValid;
    }

    /**
     * Initiates OTP verification using the provided OTP.
     * @param otp The OTP entered by the user.
     */
    private void verifyOtp(String otp) {
        showLoading(true, "Verifying OTP...");

        if (verificationId == null) {
            Utilities.showToast(this, "Verification ID not available.", Utilities.ToastType.ERROR);
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);
        handleVerificationSuccess(credential);
    }

    /**
     * Handles a successful credential verification by signing in with Firebase Auth.
     * @param credential A valid PhoneAuthCredential obtained from the OTP.
     */
    private void handleVerificationSuccess(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            /**
             * Called upon completion of the sign-in attempt.
             * @param task The result of the sign-in attempt.
             */
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    proceedWithAction();
                } else {
                    showError("OTP verification failed.");
                }
            }
        });
    }

    /**
     * Proceeds with the next step based on the action type.
     * Action types could be sign-up, sign-in, or phone number update.
     */
    private void proceedWithAction() {
        if (SignUpActivity.ACTION_SIGN_UP.equals(actionType)) {
            signUpAndNavigateToMainActivity();
        } else if (SignInActivity.ACTION_SIGN_IN.equals(actionType)) {
            signInAndNavigateToMainActivity();
        } else if (ProfileFragment.ACTION_UPDATE_PHONE.equals(actionType)) {
            updatePhoneNumberInFirestore();
        } else {
            navigateToMainActivity();
        }
    }

    /**
     * Handles new user registration:
     * - Creates a new user document in Firestore.
     * - Saves user data locally.
     * - Navigates to MainActivity.
     */
    private void signUpAndNavigateToMainActivity() {
        showLoading(true, "Registering new user...");

        final User user = new User();
        user.id = firestore.collection(Constants.KEY_COLLECTION_USERS).document().getId();
        user.phone = phoneNumber;
        user.firstName = preferenceManager.getString(Constants.KEY_FIRST_NAME, "");
        user.lastName = preferenceManager.getString(Constants.KEY_LAST_NAME, "");
        user.image = preferenceManager.getString(Constants.KEY_IMAGE, "");

        firestore.collection(Constants.KEY_COLLECTION_USERS).document(user.id)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when user details are successfully saved.
                     * @param unused Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        saveUserPreferences(user);
                        navigateToMainActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when saving user details fails.
                     * @param e The exception occurred.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showError("Failed to save user details.");
                    }
                });
    }

    /**
     * Queries Firestore for the user by the phone number:
     * - If found, saves preferences and navigates to MainActivity.
     * - If not found, displays an error.
     */
    private void signInAndNavigateToMainActivity() {
        showLoading(true, "Verifying phone number...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, phoneNumber)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Called upon successful retrieval of user data.
                     * @param queryDocumentSnapshots The query result.
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            User user = documentSnapshot.toObject(User.class);
                            saveUserPreferences(user);
                            navigateToMainActivity();
                        } else {
                            showError("User not found. Please sign up.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when user data retrieval fails.
                     * @param e The exception occurred.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showError("Failed to fetch user details.");
                    }
                });
    }

    /**
     * Handles incoming intent data:
     * - Determines the action type.
     * - Fetches the appropriate phone number.
     * - Initiates OTP sending.
     */
    private void handleIntentData() {
        showLoading(true, "Determining action...");

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Constants.KEY_ACTION_TYPE)) {
            actionType = intent.getStringExtra(Constants.KEY_ACTION_TYPE);
            phoneNumber = fetchPhoneNumber(intent);
            if (phoneNumber != null) {
                binding.textPhoneNumber.setText(String.format("Phone Number: %s", phoneNumber));
                sendOtp(phoneNumber);
            } else {
                showErrorAndFinish("Phone number not available.");
            }
        } else {
            showErrorAndFinish("No action specified.");
        }
    }

    /**
     * Fetches the phone number based on the action type.
     *
     * @param intent The intent containing action type and possibly phone number info.
     * @return The phone number if available, null otherwise.
     */
    private String fetchPhoneNumber(Intent intent) {
        showLoading(true, "Fetching Phone Number...");

        if (ProfileFragment.ACTION_UPDATE_PHONE.equals(actionType)) {
            return intent.getStringExtra(Constants.KEY_PHONE);
        } else if (SignInActivity.ACTION_SIGN_IN.equals(actionType)
                || SignUpActivity.ACTION_SIGN_UP.equals(actionType)) {
            return preferenceManager.getString(Constants.KEY_PHONE, "");
        }
        return null;
    }

    /**
     * Sends an OTP to the specified phone number using Firebase's PhoneAuthProvider.
     * @param phoneNumber The phone number to which the OTP should be sent.
     */
    private void sendOtp(String phoneNumber) {
        showLoading(true, "Sending OTP...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    /**
                     * Called when verification is completed successfully.
                     * @param credential The verified credential.
                     */
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        handleVerificationSuccess(credential);
                    }

                    /**
                     * Called when verification fails.
                     * @param e The exception that caused verification failure.
                     */
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Log.e(TAG, "OTP sending failed", e);
                        showError("Failed to send OTP. Please try again.");
                    }

                    /**
                     * Called when code is sent to the user's phone.
                     *
                     * @param id The verification ID returned by Firebase.
                     * @param token The force resending token.
                     */
                    @Override
                    public void onCodeSent(@NonNull String id, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        showLoading(false, "OTP sent successfully.\nPlease check your phone.");
                        Utilities.showToast(OtpVerificationActivity.this, "", Utilities.ToastType.SUCCESS);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    /**
     * Updates the user's phone number in Firestore.
     */
    private void updatePhoneNumberInFirestore() {
        showLoading(true, "Saving new number...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_ID, ""))
                .update(Constants.KEY_PHONE, phoneNumber)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    /**
                     * Called when phone number update is successful.
                     * @param unused Unused parameter.
                     */
                    @Override
                    public void onSuccess(Void unused) {
                        preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
                        navigateToMainActivity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    /**
                     * Called when phone number update fails.
                     * @param e The exception occurred.
                     */
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showError("Failed to update phone number.");
                    }
                });
    }

    /**
     * Navigates the user to MainActivity and clears the activity stack.
     */
    private void navigateToMainActivity() {
        Utilities.showToast(this, "", Utilities.ToastType.SUCCESS);
        showLoading(false, null);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Saves user preferences locally using the provided User object.
     * @param user The user object whose details are to be saved in preferences.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(SignInActivity.KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
    }

    /**
     * Displays or hides the loading layout and shows a message if provided.
     * Disables interactive elements while loading.
     *
     * @param isLoading Indicates if the loading spinner should be visible.
     * @param message   The message to be shown alongside the loading spinner.
     */
    private void showLoading(boolean isLoading, String message) {
        boolean displayLayout = isLoading || (message != null && !message.isEmpty());
        binding.layoutProgress.setVisibility(displayLayout ? View.VISIBLE : View.INVISIBLE);
        binding.textProgressMessage.setText(message);
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        // Disable interactive elements during loading
        binding.buttonVerifyOtp.setEnabled(!isLoading);
        binding.inputOtp.setEnabled(!isLoading);
    }

    /**
     * Displays an error message to the user and re-enables UI.
     * @param message The error message to display.
     */
    private void showError(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        showLoading(false, null);
    }

    /**
     * Displays an error message, logs it, and finishes the activity.
     * @param message The error message to display.
     */
    private void showErrorAndFinish(String message) {
        Utilities.showToast(this, message, Utilities.ToastType.ERROR);
        Log.e(TAG, message);
        finish();
    }
}