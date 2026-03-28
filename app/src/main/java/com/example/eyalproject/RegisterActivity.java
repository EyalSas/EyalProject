package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername, editTextEmail, editTextPassword, editTextRePassword;
    private TextInputLayout usernameLayout, emailLayout, passwordLayout, rePasswordLayout;
    private Button buttonSignUp, buttonLogin;
    private ProgressBar registerProgress;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        firebaseHelper = new FirebaseHelper();

        buttonSignUp.setOnClickListener(v -> validateAndRegister());

        // ✅ FIX: Don't call finish() here — let the user press Back to return from LoginActivity.
        // Using FLAG_ACTIVITY_CLEAR_TOP ensures we don't stack multiple LoginActivity instances.
        buttonLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        usernameLayout   = findViewById(R.id.usernameLayout);
        emailLayout      = findViewById(R.id.emailLayout);
        passwordLayout   = findViewById(R.id.passwordLayout);
        rePasswordLayout = findViewById(R.id.repasswordLayout);

        editTextUsername  = findViewById(R.id.username);
        editTextEmail     = findViewById(R.id.gmailedit);
        editTextPassword  = findViewById(R.id.password);
        editTextRePassword = findViewById(R.id.repassword);
        buttonSignUp      = findViewById(R.id.btnsignup);
        buttonLogin       = findViewById(R.id.buttonLogin);
        registerProgress  = findViewById(R.id.registerProgress);
    }

    private void validateAndRegister() {
        // Clear any previous errors
        clearErrors();

        String username   = editTextUsername.getText().toString().trim();
        String email      = editTextEmail.getText().toString().trim();
        String password   = editTextPassword.getText().toString().trim();
        String rePassword = editTextRePassword.getText().toString().trim();

        if (!isInputValid(username, email, password, rePassword)) return;

        showProgressAndRegister(username, email, password);
    }

    private void clearErrors() {
        usernameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        rePasswordLayout.setError(null);
    }

    private void showProgressAndRegister(String username, String email, String password) {
        registerProgress.setVisibility(View.VISIBLE);
        buttonSignUp.setEnabled(false);

        firebaseHelper.registerUser(username, email, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(String retrievedUsername) {
                registerProgress.setVisibility(View.GONE);
                showToast("Account created successfully!");
                navigateToMainActivity(retrievedUsername);
            }

            @Override
            public void onFailure(String error) {
                registerProgress.setVisibility(View.GONE);
                buttonSignUp.setEnabled(true);
                showToast("Registration failed: " + error);
            }
        });
    }

    /**
     * Validates all fields and shows inline errors on the TextInputLayout
     * instead of only showing a Toast — better UX.
     */
    private boolean isInputValid(String username, String email, String password, String rePassword) {
        if (username.isEmpty()) {
            usernameLayout.setError("Username is required");
            return false;
        }

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Please enter a valid email address");
            return false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            return false;
        }

        if (rePassword.isEmpty()) {
            rePasswordLayout.setError("Please confirm your password");
            return false;
        }

        if (!password.equals(rePassword)) {
            rePasswordLayout.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void navigateToMainActivity(String username) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        // Clear the entire back stack so the user can't navigate back to Register after login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}