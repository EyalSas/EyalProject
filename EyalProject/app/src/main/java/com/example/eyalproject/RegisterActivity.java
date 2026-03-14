package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword, editTextRePassword, editTextEmail;
    private Button buttonSignUp;
    private DBHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeViews();
        setupDatabase();
        setupSignUpButton();
    }

    private void initializeViews() {
        editTextUsername = findViewById(R.id.username);
        editTextPassword = findViewById(R.id.password);
        editTextRePassword = findViewById(R.id.repassword);
        editTextEmail = findViewById(R.id.gmailedit);
        buttonSignUp = findViewById(R.id.btnsignup);
    }

    private void setupDatabase() {
        databaseHelper = new DBHelper(this);
    }

    private void setupSignUpButton() {
        buttonSignUp.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String rePassword = editTextRePassword.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();

        if (!isInputValid(username, password, rePassword, email)) {
            return;
        }

        if (!isUserUnique(username, email)) {
            return;
        }

        registerNewUser(username, password, email);
    }

    private boolean isInputValid(String username, String password, String rePassword, String email) {
        if (username.isEmpty() || password.isEmpty() || rePassword.isEmpty() || email.isEmpty()) {
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
            showToast("Password confirmation does not match");
            return false;
        }

        return true;
    }

    private boolean isUserUnique(String username, String email) {
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
        finish(); // Prevent going back to registration
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}