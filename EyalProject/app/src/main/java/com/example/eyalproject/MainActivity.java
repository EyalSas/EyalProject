package com.example.eyalproject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.eyalproject.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String username;
    private String email; // Add email field
    private NavController navController;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        dbHelper = new DBHelper(this);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_store,
                R.id.navigation_notifications, R.id.navigation_service)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Retrieve the username passed from RegisterActivity
        username = getIntent().getStringExtra("USERNAME");
        if (username != null) {
            TextView usernameTextView = findViewById(R.id.usernameTextView);
            if (usernameTextView != null) {
                usernameTextView.setText("Welcome, " + username + "!");
                usernameTextView.setTextColor(Color.WHITE);
            }

            // Get user email from database
            email = dbHelper.getUserEmail(username);

            // Set up profile icon click listener
            setupProfileIcon();

            // Set up global action to pass username to ServiceFragment
            setupNavigationWithUsername();
        }
    }

    private void setupProfileIcon() {
        ImageView profileIcon = findViewById(R.id.user_profile_image);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProfileDialog();
                }
            });
        }
    }


    private void logoutUser() {
        // Clear any session data if you have
        // For example, if you're using SharedPreferences:
        // SharedPreferences preferences = getSharedPreferences("MyApp", MODE_PRIVATE);
        // preferences.edit().clear().apply();

        // Navigate back to WelcomeActivity
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close MainActivity
    }

    private void setupNavigationWithUsername() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.navigation_service) {
                // Create a new instance of ServiceFragment with arguments
                Bundle bundle = new Bundle();
                bundle.putString("USERNAME", username);
            }
        });
    }

    // Method to get the current username (can be used by other fragments)
    public String getUsername() {
        return username;
    }

    // Handle back button press for navigation
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
    private void showProfileDialog() {
        // Inflate the dropdown layout
        View dropdownView = getLayoutInflater().inflate(R.layout.profile_dropdown, null);

        // Initialize views
        TextView tvUsername = dropdownView.findViewById(R.id.tvDropdownUsername);
        TextView tvEmail = dropdownView.findViewById(R.id.tvDropdownEmail);
        Button btnLogout = dropdownView.findViewById(R.id.btnDropdownLogout);

        // Set user data
        tvUsername.setText(username);
        tvEmail.setText(email != null ? email : "Not available");

        // Create PopupWindow
        PopupWindow popupWindow = new PopupWindow(
                dropdownView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set background and animation
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(16f);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);

        // Get profile icon location
        ImageView profileIcon = findViewById(R.id.user_profile_image);

        // Calculate position - show below the profile icon
        popupWindow.showAsDropDown(profileIcon, -180, 10); // Adjust x-offset and y-offset as needed

        // Set logout button click listener
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                logoutUser();
            }
        });

        // Dismiss when touching outside
        dropdownView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

}