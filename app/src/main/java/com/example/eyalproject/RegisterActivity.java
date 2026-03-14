package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextUsername, editTextEmail, editTextPassword, editTextRePassword;
    private Button buttonSignUp, buttonLogin;
    private ProgressBar registerProgress;
    private DBHelper databaseHelper; // Assuming DBHelper is your database handler class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        // NOTE: This assumes you have a DBHelper class defined elsewhere
        databaseHelper = new DBHelper(this);

        buttonSignUp.setOnClickListener(v -> showProgressAndRegister());
        buttonLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initializeViews() {
        editTextUsername = findViewById(R.id.username);
        editTextEmail = findViewById(R.id.gmailedit);
        editTextPassword = findViewById(R.id.password);
        editTextRePassword = findViewById(R.id.repassword);
        buttonSignUp = findViewById(R.id.btnsignup);
        buttonLogin = findViewById(R.id.buttonLogin);
        registerProgress = findViewById(R.id.registerProgress);
    }

    private void showProgressAndRegister() {
        registerProgress.setVisibility(View.VISIBLE);
        buttonSignUp.setEnabled(false);

        // Simulate a network delay (or just ensure the progress bar shows briefly)
        new Handler().postDelayed(() -> {
            registerProgress.setVisibility(View.GONE);
            buttonSignUp.setEnabled(true);
            validateAndRegister();
        }, 1000);
    }

    private void validateAndRegister() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String rePassword = editTextRePassword.getText().toString().trim();

        if (!isInputValid(username, email, password, rePassword)) return;
        if (!isUserUnique(username, email)) return;

        registerNewUser(username, password, email);
    }

    private boolean isInputValid(String username, String email, String password, String rePassword) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || rePassword.isEmpty()) {
            showToast("Please complete all fields");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Please provide a valid email address");
            return false;
        }

        if (password.length() < 8) {
            showToast("Password must contain at least 8 characters");
            return false;
        }

        if (!password.equals(rePassword)) {
            showToast("Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean isUserUnique(String username, String email) {
        // NOTE: These methods rely on the DBHelper implementation
        if (databaseHelper.checkusername(username)) {
            showToast("Username is already taken");
            return false;
        }

        if (databaseHelper.checkemail(email)) {
            showToast("Email address is already registered");
            return false;
        }

        return true;
    }

    private void registerNewUser(String username, String password, String email) {
        // NOTE: This method relies on the DBHelper implementation
        boolean registrationSuccessful = databaseHelper.insertUser(username, password, email);

        if (registrationSuccessful) {
            showToast("Account created successfully");
            navigateToMainActivity(username);
        } else {
            showToast("Registration failed. Please try again");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToMainActivity(String username) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close(); // Close the database connection
        }
        super.onDestroy();
    }
}