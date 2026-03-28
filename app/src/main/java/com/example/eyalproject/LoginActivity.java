package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername, editTextPassword;
    private TextInputLayout usernameLayout, passwordLayout;
    private Button buttonLogin, buttonRegister;
    private ProgressBar loginProgress;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        firebaseHelper = new FirebaseHelper();

        buttonLogin.setOnClickListener(v -> validateAndLogin());

        // ✅ FIX: Wire up the Register button — was never connected before
        // FLAG_ACTIVITY_CLEAR_TOP keeps the back stack clean without killing RegisterActivity
        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void initializeViews() {
        usernameLayout  = findViewById(R.id.usernameLayout);
        passwordLayout  = findViewById(R.id.passwordLayout);
        editTextUsername = findViewById(R.id.username1);
        editTextPassword = findViewById(R.id.password1);
        buttonLogin     = findViewById(R.id.btnsignin1);
        buttonRegister  = findViewById(R.id.buttonRegister);
        loginProgress   = findViewById(R.id.loginProgress);
    }

    private void validateAndLogin() {
        // Clear previous errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty()) {
            usernameLayout.setError("Please enter your username");
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError("Please enter your password");
            return;
        }

        authenticateUser(username, password);
    }

    private void authenticateUser(String username, String password) {
        loginProgress.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        firebaseHelper.loginUserByUsername(username, password, new FirebaseHelper.AuthCallback() {
            @Override
            public void onSuccess(String retrievedUsername) {
                loginProgress.setVisibility(View.GONE);
                navigateToMainActivity(retrievedUsername);
            }

            @Override
            public void onFailure(String error) {
                loginProgress.setVisibility(View.GONE);
                buttonLogin.setEnabled(true);
                // Show error inline on the password field for wrong password
                passwordLayout.setError("Login failed: " + error);
            }
        });
    }

    private void navigateToMainActivity(String username) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}