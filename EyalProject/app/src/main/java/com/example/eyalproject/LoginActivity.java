package com.example.eyalproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonLogin;
    private DBHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        editTextUsername = findViewById(R.id.username1);
        editTextPassword = findViewById(R.id.password1);
        buttonLogin = findViewById(R.id.btnsignin1);
        databaseHelper = new DBHelper(this);
        setupLoginButton();
    }
    private void setupLoginButton() {
        buttonLogin.setOnClickListener(v -> validateAndLogin());
    }

    private void validateAndLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (!isInputValid(username, password)) {
            return;
        }

        authenticateUser(username, password);
    }

    private boolean isInputValid(String username, String password) {
        if (username.isEmpty()) {
            showToast("Please enter your username");
            return false;
        }

        if (password.isEmpty()) {
            showToast("Please enter your password");
            return false;
        }

        return true;
    }

    private void authenticateUser(String username, String password) {
        boolean isValidCredentials = databaseHelper.checkusernamepassword(username, password);

        if (isValidCredentials) {
            showToast("Login successful");
            navigateToMainActivity(username);
        } else {
            showToast("Invalid username or password");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void navigateToMainActivity(String username) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
        finish(); // Prevent going back to login
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }

}