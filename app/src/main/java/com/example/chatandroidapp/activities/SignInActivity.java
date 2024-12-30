package com.example.chatandroidapp.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatandroidapp.R;
import com.example.chatandroidapp.databinding.ActivitySignInBinding;
import com.example.chatandroidapp.fragments.ProfileFragment;
import com.example.chatandroidapp.models.User;
import com.example.chatandroidapp.utilities.Constants;
import com.example.chatandroidapp.utilities.PreferenceManager;
import com.example.chatandroidapp.utilities.Utilities;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

/**
 * Handles user sign-in workflows and provides static methods for account and task deletions.
 */
public class SignInActivity extends AppCompatActivity {
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String ACTION_SIGN_IN = "signIn";

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private String actionType;

    /**
     * Deletes the user and all their associated tasks from Firestore and FirebaseAuth.
     *
     * @param firestore    The FirebaseFirestore instance for database operations.
     * @param firebaseAuth The FirebaseAuth instance for authentication operations.
     * @param userId       The ID of the user to delete.
     * @param context      The context from which the method is called, used for displaying Toast messages.
     */
    public static void deleteUserAndTasks(@NonNull FirebaseFirestore firestore, @NonNull FirebaseAuth firebaseAuth, @NonNull String userId, @NonNull Context context) {
        // Reference to the user's Tasks subcollection
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId)
                .collection("Tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> tasks = queryDocumentSnapshots.getDocuments();

                    if (tasks.isEmpty()) {
                        // No tasks to delete, proceed to delete user document
                        deleteUserDocument(firestore, firebaseAuth, userId, context);
                    } else {
                        // Delete tasks in batches
                        deleteTasksInBatches(firestore, userId, tasks, 0, context);
                    }
                })
                .addOnFailureListener(e -> {
                    String error = "Failed to fetch tasks: " + e.getMessage();
                    Log.e("SignInActivity", error, e);
                    Utilities.showToast(context, error, Utilities.ToastType.ERROR);
                });
    }

    /**
     * Deletes tasks in batches of 500 to comply with Firestore's batch operation limits.
     *
     * @param firestore The FirebaseFirestore instance.
     * @param userId    The ID of the user.
     * @param tasks     The list of task documents.
     * @param index     The current index in the list.
     * @param context   The context for displaying Toast messages.
     */
    private static void deleteTasksInBatches(FirebaseFirestore firestore, String userId, List<DocumentSnapshot> tasks, int index, Context context) {
        int batchSize = 500; // Firestore's maximum batch size
        int end = Math.min(index + batchSize, tasks.size());

        WriteBatch batch = firestore.batch();

        for (int i = index; i < end; i++) {
            DocumentSnapshot task = tasks.get(i);
            batch.delete(task.getReference());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (end < tasks.size()) {
                        // More tasks to delete
                        deleteTasksInBatches(firestore, userId, tasks, end, context);
                    } else {
                        // All tasks deleted, proceed to delete user document
                        deleteUserDocument(firestore, FirebaseAuth.getInstance(), userId, context);
                    }
                })
                .addOnFailureListener(e -> {
                    String error = "Failed to delete tasks: " + e.getMessage();
                    Log.e("SignInActivity", error, e);
                    Utilities.showToast(context, error, Utilities.ToastType.ERROR);
                });
    }

    /**
     * Deletes the user document from Firestore and proceeds to delete the user from FirebaseAuth.
     *
     * @param firestore    The FirebaseFirestore instance.
     * @param firebaseAuth The FirebaseAuth instance.
     * @param userId       The ID of the user.
     * @param context      The context for displaying Toast messages.
     */
    private static void deleteUserDocument(FirebaseFirestore firestore, FirebaseAuth firebaseAuth, String userId, Context context) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(userId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Proceed to delete the user from FirebaseAuth
                    deleteUserFromAuth(firebaseAuth, context);
                })
                .addOnFailureListener(e -> {
                    String error = "Failed to delete user data: " + e.getMessage();
                    Log.e("SignInActivity", error, e);
                    Utilities.showToast(context, error, Utilities.ToastType.ERROR);
                });
    }

    /**
     * Deletes the user from FirebaseAuth.
     *
     * @param firebaseAuth The FirebaseAuth instance.
     * @param context      The context for displaying Toast messages.
     */
    private static void deleteUserFromAuth(FirebaseAuth firebaseAuth, Context context) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            String error = "User not authenticated.";
            Log.e("SignInActivity", error);
            Utilities.showToast(context, error, Utilities.ToastType.ERROR);
            return;
        }

        currentUser.delete()
                .addOnSuccessListener(aVoid -> {
                    String successMessage = "Account deleted successfully.";
                    Log.d("SignInActivity", successMessage);
                    Utilities.showToast(context, successMessage, Utilities.ToastType.SUCCESS);
                    // Clear preferences
                    PreferenceManager.getInstance(context).clear();
                    // Redirect to SignInActivity
                    navigateToSignInActivity(context);
                })
                .addOnFailureListener(e -> {
                    String error = "Failed to delete account: " + e.getMessage();
                    Log.e("SignInActivity", error, e);
                    Utilities.showToast(context, error, Utilities.ToastType.ERROR);
                });
    }

    /**
     * Navigates the user to the SignInActivity after account deletion.
     *
     * @param context The context from which the method is called.
     */
    private static void navigateToSignInActivity(Context context) {
        Intent intent = new Intent(context, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeComponents();
        handleActionType();
    }

    /**
     * Initializes Firebase, Firestore, and shared preferences.
     */
    private void initializeComponents() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        preferenceManager = PreferenceManager.getInstance(getApplicationContext());
    }

    /**
     * Determines the action type and sets up the UI accordingly.
     */
    private void handleActionType() {
        Intent intent = getIntent();
        actionType = intent != null && intent.hasExtra(Constants.KEY_ACTION_TYPE)
                ? intent.getStringExtra(Constants.KEY_ACTION_TYPE)
                : ACTION_SIGN_IN;

        if (preferenceManager.getBoolean(KEY_IS_SIGNED_IN, false) && !ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
            navigateToMainActivity();
        } else {
            setupUI();
            setListeners();
        }
    }

    /**
     * Sets up the UI for the current action type.
     */
    private void setupUI() {
        if (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
            binding.textTitle.setText("Delete Account");
            binding.textSubTitle.setText("Verify To Continue");
            binding.textCreateNewAccount.setText("Cancel Delete Account");
        }
        binding.countryCodePicker.setDefaultCountryUsingNameCode("US");
        binding.countryCodePicker.resetToDefaultCountry();
        binding.countryCodePicker.registerCarrierNumberEditText(binding.inputPhoneNumber);
    }

    /**
     * Sets up listeners for user interaction.
     */
    private void setListeners() {
        binding.radioGroupSignInMethod.setOnCheckedChangeListener((group, checkedId) -> toggleInputFields(checkedId));
        binding.radioGroupSignInMethod.check(R.id.radioSignInWithPhone);
        binding.buttonSignIn.setOnClickListener(v -> handleAction());
        binding.textCreateNewAccount.setOnClickListener(v -> {
            if (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
                finish();
            } else {
                startActivity(new Intent(this, SignUpActivity.class));
            }
        });
    }

    /**
     * Toggles between phone and email input fields based on the selected method.
     *
     * @param checkedId The ID of the selected radio button.
     */
    private void toggleInputFields(int checkedId) {
        boolean isPhone = checkedId == R.id.radioSignInWithPhone;
        binding.inputEmail.setVisibility(isPhone ? View.GONE : View.VISIBLE);
        binding.inputPassword.setVisibility(isPhone ? View.GONE : View.VISIBLE);
        binding.layoutCcpAndPhone.setVisibility(isPhone ? View.VISIBLE : View.GONE);
        binding.buttonSignIn.setText(isPhone ? "Send Code" : (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType) ? "Delete" : "Sign In"));
    }

    /**
     * Handles the current action (sign-in or account deletion verification).
     */
    private void handleAction() {
        showLoading(true, "Processing...");
        String phoneNumber = binding.countryCodePicker.getFullNumberWithPlus();
        String email = binding.inputEmail.getText().toString().trim();
        String password = binding.inputPassword.getText().toString().trim();

        if (binding.radioSignInWithPhone.isChecked() && isValidPhoneNumber(phoneNumber)) {
            navigateToOtpVerification(phoneNumber);
        } else if (isValidEmailAndPassword(email, password)) {
            authenticateUser(email, password);
        } else {
            showLoading(false, null);
        }
    }

    /**
     * Navigates to the OTP verification screen.
     *
     * @param phoneNumber The phone number to verify.
     */
    private void navigateToOtpVerification(String phoneNumber) {
        preferenceManager.putString(Constants.KEY_PHONE, phoneNumber);
        Intent intent = new Intent(this, OtpVerificationActivity.class);
        intent.putExtra(Constants.KEY_ACTION_TYPE, actionType);
        startActivity(intent);
    }

    /**
     * Authenticates the user with Firestore for email/password sign-in or account deletion.
     *
     * @param email    The user's email.
     * @param password The user's password.
     */
    private void authenticateUser(final String email, final String password) {
        showLoading(true, "Authenticating...");

        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        verifyPassword(task.getResult().getDocuments().get(0), password);
                    } else {
                        showAuthenticationError();
                    }
                })
                .addOnFailureListener(e -> showAuthenticationError());
    }

    /**
     * Verifies the user's password and proceeds accordingly.
     *
     * @param document The Firestore document containing the user data.
     * @param password The provided password.
     */
    private void verifyPassword(DocumentSnapshot document, String password) {
        User user = document.toObject(User.class);
        if (user != null && User.hashPassword(password).equals(user.hashedPassword)) {
            if (ProfileFragment.ACTION_DELETE_ACCOUNT.equals(actionType)) {
                deleteUserAndTasks(firestore, firebaseAuth, user.id, this);
            } else {
                saveUserPreferences(user);
                navigateToMainActivity();
            }
        } else {
            showAuthenticationError();
        }
    }

    /**
     * Navigates to the MainActivity.
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Displays or hides the loading UI.
     *
     * @param isLoading Whether to show the loading indicator.
     * @param message   The message to display during loading.
     */
    private void showLoading(boolean isLoading, String message) {
        binding.layoutProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textProgressMessage.setText(message);
        binding.buttonSignIn.setEnabled(!isLoading);
        binding.textCreateNewAccount.setEnabled(!isLoading);
    }

    /**
     * Displays an authentication error message.
     */
    private void showAuthenticationError() {
        Utilities.showToast(this, "Invalid credentials. Please try again.", Utilities.ToastType.ERROR);
        showLoading(false, null);
        preferenceManager.clear();
    }

    /**
     * Validates the phone number input.
     *
     * @param phoneNumber The phone number to validate.
     * @return True if valid, false otherwise.
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
     * Validates the email and password input.
     *
     * @param email    The email to validate.
     * @param password The password to validate.
     * @return True if valid, false otherwise.
     */
    private boolean isValidEmailAndPassword(String email, String password) {
        boolean isValid = false;
        if (email.isEmpty()) {
            Utilities.showToast(this, "Enter your email.", Utilities.ToastType.WARNING);
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utilities.showToast(this, "Enter a valid email.", Utilities.ToastType.WARNING);
        } else if (password.isEmpty()) {
            Utilities.showToast(this, "Enter your password.", Utilities.ToastType.WARNING);
        } else {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Saves user preferences after successful authentication.
     *
     * @param user The authenticated user.
     */
    private void saveUserPreferences(User user) {
        preferenceManager.putBoolean(KEY_IS_SIGNED_IN, true);
        preferenceManager.putString(Constants.KEY_ID, user.id);
        preferenceManager.putString(Constants.KEY_FIRST_NAME, user.firstName);
        preferenceManager.putString(Constants.KEY_LAST_NAME, user.lastName);
        preferenceManager.putString(Constants.KEY_PHONE, user.phone);
        preferenceManager.putString(Constants.KEY_EMAIL, user.email);
        preferenceManager.putString(Constants.KEY_IMAGE, user.image);
    }
}